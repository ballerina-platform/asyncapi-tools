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
package io.ballerina.asyncapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corrects common 3.x spec mistakes whose meaning is clear, before validation, and reports each change.
 */
final class LenientReads {

    private static final String HOST = "host";
    private static final String PROTOCOL = "protocol";
    private static final String PATHNAME = "pathname";
    private static final String PARAMETERS = "parameters";
    private static final String SCHEMA = "schema";
    private static final String ENUM = "enum";
    private static final String DEFAULT = "default";
    private static final String DESCRIPTION = "description";
    private static final String EXAMPLES = "examples";
    private static final String REF = "$ref";
    private static final String SERVERS = "servers";
    private static final String CHANNELS = "channels";
    private static final String COMPONENTS = "components";
    // An optional scheme, the host, and an optional path
    private static final Pattern HOST_PARTS = Pattern.compile("(?:([A-Za-z][A-Za-z0-9+.-]*)://)?([^/]+)(/.*)?");

    private LenientReads() {
    }

    /**
     * Corrects {@code root} in place.
     *
     * @param root     the whole 3.x document
     * @param warnings receives one message per correction
     */
    static void apply(ObjectNode root, Consumer<SpecWarning> warnings) {
        forEachObject(root.path(SERVERS), (name, server) ->
                splitHost(new Where("server '" + name + "'", pointer(SERVERS, name)), server, warnings));
        forEachObject(root.path(COMPONENTS).path(SERVERS), (name, server) -> splitHost(
                new Where("component server '" + name + "'", pointer(COMPONENTS, SERVERS, name)), server, warnings));
        forEachObject(root.path(CHANNELS), (name, channel) ->
                forEachObject(channel.path(PARAMETERS), (param, value) -> liftSchema(new Where(
                        "channel '" + name + "' parameter '" + param + "'",
                        pointer(CHANNELS, name, PARAMETERS, param)), value, warnings)));
        forEachObject(root.path(COMPONENTS).path(CHANNELS), (name, channel) ->
                forEachObject(channel.path(PARAMETERS), (param, value) -> liftSchema(new Where(
                        "component channel '" + name + "' parameter '" + param + "'",
                        pointer(COMPONENTS, CHANNELS, name, PARAMETERS, param)), value, warnings)));
        forEachObject(root.path(COMPONENTS).path(PARAMETERS), (param, value) -> liftSchema(
                new Where("component parameter '" + param + "'", pointer(COMPONENTS, PARAMETERS, param)),
                value, warnings));
    }

    /**
     * Moves a scheme in {@code host} to {@code protocol} and a path to {@code pathname}, unless the spec sets them.
     */
    private static void splitHost(Where where, ObjectNode server, Consumer<SpecWarning> warnings) {
        JsonNode hostNode = server.get(HOST);
        if (hostNode == null || !hostNode.isTextual()) {
            return;
        }
        String host = hostNode.asText();
        Matcher parts = HOST_PARTS.matcher(host);
        if (!parts.matches() || (parts.group(1) == null && parts.group(3) == null)) {
            return;
        }
        List<String> readAs = new ArrayList<>();
        server.put(HOST, parts.group(2));
        readAs.add("host '" + parts.group(2) + "'");
        String scheme = parts.group(1);
        if (scheme != null && !server.hasNonNull(PROTOCOL)) {
            server.put(PROTOCOL, scheme);
            readAs.add("protocol '" + scheme + "'");
        }
        String path = parts.group(3);
        if (path != null && !"/".equals(path) && !server.hasNonNull(PATHNAME)) {
            server.put(PATHNAME, path);
            readAs.add("pathname '" + path + "'");
        }
        warnings.accept(new SpecWarning(where.pointer() + "/" + HOST,
                String.format("%s host '%s' holds more than the host name; read it as %s", where.name(), host,
                        String.join(", ", readAs)),
                "Put only the host name in 'host', the scheme in 'protocol' and the path in 'pathname'."));
    }

    /**
     * Moves what a 2.x-style parameter {@code schema} says onto the 3.x parameter, whose values are strings.
     */
    private static void liftSchema(Where where, ObjectNode parameter, Consumer<SpecWarning> warnings) {
        if (parameter.has(REF) || !(parameter.get(SCHEMA) instanceof ObjectNode schema)) {
            return;
        }
        parameter.remove(SCHEMA);
        List<String> read = new ArrayList<>();
        if (schema.get(ENUM) instanceof ArrayNode values && !parameter.has(ENUM)) {
            parameter.set(ENUM, asStrings(values));
            read.add(ENUM);
        }
        if (schema.has(DEFAULT) && !parameter.has(DEFAULT)) {
            parameter.put(DEFAULT, asString(schema.get(DEFAULT)));
            read.add(DEFAULT);
        }
        if (schema.get(EXAMPLES) instanceof ArrayNode values && !parameter.has(EXAMPLES)) {
            parameter.set(EXAMPLES, asStrings(values));
            read.add(EXAMPLES);
        }
        if (schema.path(DESCRIPTION).isTextual() && !parameter.has(DESCRIPTION)) {
            parameter.put(DESCRIPTION, schema.get(DESCRIPTION).asText());
            read.add(DESCRIPTION);
        }
        String taken = read.isEmpty() ? "ignored it, as 3.0 parameters are strings"
                : "read its " + String.join(", ", read) + " as the parameter's own";
        warnings.accept(new SpecWarning(where.pointer() + "/" + SCHEMA,
                String.format("%s has a 2.x 'schema'; %s", where.name(), taken),
                "Remove 'schema' and set 'enum', 'default' and 'description' on the parameter itself."));
    }

    private static ArrayNode asStrings(ArrayNode values) {
        ArrayNode strings = values.arrayNode();
        values.forEach(value -> strings.add(asString(value)));
        return strings;
    }

    private static String asString(JsonNode value) {
        return value.isTextual() ? value.asText() : value.toString();
    }

    private static void forEachObject(JsonNode map, ObjectEntry action) {
        map.properties().forEach(entry -> {
            if (entry.getValue() instanceof ObjectNode value) {
                action.accept(entry.getKey(), value);
            }
        });
    }

    private static String pointer(String... keys) {
        StringBuilder pointer = new StringBuilder();
        for (String key : keys) {
            pointer.append('/').append(key.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }

    /**
     * A spec node, as named in a warning and as a JSON Pointer.
     *
     * @param name    how a warning names it, such as {@code server 'prod'}
     * @param pointer its JSON Pointer, such as {@code /servers/prod}
     */
    private record Where(String name, String pointer) {
    }

    private interface ObjectEntry {
        void accept(String name, ObjectNode value);
    }
}
