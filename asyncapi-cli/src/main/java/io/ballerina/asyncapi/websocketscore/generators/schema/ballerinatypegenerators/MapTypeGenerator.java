/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.websocketscore.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.convertAsyncAPITypeToBallerina;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMapTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;

/** Generate TypeDefinitionNode and TypeDescriptorNode for map type schema.
 *
 */
public class MapTypeGenerator extends TypeGenerator {
    public MapTypeGenerator(AsyncApi25SchemaImpl schema, String typeName) {
        super(schema, typeName);
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiExceptionWs {

        if ((schema.getAdditionalProperties() instanceof BooleanUnionValueImpl &&
                schema.getAdditionalProperties().asBoolean().equals(true))
                || (schema.getAdditionalProperties() instanceof AsyncApi25SchemaImpl &&
                ((AsyncApi25SchemaImpl) schema.getAdditionalProperties()).getType() == null)) {

            return createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
                    createTypeParameterNode(createToken(LT_TOKEN), createBuiltinSimpleNameReferenceNode(null,
                                    createIdentifierToken(convertAsyncAPITypeToBallerina("{}"))),
                            createToken(GT_TOKEN)));
        } else if (schema.getAdditionalProperties() instanceof AsyncApi25SchemaImpl && ((AsyncApi25SchemaImpl)
                schema.getAdditionalProperties()).getType() != null) {
            return createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
                    createTypeParameterNode(createToken(LT_TOKEN), createBuiltinSimpleNameReferenceNode(null,
                            createIdentifierToken(convertAsyncAPITypeToBallerina(((AsyncApi25SchemaImpl) schema.
                            getAdditionalProperties()).getType()))), createToken(GT_TOKEN)));
        }
        return createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
                createTypeParameterNode(createToken(LT_TOKEN), createBuiltinSimpleNameReferenceNode(null,
                                createIdentifierToken(convertAsyncAPITypeToBallerina(((AsyncApi25SchemaImpl) schema.
                                        getAdditionalProperties()).getType()))), createToken(GT_TOKEN)));
    }
}
