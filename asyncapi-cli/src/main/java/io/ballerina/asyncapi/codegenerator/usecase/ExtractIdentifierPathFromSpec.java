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

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.core.models.Extension;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;

import java.util.HashMap;

/**
 * Extract the identifier path from the AsyncAPI specification.
 */
public class ExtractIdentifierPathFromSpec implements Extractor {
    private final AaiDocument asyncApiSpec;

    public ExtractIdentifierPathFromSpec(AaiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public String extract() throws BallerinaAsyncApiException {
        if (asyncApiSpec.getExtension(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER) == null) {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    .concat(" attribute is not found in the Async API Specification"));
        }
        Extension identifier = asyncApiSpec.getExtension(
                Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER);
        HashMap<String, String> valuesMap = (HashMap<String, String>) identifier.value;
        if (!valuesMap.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)) {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE
                    .concat(" attribute is not found within the attribute "
                            .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                            .concat(" in the Async API Specification")));
        }
        StringBuilder eventPathString = new StringBuilder("");
        if (valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                .equals(Constants.X_BALLERINA_EVENT_TYPE_HEADER)) {
            // Handle header event path
            if (!valuesMap.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME)) {
                throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME
                        .concat(" attribute is not found within the attribute "
                                .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                                .concat(" in the Async API Specification")));
            }
            String identifierName = valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME);
            if (Constants.BAL_KEYWORDS.stream()
                    .anyMatch(identifierName::equals)) {
                eventPathString.append("'").append(identifierName);
            } else {
                eventPathString.append(identifierName);
            }
        } else if (valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                .equals(Constants.X_BALLERINA_EVENT_TYPE_BODY)) {
            // Handle body event path
            if (!valuesMap.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH)) {
                throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH
                        .concat(" attribute is not found within the attribute "
                                .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                                .concat(" in the Async API Specification")));
            }
            eventPathString.append(Constants.CLONE_WITH_TYPE_VAR_NAME);
            String identifierPath = valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH);
            String[] pathParts = identifierPath.split("\\.");
            for (String eventPathPart : pathParts) {
                eventPathString.append(".");
                if (Constants.BAL_KEYWORDS.stream()
                        .anyMatch(eventPathPart::equals)) {
                    eventPathString.append("'").append(eventPathPart);
                } else {
                    eventPathString.append(eventPathPart);
                }
            }
        } else {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_TYPE_HEADER.concat(" or ")
                    .concat(Constants.X_BALLERINA_EVENT_TYPE_BODY)
                    .concat(" is not provided as the value of ")
                    .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                    .concat(" attribute within the attribute "
                            .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                            .concat(" in the Async API Specification")));
        }
        return eventPathString.toString();
    }
}
