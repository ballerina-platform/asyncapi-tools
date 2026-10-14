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

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.utils.ChannelRoutes;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SLASH_TOKEN;

/**
 * Generates a {@code resource function post} at one channel's address when the listener routes by channel: it reads
 * the event type with the channel's own identifier and dispatches only to that channel's service, so a value that
 * also exists on another channel cannot reach the wrong service.
 */
public class GenerateChannelResourceFunctionNode implements Generator {

    private final HttpServiceType serviceType;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;

    /**
     * Creates a generator for one channel's resource function.
     *
     * @param serviceType       the channel's service type, with its effective identifier and address
     * @param webhookAuthConfig the optional webhook authentication configuration
     */
    public GenerateChannelResourceFunctionNode(HttpServiceType serviceType,
                                               Optional<WebhookAuthConfig> webhookAuthConfig) {
        this.serviceType = serviceType;
        this.webhookAuthConfig = webhookAuthConfig;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        EventIdentifierConfig identifier = serviceType.identifier();
        List<StatementNode> statements =
                new ArrayList<>(GeneratePostResourceFunctionNode.buildVerifyStatements(webhookAuthConfig));
        statements.add(NodeParser.parseStatement("json payload = check request.getJsonPayload();"));
        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(identifier.type())) {
            statements.addAll(GeneratePostResourceFunctionNode.buildEventTypeFromHeaderStatements(identifier.name()));
        } else {
            statements.add(NodeParser.parseStatement(String.format(
                    "string eventType = (check payload.%s).toString();", identifier.path())));
        }
        statements.add(NodeParser.parseStatement("http:Response ackResponse = new;"));
        statements.add(NodeParser.parseStatement("ackResponse.statusCode = http:STATUS_OK;"));
        statements.add(NodeParser.parseStatement("check caller->respond(ackResponse);"));
        statements.add(NodeParser.parseStatement(String.format(
                "boolean|error dispatchResult = self.%s(%s, eventType);",
                GenerateMatchChunkFuncNode.chunkFuncName(serviceType.serviceTypeName()),
                GenerateDispatcherServiceNode.PAYLOAD_VAR_NAME)));
        statements.add(NodeParser.parseStatement(GeneratePostResourceFunctionNode.unhandledAwareLog("eventType")));

        List<Node> path = new ArrayList<>();
        for (String segment : ChannelRoutes.segments(serviceType.address())) {
            if (!path.isEmpty()) {
                path.add(createToken(SLASH_TOKEN));
            }
            path.add(createIdentifierToken(ChannelRoutes.resourceSegment(segment)));
        }
        return GeneratePostResourceFunctionNode.buildPostResource(createNodeList(path), statements);
    }
}
