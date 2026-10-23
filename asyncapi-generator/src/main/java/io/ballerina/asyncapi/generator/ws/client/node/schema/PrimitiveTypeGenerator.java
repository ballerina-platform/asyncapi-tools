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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * Generates a Ballerina primitive type descriptor (int, string, boolean, float, etc.)
 * from an AsyncAPI primitive schema.
 */
public class PrimitiveTypeGenerator implements TypeGenerator {

    private final AsyncApiSchema schema;

    /**
     * Creates a new primitive type generator.
     *
     * @param schema the primitive schema to generate a type descriptor for
     */
    public PrimitiveTypeGenerator(AsyncApiSchema schema) {
        this.schema = schema;
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        String type = schema.type().trim();
        String typeDescriptorName = CodegenUtils.TYPE_MAP.getOrDefault(type, type);
        if ("number".equals(type) && schema.format() != null) {
            typeDescriptorName = CodegenUtils.TYPE_MAP.getOrDefault(schema.format().trim(), schema.format().trim());
        } else if ("string".equals(type) && "binary".equals(schema.format())) {
            typeDescriptorName = "record {byte[] fileContent; string fileName;}";
        }
        return createSimpleNameReferenceNode(createIdentifierToken(typeDescriptorName));
    }
}
