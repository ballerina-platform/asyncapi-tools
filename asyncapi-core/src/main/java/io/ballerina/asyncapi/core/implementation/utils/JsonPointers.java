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
package io.ballerina.asyncapi.core.implementation.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads the JSON Pointer part of a local {@code $ref} (RFC 6901), such as {@code #/channels/~1stream}.
 */
public final class JsonPointers {

    private static final String LOCAL = "#/";
    private static final String REF = "$ref";

    private JsonPointers() {
    }

    /**
     * Decodes one pointer token: {@code %xx} first, as a {@code $ref} pointer sits in a URI fragment, then
     * {@code ~1} to {@code /} and {@code ~0} to {@code ~}. A key that holds a literal {@code %xx} is written
     * percent-encoded in the {@code $ref}, such as {@code %2541} for {@code %41}.
     *
     * @param token the token as written in the {@code $ref}
     * @return the key it names
     */
    public static String decode(String token) {
        String unescaped = token;
        if (token.indexOf('%') >= 0) {
            try {
                unescaped = URLDecoder.decode(token.replace("+", "%2B"), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // Not percent-encoded after all, so the % is part of the key
            }
        }
        return unescaped.replace("~1", "/").replace("~0", "~");
    }

    /**
     * Returns the key a {@code $ref} names after {@code prefix}, decoded, such as {@code /stream} for
     * {@code #/channels/~1stream} and prefix {@code #/channels/}.
     *
     * @param ref    the {@code $ref}
     * @param prefix the part before the key
     * @return the decoded key, or {@code null} if {@code ref} does not start with {@code prefix}
     */
    public static String keyAfter(String ref, String prefix) {
        return ref != null && ref.startsWith(prefix) ? decode(ref.substring(prefix.length())) : null;
    }

    /**
     * Finds the node a local {@code $ref} points to, following any {@code $ref} met on the way, so
     * {@code #/channels/ws/messages/ping} also works when {@code channels.ws} is itself a reference.
     *
     * @param root the whole document
     * @param ref  the {@code $ref}
     * @return the node, or {@code null} if {@code ref} is not local or points nowhere; a ref into another file
     *         on the way is returned as is
     */
    public static JsonNode resolve(JsonNode root, String ref) {
        return resolve(root, ref, new HashSet<>());
    }

    private static JsonNode resolve(JsonNode root, String ref, Set<String> visiting) {
        if (ref == null || !ref.startsWith(LOCAL) || !visiting.add(ref)) {
            return null;
        }
        JsonNode node = root;
        for (String token : ref.substring(LOCAL.length()).split("/", -1)) {
            node = follow(root, node, visiting);
            if (node == null) {
                return null;
            }
            node = node.isArray() ? element(node, decode(token)) : field(node, token);
        }
        node = follow(root, node, visiting);
        visiting.remove(ref);
        return node;
    }

    // The decoded key, or the token with only ~ escapes decoded when just that names a field
    private static JsonNode field(JsonNode node, String token) {
        JsonNode value = node.get(decode(token));
        return value != null ? value : node.get(token.replace("~1", "/").replace("~0", "~"));
    }

    // A ref can hold any digits, so an index too large for an int points nowhere
    private static JsonNode element(JsonNode array, String key) {
        return key.matches("\\d{1,9}") ? array.get(Integer.parseInt(key)) : null;
    }

    private static JsonNode follow(JsonNode root, JsonNode node, Set<String> visiting) {
        // A ref into another file ends the walk here, as that file isn't part of this document
        if (node != null && node.isObject() && node.path(REF).isTextual() && node.get(REF).asText().startsWith("#")) {
            return resolve(root, node.get(REF).asText(), visiting);
        }
        return node;
    }
}
