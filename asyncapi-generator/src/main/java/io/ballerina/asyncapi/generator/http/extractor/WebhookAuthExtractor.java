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
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;

import java.util.Map;
import java.util.Optional;

/**
 * Extracts the webhook authentication configuration from the {@code x-ballerina-auth}
 * extension on an {@link AsyncApiSpec} document.
 */
public final class WebhookAuthExtractor {

    private static final String X_BALLERINA_AUTH = "x-ballerina-auth";
    private static final String X_BALLERINA_AUTH_HEADER = "header";

    private final AsyncApiSpec asyncApiSpec;

    public WebhookAuthExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the webhook auth configuration from the spec.
     *
     * @return an {@link Optional} containing the {@link WebhookAuthConfig} if the
     *         {@code x-ballerina-auth} extension is present and has a {@code header} field;
     *         {@link Optional#empty()} otherwise
     */
    public Optional<WebhookAuthConfig> extract() {
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions == null || !extensions.containsKey(X_BALLERINA_AUTH)) {
            return Optional.empty();
        }

        JsonNode authNode = extensions.get(X_BALLERINA_AUTH);
        if (authNode == null || !authNode.isObject()) {
            return Optional.empty();
        }

        JsonNode headerNode = authNode.get(X_BALLERINA_AUTH_HEADER);
        if (headerNode == null || !headerNode.isTextual()) {
            return Optional.empty();
        }

        return Optional.of(new WebhookAuthConfig(headerNode.asText()));
    }
}
