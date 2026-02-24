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
package io.ballerina.asyncapi.core.implementation.v2.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.MappedNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Operation;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Operation;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Operation;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Operation;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Operation;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Operation;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Operation;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageRefResolverV2;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Operation models to {@link AsyncApiOperation} for AsyncAPI 2.x.
 *
 */
public final class OperationMapperV2 {

    private OperationMapperV2() {

    }

    /**
     * Extracts operations from AsyncAPI 2.x channels. Each channel item may
     * have a publish and/or subscribe operation, which are mapped to
     * {@link AsyncApiOperation.Action#SEND} and
     * {@link AsyncApiOperation.Action#RECEIVE} respectively.
     *
     * @param channels   the Apicurio channels object
     * @param components the AsyncAPI components (for $ref resolution in messages and channels)
     * @return the mapped operations map, or an empty map if channels is null or empty
     */
    public static Map<String, AsyncApiOperation> map(
            AsyncApiChannels channels,
            AsyncApiComponents components) {
        if (channels == null) {
            return Map.of();
        }

        List<String> channelNames = null;
        if (channels instanceof MappedNode<?> mappedNode) {
            channelNames = mappedNode.getItemNames();
        }

        if (channelNames == null || channelNames.isEmpty()) {
            return Map.of();
        }

        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (String channelName : channelNames) {
            AsyncApiChannelItem channelItem = null;
            if (channels instanceof MappedNode<?> mappedNode) {
                channelItem = (AsyncApiChannelItem) mappedNode.getItem(channelName);
            }
            if (channelItem == null) {
                continue;
            }
            extractOperation(result, channelName, channelItem.getPublish(),
                    AsyncApiOperation.Action.SEND, "publish", components);
            extractOperation(result, channelName, channelItem.getSubscribe(),
                    AsyncApiOperation.Action.RECEIVE, "subscribe", components);
        }
        return result;
    }

    /**
     * Extracts a single operation from a channel item and adds it to the result map.
     *
     * @param result      the result map to populate
     * @param channelName the channel name
     * @param operation   the Apicurio operation (publish or subscribe), may be null
     * @param action      the action type (SEND or RECEIVE)
     * @param suffix      the suffix for the fallback key ("publish" or "subscribe")
     * @param components  the AsyncAPI components (for $ref resolution)
     */
    private static void extractOperation(Map<String, AsyncApiOperation> result,
                                         String channelName,
                                         io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
                                         AsyncApiOperation.Action action, String suffix,
                                         AsyncApiComponents components) {
        if (operation == null) {
            return;
        }
        String key = channelName + "_" + suffix;
        String operationId = switch (operation) {
            case AsyncApi26Operation typed -> typed.getOperationId();
            case AsyncApi25Operation typed -> typed.getOperationId();
            case AsyncApi24Operation typed -> typed.getOperationId();
            case AsyncApi23Operation typed -> typed.getOperationId();
            case AsyncApi22Operation typed -> typed.getOperationId();
            case AsyncApi21Operation typed -> typed.getOperationId();
            case AsyncApi20Operation typed -> typed.getOperationId();
            default -> null;
        };
        if (operationId != null) {
            key = operationId;
        }
        result.put(key, buildOperation(operation, action, components));
    }

    /**
     * Builds an {@link AsyncApiOperation} from an Apicurio operation object.
     *
     * @param operation  the Apicurio operation object
     * @param action     the action type (SEND or RECEIVE)
     * @param components the AsyncAPI components (for $ref resolution in messages)
     * @return the mapped AsyncApiOperation
     */
    private static AsyncApiOperation buildOperation(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            AsyncApiOperation.Action action,
            AsyncApiComponents components) {
        Map<String, JsonNode> extensions = null;
        if (operation instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        List<AsyncApiTag> tags = null;
        if (operation.getTags() != null) {
            tags = operation.getTags().stream()
                    .map(TagMapperV2::map)
                    .toList();
        }
        return new AsyncApiOperation(
                action,
                null, // channel
                null, // title (v2 has none)
                operation.getSummary(),
                operation.getDescription(),
                extractOperationMessages(operation, components),
                null, // security (v2 operations have none)
                null, // reply (v2 has none)
                tags,
                ExternalDocMapperV2.map((AsyncApiExternalDocumentation) operation.getExternalDocs()),
                OperationBindingsMapperV2.map(operation.getBindings()),
                OperationTraitMapperV2.mapTraits(operation.getTraits()),
                extensions
        );
    }

    /**
     * Extracts messages from an AsyncAPI 2.x operation.
     * In v2, each operation has at most one message (accessed via getMessage()).
     * The message can be inline or a $ref to components/messages.
     *
     * @param operation  the Apicurio operation object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return a list containing the mapped message, or null if no message
     */
    private static List<io.ballerina.asyncapi.core.model.message.AsyncApiMessage>
    extractOperationMessages(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            io.apicurio.datamodels.models.asyncapi.AsyncApiComponents components) {

        if (operation == null) {
            return null;
        }

        // Get the single message from the operation (version-specific)
        io.apicurio.datamodels.models.asyncapi.AsyncApiMessage rawMessage = switch (operation) {
            case AsyncApi26Operation typed -> typed.getMessage();
            case AsyncApi25Operation typed -> typed.getMessage();
            case AsyncApi24Operation typed -> typed.getMessage();
            case AsyncApi23Operation typed -> typed.getMessage();
            case AsyncApi22Operation typed -> typed.getMessage();
            case AsyncApi21Operation typed -> typed.getMessage();
            case AsyncApi20Operation typed -> typed.getMessage();
            default -> null;
        };

        if (rawMessage == null) {
            return null;
        }

        // Handle $ref resolution (same pattern as ChannelMapperV2.extractMessagesFromOperation)
        if (rawMessage instanceof AsyncApiReferenceable referenceable) {
            String $ref = referenceable.get$ref();
            if ($ref != null) {
                io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                        MessageRefResolverV2.resolveMessageRef($ref, components);
                if (resolved == null) {
                    // Message $ref could not be resolved, return null
                    return null;
                }

                // Guard against chained $refs
                if (resolved instanceof AsyncApiReferenceable resolvedRef
                        && resolvedRef.get$ref() != null) {
                    // Chained $refs not supported
                    return null;
                }

                // Use the resolved message
                rawMessage = resolved;
            }
        }

        // Map using MessageMapperV2
        io.ballerina.asyncapi.core.model.message.AsyncApiMessage mappedMessage =
                MessageMapperV2.map(rawMessage);

        return mappedMessage != null ? List.of(mappedMessage) : null;
    }
}
