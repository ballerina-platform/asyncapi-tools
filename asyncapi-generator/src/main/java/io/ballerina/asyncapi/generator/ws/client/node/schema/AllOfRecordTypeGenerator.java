/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.generator.ws.client.node.schema;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * Generates a Ballerina record type descriptor from an AsyncAPI {@code allOf} schema.
 * Type inclusions ({@code *RefType;}) are generated for resolved references; inline properties
 * are inlined as record fields.
 */
public class AllOfRecordTypeGenerator extends RecordTypeGenerator {

    /**
     * Creates a new allOf record type generator.
     *
     * @param schema   the schema with {@code allOf} sub-schemas
     * @param typeName the type name hint for nested type generation
     */
    public AllOfRecordTypeGenerator(AsyncApiSchema schema, String typeName) {
        super(schema, typeName);
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        List<AsyncApiSchema> allOfSchemas = schema.allOf();

        // Single $ref allOf: delegate to ReferencedTypeGenerator
        if (allOfSchemas != null && allOfSchemas.size() == 1 && allOfSchemas.get(0).name() != null) {
            return new ReferencedTypeGenerator(allOfSchemas.get(0)).generateTypeDescriptorNode();
        }

        List<Node> recordFieldList = allOfSchemas != null
                ? generateAllOfRecordFields(allOfSchemas)
                : new ArrayList<>();

        boolean isOpenRecord = !(Boolean.FALSE.equals(schema.additionalProperties()));
        NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFieldList);
        return NodeFactory.createRecordTypeDescriptorNode(
                createToken(RECORD_KEYWORD),
                isOpenRecord ? createToken(OPEN_BRACE_TOKEN) : createToken(OPEN_BRACE_PIPE_TOKEN),
                fieldNodes,
                null,
                isOpenRecord ? createToken(CLOSE_BRACE_TOKEN) : createToken(CLOSE_BRACE_PIPE_TOKEN));
    }

    private List<Node> generateAllOfRecordFields(List<AsyncApiSchema> allOfSchemas) throws GeneratorException {
        List<AsyncApiSchema> parts = spread(allOfSchemas);
        // Every inline part's fields as one record, as a value must match them all
        Map<String, AsyncApiSchema> inline = new LinkedHashMap<>();
        Map<String, AsyncApiSchema> firstDeclared = new LinkedHashMap<>();
        Set<String> required = new HashSet<>();
        Map<String, AsyncApiSchema> included = new LinkedHashMap<>();
        for (AsyncApiSchema part : parts) {
            if (part.name() != null) {
                included.putAll(fieldsOf(part));
                required.addAll(requiredOf(part));
                continue;
            }
            if (part.required() != null) {
                required.addAll(part.required());
            }
            if (part.properties() != null) {
                for (Map.Entry<String, AsyncApiSchema> field : part.properties().entrySet()) {
                    firstDeclared.putIfAbsent(field.getKey(), part);
                    // A typeless redeclaration, such as one that only adds a const, keeps the earlier type
                    if (!inline.containsKey(field.getKey()) || !isTypeless(field.getValue())) {
                        inline.put(field.getKey(), field.getValue());
                    }
                }
            }
        }
        Set<String> kept = new HashSet<>();
        for (Map.Entry<String, AsyncApiSchema> field : inline.entrySet()) {
            if (included.containsKey(field.getKey()) && isTypeless(field.getValue())) {
                kept.add(field.getKey());
            }
        }
        List<Node> recordFieldList = new ArrayList<>();
        for (AsyncApiSchema part : parts) {
            if (part.name() != null) {
                if (clashes(part, inline, kept)) {
                    // Ballerina rejects an override that isn't a subtype, so the included fields are copied in instead
                    recordFieldList.addAll(copiedFields(part, inline, kept));
                    continue;
                }
                recordFieldList.add(typeInclusion(part));
            } else if (part.properties() != null) {
                // Each inline field is written once, where it is first declared
                Map<String, AsyncApiSchema> properties = new LinkedHashMap<>();
                inline.forEach((name, field) -> {
                    if (firstDeclared.get(name) == part && !kept.contains(name)) {
                        properties.put(name, field);
                    }
                });
                List<String> fieldsRequired = properties.keySet().stream().filter(required::contains).toList();
                recordFieldList.addAll(addRecordFields(new ArrayList<>(fieldsRequired), properties));
            }
        }
        return recordFieldList;
    }

    // An unnamed part's allOf is part of this allOf, so its parts come in its place, followed by its own fields
    private static List<AsyncApiSchema> spread(List<AsyncApiSchema> parts) {
        List<AsyncApiSchema> result = new ArrayList<>();
        for (AsyncApiSchema part : parts) {
            if (part.name() == null && part.allOf() != null && !part.allOf().isEmpty()) {
                result.addAll(spread(part.allOf()));
                if (part.properties() != null || part.required() != null) {
                    result.add(part.withAllOf(null));
                }
            } else {
                result.add(part);
            }
        }
        return result;
    }

    /**
     * The fields of an included schema without the ones an inline part redeclares with its own type, and its own
     * included schemas handled the same way.
     */
    private List<Node> copiedFields(AsyncApiSchema base, Map<String, AsyncApiSchema> inline, Set<String> kept)
            throws GeneratorException {
        List<Node> fields = new ArrayList<>();
        if (base.allOf() != null) {
            for (AsyncApiSchema part : base.allOf()) {
                // An included part the inline fields don't clash with stays an inclusion; the rest is copied, filtered
                if (part.name() != null && !clashes(part, inline, kept)) {
                    fields.add(typeInclusion(part));
                } else {
                    fields.addAll(copiedFields(part, inline, kept));
                }
            }
        }
        if (base.properties() != null) {
            Map<String, AsyncApiSchema> properties = new LinkedHashMap<>(base.properties());
            // A kept field stays the base's; any other redeclared field is the inline part's
            properties.keySet().removeIf(name -> inline.containsKey(name) && !kept.contains(name));
            fields.addAll(addRecordFields(base.required(), properties));
        }
        return fields;
    }

    /**
     * Whether an inline part redeclares a field of {@code base} with a type of its own that differs from the base's.
     */
    private static boolean clashes(AsyncApiSchema base, Map<String, AsyncApiSchema> inline, Set<String> kept)
            throws GeneratorException {
        for (Map.Entry<String, AsyncApiSchema> field : fieldsOf(base).entrySet()) {
            AsyncApiSchema override = inline.get(field.getKey());
            if (override != null && !kept.contains(field.getKey())
                    && !typeSource(field.getValue()).equals(typeSource(override))) {
                return true;
            }
        }
        return false;
    }

    // Resolved $ref → type inclusion: *RefTypeName;
    private static TypeReferenceNode typeInclusion(AsyncApiSchema schema) {
        Token typeRef = AbstractNodeFactory.createIdentifierToken(CodegenUtils.getValidName(schema.name(), true));
        return NodeFactory.createTypeReferenceNode(createToken(ASTERISK_TOKEN), typeRef, createToken(SEMICOLON_TOKEN));
    }

    // The fields a schema requires itself and through its own allOf parts
    private static Set<String> requiredOf(AsyncApiSchema schema) {
        Set<String> required = new HashSet<>();
        if (schema.allOf() != null) {
            schema.allOf().forEach(part -> required.addAll(requiredOf(part)));
        }
        if (schema.required() != null) {
            required.addAll(schema.required());
        }
        return required;
    }

    // The fields a schema declares itself and through its own allOf parts
    private static Map<String, AsyncApiSchema> fieldsOf(AsyncApiSchema schema) {
        Map<String, AsyncApiSchema> fields = new LinkedHashMap<>();
        if (schema.allOf() != null) {
            schema.allOf().forEach(part -> fields.putAll(fieldsOf(part)));
        }
        if (schema.properties() != null) {
            fields.putAll(schema.properties());
        }
        return fields;
    }

    // No type, ref or composition: only a const, enum, description and the like
    private static boolean isTypeless(AsyncApiSchema schema) {
        return schema.type() == null && schema.name() == null && schema.properties() == null
                && schema.items() == null && schema.additionalProperties() == null && schema.allOf() == null
                && schema.oneOf() == null && schema.anyOf() == null;
    }

    private static String typeSource(AsyncApiSchema schema) throws GeneratorException {
        return TypeGenerator.of(schema, null).generateTypeDescriptorNode().toSourceCode().trim();
    }
}
