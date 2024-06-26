/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.schema.TypeGeneratorUtils;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.websocketscore.generators.schema.TypeGeneratorUtils.getTypeGenerator;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPTIONAL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;

/**
 * Generate TypeDefinitionNode and TypeDescriptorNode for anyOf and oneOf schemas.
 *
 */
public class UnionTypeGenerator extends TypeGenerator {

    public UnionTypeGenerator(AsyncApi25SchemaImpl schema, String typeName) {
        super(schema, typeName);
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiExceptionWs {
        AsyncApi25SchemaImpl composedSchema = schema;
        List<AsyncApiSchema> schemas;
        if (composedSchema.getOneOf() != null) {
            schemas = composedSchema.getOneOf();
        } else {
            schemas = composedSchema.getAnyOf();
        }
        TypeDescriptorNode unionTypeDesc = getUnionType(schemas, typeName);
        return TypeGeneratorUtils.getNullableType(schema, unionTypeDesc);
    }

    /**
     * Creates the UnionType string to generate bal type for a given oneOf or anyOf type schema.
     *
     * @param schemas  List of schemas included in the anyOf or oneOf schema
     * @param typeName This is parameter or variable name that used to populate error message meaningful
     * @return Union type
     * @throws BallerinaAsyncApiExceptionWs when unsupported combination of schemas found
     */
    private TypeDescriptorNode getUnionType(List<AsyncApiSchema> schemas, String typeName)
            throws BallerinaAsyncApiExceptionWs {

        List<TypeDescriptorNode> typeDescriptorNodes = new ArrayList<>();
        for (AsyncApiSchema schema : schemas) {
            TypeGenerator typeGenerator = getTypeGenerator((AsyncApi25SchemaImpl) schema, typeName, null);
            TypeDescriptorNode typeDescNode = typeGenerator.generateTypeDescriptorNode();
            if (typeDescNode instanceof OptionalTypeDescriptorNode) {
                Node internalTypeDesc = ((OptionalTypeDescriptorNode) typeDescNode).typeDescriptor();
                typeDescNode = (TypeDescriptorNode) internalTypeDesc;
            }
            typeDescriptorNodes.add(typeDescNode);
            if (typeGenerator instanceof ArrayTypeGenerator &&
                    !typeGenerator.getTypeDefinitionNodeList().isEmpty()) {
                typeDefinitionNodeList.addAll(typeGenerator.getTypeDefinitionNodeList());
            }
        }
        return createUnionTypeNode(typeDescriptorNodes);
    }

    private TypeDescriptorNode createUnionTypeNode(List<TypeDescriptorNode> typeDescNodes) {
        if (typeDescNodes.isEmpty()) {
            return null;
        } else if (typeDescNodes.size() == 1) {
            return typeDescNodes.get(0);
        }

        // if any of the member subtypes is an optional type descriptor, simplify the resultant union type by
        // extracting out the optional operators from all the member subtypes and, adding only to the last one.
        //
        // i.e: T1?|T2?|...|Tn? <=> T1|T2|...|Tn?
        if (typeDescNodes.stream().anyMatch(node -> node.kind() == OPTIONAL_TYPE_DESC)) {
            typeDescNodes = typeDescNodes.stream().map(node -> {
                if (node instanceof OptionalTypeDescriptorNode) {
                    return (TypeDescriptorNode) ((OptionalTypeDescriptorNode) node).typeDescriptor();
                } else {
                    return node;
                }
            }).collect(Collectors.toList());

            OptionalTypeDescriptorNode optionalTypeDesc = createOptionalTypeDescriptorNode(
                    typeDescNodes.get(typeDescNodes.size() - 1), createToken(QUESTION_MARK_TOKEN));
            typeDescNodes.set(typeDescNodes.size() - 1, optionalTypeDesc);
        }

        UnionTypeDescriptorNode unionTypeDescNode = null;
        TypeDescriptorNode leftTypeDesc = typeDescNodes.get(0);
        for (int i = 1; i < typeDescNodes.size(); i++) {
            TypeDescriptorNode rightTypeDesc = typeDescNodes.get(i);
            unionTypeDescNode = createUnionTypeDescriptorNode(leftTypeDesc, createToken(PIPE_TOKEN), rightTypeDesc);
            leftTypeDesc = unionTypeDescNode;
        }
        return unionTypeDescNode;
    }
}
