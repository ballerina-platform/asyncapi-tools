/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.generator.ws.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How a client tells apart the messages one connection receives when the spec has no dispatcher key: by a field
 * with a different fixed value in each, or else by a required field only one of them declares. A message neither
 * tells apart is left out.
 *
 * @param routes   the received messages the client can tell apart, in spec order
 * @param leftOut  the received JSON objects nothing tells apart from the others
 * @param routable whether frames on the connection can be routed at all; two kinds of message that aren't JSON
 *                 objects, such as two text messages, can't be told apart
 */
public record ContentRoutes(List<Route> routes, List<IncomingMessages.Received> leftOut, boolean routable) {

    private static final String STRING = "string";
    private static final String OBJECT = "object";
    // A schema included more deeply than this is taken to add no fields, which also ends one that includes itself
    private static final int MAX_DEPTH = 32;

    /**
     * How a received message is recognised: its field {@code field} has the value {@code value}, or, when the value
     * is {@code null}, the field is there at all.
     *
     * @param message the received message
     * @param field   the field to look at
     * @param value   the fixed value the field has, or {@code null} to look only for the field
     */
    public record Route(IncomingMessages.Received message, String field, String value) { }

    /**
     * Works out how the messages received through {@code operations} are told apart.
     *
     * @param spec       the spec the operations are in
     * @param operations the operations on one connection, by operation id
     * @return the routes, and the messages left out
     */
    public static ContentRoutes of(AsyncApiSpec spec, Map<String, AsyncApiOperation> operations) {
        Map<String, AsyncApiSchema> schemas = spec.getAsyncApiComponents().map(AsyncApiComponent::schemas)
                .orElse(Map.of());
        if (schemas == null) {
            schemas = Map.of();
        }
        List<IncomingMessages.Received> objects = new ArrayList<>();
        int others = 0;
        for (IncomingMessages.Received message : IncomingMessages.received(spec, operations)) {
            if (isRoutableRecord(message.payload())) {
                objects.add(message);
            } else {
                others++;
            }
        }
        if (others > 1) {
            return new ContentRoutes(List.of(), objects, false);
        }
        List<Map<String, AsyncApiSchema>> fields = new ArrayList<>();
        List<Set<String>> required = new ArrayList<>();
        for (IncomingMessages.Received message : objects) {
            Map<String, AsyncApiSchema> declared = new LinkedHashMap<>();
            Set<String> requires = new LinkedHashSet<>();
            collect(message.payload(), schemas, declared, requires, 0);
            fields.add(declared);
            required.add(requires);
        }
        String key = keyField(fields, schemas);
        List<Route> routes = new ArrayList<>();
        List<IncomingMessages.Received> leftOut = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            if (key != null) {
                routes.add(new Route(objects.get(i), key, fixedValue(fields.get(i).get(key), schemas)));
                continue;
            }
            String own = ownRequiredField(i, fields, required);
            if (own != null) {
                routes.add(new Route(objects.get(i), own, null));
            } else {
                leftOut.add(objects.get(i));
            }
        }
        return new ContentRoutes(routes, leftOut, true);
    }

    /**
     * Returns whether every connection of {@code spec} that receives more than one kind of message can route at
     * least one of them by content.
     *
     * @param spec the spec
     * @return whether content routing covers the spec
     */
    public static boolean coversAll(AsyncApiSpec spec) {
        for (IncomingMessages.Connection connection : IncomingMessages.connections(spec)) {
            if (IncomingMessages.received(spec, connection.operations()).size() > 1) {
                ContentRoutes routes = of(spec, connection.operations());
                if (!routes.routable() || routes.routes().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // The first field with a different fixed string value in every received object, if one has any
    private static String keyField(List<Map<String, AsyncApiSchema>> fields, Map<String, AsyncApiSchema> schemas) {
        if (fields.isEmpty()) {
            return null;
        }
        for (String name : fields.get(0).keySet()) {
            Set<String> values = new HashSet<>();
            boolean separates = true;
            for (Map<String, AsyncApiSchema> declared : fields) {
                String value = fixedValue(declared.get(name), schemas);
                if (value == null || !values.add(value)) {
                    separates = false;
                    break;
                }
            }
            if (separates) {
                return name;
            }
        }
        return null;
    }

    // A field message i requires that no other received object declares, so a frame with it can only be message i
    private static String ownRequiredField(int i, List<Map<String, AsyncApiSchema>> fields,
                                           List<Set<String>> required) {
        for (String name : required.get(i)) {
            if (!fields.get(i).containsKey(name)) {
                continue;
            }
            boolean own = true;
            for (int j = 0; j < fields.size() && own; j++) {
                own = j == i || !fields.get(j).containsKey(name);
            }
            if (own) {
                return name;
            }
        }
        return null;
    }

    // The fields a schema declares and requires, its own and those of its allOf parts
    private static void collect(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas,
                                Map<String, AsyncApiSchema> declared, Set<String> required, int depth) {
        AsyncApiSchema resolved = resolve(schema, schemas);
        if (resolved == null || depth > MAX_DEPTH) {
            return;
        }
        if (resolved.allOf() != null) {
            for (AsyncApiSchema part : resolved.allOf()) {
                collect(part, schemas, declared, required, depth + 1);
            }
        }
        if (resolved.properties() != null) {
            resolved.properties().forEach(declared::putIfAbsent);
        }
        if (resolved.required() != null) {
            required.addAll(resolved.required());
        }
    }

    private static String fixedValue(AsyncApiSchema field, Map<String, AsyncApiSchema> schemas) {
        AsyncApiSchema resolved = resolve(field, schemas);
        if (resolved == null || resolved.type() != null && !STRING.equals(resolved.type())) {
            return null;
        }
        if (resolved.constValue() != null) {
            return resolved.constValue().isTextual() ? resolved.constValue().asText() : null;
        }
        List<JsonNode> values = resolved.enumValue();
        return values != null && values.size() == 1 && values.get(0) != null && values.get(0).isTextual()
                ? values.get(0).asText() : null;
    }

    private static AsyncApiSchema resolve(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas) {
        return schema != null && schema.name() != null && schemas.containsKey(schema.name())
                ? schemas.get(schema.name()) : schema;
    }

    private static boolean isRoutableRecord(AsyncApiSchema schema) {
        return schema != null && schema.oneOf() == null && !CodegenUtils.isCloseFrameSchema(schema)
                && (schema.type() == null ? schema.properties() != null || schema.allOf() != null
                : OBJECT.equals(schema.type()));
    }
}
