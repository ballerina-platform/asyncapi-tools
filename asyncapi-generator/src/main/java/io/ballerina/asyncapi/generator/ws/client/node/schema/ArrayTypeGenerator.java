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
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayDimensionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;

/**
 * Generates a Ballerina array type descriptor from an AsyncAPI array schema.
 */
public class ArrayTypeGenerator implements TypeGenerator {

    private final AsyncApiSchema schema;
    private final String typeName;

    /**
     * Creates a new array type generator.
     *
     * @param schema   the array schema whose {@code items} defines the element type
     * @param typeName the type name hint for nested type generation
     */
    public ArrayTypeGenerator(AsyncApiSchema schema, String typeName) {
        this.schema = schema;
        this.typeName = typeName;
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        TypeDescriptorNode memberTypeDesc;
        Object items = schema.items();
        if (items instanceof AsyncApiSchema) {
            memberTypeDesc = TypeGenerator.of((AsyncApiSchema) items, typeName).generateTypeDescriptorNode();
        } else {
            memberTypeDesc = new AnyDataTypeGenerator().generateTypeDescriptorNode();
        }
        NodeList<ArrayDimensionNode> dimensions = createEmptyNodeList();
        dimensions = dimensions.add(
                createArrayDimensionNode(createToken(OPEN_BRACKET_TOKEN), null, createToken(CLOSE_BRACKET_TOKEN)));
        return createArrayTypeDescriptorNode(memberTypeDesc, dimensions);
    }
}
