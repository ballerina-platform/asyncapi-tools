/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.NodeFactory;

import java.util.Locale;
import java.util.Optional;

public class CodegenUtils {

    /**
     * This method will escape special characters used in method names and identifiers.
     *
     * @param identifier - identifier or method name
     * @return - escaped string
     */
    public String escapeIdentifier(String identifier) {
        if (!identifier.matches("\\b[_a-zA-Z][_a-zA-Z0-9]*\\b") || Constants.BAL_KEYWORDS.stream()
                .anyMatch(identifier::equals)) {

            // TODO: Remove this `if`. Refer - https://github.com/ballerina-platform/ballerina-lang/issues/23045
            if (identifier.equals("error")) {
                identifier = "_error";
            } else {
                identifier = identifier.replaceAll(Constants.ESCAPE_PATTERN, "\\\\$1");
                if (identifier.endsWith("?")) {
                    if (identifier.charAt(identifier.length() - 2) == '\\') {
                        var stringBuilder = new StringBuilder(identifier);
                        stringBuilder.deleteCharAt(identifier.length() - 2);
                        identifier = stringBuilder.toString();
                    }
                    if (Constants.BAL_KEYWORDS.stream().anyMatch(Optional.of(identifier)
                            .filter(sStr -> sStr.length() != 0)
                            .map(sStr -> sStr.substring(0, sStr.length() - 1))
                            .orElse(identifier)::equals)) {
                        identifier = "'" + identifier;
                    } else {
                        return identifier;
                    }
                } else {
                    identifier = "'" + identifier;
                }
            }
        }
        return identifier;
    }

    /**
     * Generate operationId by removing special characters.
     *
     * @param identifier input function name, record name or operation Id
     * @return string with new generated name
     */
    public String getValidName(String identifier, boolean capitalizeFirstChar) {
        //For the flatten enable we need to remove first Part of valid name check
        // this - > !identifier.matches("\\b[a-zA-Z][a-zA-Z0-9]*\\b") &&
        if (!identifier.matches("\\b[0-9]*\\b")) {
            String[] split = identifier.split(Constants.ESCAPE_PATTERN);
            var validName = new StringBuilder();
            for (String part : split) {
                if (!part.isBlank()) {
                    if (split.length > 1) {
                        part = part.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                                part.substring(1).toLowerCase(Locale.ENGLISH);
                    }
                    validName.append(part);
                }
            }
            identifier = validName.toString();
        }
        if (capitalizeFirstChar) {
            return identifier.substring(0, 1).toUpperCase(Locale.ENGLISH) + identifier.substring(1);
        } else {
            return identifier.substring(0, 1).toLowerCase(Locale.ENGLISH) + identifier.substring(1);
        }
    }

    /**
     * Resolve Ref field types
     * @param referenceVariable value of the ref field
     * @return Resolved reference
     * @throws BallerinaAsyncApiException
     */
    public String extractReferenceType(String referenceVariable) throws BallerinaAsyncApiException {
        if (referenceVariable.startsWith("#") && referenceVariable.contains("/")) {
            String[] refArray = referenceVariable.split("/");
            return escapeIdentifier(refArray[refArray.length - 1]);
        } else {
            throw new BallerinaAsyncApiException("Invalid reference value : " + referenceVariable
                    + "\nBallerina only supports local reference values.");
        }
    }

    /**
     * Create new Minutiae node list which contains a single minutiae
     * @param value value of the minutiae
     * @return minutiae list which has single minutiae
     */
    public MinutiaeList createMinutiae(String value) {
        return NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae(value));
    }

    /**
     * Get remote function name for service type when event name is provided
     * @param eventName event name as defined in async api doc
     * @return remote function name for service types
     */
    public String getFunctionNameByEventName(String eventName) {
        return Constants.REMOTE_FUNCTION_NAME_PREFIX + getValidName(eventName, true);
    }

    /**
     * Get service type name when channel name/service name is provided
     * @param serviceName service name as specified in async api doc
     * @return service type name
     */
    public String getServiceTypeNameByServiceName(String serviceName) {
        return getValidName(serviceName.trim(), true) + Constants.SERVICE_TYPE_NAME_SUFFIX;
    }

}
