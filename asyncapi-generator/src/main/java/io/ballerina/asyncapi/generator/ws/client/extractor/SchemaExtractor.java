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
package io.ballerina.asyncapi.generator.ws.client.extractor;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.Collections;
import java.util.Map;

/**
 * Extracts reusable schema definitions from an {@link AsyncApiSpec}.
 * Reads {@link AsyncApiSpec#getAsyncApiComponents()} and returns {@code components.schemas()}.
 */
public final class SchemaExtractor {

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new extractor.
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     */
    public SchemaExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts all named schemas from the spec's components section.
     *
     * @return a non-null map of schema name to schema object; empty if no schemas are defined
     * @throws GeneratorException if components are malformed or inaccessible
     */
    public Map<String, AsyncApiSchema> extract() throws GeneratorException {
        AsyncApiComponent components = asyncApiSpec.getAsyncApiComponents().orElse(null);
        if (components != null && components.schemas() != null && !components.schemas().isEmpty()) {
            return components.schemas();
        }
        return Collections.emptyMap();
    }
}
