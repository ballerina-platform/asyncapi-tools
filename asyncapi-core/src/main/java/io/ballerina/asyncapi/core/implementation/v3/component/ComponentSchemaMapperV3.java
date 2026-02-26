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
package io.ballerina.asyncapi.core.implementation.v3.component;

import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.union.MultiFormatSchemaSchemaUnion;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps schema definitions from AsyncAPI 3.0 components.
 *
 * <p>Schemas in V3 can be either MultiFormatSchema (for Avro, Protobuf, etc.)
 * or plain JSON Schema objects. Only JSON Schema entries (those where the union
 * holds an {@link AsyncApiSchema}) are mapped; non-JSON schema formats are skipped.
 */
final class ComponentSchemaMapperV3 {

    private ComponentSchemaMapperV3() {

    }

    /**
     * Maps component schemas from AsyncAPI 3.0 components.
     *
     * @param components the AsyncAPI 3.0 components object
     * @return a map of schema names to mapped schema objects, or null if empty
     */
    static Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> map(AsyncApi30Components components) {
        if (components == null) {
            return null;
        }

        Map<String, MultiFormatSchemaSchemaUnion> rawSchemas = components.getSchemas();
        if (rawSchemas == null || rawSchemas.isEmpty()) {
            return null;
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result = new LinkedHashMap<>();
        rawSchemas.forEach((key, schemaUnion) -> {
            if (schemaUnion != null && schemaUnion.isSchema()
                    && schemaUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = SchemaMapper.map(typedSchema);
                if (mapped != null) {
                    result.put(key, mapped);
                }
            }
        });

        return result.isEmpty() ? null : result;
    }
}
