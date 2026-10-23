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

import java.util.Map;
import java.util.Optional;

/**
 * Extracts the optional dispatcher stream ID from the {@code x-dispatcherStreamId}
 * document-level extension in an AsyncAPI specification.
 */
public class DispatcherStreamIdExtractor {

    /** Extension key for the dispatcher stream ID field name. */
    public static final String X_DISPATCHER_STREAM_ID = "x-dispatcherStreamId";

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new extractor.
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     */
    public DispatcherStreamIdExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the dispatcher stream ID from the spec extensions.
     * Returns {@link Optional#empty()} when the extension is absent or blank.
     *
     * @return an {@link Optional} containing the stream ID, or empty if not present
     */
    public Optional<String> extract() {
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions == null || !extensions.containsKey(X_DISPATCHER_STREAM_ID)) {
            return Optional.empty();
        }
        JsonNode node = extensions.get(X_DISPATCHER_STREAM_ID);
        if (node == null || node.asText().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(node.asText());
    }
}
