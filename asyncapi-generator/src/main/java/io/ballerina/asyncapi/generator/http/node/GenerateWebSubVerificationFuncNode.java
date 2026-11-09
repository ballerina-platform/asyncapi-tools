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
package io.ballerina.asyncapi.generator.http.node;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates the {@code resource function get .} that answers a W3C WebSub verification of intent: the hub sends
 * {@code GET ?hub.mode=subscribe&hub.topic=...&hub.challenge=...} and the subscriber confirms by echoing
 * {@code hub.challenge}. A request whose query does not have the definition's {@code const} values or one of its
 * {@code enum} values (for example another {@code hub.mode}) or lacks a parameter the query schema marks
 * {@code required} (for example {@code hub.topic}) is not confirmed: WebSub has the subscriber answer
 * {@code 404 Not Found}.
 */
public class GenerateWebSubVerificationFuncNode implements Generator {

    private static final int HTTP_OK = 200;

    private final HandshakeConfig handshake;

    public GenerateWebSubVerificationFuncNode(HandshakeConfig handshake) {
        this.handshake = handshake;
    }

    @Override
    public FunctionDefinitionNode generate() {
        List<String> conditions = new ArrayList<>();
        for (Map.Entry<String, JsonNode> parameter : handshake.when().entrySet()) {
            conditions.add(String.format("request.getQueryParamValue(%s) != %s",
                    stringLiteral(parameter.getKey()), stringLiteral(parameter.getValue().asText())));
        }
        for (Map.Entry<String, List<String>> parameter : handshake.allowedQuery().entrySet()) {
            String value = String.format("request.getQueryParamValue(%s)", stringLiteral(parameter.getKey()));
            conditions.add("!(" + String.join(" || ", parameter.getValue().stream()
                    .map(allowed -> value + " == " + stringLiteral(allowed)).toList()) + ")");
        }
        for (String required : handshake.requiredQuery()) {
            conditions.add(String.format("request.getQueryParamValue(%s) is ()", stringLiteral(required)));
        }
        String status = handshake.statusCode() == HTTP_OK ? ""
                : String.format(" handshakeResponse.statusCode = %d;", handshake.statusCode());
        String confirm = String.format("string? challenge = request.getQueryParamValue(%s);"
                + " if challenge is string { handshakeResponse.setTextPayload(challenge);%s }"
                + " else { handshakeResponse.statusCode = http:STATUS_BAD_REQUEST; }",
                stringLiteral(handshake.challengeQuery()), status);
        String body = conditions.isEmpty() ? confirm
                : String.format("if %s { handshakeResponse.statusCode = http:STATUS_NOT_FOUND; } else { %s }",
                        String.join(" || ", conditions), confirm);
        return (FunctionDefinitionNode) NodeParser.parseObjectMember(
                "resource function get .(http:Caller caller, http:Request request) returns error? {"
                        + " http:Response handshakeResponse = new; " + body
                        + " check caller->respond(handshakeResponse); }");
    }

    private static String stringLiteral(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
