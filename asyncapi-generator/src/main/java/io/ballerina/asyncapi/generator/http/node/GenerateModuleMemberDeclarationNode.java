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
package io.ballerina.asyncapi.generator.http.node;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumMemberNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ENUM_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates a {@link ModuleMemberDeclarationNode} (enum, record, or type alias) for
 * {@code data_types.bal} from a single {@link AsyncApiSchema} entry.
 */
public class GenerateModuleMemberDeclarationNode implements Generator {

    private static final Logger LOG = LogManager.getLogger(GenerateModuleMemberDeclarationNode.class);
    private static final String X_NULLABLE = "x-nullable";
    private static final String SCHEMA_TYPE_INTEGER = "integer";
    private static final String SCHEMA_TYPE_STRING = "string";
    private static final String SCHEMA_TYPE_BOOLEAN = "boolean";
    private static final String SCHEMA_TYPE_NUMBER = "number";
    private static final String SCHEMA_TYPE_DECIMAL = "decimal";
    private static final String SCHEMA_TYPE_FLOAT = "float";
    private static final String SCHEMA_TYPE_DOUBLE = "double";
    private static final String SCHEMA_TYPE_ARRAY = "array";
    private static final String SCHEMA_TYPE_OBJECT = "object";

    private final Map.Entry<String, AsyncApiSchema> entry;
    private final Map<String, AsyncApiSchema> allSchemas;

    /**
     * Creates a generator for a single schema entry.
     *
     * @param entry      the schema name and its definition
     * @param allSchemas the full schema map, used to resolve {@code allOf} sub-schema stubs
     */
    public GenerateModuleMemberDeclarationNode(Map.Entry<String, AsyncApiSchema> entry,
                                               Map<String, AsyncApiSchema> allSchemas) {
        this.entry = entry;
        this.allSchemas = allSchemas;
    }

    @Override
    public ModuleMemberDeclarationNode generate() throws GeneratorException {
        String schemaName = entry.getKey();
        AsyncApiSchema schema = entry.getValue();
        IdentifierToken typeName = AbstractNodeFactory.createIdentifierToken(
                CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(schemaName.trim()), true));

