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
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.http.node.GenerateAddServiceRefFuncNode.buildErrorReturnType;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_KEYWORD;

/**
 * Generates the {@code resource function post .(http:Caller caller, http:Request request) returns error?}
 * method node for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 *
 * <p>For {@code "header"}, {@code eventType} comes from the named request header and is forwarded
 * as the second argument of {@code matchRemoteFunc}. For {@code "body"}, {@code eventType} is
 * extracted from the payload at the configured dot-notation path and forwarded as the second
 * argument. For {@code "composite"}, {@code eventType} comes from the header and {@code action}
 * from the body path; these are combined into a compound {@code eventIdentifier} and forwarded
 * as the second and third arguments of {@code matchRemoteFunc}.
 *
 * <p>When {@link EventIdentifierConfig#batched()} is set, the POST body is instead treated as a
 * JSON array of events (some providers, e.g. HubSpot, batch multiple events into one delivery):
 * the response is acknowledged once for the whole batch, then each array element is identified,
 * converted, and dispatched independently, with per-element failures logged and skipped rather
 * than aborting the rest of the batch.
 */
public class GeneratePostResourceFunctionNode implements Generator {

    private final EventIdentifierConfig identifierConfig;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;

    /**
     * Creates a generator for the post resource function.
     *
     * @param identifierConfig  the resolved event identifier type and path
     * @param webhookAuthConfig the optional webhook authentication configuration
     */
    public GeneratePostResourceFunctionNode(EventIdentifierConfig identifierConfig,
            Optional<WebhookAuthConfig> webhookAuthConfig) {
        this.identifierConfig = identifierConfig;
        this.webhookAuthConfig = webhookAuthConfig;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createQualifiedNameReferenceNode(
                                        createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                                        createToken(COLON_TOKEN),
                                        createIdentifierToken("Caller")),
                                createIdentifierToken("caller")),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createQualifiedNameReferenceNode(
                                        createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                                        createToken(COLON_TOKEN),
                                        createIdentifierToken("Request")),
                                createIdentifierToken("request"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        String type = identifierConfig.type();

        List<StatementNode> statements = new ArrayList<>();
        if (webhookAuthConfig.isPresent()) {
            statements.add(NodeParser.parseStatement(String.format(
                    "error? verifyResult = self.%s(request, self.%s);",
                    GenerateVerifyWebhookSignatureFuncNode.VERIFY_WEBHOOK_SIGNATURE_FUNC,
                    GenerateDispatcherServiceNode.WEBHOOK_SECRET_FIELD)));
            statements.add(NodeParser.parseStatement(
                    "if verifyResult is error {"
                            + " http:Response r = new; r.statusCode = http:STATUS_UNAUTHORIZED;"
                            + " check caller->respond(r); return; }"));
        }
        statements.add(NodeParser.parseStatement("json payload = check request.getJsonPayload();"));
        if (identifierConfig.batched()) {
            statements.addAll(buildBatchedDispatchStatements(type));
        } else {
            statements.addAll(buildSingleEventDispatchStatements(type));
        }

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                RESOURCE_ACCESSOR_DEFINITION, null,
                createNodeList(createToken(RESOURCE_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("post"),
                createNodeList(createToken(DOT_TOKEN)),
                signature, body);
    }

    /**
     * Builds the single-event dispatch statements: extract one {@code eventType} (and, for
     * {@code composite}, one compound {@code eventIdentifier}), convert the whole body with one
     * {@code cloneWithType}, acknowledge, and dispatch once.
     *
     * @param type the event identifier type ({@code header}, {@code body}, or {@code composite})
     * @return the statements to append to the resource function body
     */
    private List<StatementNode> buildSingleEventDispatchStatements(String type) {
        List<StatementNode> statements = new ArrayList<>();
        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(type)) {
            statements.addAll(buildEventTypeFromHeaderStatements(identifierConfig.name()));
        } else if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(type)) {
            statements.add(NodeParser.parseStatement(String.format(
                    "string eventType = (check payload.%s).toString();",
                    identifierConfig.path())));
        } else {
            statements.addAll(buildEventTypeFromHeaderStatements(identifierConfig.name()));
            statements.add(NodeParser.parseStatement(String.format(
                    "json|error actionField = payload.%s;",
                    identifierConfig.path())));
            statements.add(NodeParser.parseStatement("string eventIdentifier = eventType;"));
            statements.add(NodeParser.parseStatement(
                    "if actionField is json && actionField != () {"
                    + " eventIdentifier = eventType + \"_\" + actionField.toString(); }"));
        }
        statements.add(NodeParser.parseStatement(String.format(
                "%s %s = check payload.cloneWithType(%s);",
                DataTypesGenerator.GENERIC_DATA_TYPE, GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME,
                DataTypesGenerator.GENERIC_DATA_TYPE)));
        statements.add(NodeParser.parseStatement("http:Response ackResponse = new;"));
        statements.add(NodeParser.parseStatement("ackResponse.statusCode = http:STATUS_OK;"));
        statements.add(NodeParser.parseStatement("check caller->respond(ackResponse);"));
        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type)) {
            statements.add(NodeParser.parseStatement(String.format(
                    "error? dispatchResult = self.%s(%s, eventIdentifier, eventType);",
                    GenerateMatchRemoteFuncNode.DISPATCHER_MATCH_REMOTE_FUNC,
                    GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)));
        } else {
            statements.add(NodeParser.parseStatement(String.format(
                    "error? dispatchResult = self.%s(%s, eventType);",
                    GenerateMatchRemoteFuncNode.DISPATCHER_MATCH_REMOTE_FUNC,
                    GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)));
        }
        statements.add(NodeParser.parseStatement(
                "if dispatchResult is error { log:printError(\"DISPATCH_FAILED\", dispatchResult); }"));
        return statements;
    }

    /**
     * Builds the batched dispatch statements: the body is a JSON array of events. A
     * {@code header}/{@code composite} identifier header is read once (it applies to the whole
     * batch); a {@code body} identifier path is read per element (each event carries its own). The
     * batch is acknowledged once, then each element is converted and dispatched independently --
     * an element that fails to identify or convert is logged and skipped via {@code continue}
     * rather than aborting the rest of the batch.
     *
     * @param type the event identifier type ({@code header}, {@code body}, or {@code composite})
     * @return the statements to append to the resource function body
     */
    private List<StatementNode> buildBatchedDispatchStatements(String type) {
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement("json[] eventsArray = check payload.ensureType();"));

        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(type)
                || EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type)) {
            statements.addAll(buildEventTypeFromHeaderStatements(identifierConfig.name()));
        }

        statements.add(NodeParser.parseStatement("http:Response ackResponse = new;"));
        statements.add(NodeParser.parseStatement("ackResponse.statusCode = http:STATUS_OK;"));
        statements.add(NodeParser.parseStatement("check caller->respond(ackResponse);"));

        // The batch is acknowledged above; the actual per-element dispatch loop runs on a separate
        // strand so it doesn't hold this HTTP worker thread for the loop's duration. For a "body"
        // identifier there's no outer eventType (each element carries its own), so pass a placeholder
        // the callee simply won't use.
        boolean hasOuterEventType = EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(type)
                || EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type);
        statements.add(NodeParser.parseStatement(String.format(
                "_ = start self.%s(eventsArray, %s);",
                GenerateDispatchBatchFuncNode.DISPATCH_BATCH_FUNC, hasOuterEventType ? "eventType" : "\"\"")));
        return statements;
    }

    /**
     * Builds the statements that read {@code eventType} from a required request header, responding
     * {@code 400 Bad Request} and returning early if it's absent - a missing header is a malformed
     * request (client error), not a server fault, so it must not surface as a plain {@code check}
     * failure (which the framework reports as a 500).
     *
     * @param headerName the request header to read (e.g. {@code "X-GitHub-Event"})
     * @return the statements, ending with {@code eventType} bound to the header's value
     */
    private List<StatementNode> buildEventTypeFromHeaderStatements(String headerName) {
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "string|error eventTypeResult = request.getHeader(\"%s\");", headerName)));
        statements.add(NodeParser.parseStatement(
                "if eventTypeResult is error {"
                        + " http:Response badRequest = new; badRequest.statusCode = http:STATUS_BAD_REQUEST;"
                        + " check caller->respond(badRequest); return; }"));
        statements.add(NodeParser.parseStatement("string eventType = eventTypeResult;"));
        return statements;
    }
}
