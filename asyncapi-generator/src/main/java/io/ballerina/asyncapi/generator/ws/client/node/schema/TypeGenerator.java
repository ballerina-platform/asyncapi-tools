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

import java.util.ArrayList;
import java.util.List;

/**
 * Common interface for all Ballerina type descriptor generators.
 */
public interface TypeGenerator {

    /**
     * Generates the Ballerina type descriptor node for an AsyncAPI schema.
     *
     * @return the generated type descriptor node
     * @throws GeneratorException if the schema cannot be converted to a Ballerina type
     */
    TypeDescriptorNode generateTypeDescriptorNode() throws GeneratorException;

    /**
     * Selects and returns the appropriate {@link TypeGenerator} for the given schema.
     *
     * @param schema   the AsyncAPI schema to resolve
     * @param typeName the type name hint, used for nested type generation
     * @return a concrete {@link TypeGenerator} instance
     */
    static TypeGenerator of(AsyncApiSchema schema, String typeName) {
        if (schema.name() != null) {
            return new ReferencedTypeGenerator(schema);
        } else if (schema.allOf() != null && !schema.allOf().isEmpty()) {
            return new AllOfRecordTypeGenerator(schema, typeName);
        } else if ((schema.oneOf() != null || schema.anyOf() != null) && !onlyNarrowsFields(schema)) {
            return new UnionTypeGenerator(schema, typeName);
        } else if ("array".equals(schema.type())) {
            return new ArrayTypeGenerator(schema, typeName);
        } else if ("object".equals(schema.type()) || schema.properties() != null) {
            if (schema.properties() == null && schema.additionalProperties() != null) {
                return new MapTypeGenerator(schema);
            }
            return new RecordTypeGenerator(schema, typeName);
        } else if (schema.additionalProperties() != null) {
            return new MapTypeGenerator(schema);
        } else if (schema.type() != null) {
            return new PrimitiveTypeGenerator(schema);
        } else {
            return new AnyDataTypeGenerator();
        }
    }

    /**
     * Whether every {@code oneOf} and {@code anyOf} member of an object only says which of its fields must be
     * there, as {@code anyOf: [{required: [text]}, {required: [attachmentIds]}]} does, so the object keeps its type.
     */
    private static boolean onlyNarrowsFields(AsyncApiSchema schema) {
        if (schema.properties() == null || schema.type() != null && !"object".equals(schema.type())) {
            return false;
        }
        List<AsyncApiSchema> members = new ArrayList<>();
        if (schema.oneOf() != null) {
            members.addAll(schema.oneOf());
        }
        if (schema.anyOf() != null) {
            members.addAll(schema.anyOf());
        }
        return members.stream().allMatch(member -> member.name() == null
                && (member.type() == null || "object".equals(member.type()))
                && member.items() == null && member.additionalProperties() == null && member.allOf() == null
                && member.oneOf() == null && member.anyOf() == null && member.enumValue() == null
                && member.constValue() == null
                // A field it restates, such as to add minItems, is one the object already declares
                && (member.properties() == null || schema.properties().keySet().containsAll(
                        member.properties().keySet())));
    }
}
