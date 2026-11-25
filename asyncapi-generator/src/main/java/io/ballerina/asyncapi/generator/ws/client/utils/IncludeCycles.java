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

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stops a spec whose schemas include each other through {@code allOf}, such as {@code Base} including
 * {@code JoinBody} and {@code JoinBody} including {@code Base}, as a Ballerina record can't include itself.
 *
 * <p>Only {@code allOf} counts: a schema whose property refers back to it, such as a tree node, is fine.
 */
public final class IncludeCycles {

    private IncludeCycles() {
    }

    /**
     * Throws if a component schema includes itself through {@code allOf}, directly or through other schemas.
     *
     * @param spec the parsed spec
     * @throws GeneratorException naming the schemas in the cycle and how to break it
     */
    public static void check(AsyncApiSpec spec) throws GeneratorException {
        Map<String, AsyncApiSchema> schemas = spec.getAsyncApiComponents().map(c -> c.schemas()).orElse(null);
        if (schemas == null) {
            return;
        }
        Map<String, List<String>> includes = new LinkedHashMap<>();
        schemas.forEach((name, schema) -> includes.put(name, included(schema, schemas)));
        Set<String> done = new HashSet<>();
        for (String name : includes.keySet()) {
            List<String> cycle = cycleFrom(name, includes, new ArrayList<>(), done);
            if (cycle != null) {
                throw new GeneratorException(message(cycle));
            }
        }
    }

    // The named schemas an allOf includes, its inline parts' allOf included
    private static List<String> included(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas) {
        List<String> names = new ArrayList<>();
        if (schema == null || schema.allOf() == null) {
            return names;
        }
        for (AsyncApiSchema part : schema.allOf()) {
            if (part.name() != null && schemas.containsKey(part.name())) {
                names.add(part.name());
            } else {
                names.addAll(included(part, schemas));
            }
        }
        return names;
    }

    private static List<String> cycleFrom(String name, Map<String, List<String>> includes, List<String> path,
                                          Set<String> done) {
        int at = path.indexOf(name);
        if (at >= 0) {
            List<String> cycle = new ArrayList<>(path.subList(at, path.size()));
            cycle.add(name);
            return cycle;
        }
        if (done.contains(name)) {
            return null;
        }
        path.add(name);
        for (String next : includes.getOrDefault(name, List.of())) {
            List<String> cycle = cycleFrom(next, includes, path, done);
            if (cycle != null) {
                return cycle;
            }
        }
        path.remove(path.size() - 1);
        done.add(name);
        return null;
    }

    private static String message(List<String> cycle) {
        String first = cycle.get(0);
        String last = cycle.get(cycle.size() - 2);
        String chain = String.join(" -> ", cycle);
        String what = cycle.size() == 2
                ? String.format("schema '%s' includes itself through allOf", first)
                : String.format("schemas %s include each other through allOf", quoted(cycle.subList(0,
                        cycle.size() - 1)));
        String location = last.replace("~", "~0").replace("/", "~1");
        return String.format("%s (%s), and a Ballerina record can't include itself\n"
                + "Remove '%s' from the allOf of '%s' at /components/schemas/%s, and add the fields it needs from "
                + "'%s' to its properties.", what, chain, first, last, location, first);
    }

    private static String quoted(List<String> names) {
        List<String> quoted = new ArrayList<>();
        names.forEach(name -> quoted.add("'" + name + "'"));
        return quoted.size() == 2 ? quoted.get(0) + " and " + quoted.get(1) : String.join(", ", quoted);
    }
}
