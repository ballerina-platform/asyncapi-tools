/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
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
    private static final String X_BALLERINA_AUTH_SIGNATURE = "signature";
    
    // New DSL Keys
    private static final String SIGNATURE_ALGORITHM = "algorithm";
    private static final String SIGNATURE_ENCODING = "encoding";
    private static final String SIGNATURE_HEADER_FORMAT = "headerFormat";
    private static final String SIGNATURE_INPUT = "input";

    private final AsyncApiSpec asyncApiSpec;

    public WebhookAuthExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the webhook auth configuration from the spec.
     *
     * @return an {@link Optional} containing the {@link WebhookAuthConfig} if the
     * {@code x-ballerina-auth} extension is present and has a {@code header} field;
     * {@link Optional#empty()} otherwise
     * @throws GeneratorException if {@code signature} is present but is not an object
     */
    public Optional<WebhookAuthConfig> extract() throws GeneratorException {
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

        String header = headerNode.asText();

        // Extract the new nested signature properties
        JsonNode signatureNode = authNode.get(X_BALLERINA_AUTH_SIGNATURE);
        if (signatureNode != null) {
            if (!signatureNode.isObject()) {
                throw new GeneratorException(
                        "Invalid x-ballerina-auth: 'signature' must be an object when present.");
            }

            String algorithm = extractTextNode(signatureNode, SIGNATURE_ALGORITHM);
            String encoding = extractTextNode(signatureNode, SIGNATURE_ENCODING);
            String headerFormat = extractTextNode(signatureNode, SIGNATURE_HEADER_FORMAT);
            String input = extractTextNode(signatureNode, SIGNATURE_INPUT);

            return Optional.of(new WebhookAuthConfig(header, algorithm, encoding, headerFormat, input));
        }

        // Fallback for simple/legacy configs that only define a header.
        // Per the DSL backwards-compatibility guarantee, default to GitHub's
        // HMAC-SHA256 configuration so existing pipelines keep working unchanged.
        return Optional.of(new WebhookAuthConfig(header, "sha256", "hex", "{signature}", "$body"));
    }

    private String extractTextNode(JsonNode parentNode, String fieldName) {
        JsonNode childNode = parentNode.get(fieldName);
        return (childNode != null && childNode.isTextual()) ? childNode.asText() : null;
    }
}
