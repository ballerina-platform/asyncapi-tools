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
package io.ballerina.asyncapi.cmd;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Finds where a JSON Pointer points in a YAML or JSON definition, so a warning can name the line and column. A
 * pointer the file does not have, for example one that goes through a {@code $ref}, falls back to its longest
 * prefix the file does have.
 */
final class SpecLocator {

    private final Map<String, int[]> positions;

    private SpecLocator(Map<String, int[]> positions) {
        this.positions = positions;
    }

    /**
     * Reads the positions of every member of a definition. A file that can't be read gives no positions.
     *
     * @param spec the definition
     * @return the locator
     */
    static SpecLocator of(Path spec) {
        Map<String, int[]> positions = new HashMap<>();
        String name = spec.getFileName().toString().toLowerCase();
        JsonFactory factory = name.endsWith(".json") ? new JsonFactory() : new YAMLFactory();
        try (JsonParser parser = factory.createParser(Files.readString(spec))) {
            read(parser, positions);
        } catch (IOException | RuntimeException e) {
            // Positions are a help for the reader, so a definition the parser can't read just has none
        }
        return new SpecLocator(positions);
    }

    /**
     * Returns the 1-based line and column a pointer points to.
     *
     * @param pointer a JSON Pointer such as {@code /channels/chat}
     * @return the line and column, or empty if not even a prefix of the pointer is in the file
     */
    Optional<int[]> locate(String pointer) {
        String current = pointer;
        while (!current.isEmpty()) {
            int[] position = positions.get(current);
            if (position != null) {
                return Optional.of(position);
            }
            current = current.substring(0, current.lastIndexOf('/'));
        }
        return Optional.empty();
    }

    // Walks the tokens, keeping the pointer to the current member, and records where each member starts.
    private static void read(JsonParser parser, Map<String, int[]> positions) throws IOException {
        Deque<String> pointers = new ArrayDeque<>();
        Deque<int[]> arrayIndexes = new ArrayDeque<>();
        String pending = "";
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token == JsonToken.FIELD_NAME) {
                pending = pointers.isEmpty() ? "" : pointers.peek();
                pending = pending + "/" + escape(parser.currentName());
                positions.putIfAbsent(pending, position(parser.currentTokenLocation()));
                continue;
            }
            String here = pending;
            if (token != JsonToken.END_ARRAY && !arrayIndexes.isEmpty()
                    && arrayIndexes.peek()[1] == pointers.size()) {
                here = pointers.peek() + "/" + arrayIndexes.peek()[0]++;
                positions.putIfAbsent(here, position(parser.currentTokenLocation()));
            }
            if (token == JsonToken.START_OBJECT) {
                pointers.push(here);
            } else if (token == JsonToken.START_ARRAY) {
                pointers.push(here);
                arrayIndexes.push(new int[]{0, pointers.size()});
            } else if (token == JsonToken.END_OBJECT) {
                pointers.pop();
            } else if (token == JsonToken.END_ARRAY) {
                pointers.pop();
                arrayIndexes.pop();
            }
        }
    }

    private static int[] position(JsonLocation location) {
        return new int[]{location.getLineNr(), location.getColumnNr()};
    }

    private static String escape(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }
}
