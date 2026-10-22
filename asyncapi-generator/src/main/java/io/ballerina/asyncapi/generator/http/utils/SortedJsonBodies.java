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

import java.util.List;
import java.util.regex.Pattern;

/**
 * The {@code $sortedJsonBody} signature input: the JSON body with its top-level keys sorted, written as compact JSON
 * the way JavaScript's {@code JSON.stringify} writes it, with nested values in their received order. Aayu MFT Gateway
 * signs this rather than the body it sends ({@code @aayu-tech/json-sign}).
 */
public final class SortedJsonBodies {

    public static final String SORTED_JSON_BODY_FUNC = "sortedJsonBody";
    public static final String COMPACT_JSON_FUNC = "compactJson";
    // The whole token only, so a longer name that starts with it does not count.
    private static final Pattern TOKEN = Pattern.compile("\\$sortedJsonBody(?![A-Za-z0-9_])|\\{sortedJsonBody}");

    private SortedJsonBodies() {
    }

    /**
     * Whether a signature input uses {@code $sortedJsonBody}.
     *
     * @param input the {@code x-ballerina-auth.signature.input} expression, or {@code null}
     * @return {@code true} if it refers to the sorted JSON body
     */
    public static boolean usesSortedJsonBody(String input) {
        return input != null && TOKEN.matcher(input).find();
    }

    /**
     * The Ballerina source of the two helpers behind the token, as dispatcher methods or as test-file functions.
     *
     * @param qualifiers the function qualifiers, e.g. {@code "private isolated"} or {@code "isolated"}
     * @param receiver   how one helper calls the other, e.g. {@code "self."} for methods or {@code ""} for functions
     * @return the two function definitions
     */
    public static List<String> helperFunctions(String qualifiers, String receiver) {
        String sorted = String.format(
                "%1$s function %2$s(json payload) returns string|error {"
                        + " map<json> fields = check payload.ensureType();"
                        + " string[] members = from string name in fields.keys().sort()"
                        + " select name.toJsonString() + \":\" + %3$s%4$s(fields.get(name));"
                        + " return \"{\" + string:'join(\",\", ...members) + \"}\"; }",
                qualifiers, SORTED_JSON_BODY_FUNC, receiver, COMPACT_JSON_FUNC);
        String compact = String.format(
                "%1$s function %2$s(json value) returns string {"
                        + " if value is map<json> {"
                        + " string[] members = from var [name, member] in value.entries()"
                        + " select name.toJsonString() + \":\" + %3$s%2$s(member);"
                        + " return \"{\" + string:'join(\",\", ...members) + \"}\"; }"
                        + " if value is json[] {"
                        + " string[] items = from json item in value select %3$s%2$s(item);"
                        + " return \"[\" + string:'join(\",\", ...items) + \"]\"; }"
                        + " return value.toJsonString(); }",
                qualifiers, COMPACT_JSON_FUNC, receiver);
        return List.of(sorted, compact);
    }
}
