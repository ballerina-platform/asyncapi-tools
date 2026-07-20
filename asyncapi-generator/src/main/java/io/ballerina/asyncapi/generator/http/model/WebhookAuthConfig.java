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
package io.ballerina.asyncapi.generator.http.model;

import java.util.List;

/**
 * Holds the resolved webhook authentication configuration from the {@code x-ballerina-auth}
 * extension on an AsyncAPI document.
 *
 * @param headerName             the HTTP header name used to carry the authentication token
 * @param algorithm              the cryptographic hashing algorithm (e.g., sha256, sha1)
 * @param encoding               the encoding format for the signature (e.g., hex, base64)
 * @param headerFormat           the DSL format string for extracting the signature
 *                               (e.g., ${signature}, v0=${signature})
 * @param input                  the DSL format string defining the payload construction
 *                               (e.g., ${body}, v0:${ts}:${body})
 * @param strategy               the signature strategy: {@code "hmac"} (default/null) for a keyed HMAC,
 *                               or {@code "hash"} for a plain, unkeyed digest (the secret must then be
 *                               folded into {@code input} explicitly via {@code $secret})
 * @param configFields           distinct {@code $config('name')} references found in {@code input}; each
 *                               becomes a configurable field threaded through the generated listener
 * @param freshnessHeader        the header carrying a request timestamp to validate for staleness, or
 *                               {@code null} if no freshness check is configured
 * @param freshnessToleranceMillis the maximum allowed age of a request, in milliseconds, before it's
 *                               rejected as stale; only meaningful when {@code freshnessHeader} is set
 */
public record WebhookAuthConfig(
        String headerName,
        String algorithm,
        String encoding,
        String headerFormat,
        String input,
        String strategy,
        List<String> configFields,
        String freshnessHeader,
        Long freshnessToleranceMillis
) {
}
