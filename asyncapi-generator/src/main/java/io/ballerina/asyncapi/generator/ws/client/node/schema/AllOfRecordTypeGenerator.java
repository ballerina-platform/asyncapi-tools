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
import java.util.HashMap;
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
        Map<String, AsyncApiSchema> inline = new LinkedHashMap<>();
        Map<String, AsyncApiSchema> included = new LinkedHashMap<>();
        Set<String> includedRequired = new HashSet<>();
        Set<String> inlineRequired = new HashSet<>();
        for (AsyncApiSchema allOfSchema : allOfSchemas) {
            if (allOfSchema.name() != null) {
                included.putAll(fieldsOf(allOfSchema));
                includedRequired.addAll(requiredOf(allOfSchema));
            } else if (allOfSchema.properties() != null) {
                inline.putAll(allOfSchema.properties());
                if (allOfSchema.required() != null) {
                    inlineRequired.addAll(allOfSchema.required());
                }
            }
        }
        // A typeless redeclaration, such as one that only adds a const, keeps the included field's type
        Set<String> kept = new HashSet<>();
        Map<String, AsyncApiSchema> retyped = new HashMap<>();
        for (Map.Entry<String, AsyncApiSchema> field : inline.entrySet()) {
            String name = field.getKey();
            if (included.containsKey(name) && isTypeless(field.getValue())) {
                if (inlineRequired.contains(name) && !includedRequired.contains(name)) {
                    // It also makes an optional field required, so it's written again with the included type
                    retyped.put(name, included.get(name));
                } else {
                    kept.add(name);
                }
            }
        }
        inline.putAll(retyped);
        return generateAllOfRecordFields(allOfSchemas, inline, kept, includedRequired, retyped);
    }

    private List<Node> generateAllOfRecordFields(List<AsyncApiSchema> allOfSchemas,
                                                 Map<String, AsyncApiSchema> inline, Set<String> kept,
                                                 Set<String> includedRequired, Map<String, AsyncApiSchema> retyped)
            throws GeneratorException {
        List<Node> recordFieldList = new ArrayList<>();
        for (AsyncApiSchema allOfSchema : allOfSchemas) {
            if (allOfSchema.name() != null) {
                if (clashes(allOfSchema, inline, kept)) {
                    // Ballerina rejects an override that isn't a subtype, so the included fields are copied in instead
                    recordFieldList.addAll(copiedFields(allOfSchema, inline, kept));
                    continue;
                }
                recordFieldList.add(typeInclusion(allOfSchema));
            } else if (allOfSchema.properties() != null) {
                // Inline properties → add as record fields
                Map<String, AsyncApiSchema> properties = new LinkedHashMap<>(allOfSchema.properties());
                properties.keySet().removeAll(kept);
                properties.replaceAll((name, field) -> retyped.getOrDefault(name, field));
                // A field an included schema requires stays required when an inline part redeclares it
                Set<String> required = new HashSet<>(allOfSchema.required() != null ? allOfSchema.required()
                        : List.of());
                properties.keySet().stream().filter(includedRequired::contains).forEach(required::add);
                recordFieldList.addAll(addRecordFields(new ArrayList<>(required), properties));
            } else if (allOfSchema.allOf() != null && !allOfSchema.allOf().isEmpty()) {
                // Nested allOf → recurse
                recordFieldList.addAll(generateAllOfRecordFields(allOfSchema.allOf()));
            }
        }
        return recordFieldList;
    }

    /**
     * The fields of an included schema without the ones an inline part redeclares with its own type, and its own
     * included schemas handled the same way.
     */
    private List<Node> copiedFields(AsyncApiSchema base, Map<String, AsyncApiSchema> inline, Set<String> kept)
            throws GeneratorException {
        List<Node> nodes = new ArrayList<>();
        Map<String, AsyncApiSchema> fields = new LinkedHashMap<>();
        Set<String> required = new HashSet<>();
        collectCopied(base, inline, kept, nodes, fields, required);
        // A kept field stays the base's; any other redeclared field is the inline part's
        fields.keySet().removeIf(name -> inline.containsKey(name) && !kept.contains(name));
        required.retainAll(fields.keySet());
        nodes.addAll(addRecordFields(new ArrayList<>(required), fields));
        return nodes;
    }

    // Gathers a base's fields with its own redeclarations already applied, so each copied field is written once
    private static void collectCopied(AsyncApiSchema base, Map<String, AsyncApiSchema> inline, Set<String> kept,
                                      List<Node> inclusions, Map<String, AsyncApiSchema> fields,
                                      Set<String> required) throws GeneratorException {
        if (base.allOf() != null) {
            for (AsyncApiSchema part : base.allOf()) {
                // An included part the inline fields don't clash with stays an inclusion; the rest is copied
                if (part.name() != null && !clashes(part, inline, kept)) {
                    inclusions.add(typeInclusion(part));
                    required.addAll(requiredOf(part));
                } else {
                    collectCopied(part, inline, kept, inclusions, fields, required);
                }
            }
        }
        if (base.properties() != null) {
            base.properties().forEach((name, field) -> fields.merge(name, field, AllOfRecordTypeGenerator::redeclared));
        }
        if (base.required() != null) {
            required.addAll(base.required());
        }
    }

    // A later declaration of a field wins, unless it's typeless and only adds a const or a description
    private static AsyncApiSchema redeclared(AsyncApiSchema earlier, AsyncApiSchema later) {
        return isTypeless(later) ? earlier : later;
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
            schema.allOf().forEach(part -> fieldsOf(part)
                    .forEach((name, field) -> fields.merge(name, field, AllOfRecordTypeGenerator::redeclared)));
        }
        if (schema.properties() != null) {
            schema.properties().forEach((name, field) ->
                    fields.merge(name, field, AllOfRecordTypeGenerator::redeclared));
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
