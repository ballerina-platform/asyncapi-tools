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
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @param channels   the Apicurio channels object
     * @param components the AsyncAPI components (for $ref resolution in messages and channels)
     * @return the mapped operations map, or an empty map if channels is null or empty
     */
    public static Map<String, AsyncApiOperation> map(AsyncApiChannels channels, AsyncApiComponents components) {
        if (channels == null) {
            return Collections.emptyMap();
        }
        if (!(channels instanceof MappedNode<?> mappedNode)) {
            return Collections.emptyMap();
        }
        List<String> channelNames = mappedNode.getItemNames();
        if (channelNames == null || channelNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AsyncApiOperation> result = new HashMap<>();
        for (String channelName : channelNames) {
            AsyncApiChannelItem channelItem = (AsyncApiChannelItem) mappedNode.getItem(channelName);
            if (channelItem == null) {
                continue;
            }
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation publishOp = channelItem.getPublish();
            if (publishOp != null) {
                String key = channelName + "_publish";
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
                result.put(key, buildOperation(publishOp, AsyncApiOperation.Action.SEND, components));
            }
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation subscribeOp = channelItem.getSubscribe();
            if (subscribeOp != null) {
                String key = channelName + "_subscribe";
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
                result.put(key, buildOperation(subscribeOp, AsyncApiOperation.Action.RECEIVE, components));
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
     * @return the mapped AsyncApiOperation
     */
    private static AsyncApiOperation buildOperation(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            AsyncApiOperation.Action action,
            AsyncApiComponents components) {
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
        if (rawMessage instanceof AsyncApiReferenceable referenceable) {
            String $ref = referenceable.get$ref();
            if ($ref != null) {
                if (!$ref.startsWith(Constants.MESSAGES_REF_PREFIX) || components == null) {
                    rawMessage = null;
                } else {
                    String refName = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
                    Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiMessage> refMessages =
                            components.getMessages();
                    io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                            refMessages != null ? refMessages.get(refName) : null;
                    if (resolved == null
                            || (resolved instanceof AsyncApiReferenceable r && r.get$ref() != null)) {
                        rawMessage = null;
                    } else {
                        rawMessage = resolved;
                    }
                }
            }
        }
        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages = null;
        if (rawMessage != null) {
            io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped = MessageMapperV2.map(rawMessage);
            if (mapped != null) {
                messages = List.of(mapped);
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
                null,
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
}
