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

package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;

import java.util.*;

/**
 * This class contains util functions for Async api object model access
 */
public class VisitorUtils {

    /**
     * Get event name path(Yaml path to event name field) from async api document
     * @param aaiDocument Async API Document
     * @return String event path
     */
    public String getEventNamePath(AaiDocument aaiDocument) {
        List<String> eventPath = new ArrayList<>();
        StringBuilder eventPathString = new StringBuilder("genericEvent");
        String fieldType = aaiDocument.getExtension(Constants.X_BALLERINA_EVENT_FIELD_TYPE).value.toString();
        if (Constants.X_BALLERINA_EVENT_TYPE_HEADER.equals(fieldType)) {
            //TODO: Handle header event path
        } else { // Defaults to BODY
            String eventFieldPath = aaiDocument.getExtension(Constants.X_BALLERINA_EVENT_FIELD).value.toString();
            String [] yamlPathComponents = eventFieldPath.split("/");
            for (String pathComponent: yamlPathComponents) {
                if (!(pathComponent.equals("#") || pathComponent.equals("components") || pathComponent.equals("schemas"))) {
                    eventPath.add(pathComponent);
                }
            }
            for (int i = 1; i < eventPath.size(); i++) { //Omit first element since we've already created records by that name
                String eventPathPart = eventPath.get(i);
                if(i != eventPath.size()) {
                    eventPathString.append(".");
                }
                if(Constants.BAL_KEYWORDS.stream()
                        .anyMatch(eventPathPart::equals)) {
                    eventPathString.append("'" + eventPathPart);
                } else {
                    eventPathString.append(eventPathPart);
                }
            }
        }
        return eventPathString.toString();
    }
}
