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

import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.models.AaiMessage;
import io.apicurio.datamodels.compat.JsonCompat;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extract the service types from the AsyncAPI specification.
 */
public class ExtractServiceTypesFromSpec implements Extractor {
    private final AaiDocument asyncApiSpec;
    private final CodegenUtils codegenUtils = new CodegenUtils();

    public ExtractServiceTypesFromSpec(AaiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public List<ServiceType> extract() throws BallerinaAsyncApiException {
        List<ServiceType> serviceTypes = new ArrayList<>();
        for (Map.Entry<String, AaiChannelItem> channel : asyncApiSpec.channels.entrySet()) {
            String serviceTypeName;
            List<RemoteFunction> remoteFunctions = new ArrayList<>();
            if (channel.getValue().getExtension(Constants.X_BALLERINA_SERVICE_TYPE) == null) {
                serviceTypeName = codegenUtils.getValidName(channel.getKey(), true);
            } else {
                serviceTypeName = channel.getValue()
                        .getExtension(Constants.X_BALLERINA_SERVICE_TYPE).value.toString();
            }
            AaiMessage mainMessage = channel.getValue().subscribe.message;
            if (mainMessage.oneOf != null) {
                for (AaiMessage message : mainMessage.oneOf) {
                    validateMessage(channel, message);
                    RemoteFunction remoteFunction = new RemoteFunction(
                            message.getExtension(Constants.X_BALLERINA_EVENT_TYPE).value.toString(),
                            getEventType(message, channel.getKey()));
                    remoteFunctions.add(remoteFunction);
                }
            } else {
                validateMessage(channel, mainMessage);
                RemoteFunction remoteFunction = new RemoteFunction(channel.getValue()
                        .subscribe.message.getExtension(Constants.X_BALLERINA_EVENT_TYPE).value.toString(),
                        getEventType(mainMessage, channel.getKey()));
                remoteFunctions.add(remoteFunction);
            }
            ServiceType serviceType = new ServiceType(serviceTypeName, remoteFunctions);
            serviceTypes.add(serviceType);
        }
        return serviceTypes;
    }

    private String getEventType(AaiMessage message, String channelName) throws BallerinaAsyncApiException {
        if (!JsonCompat.isPropertyDefined(message.payload, "$ref")) {
            throw new BallerinaAsyncApiException(
                    "Could not find the $ref attribute in the payload of the message of the channel "
                            .concat(channelName));
        }
        String ref = JsonCompat.getPropertyString(message.payload, "$ref");
        String[] refParts = ref.split("/");
        String schemaName = refParts[refParts.length - 1];
        if (!asyncApiSpec.components.schemas.containsKey(schemaName)) {
            throw new BallerinaAsyncApiException("Could not find the schema '" + schemaName
                    + "' in the the path #/components/schemas");
        }
        return schemaName;
    }

    private void validateMessage(Map.Entry<String, AaiChannelItem> channel, AaiMessage message)
            throws BallerinaAsyncApiException {
        if (message.getExtension(Constants.X_BALLERINA_EVENT_TYPE) == null) {
            throw new BallerinaAsyncApiException(
                    "Could not find the ".concat(Constants.X_BALLERINA_EVENT_TYPE)
                            .concat(" attribute in the message of the channel ").concat(channel.getKey()));
        }
        if (message.payload == null) {
            throw new BallerinaAsyncApiException(
                    "Could not find the payload reference in the message of the channel "
                            .concat(channel.getKey()));
        }
    }
}
