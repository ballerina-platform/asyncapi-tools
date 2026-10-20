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
 * challenge instead of dispatching it to a service.
 *
 * @param when          the body fields that mark a request as the handshake, as dot-notation paths to the
 *                      {@code const} value each must have, taken from the request message's payload schema
 * @param challengePath the dot-notation body path of the challenge to echo, from the request message's
 *                      {@code correlationId}
 * @param statusCode    the status code of the reply, from the reply message's HTTP binding, {@code 200} if unset
 */
public record HandshakeConfig(
        Map<String, JsonNode> when,
        String challengePath,
        int statusCode
) {
}
