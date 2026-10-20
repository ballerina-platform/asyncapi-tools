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
package io.ballerina.asyncapi.generator.http.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * An endpoint verification handshake, described in the definition as a {@code receive} operation with a
 * {@code reply}: a request the provider sends to check the endpoint, which the listener answers by echoing a
 * challenge instead of dispatching it to a service. It arrives either as a {@code POST} with the challenge in the
 * body, or as a {@code GET} with the challenge in the query (a WebSub verification of intent).
 *
 * @param when           the fields that mark a request as the handshake, each with the {@code const} value it must
 *                       have: dot-notation body paths for a {@code POST}, query parameter names for a {@code GET}
 * @param challengePath  for a {@code POST}, the dot-notation body path of the challenge, from the request message's
 *                       {@code correlationId}; {@code null} for a {@code GET}
 * @param challengeQuery for a {@code GET}, the query parameter holding the challenge ({@code hub.challenge});
 *                       {@code null} for a {@code POST}
 * @param statusCode     the status code of the reply, from the reply message's HTTP binding, {@code 200} if unset
 */
public record HandshakeConfig(
        Map<String, JsonNode> when,
        String challengePath,
        String challengeQuery,
        int statusCode
) {

    /**
     * Whether the handshake is a {@code GET} with the challenge in the query.
     *
     * @return {@code true} for a WebSub verification of intent, {@code false} for a {@code POST}
     */
    public boolean isQuery() {
        return challengeQuery != null;
    }
}
