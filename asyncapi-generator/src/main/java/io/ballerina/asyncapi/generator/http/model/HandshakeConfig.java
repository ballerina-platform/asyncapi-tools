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

/**
 * An endpoint verification handshake from the {@code handshake} block of {@code x-ballerina-event-identifier}: a
 * request the provider sends to check the endpoint, which the listener answers by echoing a challenge instead of
 * dispatching it to a service.
 *
 * @param method       {@code GET} (challenge in a query parameter) or {@code POST} (challenge in the JSON body)
 * @param whenPath     for {@code POST}, the dot-notation body path that marks a request as the handshake;
 *                     {@code null} for {@code GET}
 * @param whenEquals   for {@code POST}, the value at {@code whenPath} that marks the handshake; {@code null} for
 *                     {@code GET}
 * @param challengeKey the dot-notation body path ({@code POST}) or query parameter name ({@code GET}) holding the
 *                     challenge to echo
 */
public record HandshakeConfig(
        String method,
        String whenPath,
        String whenEquals,
        String challengeKey
) {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    /**
     * Whether the handshake arrives as a {@code GET} with the challenge in the query.
     *
     * @return {@code true} for {@code GET}, {@code false} for {@code POST}
     */
    public boolean isGet() {
        return METHOD_GET.equals(method);
    }
}
