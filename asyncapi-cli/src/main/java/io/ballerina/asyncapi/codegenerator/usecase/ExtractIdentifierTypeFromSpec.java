/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 * Extract the identifier type from the AsyncAPI specification.
 */
public class ExtractIdentifierTypeFromSpec implements Extractor {
    private final AsyncApiDocument asyncApiSpec;

    public ExtractIdentifierTypeFromSpec(AsyncApiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public String extract() throws BallerinaAsyncApiException {
        if (!ExtensionExtractor.getExtensions(asyncApiSpec).containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)) {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    .concat(" attribute is not found in the Async API Specification"));
        }
        JsonNode identifier = ExtensionExtractor.getExtensions(asyncApiSpec)
                .get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER);
        Iterator<Map.Entry<String, JsonNode>> values = identifier.fields();
        HashMap<String, String> valuesMap = new HashMap<>();
        while (values.hasNext()) {
            Map.Entry<String, JsonNode> entry = values.next();
            valuesMap.put(entry.getKey(), entry.getValue().asText());
        }
        if (!valuesMap.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)) {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE
                    .concat(" attribute is not found within the attribute "
                            .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                            .concat(" in the Async API Specification")));
        }
        StringBuilder eventIdentifierType = new StringBuilder("");
        if (valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                .equals(Constants.X_BALLERINA_EVENT_TYPE_HEADER)) {
            // Handle header event path
            eventIdentifierType.append(Constants.X_BALLERINA_EVENT_TYPE_HEADER);
        } else if (valuesMap.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                .equals(Constants.X_BALLERINA_EVENT_TYPE_BODY)) {
            // Handle body event path
            eventIdentifierType.append(Constants.X_BALLERINA_EVENT_TYPE_BODY);
        } else {
            throw new BallerinaAsyncApiException(Constants.X_BALLERINA_EVENT_TYPE_HEADER.concat(" or ")
                    .concat(Constants.X_BALLERINA_EVENT_TYPE_BODY)
                    .concat(" is not provided as the value of ")
                    .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)
                    .concat(" attribute within the attribute "
                            .concat(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)
                            .concat(" in the Async API Specification")));
        }
        return eventIdentifierType.toString();
    }
}
