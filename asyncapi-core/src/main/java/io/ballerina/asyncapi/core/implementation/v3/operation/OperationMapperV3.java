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
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Operations;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Reference;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.utils.StringUtils;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.SecuritySchemeMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
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
     * {@link AsyncApiOperation}.
     *
     * @param document    the AsyncAPI document
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @return the mapped operations map, or an empty map if the document or its
     * operations are null or unsupported
     */
    public static Map<String, AsyncApiOperation> map(AsyncApiDocument document,
                                                     Map<String, AsyncApiChannel> channelsMap) {
        if (!(document instanceof AsyncApi30Document v3Doc)) {
            return Collections.emptyMap();
        }
        AsyncApi30Operations operations = v3Doc.getOperations();
        if (operations == null) {
            return Collections.emptyMap();
        }
        List<String> operationIds = operations.getItemNames();
        if (operationIds == null || operationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        AsyncApi30Channels rawChannels = v3Doc.getChannels() instanceof AsyncApi30Channels c ? c : null;
        AsyncApi30Components components = v3Doc.getComponents() instanceof AsyncApi30Components c ? c : null;
        Map<String, AsyncApiOperation> result = new HashMap<>();
        for (String operationId : operationIds) {
            Object item = operations.getItem(operationId);
            if (item instanceof AsyncApi30Operation op) {
                AsyncApiOperation mapped = mapOperationItem(op, rawChannels, channelsMap, components);
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
     *
     * @param operation   the Apicurio operation object
     * @param rawChannels the raw AsyncAPI 3.x channels object (for message $ref resolution)
     * @param channelsMap the map of channel names to resolved AsyncApiChannel objects
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiOperation, or null if operation type is not supported
     */
    public static AsyncApiOperation mapOperationItem(io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            AsyncApiChannels rawChannels, Map<String, AsyncApiChannel> channelsMap, AsyncApi30Components components) {
        if (!(operation instanceof AsyncApi30Operation typed)) {
            // Add additional version branches here as new AsyncAPI 3.x versions are supported
            return null;
        }
        AsyncApi30Channels typedChannels = rawChannels instanceof AsyncApi30Channels c ? c : null;

        String actionStr = typed.getAction();
        AsyncApiOperation.Action action = null;
        if (actionStr != null) {
            action = switch (actionStr.toLowerCase()) {
                case "send" -> AsyncApiOperation.Action.SEND;
                case "receive" -> AsyncApiOperation.Action.RECEIVE;
                default -> null;
            };
        }

        AsyncApiChannel channel = null;
        String channelId = null;
        AsyncApi30Reference channelRef = typed.getChannel();
        if (channelRef != null) {
            String ref = channelRef.get$ref();
            if (ref == null) {
                LOG.warn("Operation channel reference has no $ref. Skipping.");
            } else {
                channel = resolveChannelRef(ref, channelsMap);
                if (ref.startsWith(Constants.V3_CHANNELS_REF_PREFIX)) {
                    channelId = StringUtils.toPascalCase(ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length()));
                }
            }
        }

        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages = null;
        List<AsyncApi30Reference> messageRefs = typed.getMessages();
        if (messageRefs != null && !messageRefs.isEmpty()) {
            Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> msgMap = new HashMap<>();
            for (AsyncApi30Reference messageRef : messageRefs) {
                if (messageRef == null) {
                    continue;
                }
                String ref = messageRef.get$ref();
                if (ref == null) {
                    LOG.warn("Operation message reference has no $ref. Skipping.");
                    continue;
                }
                String messageName = ref.substring(ref.lastIndexOf('/') + 1);
                AsyncApi30Message resolved = resolveMessageRef(ref, typedChannels, components);
                if (resolved == null) {
                    LOG.warn("Could not resolve message $ref: {}. Skipping.", ref);
                    continue;
                }
                if (resolved instanceof AsyncApiReferenceable resolvedRef && resolvedRef.get$ref() != null) {
                    LOG.warn("Resolved message $ref points to another $ref: {}. Skipping.",
                            resolvedRef.get$ref());
                    continue;
                }
                io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped =
                        MessageMapperV3.mapMessageItem(resolved, components);
                if (mapped != null) {
                    msgMap.put(messageName, mapped);
                }
            }
            messages = msgMap.isEmpty() ? null : msgMap;
        }

        List<AsyncApiTag> tags = null;
        if (typed.getTags() != null) {
            tags = typed.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, components))
                    .toList();
        }
        return new AsyncApiOperation(
                action,
                channelId,
                channel,
                typed.getTitle(),
                typed.getSummary(),
                typed.getDescription(),
                messages,
                SecuritySchemeMapperV3.map(typed.getSecurity(), components),
                OperationReplyMapperV3.mapReply(typed.getReply(), typedChannels, channelsMap, components),
                tags,
                ExternalDocMapperV3.map((AsyncApiExternalDocumentation) typed.getExternalDocs(), components),
                OperationBindingsMapperV3.map(typed.getBindings()),
                OperationTraitMapperV3.mapTraits(typed.getTraits(), components),
                typed.getExtensions()
        );
    }

    /**
     * Resolves a $ref to a channel.
     *
     * @param ref the $ref string (e.g., "#/channels/channelName")
     * @param channelsMap the map of channel names to AsyncApiChannel objects
     * @return the resolved channel, or null if not found or invalid
     */
    static AsyncApiChannel resolveChannelRef(String ref, Map<String, AsyncApiChannel> channelsMap) {
        if (!ref.startsWith(Constants.V3_CHANNELS_REF_PREFIX)) {
            LOG.warn("Unsupported channel $ref format: {}. Expected {}",
                    ref, Constants.V3_CHANNELS_REF_PREFIX);
            return null;
        }
        String channelName = ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length());
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
     * Resolves a $ref to a message in either channels or components.
     *
     * @param ref the $ref string (e.g., "#/channels/channelName/messages/messageName"
     *             or "#/components/messages/messageName")
     * @param rawChannels the raw AsyncAPI 3.0 channels object
     * @param components the AsyncAPI components object
     * @return the resolved message, or null if not found or invalid
     */
    static AsyncApi30Message resolveMessageRef(String ref, AsyncApi30Channels rawChannels,
                                               AsyncApi30Components components) {
        if (ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
            String name = ref.substring(Constants.MESSAGES_REF_PREFIX.length());
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

        if (ref.startsWith(Constants.V3_CHANNELS_REF_PREFIX)) {
            String remaining = ref.substring(Constants.V3_CHANNELS_REF_PREFIX.length());
            String[] parts = remaining.split("/messages/", 2);
            if (parts.length != 2) {
                LOG.warn("Invalid channel message $ref format: {}", ref);
                return null;
            }
            String channelName = parts[0];
            String messageName = parts[1];
            if (rawChannels == null) {
                return null;
            }
            AsyncApi30Channel channel = rawChannels.getItem(channelName);
            if (channel == null) {
                LOG.warn("Channel '{}' not found for message $ref: {}", channelName, ref);
                return null;
            }
            Map<String, AsyncApi30Message> channelMessages = channel.getMessages();
            if (channelMessages == null) {
                return null;
            }
            AsyncApi30Message message = channelMessages.get(messageName);
            if (message == null) {
                LOG.warn("Message '{}' not found in channel '{}'", messageName, channelName);
                return null;
            }
            if (message instanceof AsyncApiReferenceable refMsg && refMsg.get$ref() != null) {
                return resolveMessageRef(refMsg.get$ref(), rawChannels, components);
            }
            return message;
        }

        LOG.warn("Unsupported message $ref format: {}", ref);
        return null;
    }
}
