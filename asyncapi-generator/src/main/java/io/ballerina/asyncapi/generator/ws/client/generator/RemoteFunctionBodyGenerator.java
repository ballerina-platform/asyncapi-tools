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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.ATTEMPT_TO_CLOSE_CONNECTION;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.IS_ACTIVE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.PIPES;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;

/**
 * Generates a {@link FunctionBodyNode} for a WebSocket client remote function.
 */
public class RemoteFunctionBodyGenerator {

    private static final String STREAM_GENERATOR_CAPITAL = "StreamGenerator";
    private static final String STREAM_GENERATOR = "streamGenerator";
    private static final String LOG_PRINT_DEBUG_TEMPLATE = "log:printDebug(\"%s: %s\", %s);";
    private static final String CLONE_WITH_TYPE = "cloneWithType";
    private static final String CONSUME = "consume";
    private static final String PRODUCE = "produce";
    private static final String MESSAGE_TYPE = "Message";
    private static final String PIPE_ERROR_TYPE = "pipe:Error";
    private static final String MESSAGE_VAR = "message";
    private static final String PIPE_ERR_VAR = "pipeErr";
    private static final String PIPE_ERR_CAPITAL = "PipeError";
    private static final String RESPONSE_MESSAGE_VAR = "responseMessage";
    private static final String PIPE_SEP = "|";
    private static final String CONNECTION_CLOSED_MSG = "\"ConnectionError: Connection has been closed\"";
    private static final String DATABINDING_ERR_MSG = "\"DataBindingError: Error in cloning message\"";
    private static final String PIPE_ERR_TEMPLATE = "\"PipeError: Error in %s message\"";
    private static final String CONSUMING = "consuming";
    private static final String PRODUCING = "producing";
    private static final String ERROR_PIPE_CLOSE = "Error in closing pipe.";
    private static final String ERROR = "error";
    private static final String SELF_PIPES_GET_PIPE = "self.pipes.getPipe(%s)";
    private static final String PIPE_CLOSE_STATEMENT = "error? %s = self.pipes.removePipe(%s);";
    private static final String CREATE_UUID_STATEMENT = "%s.%s = uuid:createType1AsString();";
    private static final String STREAM_GENERATORS = "streamGenerators";

    private final List<ImportDeclarationNode> imports;

    /**
     * Creates a new function body generator.
     *
     * @param imports the import declaration list (will be wrapped as unmodifiable)
     */
    public RemoteFunctionBodyGenerator(List<ImportDeclarationNode> imports) {
        this.imports = Collections.unmodifiableList(imports);
    }

    /**
     * Returns the import declaration list.
     *
     * @return unmodifiable import list
     */
    public List<ImportDeclarationNode> getImports() {
        return imports;
    }

