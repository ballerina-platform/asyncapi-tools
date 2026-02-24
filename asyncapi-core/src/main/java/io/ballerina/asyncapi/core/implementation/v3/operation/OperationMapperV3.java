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

import io.apicurio.datamodels.models.MappedNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channels;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Operation;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReply;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReplyAddress;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Operations;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Reference;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.SecuritySchemeMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReply;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReplyAddress;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Operation models to {@link AsyncApiOperation}
 * for AsyncAPI 3.0.
 */
public final class OperationMapperV3 {

    private static final Logger LOG = LogManager.getLogger(OperationMapperV3.class);

    private OperationMapperV3() {

    }

    /**
     * Maps an {@link AsyncApiDocument} to a map of operation IDs to
     * {@link AsyncApiOperation}, dispatching based on the concrete document type.
     * <p>
     * This is the main entry point for AsyncAPI v3 operation mapping, and is
     * designed to be easily extensible for future document versions
     * (e.g., AsyncApi31Document).
     *
     * @param document    the AsyncAPI document
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @return the mapped operations map, or an empty map if the document or its
     * operations are null or unsupported
     */
    public static Map<String, AsyncApiOperation> map(
            AsyncApiDocument document,
            Map<String, AsyncApiChannel> channelsMap) {

        if (document == null) {
            return Map.of();
        }

        return switch (document) {
            case AsyncApi30Document v3Doc -> {
                AsyncApi30Operations operations = v3Doc.getOperations();
                AsyncApiChannels rawChannels = v3Doc.getChannels();
                AsyncApiComponents components = v3Doc.getComponents();
                yield map(operations, rawChannels, channelsMap, components);
            }
            default -> Map.of();
        };
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
            MappedNode<?> operations,
            AsyncApiChannels rawChannels,
            Map<String, AsyncApiChannel> channelsMap,
            AsyncApiComponents components) {
        if (operations == null) {
            return Map.of();
        }
        List<String> operationIds = operations.getItemNames();
        if (operationIds == null || operationIds.isEmpty()) {
            return Map.of();
        }
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();

        for (String operationId : operationIds) {
            Object item = operations.getItem(operationId);
            if (item instanceof AsyncApi30Operation op && rawChannels instanceof AsyncApi30Channels typedChannels) {
                AsyncApiOperation mapped = buildOperation(op, typedChannels, channelsMap, (AsyncApi30Components) components);
                if (mapped != null) {
                    result.put(operationId, mapped);
                }
            }
            // Add additional version branches here as new AsyncAPI 3.x versions are supported
        }
        return result;
    }

    /**
     * Builds an {@link AsyncApiOperation} from an Apicurio operation object.
     * Dispatches by concrete type (AsyncApi30Operation, future AsyncApi31Operation, etc.).
     *
     * @param operation  the Apicurio operation object
     * @param rawChannels the raw AsyncAPI 3.x channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiOperation, or null if operation type is not supported
     */
    public static AsyncApiOperation buildOperation(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            AsyncApiChannels rawChannels,
            Map<String, AsyncApiChannel> channelsMap,
            AsyncApi30Components components) {
        if (operation instanceof AsyncApi30Operation typed) {
            return buildOperationFrom30(typed, (AsyncApi30Channels) rawChannels, channelsMap, components);
        }
        // Add additional version branches here as new AsyncAPI 3.x versions are supported
        return null;
    }

    /**
     * Builds an {@link AsyncApiOperation} from an Apicurio {@link AsyncApi30Operation}.
     *
     * @param operation   the Apicurio AsyncAPI 3.0 operation object
     * @param rawChannels the raw AsyncAPI 3.0 channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiOperation
     */
    private static AsyncApiOperation buildOperationFrom30(
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
        AsyncApiOperation.Action action = mapAction(operation.getAction());
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
                OperationBindingsMapperV3.map(operation.getBindings()),
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
            LOG.warn("Operation channel reference has no $ref. Skipping.");
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
            LOG.warn("Unsupported channel $ref format: {}. Expected {}",
                    $ref, Constants.V3_CHANNELS_REF_PREFIX);
            return null;
        }

        String channelName = $ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length());
        if (channelsMap == null) {
            return null;
        }

        AsyncApiChannel channel = channelsMap.get(channelName);
        if (channel == null) {
            LOG.warn("Channel reference '{}' not found in channels map. Skipping.", channelName);
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
                LOG.warn("Operation message reference has no $ref. Skipping.");
                continue;
            }

            AsyncApi30Message resolved = resolveMessageRef($ref, rawChannels, components);
            if (resolved == null) {
                LOG.warn("Could not resolve message $ref: {}. Skipping.", $ref);
                continue;
            }

            // Guard against chained $refs
            if (resolved instanceof AsyncApiReferenceable resolvedRef
                    && resolvedRef.get$ref() != null) {
                LOG.warn("Resolved message $ref points to another $ref: {}. Skipping.",
                        resolvedRef.get$ref());
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
                LOG.warn("Invalid channel message $ref format: {}", $ref);
                return null;
            }

            String channelName = parts[0];
            String messageName = parts[1];

            if (rawChannels == null) {
                return null;
            }

            AsyncApi30Channel channel = rawChannels.getItem(channelName);
            if (channel == null) {
                LOG.warn("Channel '{}' not found for message $ref: {}", channelName, $ref);
                return null;
            }

            Map<String, AsyncApi30Message> channelMessages = channel.getMessages();
            if (channelMessages == null) {
                return null;
            }

            AsyncApi30Message message = channelMessages.get(messageName);
            if (message == null) {
                LOG.warn("Message '{}' not found in channel '{}'", messageName, channelName);
            }
            return message;
        }

        LOG.warn("Unsupported message $ref format: {}", $ref);
        return null;
    }
}
