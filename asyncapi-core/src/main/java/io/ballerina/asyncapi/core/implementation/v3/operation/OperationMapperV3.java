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
package io.ballerina.asyncapi.core.implementation.v3.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperationBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channels;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Operation;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReply;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReplyAddress;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Operations;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Reference;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.component.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.SecuritySchemeMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReply;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReplyAddress;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings;
import io.ballerina.asyncapi.core.model.operation.WsOperationBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Operation models to {@link AsyncApiOperation}
 * for AsyncAPI 3.0.
 */
public final class OperationMapperV3 {

    private static final PrintStream outStream = System.err;

    private OperationMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApi30Operations} to a map of operation IDs to
     * {@link AsyncApiOperation}.
     *
     * @param operations the Apicurio operations object
     * @param rawChannels the raw AsyncAPI 3.0 channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped operations map, or an empty map if operations is null
     */
    public static Map<String, AsyncApiOperation> map(
            AsyncApi30Operations operations,
            AsyncApi30Channels rawChannels,
            Map<String, AsyncApiChannel> channelsMap,
            AsyncApi30Components components) {
        if (operations == null) {
            return Map.of();
        }
        List<String> operationIds = operations.getItemNames();
        if (operationIds == null || operationIds.isEmpty()) {
            return Map.of();
        }
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (String operationId : operationIds) {
            AsyncApi30Operation operation =
                    operations.getItem(operationId);
            if (operation != null) {
                result.put(operationId, buildOperation(operation, rawChannels, channelsMap, components));
            }
        }
        return result;
    }

    /**
     * Builds an {@link AsyncApiOperation} from an Apicurio
     * {@link AsyncApi30Operation} object.
     *
     * @param operation the Apicurio AsyncAPI 3.0 operation object
     * @param rawChannels the raw AsyncAPI 3.0 channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiOperation
     */
    public static AsyncApiOperation buildOperation(
            AsyncApi30Operation operation,
            AsyncApi30Channels rawChannels,
            Map<String, AsyncApiChannel> channelsMap,
            AsyncApi30Components components) {
        List<AsyncApiTag> tags = null;
        if (operation.getTags() != null) {
            tags = operation.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }
        AsyncApiOperation.Action action =
                mapAction(operation.getAction());
        return new AsyncApiOperation(
                action,
                extractChannel(operation, channelsMap),
                operation.getTitle(),
                operation.getSummary(),
                operation.getDescription(),
                extractMessages(operation, rawChannels, components),
                SecuritySchemeMapperV3.mapSecurityList(operation.getSecurity(), components),
                OperationReplyMapperV3.mapReply(operation.getReply(), rawChannels, channelsMap, components),
                tags,
                ExternalDocMapperV3.map((AsyncApiExternalDocumentation) operation.getExternalDocs(), null),
                mapBindings(operation.getBindings()),
                OperationTraitMapperV3.mapTraits(operation.getTraits(), components),
                operation.getExtensions()
        );
    }

    /**
     * Maps an Apicurio operation trait to an {@link AsyncApiOperationTrait}.
     *
     * @param trait the Apicurio operation trait object
     * @return the mapped AsyncApiOperationTrait
     */
    public static AsyncApiOperationTrait mapTrait(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperationTrait trait,
            AsyncApi30Components components) {
        return OperationTraitMapperV3.map(trait, components);
    }

    /**
     * Maps an Apicurio {@link AsyncApi30OperationReply} to an
     * {@link AsyncApiOperationReply}.
     *
     * @param reply the Apicurio operation reply object
     * @param rawChannels the raw AsyncAPI 3.0 channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiOperationReply, or null if reply is null
     */
    public static AsyncApiOperationReply mapReply(
            AsyncApi30OperationReply reply,
            AsyncApi30Channels rawChannels,
            Map<String, AsyncApiChannel> channelsMap,
            AsyncApi30Components components) {
        return OperationReplyMapperV3.mapReply(reply, rawChannels, channelsMap, components);
    }

    /**
     * Maps an Apicurio {@link AsyncApi30OperationReplyAddress} to an
     * {@link AsyncApiOperationReplyAddress}.
     *
     * @param address the Apicurio operation reply address object
     * @return the mapped AsyncApiOperationReplyAddress, or null if null
     */
    public static AsyncApiOperationReplyAddress mapReplyAddress(
            AsyncApi30OperationReplyAddress address) {
        return OperationReplyMapperV3.mapReplyAddress(address);
    }

    /**
     * Maps Apicurio {@link AsyncApiOperationBindings} to
     * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings}.
     *
     * @param bindings the Apicurio operation bindings object
     * @return the mapped AsyncApiOperationBindings, or null if bindings
     *         is null or empty
     */
    public static io.ballerina.asyncapi.core.model.operation
            .AsyncApiOperationBindings mapBindings(
            AsyncApiOperationBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpOperationBindings http = HttpOperationBindingMapperV3.map(bindings.getHttp());
        WsOperationBindings ws = bindings.getWs() != null
                ? new WsOperationBindings() : null;
        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.operation
                .AsyncApiOperationBindings(http, ws, extensions);
    }

    /**
     * Maps action string to {@link AsyncApiOperation.Action} enum.
     *
     * @param action the action string (send or receive)
     * @return the mapped Action enum, or null if action is null
     */
    private static AsyncApiOperation.Action mapAction(String action) {
        if (action == null) {
            return null;
        }
        return switch (action.toLowerCase()) {
            case "send" -> AsyncApiOperation.Action.SEND;
            case "receive" -> AsyncApiOperation.Action.RECEIVE;
            default -> null;
        };
    }

    /**
     * Extracts the channel from an AsyncAPI 3.0 operation.
     *
     * @param operation the AsyncAPI 3.0 operation object
     * @param channelsMap the map of channel names to AsyncApiChannel objects
     * @return the resolved AsyncApiChannel, or null if not found
     */
    private static AsyncApiChannel extractChannel(
            AsyncApi30Operation operation,
            Map<String, AsyncApiChannel> channelsMap) {

        if (operation == null || channelsMap == null) {
            return null;
        }

        AsyncApi30Reference channelRef = operation.getChannel();
        if (channelRef == null) {
            return null;
        }

        String $ref = channelRef.get$ref();
        if ($ref == null) {
            outStream.println("Operation channel reference has no $ref. Skipping.");
            return null;
        }

        return resolveChannelRef($ref, channelsMap);
    }

    /**
     * Resolves a $ref to a channel.
     *
     * @param $ref the $ref string (e.g., "#/channels/channelName")
     * @param channelsMap the map of channel names to AsyncApiChannel objects
     * @return the resolved channel, or null if not found or invalid
     */
    static AsyncApiChannel resolveChannelRef(
            String $ref,
            Map<String, AsyncApiChannel> channelsMap) {

        if (!$ref.startsWith(Constants.V3_CHANNELS_REF_PREFIX)) {
            outStream.println("Unsupported channel $ref format: " + $ref + ". Expected " +
                    Constants.V3_CHANNELS_REF_PREFIX);
            return null;
        }

        String channelName = $ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length());
        if (channelsMap == null) {
            return null;
        }

        AsyncApiChannel channel = channelsMap.get(channelName);
        if (channel == null) {
            outStream.println("Channel reference '" + channelName +
                    "' not found in channels map. Skipping.");
        }
        return channel;
    }

    /**
     * Extracts messages from an AsyncAPI 3.0 operation.
     *
     * @param operation the AsyncAPI 3.0 operation object
     * @param rawChannels the raw AsyncAPI 3.0 channels object
     * @param components the AsyncAPI components object
     * @return a list of AsyncApiMessage objects, or null if no messages found
     */
    private static List<io.ballerina.asyncapi.core.model.message.AsyncApiMessage> extractMessages(
            AsyncApi30Operation operation,
            AsyncApi30Channels rawChannels,
            AsyncApi30Components components) {

        if (operation == null) {
            return null;
        }

        List<AsyncApi30Reference> messageRefs = operation.getMessages();
        if (messageRefs == null || messageRefs.isEmpty()) {
            return null;
        }

        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessage> result = new ArrayList<>();
        for (AsyncApi30Reference messageRef : messageRefs) {
            if (messageRef == null) {
                continue;
            }

            String $ref = messageRef.get$ref();
            if ($ref == null) {
                outStream.println("Operation message reference has no $ref. Skipping.");
                continue;
            }

            AsyncApi30Message resolved = resolveMessageRef($ref, rawChannels, components);
            if (resolved == null) {
                outStream.println("Could not resolve message $ref: " + $ref + ". Skipping.");
                continue;
            }

            // Guard against chained $refs
            if (resolved instanceof AsyncApiReferenceable resolvedRef
                    && resolvedRef.get$ref() != null) {
                outStream.println("Resolved message $ref points to another $ref: " +
                        resolvedRef.get$ref() + ". Skipping.");
                continue;
            }

            io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped = MessageMapperV3.map(resolved);
            if (mapped != null) {
                result.add(mapped);
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Resolves a $ref to a message in either channels or components.
     *
     * @param $ref the $ref string (e.g., "#/channels/channelName/messages/messageName"
     *             or "#/components/messages/messageName")
     * @param rawChannels the raw AsyncAPI 3.0 channels object
     * @param components the AsyncAPI components object
     * @return the resolved message, or null if not found or invalid
     */
    static AsyncApi30Message resolveMessageRef(
            String $ref,
            AsyncApi30Channels rawChannels,
            AsyncApi30Components components) {

        // Check if ref is to components/messages
        if ($ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
            String name = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
            if (components == null) {
                return null;
            }
            Map<String, ? extends AsyncApiMessage> messagesMap = components.getMessages();
            if (messagesMap == null) {
                return null;
            }
            AsyncApiMessage message = messagesMap.get(name);
            return message instanceof AsyncApi30Message typed ? typed : null;
        }

        // Check if ref is to channels/channelName/messages/messageName
        if ($ref.startsWith(Constants.V3_CHANNELS_REF_PREFIX)) {
            String remaining = $ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length());
            String[] parts = remaining.split("/messages/", 2);

            if (parts.length != 2) {
                outStream.println("Invalid channel message $ref format: " + $ref);
                return null;
            }

            String channelName = parts[0];
            String messageName = parts[1];

            if (rawChannels == null) {
                return null;
            }

            AsyncApi30Channel channel = rawChannels.getItem(channelName);
            if (channel == null) {
                outStream.println("Channel '" + channelName + "' not found for message $ref: " + $ref);
                return null;
            }

            Map<String, AsyncApi30Message> channelMessages = channel.getMessages();
            if (channelMessages == null) {
                return null;
            }

            AsyncApi30Message message = channelMessages.get(messageName);
            if (message == null) {
                outStream.println("Message '" + messageName + "' not found in channel '" +
                        channelName + "'");
            }
            return message;
        }

        outStream.println("Unsupported message $ref format: " + $ref);
        return null;
    }
}
