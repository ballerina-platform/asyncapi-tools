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
import io.ballerina.asyncapi.generator.ws.client.generator.DocCommentsGenerator;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordRestDescriptorNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BITWISE_AND_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELLIPSIS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * Generates a Ballerina record type descriptor from an AsyncAPI object schema.
 */
public class RecordTypeGenerator implements TypeGenerator {

    /** The schema to generate the record for. */
    protected final AsyncApiSchema schema;

    /** The type name hint for nested type generation. */
    protected final String typeName;

    /**
     * Creates a new record type generator.
     *
     * @param schema   the object schema with {@code properties}
     * @param typeName the type name hint for nested type generation
     */
    public RecordTypeGenerator(AsyncApiSchema schema, String typeName) {
        this.schema = schema;
        this.typeName = typeName;
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        boolean isOpenRecord = true;
        RecordRestDescriptorNode restDescNode = null;

        Object additionalProps = schema.additionalProperties();
        if (additionalProps instanceof Boolean) {
            if (Boolean.FALSE.equals(additionalProps)) {
                isOpenRecord = false;
            }
        } else if (additionalProps instanceof AsyncApiSchema) {
            isOpenRecord = false;
            restDescNode = buildRestDescriptorNode((AsyncApiSchema) additionalProps);
        }

        List<Node> recordFields = new ArrayList<>();
        if (schema.properties() != null) {
            recordFields.addAll(addRecordFields(schema.required(), schema.properties()));
        }

        NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFields);
        RecordTypeDescriptorNode recordTypeDesc = NodeFactory.createRecordTypeDescriptorNode(
                createToken(RECORD_KEYWORD),
                isOpenRecord ? createToken(OPEN_BRACE_TOKEN) : createToken(OPEN_BRACE_PIPE_TOKEN),
                fieldNodes,
                restDescNode,
                isOpenRecord ? createToken(CLOSE_BRACE_TOKEN) : createToken(CLOSE_BRACE_PIPE_TOKEN));

        if (Boolean.TRUE.equals(schema.readOnly())) {
            return NodeFactory.createIntersectionTypeDescriptorNode(
                    NodeFactory.createSimpleNameReferenceNode(
                            AbstractNodeFactory.createIdentifierToken("readonly")),
                    createToken(BITWISE_AND_TOKEN),
                    recordTypeDesc);
        }
        return recordTypeDesc;
    }

    /**
     * Generates record field nodes for each property in the given map.
     * Optional fields (not in {@code required}) have their type wrapped in {@code T?}.
     *
     * @param required   list of required property names (may be {@code null})
     * @param properties map of property name to schema
     * @return list of {@link RecordFieldNode} nodes
     * @throws GeneratorException if a field type cannot be generated
     */
    protected List<Node> addRecordFields(List<String> required, Map<String, AsyncApiSchema> properties)
            throws GeneratorException {
        List<Node> recordFieldList = new ArrayList<>();
        for (Map.Entry<String, AsyncApiSchema> field : properties.entrySet()) {
            String rawName = field.getKey().trim();
            String escapedName = CodegenUtils.escapeIdentifier(rawName);
            AsyncApiSchema fieldSchema = field.getValue();

            TypeDescriptorNode fieldType = TypeGenerator.of(fieldSchema, escapedName).generateTypeDescriptorNode();
            boolean isRequired = required != null && required.contains(rawName);

            IdentifierToken fieldName = AbstractNodeFactory.createIdentifierToken(escapedName);
            MetadataNode metadata = null;
            if (fieldSchema.description() != null) {
                List<Node> docLines = new ArrayList<>(
                        DocCommentsGenerator.createAPIDescriptionDoc(fieldSchema.description(), false));
                MarkdownDocumentationNode docNode = NodeFactory.createMarkdownDocumentationNode(
                        AbstractNodeFactory.createNodeList(docLines));
                metadata = NodeFactory.createMetadataNode(docNode, AbstractNodeFactory.createEmptyNodeList());
            }
            RecordFieldNode recordField = NodeFactory.createRecordFieldNode(
                    metadata, null, fieldType, fieldName,
                    isRequired ? null : createToken(QUESTION_MARK_TOKEN),
                    createToken(SEMICOLON_TOKEN));
            recordFieldList.add(recordField);
        }
        return recordFieldList;
    }

    private RecordRestDescriptorNode buildRestDescriptorNode(AsyncApiSchema additionalPropSchema)
            throws GeneratorException {
        TypeDescriptorNode restType = TypeGenerator.of(additionalPropSchema, null).generateTypeDescriptorNode();
        return NodeFactory.createRecordRestDescriptorNode(
                restType, createToken(ELLIPSIS_TOKEN), createToken(SEMICOLON_TOKEN));
    }
}