        if (schema.enumValue() != null && !schema.enumValue().isEmpty()) {
            return generateEnum(typeName, schema);
        } else if (schema.properties() != null && !schema.properties().isEmpty()) {
            return generateRecord(typeName, schema);
        } else if (schema.allOf() != null && !schema.allOf().isEmpty()) {
            return generateAllOfRecord(typeName, schema);
        } else if (schema.type() != null) {
            return generateTypeAlias(typeName, schema);
        } else {
            Token anydata = AbstractNodeFactory.createIdentifierToken("anydata");
            MetadataNode metadataNode = createMetadataNode(
                    createMarkdownDocumentationNode(createNodeList(new ArrayList<>())), createEmptyNodeList());
            return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                    typeName, createBuiltinSimpleNameReferenceNode(null, anydata), createToken(SEMICOLON_TOKEN));
        }
    }

    private ModuleMemberDeclarationNode generateEnum(IdentifierToken typeName, AsyncApiSchema schema) {
        List<Node> enums = new ArrayList<>();
        List<JsonNode> enumValues = schema.enumValue();
        for (int i = 0; i < enumValues.size(); i++) {
            if (i > 0) {
                enums.add(createToken(SyntaxKind.COMMA_TOKEN));
            }
            enums.add(createEnumMemberNode(
                    null,                                                   // no metadata/annotations
                    createIdentifierToken(enumValues.get(i).asText()),
                    null,                                                   // no explicit value assignment (= ...)
                    null));                                                 // no constant expression value
        }
        return createEnumDeclarationNode(null, createToken(PUBLIC_KEYWORD), createToken(ENUM_KEYWORD),
                typeName, createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(enums),
                createToken(CLOSE_BRACE_TOKEN), null);
    }

    private TypeDefinitionNode generateRecord(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        List<String> required = schema.required() != null ? schema.required() : List.of();
        NodeList<Node> fieldNodes = createNodeList(buildRecordFields(schema.properties(), required));
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(
                createToken(SyntaxKind.RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                fieldNodes, null, createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(new ArrayList<>())), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode generateAllOfRecord(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        Map<String, AsyncApiSchema> merged = new LinkedHashMap<>();
        List<String> mergedRequired = new ArrayList<>();
        for (AsyncApiSchema sub : schema.allOf()) {
            AsyncApiSchema resolved = sub;
            if (sub.name() != null && sub.type() == null && sub.properties() == null && sub.allOf() == null) {
                resolved = allSchemas.get(sub.name());
                if (resolved == null) {
                    LOG.warn("Could not resolve allOf sub-schema reference: {} in schema: {}",
                            sub.name(), entry.getKey());
                    continue;
                }
            }
            if (resolved.properties() != null) {
                merged.putAll(resolved.properties());
            }
            if (resolved.required() != null) {
                mergedRequired.addAll(resolved.required());
            }
        }
        NodeList<Node> fieldNodes = createNodeList(buildRecordFields(merged, mergedRequired));
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(
                createToken(SyntaxKind.RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                fieldNodes, null, createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(new ArrayList<>())), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode generateTypeAlias(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        TypeDescriptorNode fieldTypeName = getTypeDescriptorNode(schema);
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(new ArrayList<>())), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, createOptionalTypeDescriptorNode(fieldTypeName, createToken(QUESTION_MARK_TOKEN)),
                createToken(SEMICOLON_TOKEN));
    }

    private List<Node> buildRecordFields(Map<String, AsyncApiSchema> properties, List<String> required)
            throws GeneratorException {
        List<Node> fields = new ArrayList<>();
        for (Map.Entry<String, AsyncApiSchema> field : properties.entrySet()) {
            String fieldName = CodegenUtils.escapeIdentifier(field.getKey().trim());
            IdentifierToken fieldNameToken = AbstractNodeFactory.createIdentifierToken(fieldName);
            TypeDescriptorNode fieldType = getTypeDescriptorNode(field.getValue());
            boolean isOptional = !required.contains(field.getKey().trim());
            Token questionMark = isOptional ? createToken(QUESTION_MARK_TOKEN) : null;
            Token semicolon = createToken(SEMICOLON_TOKEN);
            List<Node> fieldDoc = new ArrayList<>();
            String docText = field.getValue().title() != null ? field.getValue().title()
                    : field.getValue().description();
            if (docText != null) {
                for (String line : docText.split("\n")) {
                    fieldDoc.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                            createToken(HASH_TOKEN), createNodeList(createIdentifierToken(line))));
                }
            }
            MetadataNode fieldMetadata = createMetadataNode(
                    createMarkdownDocumentationNode(createNodeList(fieldDoc)), createEmptyNodeList());
            RecordFieldNode recordField = createRecordFieldNode(fieldMetadata,
                    null, // no readonly keyword
                    fieldType, fieldNameToken, questionMark, semicolon);
            fields.add(recordField);
        }
        return fields;
    }

    private TypeDescriptorNode getTypeDescriptorNode(AsyncApiSchema schema) throws GeneratorException {
        TypeDescriptorNode typeDesc;
        if (schema.properties() != null && !schema.properties().isEmpty()) {
            typeDesc = buildInlineRecord(schema);
        } else if (schema.type() != null) {
            typeDesc = getTypeDescriptorForPrimitive(schema);
        } else if (schema.name() != null) {
            String typeName = CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(schema.name()), true);
            typeDesc = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(typeName));
        } else {
            typeDesc = createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken("anydata"));
        }
        return applyNullable(schema, typeDesc);
    }

    private TypeDescriptorNode getTypeDescriptorForPrimitive(AsyncApiSchema schema) throws GeneratorException {
        return switch (schema.type()) {
            case SCHEMA_TYPE_INTEGER ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("int"));
            case SCHEMA_TYPE_STRING ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string"));
            case SCHEMA_TYPE_BOOLEAN ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("boolean"));
            case SCHEMA_TYPE_NUMBER, SCHEMA_TYPE_DECIMAL -> {
                if (schema.format() != null
                        && (schema.format().equals("float") || schema.format().equals("double"))) {
                    yield createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("float"));
                }
                yield createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("decimal"));
            }
            case SCHEMA_TYPE_FLOAT, SCHEMA_TYPE_DOUBLE ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("float"));
            case SCHEMA_TYPE_ARRAY -> getArrayTypeDescriptor(schema);
            case SCHEMA_TYPE_OBJECT -> {
                if (schema.properties() != null && !schema.properties().isEmpty()) {
                    yield buildInlineRecord(schema);
                }
                yield createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                        createEmptyNodeList(), null, createToken(CLOSE_BRACE_TOKEN));
            }
            default -> createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("anydata"));
        };
    }

    private TypeDescriptorNode getArrayTypeDescriptor(AsyncApiSchema schema) throws GeneratorException {
        if (schema.items() == null) {
            throw new GeneratorException("Array schema is missing the 'items' attribute");
        }
        ArrayDimensionNode dimension = NodeFactory.createArrayDimensionNode(
                createToken(OPEN_BRACKET_TOKEN), null, createToken(CLOSE_BRACKET_TOKEN));
        TypeDescriptorNode memberType;
        if (schema.items() instanceof AsyncApiSchema itemSchema) {
            memberType = getTypeDescriptorNode(itemSchema);
        } else {
            memberType = createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken("anydata"));
        }
        return createArrayTypeDescriptorNode(memberType, createNodeList(dimension));
    }

    private RecordTypeDescriptorNode buildInlineRecord(AsyncApiSchema schema) throws GeneratorException {
        List<String> required = schema.required() != null ? schema.required() : List.of();
        NodeList<Node> fieldNodes = createNodeList(buildRecordFields(schema.properties(), required));
        return createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                fieldNodes, null, createToken(CLOSE_BRACE_TOKEN));
    }

    private TypeDescriptorNode applyNullable(AsyncApiSchema schema, TypeDescriptorNode typeDesc) {
        Map<String, JsonNode> extensions = schema.extensions();
        if (extensions != null && extensions.containsKey(X_NULLABLE)
                && "true".equals(extensions.get(X_NULLABLE).asText())) {
            return createOptionalTypeDescriptorNode(typeDesc, createToken(QUESTION_MARK_TOKEN));
        }
        return typeDesc;
    }
}
