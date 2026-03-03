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
package io.ballerina.asyncapi.core.implementation.v2.component;

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Components;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Components;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps schema definitions from AsyncAPI 2.x components.
 */
final class ComponentSchemaMapperV2 {

    private ComponentSchemaMapperV2() {

    }

    /**
     * Maps schemas from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of schema names to schema objects, or null if no schemas
     */
    static Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends AsyncApiSchema> rawSchemas = switch (components) {
            case AsyncApi26Components typed -> typed.getSchemas();
            case AsyncApi25Components typed -> typed.getSchemas();
            case AsyncApi24Components typed -> typed.getSchemas();
            case AsyncApi23Components typed -> typed.getSchemas();
            case AsyncApi22Components typed -> typed.getSchemas();
            case AsyncApi21Components typed -> typed.getSchemas();
            case AsyncApi20Components typed -> typed.getSchemas();
            default -> null;
        };

        if (rawSchemas == null || rawSchemas.isEmpty()) {
            return null;
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result = new LinkedHashMap<>();
        rawSchemas.forEach((key, schema) -> {
            io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped =
                    SchemaMapper.map(schema, rawSchemas);
            if (mapped != null) {
                result.put(key, mapped);
            }
        });

        return result.isEmpty() ? null : result;
    }
}
