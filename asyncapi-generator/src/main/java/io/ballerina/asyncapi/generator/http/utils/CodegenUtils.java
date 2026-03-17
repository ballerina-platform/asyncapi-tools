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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Utility methods for Ballerina identifier and name generation during HTTP code generation.
 */
public final class CodegenUtils {

    public static final String ESCAPE_PATTERN = "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/\\_{}\\s|.$])";
    public static final List<String> BAL_KEYWORDS = SyntaxInfo.keywords();
    private static final String REMOTE_FUNCTION_NAME_PREFIX = "on";
    private static final String SERVICE_TYPE_NAME_SUFFIX = "Service";

    private CodegenUtils() {
    }

    /**
     * Escapes special characters used in method names and identifiers.
     *
     * @param identifier the identifier or method name to escape
     * @return the escaped string
     */
    public static String escapeIdentifier(String identifier) {
        if (identifier.matches("\\S*\\d+\\S*")) {
            return String.format("'%s", identifier);
        }
        if (!identifier.matches("\\b[_a-zA-Z][_a-zA-Z0-9]*\\b") || BAL_KEYWORDS.stream()
                .anyMatch(identifier::equals)) {
            identifier = identifier.replaceAll(ESCAPE_PATTERN, "\\\\$1");
            if (identifier.endsWith("?")) {
                if (identifier.charAt(identifier.length() - 2) == '\\') {
                    StringBuilder stringBuilder = new StringBuilder(identifier);
                    stringBuilder.deleteCharAt(identifier.length() - 2);
                    identifier = stringBuilder.toString();
                }
                if (BAL_KEYWORDS.stream().anyMatch(Optional.of(identifier)
                        .filter(sStr -> sStr.length() != 0)
                        .map(sStr -> sStr.substring(0, sStr.length() - 1))
                        .orElse(identifier)::equals)) {
                    identifier = String.format("'%s", identifier);
                }
            } else if (BAL_KEYWORDS.stream().anyMatch(identifier::equals)) {
                identifier = String.format("'%s", identifier);
            }
        }
        return identifier;
    }

    /**
     * Generates a valid Ballerina name by removing special characters from a function name,
     * record name, or operation ID.
     *
     * @param identifier         the input name to sanitize
     * @param capitalizeFirstChar whether to capitalize the first character
     * @return the sanitized name
     */
    public static String getValidName(String identifier, boolean capitalizeFirstChar) {
        if (!identifier.matches("\\b[0-9]*\\b")) {
            String[] split = identifier.split(ESCAPE_PATTERN);
            StringBuilder validName = new StringBuilder();
            for (String part : split) {
                if (!part.isBlank()) {
                    if (split.length > 1) {
                        part = part.substring(0, 1).toUpperCase(Locale.ENGLISH)
                                + part.substring(1).toLowerCase(Locale.ENGLISH);
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
     * Resolves a {@code $ref} field type. Only local references (starting with {@code #/})
     * are supported.
     *
     * @param referenceVariable the value of the {@code $ref} field
     * @return the resolved reference type name
     * @throws GeneratorException if the reference is not a local reference
     */
    public static String extractReferenceType(String referenceVariable) throws GeneratorException {
        if (referenceVariable.startsWith("#/")) {
            String[] refArray = referenceVariable.split("/");
            return escapeIdentifier(refArray[refArray.length - 1]);
        } else {
            throw new GeneratorException(String.format(
                    "Invalid reference value: %s%nBallerina only supports local reference values.",
                    referenceVariable));
        }
    }

    /**
     * Returns the remote function name for a given event name, prefixed with
     * {@link CodegenUtils#REMOTE_FUNCTION_NAME_PREFIX}.
     *
     * @param eventName the event name as defined in the AsyncAPI document
     * @return the remote function name for the corresponding service type method
     */
    public static String getFunctionNameByEventName(String eventName) {
        return String.format("%s%s", REMOTE_FUNCTION_NAME_PREFIX,
                getValidName(eventName, true));
    }

    /**
     * Returns the service type name for a given channel or service name, appending
     * {@link CodegenUtils#SERVICE_TYPE_NAME_SUFFIX} if not already present.
     *
     * @param serviceName the service name as specified in the AsyncAPI document
     * @return the Ballerina service type name
     */
    public static String getServiceTypeNameByServiceName(String serviceName) {
        if (serviceName.trim().endsWith(SERVICE_TYPE_NAME_SUFFIX)) {
            return getValidName(serviceName.trim(), true);
        }
        return String.format("%s%s", getValidName(serviceName.trim(), true),
                SERVICE_TYPE_NAME_SUFFIX);
    }
}
