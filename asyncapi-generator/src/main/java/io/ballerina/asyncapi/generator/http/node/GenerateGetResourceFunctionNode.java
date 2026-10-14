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

import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.List;

/**
 * Generates the {@code resource function get .} method of the {@code DispatcherService} that answers a {@code GET}
 * verification handshake (for example a WebSub hub's {@code hub.challenge}) by echoing the challenge from the query.
 * The request has no body, so there is no signature to verify, and nothing is dispatched to a service.
 */
public class GenerateGetResourceFunctionNode implements Generator {

    private final HandshakeConfig handshake;

    /**
     * Creates a generator for the handshake resource function.
     *
     * @param handshake the {@code GET} handshake from the event identifier
     */
    public GenerateGetResourceFunctionNode(HandshakeConfig handshake) {
        this.handshake = handshake;
    }

    @Override
    public FunctionDefinitionNode generate() {
        List<StatementNode> statements = List.of(
                NodeParser.parseStatement(String.format("string? challenge = request.getQueryParamValue(%s);",
                        GeneratePostResourceFunctionNode.balStringLiteral(handshake.challengeKey()))),
                NodeParser.parseStatement("http:Response handshakeResponse = new;"),
                NodeParser.parseStatement("if challenge is string { handshakeResponse.setTextPayload(challenge); }"
                        + " else { handshakeResponse.statusCode = http:STATUS_BAD_REQUEST; }"),
                NodeParser.parseStatement("check caller->respond(handshakeResponse);"));
        return GeneratePostResourceFunctionNode.buildResourceFunction("get", statements);
    }
}
