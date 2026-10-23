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
import java.util.List;

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
        List<Node> recordFieldList = new ArrayList<>();
        for (AsyncApiSchema allOfSchema : allOfSchemas) {
            if (allOfSchema.name() != null) {
                // Resolved $ref → generate type inclusion: *RefTypeName;
                String refTypeName = CodegenUtils.getValidName(allOfSchema.name(), true);
                Token typeRef = AbstractNodeFactory.createIdentifierToken(refTypeName);
                TypeReferenceNode recordField = NodeFactory.createTypeReferenceNode(
                        createToken(ASTERISK_TOKEN), typeRef, createToken(SEMICOLON_TOKEN));
                recordFieldList.add(recordField);
            } else if (allOfSchema.properties() != null) {
                // Inline properties → add as record fields
                recordFieldList.addAll(addRecordFields(allOfSchema.required(), allOfSchema.properties()));
            } else if (allOfSchema.allOf() != null && !allOfSchema.allOf().isEmpty()) {
                // Nested allOf → recurse
                recordFieldList.addAll(generateAllOfRecordFields(allOfSchema.allOf()));
            }
        }
        return recordFieldList;
    }
}
