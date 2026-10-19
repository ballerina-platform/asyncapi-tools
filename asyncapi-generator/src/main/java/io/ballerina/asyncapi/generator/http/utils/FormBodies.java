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

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Form-encoded webhook bodies ({@code application/x-www-form-urlencoded}), as Twilio sends them: every parameter
 * arrives as a string, so fields the schema types as numbers or booleans are converted before binding.
 */
public final class FormBodies {

    public static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    public static final String SORTED_FORM_PARAMS_FUNC = "sortedFormParams";
    public static final String STRING_TYPE = "string";
    // The whole token only, so a name such as $formParamsDigest does not count.
    private static final Pattern FORM_PARAMS_TOKEN = Pattern.compile("\\$formParams(?![A-Za-z0-9_])|\\{formParams}");

    private FormBodies() {
    }

    /**
     * Whether a content type is the form encoding, ignoring parameters such as {@code charset}.
     *
     * @param contentType a message or default content type, or {@code null}
     * @return {@code true} for {@code application/x-www-form-urlencoded}
     */
    public static boolean isFormContentType(String contentType) {
        return contentType != null
                && FORM_CONTENT_TYPE.equals(contentType.split(";")[0].trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Whether a signature input uses {@code $formParams}.
     *
     * @param input the {@code x-ballerina-auth.signature.input} expression, or {@code null}
     * @return {@code true} if it refers to the sorted form parameters
     */
    public static boolean usesFormParams(String input) {
        return input != null && FORM_PARAMS_TOKEN.matcher(input).find();
    }

    /**
     * The Ballerina type a form value must be converted to for a property schema, the same type the generator
     * gives the record field.
     *
     * @param schema the property schema
     * @return {@code int}, {@code decimal}, {@code float} or {@code boolean}, or {@code null} to keep the string
     */
    public static String conversionFor(AsyncApiSchema schema) {
        if (schema == null || schema.type() == null) {
            return null;
        }
        String format = schema.format();
        boolean floating = "float".equals(format) || "double".equals(format);
        return switch (schema.type()) {
            case "integer" -> "int";
            case "boolean" -> "boolean";
            case "number", "decimal" -> floating ? "float" : "decimal";
            case "float", "double" -> "float";
            default -> null;
        };
    }
}
