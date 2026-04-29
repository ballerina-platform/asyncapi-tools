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
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMapTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;

/**
 * Generates a Ballerina map type descriptor from an AsyncAPI schema with
 * {@code additionalProperties}.
 */
public class MapTypeGenerator implements TypeGenerator {

    private final AsyncApiSchema schema;

    /**
     * Creates a new map type generator.
     *
     * @param schema the schema with {@code additionalProperties} defining the value type
     */
    public MapTypeGenerator(AsyncApiSchema schema) {
        this.schema = schema;
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        String valueType = "json";
        Object additionalProps = schema.additionalProperties();
        if (additionalProps instanceof AsyncApiSchema) {
            AsyncApiSchema additionalPropSchema = (AsyncApiSchema) additionalProps;
            if (additionalPropSchema.type() != null) {
                valueType = CodegenUtils.TYPE_MAP.getOrDefault(
                        additionalPropSchema.type(), additionalPropSchema.type());
            }
        }
        // Boolean true or null → map<json>; typed schema → map<T>
        return createMapTypeDescriptorNode(
                createToken(MAP_KEYWORD),
                createTypeParameterNode(
                        createToken(LT_TOKEN),
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(valueType)),
                        createToken(GT_TOKEN)));
    }
}
