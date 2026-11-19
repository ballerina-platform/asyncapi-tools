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
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherStreamIdExtractor;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.client.utils.ContentRoutes;
import io.ballerina.asyncapi.generator.ws.client.utils.IncomingMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;

/**
 * How the client tells incoming messages apart and which pipe each one goes to, worked out once from the spec
 * before any code is generated.
 *
 * @param dispatcherKey         the field every routed message carries, or {@code null} when the connection receives
 *                              one kind of message and the spec gives no key
 * @param dispatcherStreamId    the field that pairs a streamed reply with its request, or {@code null}
 * @param responseMap           incoming type name to the pipe that receives it
 * @param dispatchValueMap      fixed dispatcher value to its pipe, checked before {@code responseMap}
 * @param streamReturns         return types of the server-streaming functions
 * @param keylessReplies        whether some call waits for a message without the dispatcher key
 * @param keylessReceiveOnly    whether a receive-only function waits for a message without the dispatcher key
 * @param sendsWithoutKey       whether some message is sent without the dispatcher key, as it is
 * @param contentRoutes         with no dispatcher key, how each received JSON object is recognised by what it
 *                              carries, in the order the reader checks them
 * @param leftOutTypes          received types nothing tells apart, which get no function
 */
public record RoutingPlan(String dispatcherKey, String dispatcherStreamId, Map<String, String> responseMap,
                          Map<String, String> dispatchValueMap, List<String> streamReturns, boolean keylessReplies,
                          boolean keylessReceiveOnly, boolean sendsWithoutKey, List<ContentRoute> contentRoutes,
                          Set<String> leftOutTypes) {

    /**
     * A received type recognised by what a frame carries.
     *
     * @param typeName the Ballerina type the frame is read as
     * @param field    the field the reader looks at
     * @param value    the value the field has, or {@code null} when the field being there is enough
     * @param pipe     the pipe the frame goes to
     */
    public record ContentRoute(String typeName, String field, String value, String pipe) { }

    private static final String MESSAGE_TYPE = "Message";
    private static final String SHARED_FIXED_DISPATCH_VALUE = "The replies waited for on '%s' and '%s' both fix the "
            + "dispatcher value '%s', so a message with it can't be routed to the right call. "
            + "Give each its own value.";

    public RoutingPlan {
        responseMap = Collections.unmodifiableMap(new LinkedHashMap<>(responseMap));
        dispatchValueMap = Collections.unmodifiableMap(new LinkedHashMap<>(dispatchValueMap));
        streamReturns = List.copyOf(streamReturns);
        contentRoutes = List.copyOf(contentRoutes);
        leftOutTypes = Set.copyOf(leftOutTypes);
    }

    /**
     * Works out the routing for {@code spec}.
     *
     * @param spec the spec, with its dispatcher key already fixed up
     * @return the routing plan
     * @throws GeneratorException if the spec has no dispatcher key and needs one, or two replies can't be told apart
     */
    public static RoutingPlan of(AsyncApiSpec spec) throws GeneratorException {
        String dispatcherKey = new DispatcherKeyExtractor(spec).extractIfNeeded().orElse(null);
        String dispatcherStreamId = new DispatcherStreamIdExtractor(spec).extract().orElse(null);
        List<String> streamReturns = new ArrayList<>();
        Map<String, String> dispatchValueMap = new LinkedHashMap<>();
        Map<String, String> responseMap = buildResponseMap(spec, dispatcherKey, streamReturns, dispatchValueMap);
        RemoteFunctionReturnTypeGenerator returnTypeGen = new RemoteFunctionReturnTypeGenerator(spec);
        List<ContentRoute> contentRoutes = new ArrayList<>();
        Set<String> leftOutTypes = new HashSet<>();
        if (dispatcherKey == null && IncomingMessages.needsKey(spec)) {
            routeByContent(spec, responseMap, contentRoutes, leftOutTypes);
        }
        return new RoutingPlan(dispatcherKey, dispatcherStreamId, responseMap, dispatchValueMap, streamReturns,
                returnTypeGen.hasRepliesWithoutDispatcherKey(), returnTypeGen.hasReceiveOnlyWithoutDispatcherKey(),
                sendsWithoutKey(spec, returnTypeGen), contentRoutes, leftOutTypes);
    }

    /**
     * Whether the reader reads every frame as one without the dispatcher key, as no incoming message carries it.
     *
     * @return whether the reader skips routing on the key
     */
    public boolean readsWithoutKey() {
        return contentRoutes.isEmpty() && (dispatcherKey == null || responseMap.isEmpty() && keylessReplies);
    }

    /**
     * Whether the reader recognises received JSON objects by what they carry, as the spec has no dispatcher key.
     *
     * @return whether the reader routes by content
     */
    public boolean routesByContent() {
        return !contentRoutes.isEmpty();
    }

    /**
     * Returns whether a received type is recognised by what its frames carry.
     *
     * @param typeName the Ballerina type name
     * @return whether frames of that type are routed by content
     */
    public boolean isRoutedByContent(String typeName) {
        return contentRoutes.stream().anyMatch(route -> route.typeName().equals(typeName));
    }

    // Each received object's route goes to the pipe responseMap gives its type; a type left out has no pipe
    private static void routeByContent(AsyncApiSpec spec, Map<String, String> responseMap,
                                       List<ContentRoute> contentRoutes, Set<String> leftOutTypes) {
        for (IncomingMessages.Connection connection : IncomingMessages.connections(spec)) {
            ContentRoutes routes = ContentRoutes.of(spec, connection.operations());
            for (ContentRoutes.Route route : routes.routes()) {
                String typeName = typeName(route.message());
                String pipe = responseMap.get(typeName);
                if (typeName != null && pipe != null) {
                    contentRoutes.add(new ContentRoute(typeName, route.field(), route.value(), pipe));
                }
            }
            for (IncomingMessages.Received message : routes.leftOut()) {
                String typeName = typeName(message);
                if (typeName != null) {
                    leftOutTypes.add(typeName);
                    responseMap.remove(typeName);
                }
            }
        }
    }

    private static String typeName(IncomingMessages.Received message) {
        return message.schemaName() != null ? CodegenUtils.getValidName(message.schemaName(), true) : null;
    }

    // A sent message whose payload doesn't carry the key, such as a string or a record without it
    private static boolean sendsWithoutKey(AsyncApiSpec spec, RemoteFunctionReturnTypeGenerator returnTypeGen)
            throws GeneratorException {
        for (AsyncApiOperation op : spec.getAsyncApiOperations().orElse(Map.of()).values()) {
            if (op.action() != AsyncApiOperation.Action.SEND || op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                if (message.payload() instanceof AsyncApiSchema payload && !CodegenUtils.isCloseFrameSchema(payload)
                        && !returnTypeGen.carriesDispatcherKey(payload)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Builds a map from each inbound response-schema name to the pipe that receives it, used by the reader to
     * route incoming frames: a send function's reply goes to its request type's pipe, and a receive-only message
     * to a pipe of its own type. A reply type that fixes its dispatcher value also gets an entry in
     * {@code dispatchValueMap}, which the reader checks first.
     */
    private static Map<String, String> buildResponseMap(AsyncApiSpec asyncApiSpec, String dispatcherKey,
                                                        List<String> streamReturns,
                                                        Map<String, String> dispatchValueMap)
            throws GeneratorException {
        Map<String, String> responseMap = new LinkedHashMap<>();
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return responseMap;
        }
        Map<String, AsyncApiSchema> schemas = asyncApiSpec.getAsyncApiComponents()
                .map(c -> c.schemas()).orElse(Map.of());
        RemoteFunctionReturnTypeGenerator returnTypeGen = new RemoteFunctionReturnTypeGenerator(asyncApiSpec);

        Set<String> claimedResponses = new HashSet<>();
        // A shared reply type's pipe is final only after every send function, so routes are added at the end
        Map<String, AsyncApiSchema> routeSchemas = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiOperation> opEntry : operations.entrySet()) {
            AsyncApiOperation operation = opEntry.getValue();
            if (operation.action() != AsyncApiOperation.Action.SEND || operation.messages() == null) {
                continue;
            }

            for (Map.Entry<String, AsyncApiMessage> msgEntry : operation.messages().entrySet()) {
                AsyncApiMessage message = msgEntry.getValue();
                if (!isSentWithReply(message)) {
                    continue;
                }
                JsonNode xResponseType = message.extensions().get(X_RESPONSE_TYPE);
                if (xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText())) {
                    continue;
                }
                JsonNode xResponse = message.extensions().get(X_RESPONSE);
                List<String> responseMessages = new ArrayList<>();
                String returnType = returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
                // A reply without the dispatcher key is never routed by its value
                if (!"null".equals(returnType) && !returnTypeGen.isReplyWithoutDispatcherKey(returnType)) {
                    String requestType = getRequestTypeFromMessage(message);
                    for (String responseMsg : responseMessages) {
                        String key = CodegenUtils.getValidName(responseMsg, true);
                        responseMap.put(key, requestType);
                        claimedResponses.add(key);
                        routeSchemas.putIfAbsent(key, schemas.get(responseMsg));
                    }
                }
            }

            for (Map.Entry<String, AsyncApiMessage> msgEntry : operation.messages().entrySet()) {
                AsyncApiMessage message = msgEntry.getValue();
                if (!isSentWithReply(message)) {
                    continue;
                }
                JsonNode xResponseType = message.extensions().get(X_RESPONSE_TYPE);
                if (xResponseType == null || !SERVER_STREAMING.equals(xResponseType.asText())) {
                    continue;
                }
                JsonNode xResponse = message.extensions().get(X_RESPONSE);
                List<String> responseMessages = new ArrayList<>();
                String returnType = returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
                if (!"null".equals(returnType)) {
                    if (!streamReturns.contains(returnType)) {
                        streamReturns.add(returnType);
                    }
                    String requestType = getRequestTypeFromMessage(message);
                    for (String responseMsg : responseMessages) {
                        String key = CodegenUtils.getValidName(responseMsg, true);
                        if (!claimedResponses.contains(key)) {
                            responseMap.put(key, requestType);
                            routeSchemas.putIfAbsent(key, schemas.get(responseMsg));
                        }
                    }
                }
            }
        }

        // A receive-only message no send function returns waits on a pipe named after its own type
        for (AsyncApiOperation operation : operations.values()) {
            if (operation.action() != AsyncApiOperation.Action.RECEIVE || operation.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : operation.messages().values()) {
                if (!(message.payload() instanceof AsyncApiSchema payload) || payload.name() == null) {
                    continue;
                }
                String key = CodegenUtils.getValidName(payload.name(), true);
                if (responseMap.containsKey(key) || returnTypeGen.isReplyWithoutDispatcherKey(key)) {
                    continue;
                }
                String pipeName = CodegenUtils.getValidName(payload.name(), false);
                responseMap.put(key, pipeName);
                routeSchemas.putIfAbsent(key, schemas.get(payload.name()));
            }
        }
        for (Map.Entry<String, AsyncApiSchema> route : routeSchemas.entrySet()) {
            addFixedValueRoute(route.getValue(), dispatcherKey, responseMap.get(route.getKey()), dispatchValueMap);
        }
        return responseMap;
    }

    /**
     * Routes the dispatcher value {@code schema} fixes, if any, straight to {@code pipeName}. A value already
     * routed to another pipe is rejected, as the reader couldn't tell which call a message with it belongs to.
     */
    private static void addFixedValueRoute(AsyncApiSchema schema, String dispatcherKey, String pipeName,
                                           Map<String, String> dispatchValueMap) throws GeneratorException {
        String value = CodegenUtils.getFixedDispatchValue(schema, dispatcherKey);
        if (value == null) {
            return;
        }
        String routed = dispatchValueMap.putIfAbsent(value, pipeName);
        if (routed != null && !routed.equals(pipeName)) {
            throw new GeneratorException(String.format(SHARED_FIXED_DISPATCH_VALUE, routed, pipeName, value));
        }
    }

    // A sent message with a reply; one without a payload schema gets no function, so its reply gets no route either
    private static boolean isSentWithReply(AsyncApiMessage message) {
        return message.payload() instanceof AsyncApiSchema && message.extensions() != null
                && message.extensions().containsKey(X_RESPONSE);
    }

    private static String getRequestTypeFromMessage(AsyncApiMessage message) {
        if (message.payload() instanceof AsyncApiSchema payload && payload.name() != null) {
            return CodegenUtils.getValidName(payload.name(), false);
        }
        return MESSAGE_TYPE;
    }
}
