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
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;

/**
 * Generates a Ballerina union type descriptor from an AsyncAPI schema with {@code oneOf} or
 * multiple types.
 */
public class UnionTypeGenerator implements TypeGenerator {

    private final AsyncApiSchema schema;
    private final String typeName;

    /**
     * Creates a new union type generator.
     *
     * @param schema   the schema with {@code oneOf} or {@code anyOf} sub-schemas
     * @param typeName the type name hint for nested type generation
     */
    public UnionTypeGenerator(AsyncApiSchema schema, String typeName) {
        this.schema = schema;
        this.typeName = typeName;
    }

    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException {
        List<AsyncApiSchema> schemas = schema.oneOf() != null ? schema.oneOf() : schema.anyOf();
        List<TypeDescriptorNode> typeDescNodes = new ArrayList<>();
        for (AsyncApiSchema subSchema : schemas) {
            typeDescNodes.add(TypeGenerator.of(subSchema, typeName).generateTypeDescriptorNode());
        }
        if (typeDescNodes.isEmpty()) {
            return new AnyDataTypeGenerator().generateTypeDescriptorNode();
        } else if (typeDescNodes.size() == 1) {
            return typeDescNodes.get(0);
        }
        UnionTypeDescriptorNode unionNode = null;
        TypeDescriptorNode leftDesc = typeDescNodes.get(0);
        for (int i = 1; i < typeDescNodes.size(); i++) {
            unionNode = createUnionTypeDescriptorNode(leftDesc, createToken(PIPE_TOKEN), typeDescNodes.get(i));
            leftDesc = unionNode;
        }
        return unionNode;
    }
}
