/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Schema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20SchemaImpl;
import io.apicurio.datamodels.models.asyncapi.v20.io.AsyncApi20ModelReader;
import io.apicurio.datamodels.models.util.JsonUtil;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.MultiChannel;
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.asyncapi.codegenerator.usecase.utils.ExtensionExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extract the service types from the AsyncAPI specification.
 */
public class ExtractChannelsFromSpec implements Extractor {
    private final AsyncApiDocument asyncApiSpec;
    private final CodegenUtils codegenUtils = new CodegenUtils();
    private Map<String, AsyncApiSchema> inlineSchemas;
    public ExtractChannelsFromSpec(AsyncApiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
        this.inlineSchemas = new HashMap<>();
    }

    @Override
    public MultiChannel extract() throws BallerinaAsyncApiException {
        List<ServiceType> serviceTypes = new ArrayList<>();
        AsyncApiChannels channels = asyncApiSpec.getChannels();
        List<String> channelNames = channels.getItemNames();
        List<AsyncApiChannelItem> channelItems = channels.getItems();
        for (int i = 0; i < channelNames.size(); i++) {
            String serviceTypeName;
            List<RemoteFunction> remoteFunctions = new ArrayList<>();
            if (!ExtensionExtractor.getExtensions(channelItems.get(i))
                    .containsKey(Constants.X_BALLERINA_SERVICE_TYPE)) {
                serviceTypeName = codegenUtils.getValidName(channelNames.get(i), true);
            } else {
                serviceTypeName = ExtensionExtractor.getExtensions(channelItems.get(i))
                        .get(Constants.X_BALLERINA_SERVICE_TYPE).asText();
            }
            AsyncApiMessage mainMessage = channelItems.get(i).getSubscribe().getMessage();
            if (mainMessage.getOneOf() != null) {
                for (AsyncApiMessage message : mainMessage.getOneOf()) {
                    validateMessage(channelNames.get(i), channelItems.get(i), message);
                    String xBallerinaEventType = ExtensionExtractor.getExtensions(message)
                            .get(Constants.X_BALLERINA_EVENT_TYPE).asText();
                    RemoteFunction remoteFunction = new RemoteFunction(
                            xBallerinaEventType,
                            getEventType(message, channelNames.get(i), xBallerinaEventType));
                    remoteFunctions.add(remoteFunction);
                }
            } else {
                validateMessage(channelNames.get(i), channelItems.get(i), mainMessage);
                String xBallerinaEventType = ExtensionExtractor.getExtensions(channelItems.get(i)
                                .getSubscribe().getMessage()).get(Constants.X_BALLERINA_EVENT_TYPE).asText();
                RemoteFunction remoteFunction = new RemoteFunction(xBallerinaEventType,
                        getEventType(mainMessage, channelNames.get(i), xBallerinaEventType));
                remoteFunctions.add(remoteFunction);
            }
            ServiceType serviceType = new ServiceType(serviceTypeName, remoteFunctions);
            serviceTypes.add(serviceType);
        }
        return new MultiChannel(serviceTypes, inlineSchemas);
    }

    private String getEventType(AsyncApiMessage message, String channelName, String xBallerinaEventType)
            throws BallerinaAsyncApiException {
        if (!JsonUtil.isPropertyDefined(message.getPayload(), "$ref")) {
            AsyncApi20Schema schemaModel = new AsyncApi20SchemaImpl().createSchema();
            AsyncApi20ModelReader reader = new AsyncApi20ModelReader();
            if (message.getPayload() instanceof ObjectNode) {
                reader.readSchema((ObjectNode) message.getPayload(), schemaModel);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode messagePayLoadObject = mapper.createObjectNode();
                message.getPayload().fields().forEachRemaining(entry ->
                        messagePayLoadObject.set(entry.getKey(), entry.getValue()));
                reader.readSchema(messagePayLoadObject, schemaModel);
            }
            inlineSchemas.put(xBallerinaEventType, schemaModel);
            return xBallerinaEventType;
            //TODO: handle the scenario with both $ref is there directly under the properties
        }
        String ref = JsonUtil.getStringProperty((ObjectNode) message.getPayload(), "$ref");
        String[] refParts = ref.split("/");
        String schemaName = refParts[refParts.length - 1];
        if (asyncApiSpec.getComponents() == null || asyncApiSpec.getComponents().getSchemas() == null
                || !asyncApiSpec.getComponents().getSchemas().containsKey(schemaName)) {
            throw new BallerinaAsyncApiException("Could not find the schema '" + schemaName
                    + "' in the the path #/components/schemas");
        }
        return schemaName;
    }

    private void validateMessage(String channelName, AsyncApiChannelItem channelItem, AsyncApiMessage message)
            throws BallerinaAsyncApiException {
        if (!ExtensionExtractor.getExtensions(message).containsKey(Constants.X_BALLERINA_EVENT_TYPE)) {
            throw new BallerinaAsyncApiException(
                    "Could not find the ".concat(Constants.X_BALLERINA_EVENT_TYPE)
                            .concat(" attribute in the message of the channel ").concat(channelName));
        }
        if (message.getPayload() == null) {
            throw new BallerinaAsyncApiException(
                    "Could not find the payload reference in the message of the channel "
                            .concat(channelName));
        }
    }
}
