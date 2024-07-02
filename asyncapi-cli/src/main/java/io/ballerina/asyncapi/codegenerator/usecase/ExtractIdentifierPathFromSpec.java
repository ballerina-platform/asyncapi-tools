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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.utils.ExtensionExtractor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Extract the identifier path from the AsyncAPI specification.
 */
public class ExtractIdentifierPathFromSpec implements Extractor {
    private final AsyncApiDocument asyncApiSpec;

    public ExtractIdentifierPathFromSpec(AsyncApiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public String extract() throws BallerinaAsyncApiException {
        JsonNode identifier = ExtensionExtractor.getExtensions(asyncApiSpec)
                .get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER);
        Iterator<Map.Entry<String, JsonNode>> values = identifier.fields();
        HashMap<String, String> valuesMap = new HashMap<>();
        while (values.hasNext()) {
            Map.Entry<String, JsonNode> entry = values.next();
            valuesMap.put(entry.getKey(), entry.getValue().asText());
        }
        StringBuilder eventPathString = new StringBuilder();
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
            String identifierPath = valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH);
            String[] pathParts = identifierPath.split("\\.");
            String prefix = "";
            for (String eventPathPart : pathParts) {
                eventPathString.append(prefix);
                prefix = ".";
                if (Constants.BAL_KEYWORDS.stream()
                        .anyMatch(eventPathPart::equals)) {
                    eventPathString.append("'").append(eventPathPart);
                } else {
                    eventPathString.append(eventPathPart);
                }
            }
        }
        return eventPathString.toString();
    }
}
