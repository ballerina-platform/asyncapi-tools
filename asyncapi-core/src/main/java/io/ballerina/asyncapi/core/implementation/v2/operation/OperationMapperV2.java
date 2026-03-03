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
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Message;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Message;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Message;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Message;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Message;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Message;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio Operation models to {@link AsyncApiOperation} for AsyncAPI 2.x.
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
     * @param channels       the Apicurio channels object
     * @param components     the AsyncAPI components (for $ref resolution in messages and channels)
     * @param mappedChannels the already-mapped channel models keyed by channel name,
     *                       used to wire the channel reference onto each operation
     * @return the mapped operations map, or an empty map if channels is null or empty
     */
    public static Map<String, AsyncApiOperation> map(AsyncApiChannels channels, AsyncApiComponents components,
            Map<String, AsyncApiChannel> mappedChannels) {
        if (channels == null) {
            return Collections.emptyMap();
        }
        if (!(channels instanceof MappedNode<?> mappedNode)) {
            return Collections.emptyMap();
        }
        List<String> channelIds = mappedNode.getItemNames();
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AsyncApiOperation> result = new HashMap<>();
        for (String channelId : channelIds) {
            AsyncApiChannelItem channelItem = (AsyncApiChannelItem) mappedNode.getItem(channelId);
            if (channelItem == null) {
                continue;
            }
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation publishOp = channelItem.getPublish();
            if (publishOp != null) {
                String key = channelId + "_publish";
                String operationId = switch (publishOp) {
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
                AsyncApiChannel publishChannel = mappedChannels != null ? mappedChannels.get(channelId) : null;
                result.put(key, mapOperationItem(publishOp, AsyncApiOperation.Action.SEND, components,
                        publishChannel));
            }
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation subscribeOp = channelItem.getSubscribe();
            if (subscribeOp != null) {
                String key = channelId + "_subscribe";
                String operationId = switch (subscribeOp) {
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
                AsyncApiChannel subscribeChannel = mappedChannels != null ? mappedChannels.get(channelId) : null;
                result.put(key, mapOperationItem(subscribeOp, AsyncApiOperation.Action.RECEIVE, components,
                        subscribeChannel));
            }
        }
        return result;
    }

    /**
     * Builds an {@link AsyncApiOperation} from an Apicurio operation object.
     *
     * @param operation  the Apicurio operation object
     * @param action     the action type (SEND or RECEIVE)
     * @param components the AsyncAPI components (for $ref resolution in messages)
     * @param channel    the mapped channel this operation belongs to
     * @return the mapped AsyncApiOperation
     */
    private static AsyncApiOperation mapOperationItem(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            AsyncApiOperation.Action action,
            AsyncApiComponents components,
            AsyncApiChannel channel) {
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
        String refName = null;
        if (rawMessage instanceof AsyncApiReferenceable referenceable) {
            String $ref = referenceable.get$ref();
            if ($ref != null) {
                if ($ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
                    refName = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
                }
                rawMessage = resolveMessageRef($ref, components);
            }
        }
        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages = null;
        if (rawMessage != null) {
            List<? extends io.apicurio.datamodels.models.asyncapi.AsyncApiMessage> oneOf = switch (rawMessage) {
                case AsyncApi26Message typed -> typed.getOneOf();
                case AsyncApi25Message typed -> typed.getOneOf();
                case AsyncApi24Message typed -> typed.getOneOf();
                case AsyncApi23Message typed -> typed.getOneOf();
                case AsyncApi22Message typed -> typed.getOneOf();
                case AsyncApi21Message typed -> typed.getOneOf();
                case AsyncApi20Message typed -> typed.getOneOf();
                default -> null;
            };
            if (oneOf != null && !oneOf.isEmpty()) {
                Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> msgMap = new HashMap<>();
                for (io.apicurio.datamodels.models.asyncapi.AsyncApiMessage msg : oneOf) {
                    io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolvedMsg = msg;
                    String oneOfRefName = null;
                    if (msg instanceof AsyncApiReferenceable ref && ref.get$ref() != null) {
                        String $ref = ref.get$ref();
                        if ($ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
                            oneOfRefName = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
                        }
                        resolvedMsg = resolveMessageRef($ref, components);
                        if (resolvedMsg == null) {
                            continue;
                        }
                    }
                    io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped =
                            MessageMapperV2.mapMessageItem(resolvedMsg, components);
                    if (mapped != null) {
                        String key = resolvedMsg.getName();
                        if (key == null || key.isBlank()) {
                            key = resolvedMsg.getTitle();
                        }
                        if (key == null || key.isBlank()) {
                            key = oneOfRefName;
                        }
                        if (key == null || key.isBlank()) {
                            key = "message_" + msgMap.size();
                        }
                        msgMap.put(key, mapped);
                    }
                }
                messages = msgMap.isEmpty() ? null : msgMap;
            } else {
                io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped =
                        MessageMapperV2.mapMessageItem(rawMessage, components);
                if (mapped != null) {
                    String key = rawMessage.getName();
                    if (key == null || key.isBlank()) {
                        key = rawMessage.getTitle();
                    }
                    if (key == null || key.isBlank()) {
                        key = refName != null ? refName : "message_0";
                    }
                    messages = Map.of(key, mapped);
                }
            }
        }
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
        List<? extends io.apicurio.datamodels.models.asyncapi.AsyncApiOperationTrait> rawTraits =
                operation.getTraits();
        List<io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait> traits =
                (rawTraits == null || rawTraits.isEmpty()) ? null :
                        rawTraits.stream().map(OperationTraitMapperV2::map).toList();
        return new AsyncApiOperation(
                action,
                channel,
                null,
                operation.getSummary(),
                operation.getDescription(),
                messages,
                null,
                null,
                tags,
                ExternalDocMapperV2.map((AsyncApiExternalDocumentation) operation.getExternalDocs()),
                OperationBindingsMapperV2.map(operation.getBindings()),
                traits,
                extensions
        );
    }

    /**
     * Resolves a message $ref through the components map, handling chained references
     * with cycle detection.
     *
     * @param $ref       the $ref string (e.g., "#/components/messages/MyMessage")
     * @param components the AsyncAPI components object
     * @return the resolved message, or null if not found, unsupported format, or cyclic
     */
    private static io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolveMessageRef(
            String $ref, AsyncApiComponents components) {
        if (components == null) {
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.MESSAGES_REF_PREFIX)) {
                return null;
            }
            if (!visited.add(current)) {
                return null; // cyclic reference
            }
            String name = current.substring(Constants.MESSAGES_REF_PREFIX.length());
            Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiMessage> refMessages =
                    components.getMessages();
            io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                    refMessages != null ? refMessages.get(name) : null;
            if (resolved == null) {
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable r && r.get$ref() != null) {
                current = r.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }

}
