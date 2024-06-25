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
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.JSON_KEYWORD;

/**
 * Generate TypeDefinitionNode and TypeDescriptorNode for Json type schema.
 *
 */
public class JsonTypeGenerator extends TypeGenerator {
    public JsonTypeGenerator(AsyncApi25SchemaImpl schema, String typeName) {
        super(schema, typeName);
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiExceptionWs {

        //TODO: Uncomment below code with improvements
//        if ((schema.getAdditionalProperties() instanceof BooleanUnionValueImpl &&
//                schema.getAdditionalProperties().asBoolean().equals(true))
//                || ((AsyncApi25SchemaImpl) schema.getAdditionalProperties()).getType() == null) {

//            paramType = "map<" + convertAsyncAPITypeToBallerina("{}") + ">";
//            convertAsyncAPITypeToBallerina("{}")


        return createBuiltinSimpleNameReferenceNode(JSON_KEYWORD, createIdentifierToken("json"));
    }
}
