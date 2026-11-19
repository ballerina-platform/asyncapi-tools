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
package io.ballerina.asyncapi.core.implementation.common;

import java.util.List;
import java.util.Locale;

/**
 * The {@code schemaFormat} values whose schemas are read as JSON Schema. A payload in any other format, such as Avro
 * or Protobuf, has no JSON Schema to read, so it is left out rather than misread.
 */
public final class SchemaFormats {

    // AsyncAPI and OpenAPI schemas are JSON Schema with a few keywords of their own
    private static final List<String> JSON_SCHEMA_MEDIA_TYPES = List.of("application/vnd.aai.asyncapi",
            "application/schema+json", "application/schema+yaml", "application/vnd.oai.openapi");

    private SchemaFormats() {
    }

    /**
     * Returns whether a schema in {@code schemaFormat} is read as JSON Schema. No format means the default, an
     * AsyncAPI schema.
     *
     * @param schemaFormat the {@code schemaFormat} of a message or payload, or {@code null}
     * @return whether the schema is JSON Schema
     */
    public static boolean isJsonSchema(String schemaFormat) {
        if (schemaFormat == null || schemaFormat.isBlank()) {
            return true;
        }
        // The media type without its parameters, such as ;version=3.0.0, and its +json or +yaml suffix
        String mediaType = schemaFormat.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return JSON_SCHEMA_MEDIA_TYPES.stream().anyMatch(type -> mediaType.equals(type)
                || mediaType.equals(type + "+json") || mediaType.equals(type + "+yaml"));
    }
}
