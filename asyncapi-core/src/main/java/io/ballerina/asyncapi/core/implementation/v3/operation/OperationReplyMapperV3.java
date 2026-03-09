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

import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channels;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReply;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationReplyAddress;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Reference;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReply;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReplyAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio OperationReply models to {@link AsyncApiOperationReply}
 * for AsyncAPI 3.0.
 */
public final class OperationReplyMapperV3 {

    private static final Logger LOG = LogManager.getLogger(OperationReplyMapperV3.class);

    private OperationReplyMapperV3() {

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
    public static AsyncApiOperationReply mapReply(AsyncApi30OperationReply reply, AsyncApi30Channels rawChannels,
            Map<String, AsyncApiChannel> channelsMap, AsyncApi30Components components) {
        if (reply == null) {
            return null;
        }
        return new AsyncApiOperationReply(
                extractChannel(reply, channelsMap),
                mapReplyAddress(reply.getAddress()),
                extractMessages(reply, rawChannels, components),
                reply.getExtensions()
        );
    }

    /**
     * Maps an Apicurio {@link AsyncApi30OperationReplyAddress} to an
     * {@link AsyncApiOperationReplyAddress}.
     *
     * @param address the Apicurio operation reply address object
     * @return the mapped AsyncApiOperationReplyAddress, or null if null
     */
    public static AsyncApiOperationReplyAddress mapReplyAddress(AsyncApi30OperationReplyAddress address) {
        if (address == null) {
            return null;
        }
        return new AsyncApiOperationReplyAddress(
                address.getLocation(),
                address.getDescription(),
                address.getExtensions()
        );
    }

    /**
     * Extracts the channel from an AsyncAPI 3.0 operation reply.
     *
     * @param reply the AsyncAPI 3.0 operation reply object
     * @param channelsMap the map of channel names to AsyncApiChannel objects
     * @return the resolved AsyncApiChannel, or null if not found
     */
    private static AsyncApiChannel extractChannel(AsyncApi30OperationReply reply,
                                                  Map<String, AsyncApiChannel> channelsMap) {

        if (reply == null || channelsMap == null) {
            return null;
        }

        AsyncApi30Reference channelRef = reply.getChannel();
        if (channelRef == null) {
            return null;
        }

        String $ref = channelRef.get$ref();
        if ($ref == null) {
            LOG.warn("Reply channel reference has no $ref. Skipping.");
            return null;
        }

        return OperationMapperV3.resolveChannelRef($ref, channelsMap);
    }

    /**
     * Extracts messages from an AsyncAPI 3.0 operation reply.
     * Note: Returns a Map, unlike operation messages which return a List.
     *
     * @param reply the AsyncAPI 3.0 operation reply object
     * @param rawChannels the raw AsyncAPI 3.0 channels object
     * @param components the AsyncAPI components object
     * @return a map of message names to AsyncApiMessage objects, or null if no messages found
     */
    private static Map<String, AsyncApiMessage> extractMessages(AsyncApi30OperationReply reply,
                                                                AsyncApi30Channels rawChannels,
                                                                AsyncApi30Components components) {

        if (reply == null) {
            return null;
        }

        List<AsyncApi30Reference> messageRefs = reply.getMessages();
        if (messageRefs == null || messageRefs.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiMessage> result = new HashMap<>();
        for (AsyncApi30Reference messageRef : messageRefs) {
            if (messageRef == null) {
                continue;
            }

            String $ref = messageRef.get$ref();
            if ($ref == null) {
                LOG.warn("Reply message reference has no $ref. Skipping.");
                continue;
            }

            // Extract message name from $ref (last segment after last '/')
            String messageName = extractMessageNameFromRef($ref);

            AsyncApi30Message resolved = OperationMapperV3.resolveMessageRef($ref, rawChannels, components);
            if (resolved == null) {
                LOG.warn("Could not resolve message $ref: {}. Skipping.", $ref);
                continue;
            }

            // Guard against chained $refs
            if (resolved instanceof AsyncApiReferenceable resolvedRef && resolvedRef.get$ref() != null) {
                LOG.warn("Resolved message $ref points to another $ref: {}. Skipping.", resolvedRef.get$ref());
                continue;
            }

            AsyncApiMessage mapped = MessageMapperV3.mapMessageItem(resolved, components);
            if (mapped != null) {
                result.put(messageName, mapped);
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Extracts the message name from a $ref string.
     *
     * @param $ref the $ref string
     * @return the message name (last segment after last '/')
     */
    private static String extractMessageNameFromRef(String $ref) {
        int lastSlash = $ref.lastIndexOf('/');
        return lastSlash >= 0 ? $ref.substring(lastSlash + 1) : $ref;
    }
}