    /**
     * Builds the function body source text for a remote function.
     *
     * @param extensions             message-level extensions ({@code x-response-type}, etc.)
     * @param requestType            the Ballerina request type name
     * @param specDispatcherStreamId the dispatcher stream ID field name (may be {@code null})
     * @param isSubscribe            {@code true} for receive-only (subscribe) operations
     * @param responseType           the Ballerina response type name
     * @param pipeName               the canonical pipe key for this response type (from the responseMap)
     * @return function body source text (statements only, no braces)
     */
    public String buildBodySource(Map<String, JsonNode> extensions, String requestType,
                                  String specDispatcherStreamId, boolean isSubscribe,
                                  String responseType, String pipeName) {
        StringBuilder sb = new StringBuilder();
        if (extensions != null) {
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);
            if (xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText())) {
                buildStreamBodySource(sb, requestType, responseType, specDispatcherStreamId, isSubscribe, pipeName);
            } else {
                buildSimpleRpcBodySource(sb, requestType, responseType, specDispatcherStreamId, isSubscribe, pipeName);
            }
        } else {
            buildNoResponseBodySource(sb, requestType);
        }
        return sb.toString();
    }

    /**
     * Generates the function body for a remote function based on the response type pattern.
     *
     * @param extensions             message-level extensions ({@code x-response-type}, etc.)
     * @param requestType            the Ballerina request type name
     * @param specDispatcherStreamId the dispatcher stream ID field name (may be {@code null})
     * @param isSubscribe            {@code true} for receive-only (subscribe) operations
     * @param responseType           the Ballerina response type name
     * @param pipeName               the canonical pipe key for this response type (from the responseMap)
     * @return the function body node
     */
    public FunctionBodyNode getFunctionBodyNode(Map<String, JsonNode> extensions, String requestType,
                                                String specDispatcherStreamId, boolean isSubscribe,
                                                String responseType, String pipeName) {
        List<StatementNode> statementsList = new ArrayList<>();
        String bodySource = buildBodySource(extensions, requestType, specDispatcherStreamId, isSubscribe,
                responseType, pipeName);
        String[] lines = bodySource.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                statementsList.add(NodeParser.parseStatement(trimmed));
            }
        }
        NodeList<StatementNode> statements = createNodeList(statementsList);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN), null, statements,
                createToken(CLOSE_BRACE_TOKEN), null);
    }

    private void buildStreamBodySource(StringBuilder sb, String requestType, String responseType,
                                       String dispatcherStreamId, boolean isSubscribe, String pipeName) {
        String camelCaseRequestType = Character.toLowerCase(requestType.charAt(0)) + requestType.substring(1);
        String pipeId = String.format("\"%s\"", camelCaseRequestType);
        if (!isSubscribe) {
            appendConnectionActiveCheck(sb);
        }
        if (!Objects.isNull(dispatcherStreamId)) {
            pipeId = String.format("%s.%s", camelCaseRequestType, CodegenUtils.escapeIdentifier(dispatcherStreamId));
        }
        appendProduceStatements(sb, requestType, isSubscribe);

        sb.append(String.format("stream<%s,error?> streamMessages;\n", responseType));

        String streamGenTypeName = CodegenUtils.getStreamGeneratorName(responseType) + STREAM_GENERATOR_CAPITAL;
        sb.append("lock {\n");
        sb.append(String.format("    %s %s = new(self.%s, %s, timeout);\n",
                streamGenTypeName, STREAM_GENERATOR, PIPES, pipeId));
        sb.append(String.format("    self.%s.addStreamGenerator(%s);\n", STREAM_GENERATORS, STREAM_GENERATOR));
        sb.append(String.format("    streamMessages = new(%s);\n", STREAM_GENERATOR));
        sb.append("}\n");
        sb.append("return streamMessages;\n");
    }

    private void buildSimpleRpcBodySource(StringBuilder sb, String requestType, String responseType,
                                          String dispatcherStreamId, boolean isSubscribe, String pipeName) {
        String camelCaseRequestType = Character.toLowerCase(requestType.charAt(0)) + requestType.substring(1);
        String responseTypeCamelCase;
        if (responseType.contains(PIPE_SEP)) {
            responseTypeCamelCase = "unionResult";
        } else {
            responseTypeCamelCase = String.format("%s%s",
                    Character.toLowerCase(responseType.charAt(0)), responseType.substring(1));
        }
        String pipeId = String.format("\"%s\"", pipeName);
        if (!isSubscribe) {
            appendConnectionActiveCheck(sb);
        }
        if (!Objects.isNull(dispatcherStreamId)) {
            pipeId = String.format("%s.%s", camelCaseRequestType, CodegenUtils.escapeIdentifier(dispatcherStreamId));
        }
        appendProduceStatements(sb, requestType, isSubscribe);

        // consume
        sb.append(String.format("%s|%s %s = %s.%s(timeout);\n",
                MESSAGE_TYPE, PIPE_ERROR_TYPE, RESPONSE_MESSAGE_VAR,
                String.format(SELF_PIPES_GET_PIPE, pipeId), CONSUME));
        sb.append(String.format("if %s is %s { self.%s(); return error(%s, %s); }\n",
                RESPONSE_MESSAGE_VAR, PIPE_ERROR_TYPE, ATTEMPT_TO_CLOSE_CONNECTION,
                String.format(PIPE_ERR_TEMPLATE, CONSUMING), RESPONSE_MESSAGE_VAR));

        if (!Objects.isNull(dispatcherStreamId)) {
            String pipeCloseErr = "pipeCloseError";
            sb.append(String.format(PIPE_CLOSE_STATEMENT, pipeCloseErr, pipeId)).append("\n");
            sb.append(String.format("if %s is error { %s }\n", pipeCloseErr,
                    String.format(LOG_PRINT_DEBUG_TEMPLATE, PIPE_ERR_CAPITAL, ERROR_PIPE_CLOSE, pipeCloseErr)));
        }

        String cloneVar = requestType.equals(ERROR)
                ? responseTypeCamelCase + MESSAGE_TYPE : responseTypeCamelCase;
        sb.append(String.format("%s|error %s = %s.%s();\n",
                responseType, cloneVar, RESPONSE_MESSAGE_VAR, CLONE_WITH_TYPE));
        sb.append(String.format("if %s is error { self.%s(); return error(%s, %s); }\n",
                cloneVar, ATTEMPT_TO_CLOSE_CONNECTION, DATABINDING_ERR_MSG, cloneVar));
        sb.append(String.format("return %s;\n", cloneVar));
    }

    private void buildNoResponseBodySource(StringBuilder sb, String requestType) {
        appendConnectionActiveCheck(sb);
        String paramName = Character.toLowerCase(requestType.charAt(0)) + requestType.substring(1);
        sb.append(String.format("%s|error %s = %s.%s();\n",
                MESSAGE_TYPE, MESSAGE_VAR, paramName, CLONE_WITH_TYPE));
        sb.append(String.format("if %s is error { self.%s(); return error(%s, %s); }\n",
                MESSAGE_VAR, ATTEMPT_TO_CLOSE_CONNECTION, DATABINDING_ERR_MSG, MESSAGE_VAR));
        appendProduceToWriteQueue(sb);
    }

    private void appendConnectionActiveCheck(StringBuilder sb) {
        sb.append(String.format("lock { if !self.%s { return error(%s); } }\n", IS_ACTIVE, CONNECTION_CLOSED_MSG));
    }

    private void appendProduceStatements(StringBuilder sb, String requestType, boolean isSubscribe) {
        if (isSubscribe) {
            return;
        }
        String paramName = Character.toLowerCase(requestType.charAt(0)) + requestType.substring(1);
        sb.append(String.format("%s|error %s = %s.%s();\n",
                MESSAGE_TYPE, MESSAGE_VAR, paramName, CLONE_WITH_TYPE));
        sb.append(String.format("if %s is error { self.%s(); return error(%s, %s); }\n",
                MESSAGE_VAR, ATTEMPT_TO_CLOSE_CONNECTION, DATABINDING_ERR_MSG, MESSAGE_VAR));
        appendProduceToWriteQueue(sb);
    }

    private void appendProduceToWriteQueue(StringBuilder sb) {
        sb.append(String.format("%s? %s = self.%s.%s(%s, timeout);\n",
                PIPE_ERROR_TYPE, PIPE_ERR_VAR, WRITE_MESSAGE_QUEUE, PRODUCE, MESSAGE_VAR));
        sb.append(String.format("if %s is %s { self.%s(); return error(%s, %s); }\n",
                PIPE_ERR_VAR, PIPE_ERROR_TYPE, ATTEMPT_TO_CLOSE_CONNECTION,
                String.format(PIPE_ERR_TEMPLATE, PRODUCING), PIPE_ERR_VAR));
    }

    @SuppressWarnings("unused")
    private static StatementNode getStatementToGenerateUuid(String requestType, String dispatcherStreamId) {
        return NodeParser.parseStatement(String.format(CREATE_UUID_STATEMENT, requestType,
                CodegenUtils.escapeIdentifier(dispatcherStreamId)));
    }
}
