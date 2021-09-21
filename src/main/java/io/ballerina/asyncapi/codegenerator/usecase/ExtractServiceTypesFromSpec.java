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
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractServiceTypesFromSpec implements UseCase {
    private final AaiDocument asyncApiSpec;
    private final CodegenUtils codegenUtils = new CodegenUtils();

    public ExtractServiceTypesFromSpec(AaiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public Map<String, List<String>> execute() throws BallerinaAsyncApiException {
        Map<String, List<String>> serviceTypes = new HashMap<>();
        for (Map.Entry<String, AaiChannelItem> channel : asyncApiSpec.channels.entrySet()) {
            List<String> remoteFunctions = new ArrayList<>();
            String serviceType;
            if (channel.getValue().getExtension(Constants.X_BALLERINA_SERVICE_TYPE) == null) {
                serviceType = codegenUtils.getValidName(channel.getKey(), true);
            } else {
                serviceType = channel.getValue().getExtension(Constants.X_BALLERINA_SERVICE_TYPE).value.toString();
            }
            AaiMessage mainMessage = channel.getValue().subscribe.message;
            if (mainMessage.oneOf != null) {
                for(AaiMessage message: mainMessage.oneOf) {
                    if (message.getExtension(Constants.X_BALLERINA_EVENT_TYPE) == null) {
                        throw new BallerinaAsyncApiException(
                                "Could not find the ".concat(Constants.X_BALLERINA_EVENT_TYPE)
                                        .concat(" attribute in the message of the channel ").concat(channel.getKey()));
                    }
                    remoteFunctions.add(
                            message.getExtension(Constants.X_BALLERINA_EVENT_TYPE).value.toString());
                }
            } else {
                if (mainMessage.getExtension(Constants.X_BALLERINA_EVENT_TYPE) == null) {
                    throw new BallerinaAsyncApiException(
                            "Could not find the ".concat(Constants.X_BALLERINA_EVENT_TYPE)
                                    .concat(" attribute in the message of the channel ").concat(channel.getKey()));
                }
                remoteFunctions.add(channel.getValue()
                        .subscribe.message.getExtension(Constants.X_BALLERINA_EVENT_TYPE).value.toString());
            }
            serviceTypes.put(serviceType, remoteFunctions);
        }
        return serviceTypes;
    }
}
