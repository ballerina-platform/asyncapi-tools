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

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.Map;

/**
 * Extracts the dispatcher key from the {@code x-dispatcherKey} document-level extension
 * in an AsyncAPI specification.
 */
public class DispatcherKeyExtractor {

    /** Extension key for the dispatcher field name. */
    public static final String X_DISPATCHER_KEY = "x-dispatcherKey";

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new extractor backed by the given spec.
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     */
    public DispatcherKeyExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts and validates the dispatcher key from the spec extensions.
     *
     * @return the dispatcher key string
     * @throws GeneratorException if the extension is absent or blank
     */
    public String extract() throws GeneratorException {
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions == null || !extensions.containsKey(X_DISPATCHER_KEY)) {
            throw new GeneratorException("x-dispatcherKey extension is required in the AsyncAPI spec");
        }
        String dispatcherKey = extensions.get(X_DISPATCHER_KEY).asText();
        if (dispatcherKey == null || dispatcherKey.isBlank()) {
            throw new GeneratorException("x-dispatcherKey extension must not be empty");
        }
        return dispatcherKey;
    }
}
