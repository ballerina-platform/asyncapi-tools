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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps Apicurio {@link AsyncApiSchema} to
 * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema},
 * version-independently.
 *
 * <p>Both V2.x and V3.0 schema classes implement the Apicurio {@link AsyncApiSchema} base
 * interface, so this mapper can be shared across all AsyncAPI versions.
 */
public final class SchemaMapper {

    private SchemaMapper() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiSchema} to a
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * Self-referential sub-schema fields are mapped via recursive calls.
     *
     * @param schema the Apicurio schema object
     * @return the mapped AsyncApiSchema, or null if schema is null
     */
    public static io.ballerina.asyncapi.core.model.component.AsyncApiSchema map(AsyncApiSchema schema) {
        return map(schema, null);
    }

    /**
     * Maps an Apicurio {@link AsyncApiSchema} to a
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}, resolving any
     * {@code $ref} entries found in {@code allOf} against the provided {@code rawSchemas} map.
     * Pass {@code null} for {@code rawSchemas} when no resolver is available; {@code $ref}
     * entries will fall back to name-only stubs in that case.
     *
     * @param schema     the Apicurio schema object
     * @param rawSchemas the raw Apicurio schemas map used to resolve {@code allOf} {@code $ref}s,
     *                   or {@code null}
     * @return the mapped AsyncApiSchema, or null if schema is null
     */
    public static io.ballerina.asyncapi.core.model.component.AsyncApiSchema map(
            AsyncApiSchema schema, Map<String, ? extends AsyncApiSchema> rawSchemas) {
        if (schema == null) {
            return null;
        }

        // additionalProperties: BooleanSchemaUnion → Boolean or our AsyncApiSchema
        Object additionalProps = null;
        if (schema.getAdditionalProperties() != null) {
            var bsu = schema.getAdditionalProperties();
            if (bsu.isBoolean()) {
                additionalProps = bsu.asBoolean();
            } else if (bsu.isSchema() && bsu.asSchema() instanceof AsyncApiSchema typedSchema) {
                additionalProps = map(typedSchema);
            }
        }

        // items: SchemaSchemaListUnion → our AsyncApiSchema or List<our AsyncApiSchema>
        Object items = null;
        if (schema.getItems() != null) {
            var ssl = schema.getItems();
            if (ssl.isSchema() && ssl.asSchema() instanceof AsyncApiSchema typedSchema) {
                items = map(typedSchema);
            } else if (ssl.isSchemaList()) {
                List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> list = ssl.asSchemaList().stream()
                                .filter(e -> e instanceof AsyncApiSchema)
                                .map(e -> map((AsyncApiSchema) e)).filter(Objects::nonNull)
                                .toList();
                items = list.isEmpty() ? null : list;
            }
        }

        // properties: Map<String, Schema> → Map<String, our AsyncApiSchema>
        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> properties = null;
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            properties = new LinkedHashMap<>();
            for (Map.Entry<String, ? extends Schema> entry : schema.getProperties().entrySet()) {
                if (entry.getValue() instanceof AsyncApiSchema typedSchema) {
                    io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = map(typedSchema);
                    if (mapped != null) {
                        properties.put(entry.getKey(), mapped);
                    }
                }
            }
            if (properties.isEmpty()) {
                properties = null;
            }
        }

        // allOf: resolve $refs using rawSchemas when available
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> allOf =
                mapBaseSchemaList(schema.getAllOf(), rawSchemas);

        Map<String, JsonNode> extensions = null;
        if (schema instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        AsyncApiExternalDocumentation externalDocs = null;
        if (schema.getExternalDocs() instanceof AsyncApiExternalDocumentation typedDoc) {
            externalDocs = typedDoc;
        }

        return new io.ballerina.asyncapi.core.model.component.AsyncApiSchema(
                schema.getTitle(),
                schema.getType(),
                schema.getRequired(),
                schema.getMultipleOf(),
                schema.getMaximum(),
                schema.getExclusiveMaximum(),
                schema.getMinimum(),
                schema.getExclusiveMinimum(),
                schema.getMaxLength(),
                schema.getMinLength(),
                schema.getPattern(),
                schema.getMaxItems(),
                schema.getMinItems(),
                schema.isUniqueItems(),
                schema.getMaxProperties(),
                schema.getMinProperties(),
                schema.getEnum(),
                schema.getConst(),
                schema.getExamples(),
                map(schema.getIf()),
                map(schema.getThen()),
                map(schema.getElse()),
                schema.isReadOnly(),
                schema.isWriteOnly(),
                properties,
                schema.getPatternProperties(),
                additionalProps,
                map(schema.getAdditionalItems()),
                items,
                map(schema.getPropertyNames()),
                map(schema.getContains()),
                allOf,
                mapAsyncSchemaList(schema.getOneOf()),
                mapAsyncSchemaList(schema.getAnyOf()),
                map(schema.getNot()),
                schema.getDescription(),
                schema.getFormat(),
                schema.getDefault(),
                schema.getDiscriminator(),
                ExternalDocMapperV2.map(externalDocs),
                schema.isDeprecated(),
                extensions,
                null
        );
    }

    /**
     * Maps a raw JSON payload {@link ObjectNode} (inline schema) to a
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * Parses the common JSON Schema fields ({@code type}, {@code properties},
     * {@code required}, {@code items}, {@code enum}, {@code format},
     * {@code description}, {@code title}, and {@code x-*} extensions) directly
     * from the Jackson node.
     *
     * @param node the raw Jackson node representing the inline schema
     * @return the mapped schema, or {@code null} if {@code node} is not an {@link ObjectNode}
     */
    public static io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapFromJsonNode(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return null;
        }

        String title = objectNode.has("title") ? objectNode.get("title").asText(null) : null;
        String type = objectNode.has("type") ? objectNode.get("type").asText(null) : null;
        String format = objectNode.has("format") ? objectNode.get("format").asText(null) : null;
        String description = objectNode.has("description") ? objectNode.get("description").asText(null) : null;

        List<String> required = null;
        if (objectNode.has("required") && objectNode.get("required").isArray()) {
            required = new ArrayList<>();
            for (JsonNode req : objectNode.get("required")) {
                required.add(req.asText());
            }
        }

        List<JsonNode> enumValues = null;
        if (objectNode.has("enum") && objectNode.get("enum").isArray()) {
            enumValues = new ArrayList<>();
            for (JsonNode val : objectNode.get("enum")) {
                enumValues.add(val);
            }
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> properties = null;
        if (objectNode.has("properties") && objectNode.get("properties").isObject()) {
            properties = new LinkedHashMap<>();
            final Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> propsRef = properties;
            objectNode.get("properties").properties().forEach(entry -> {
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema propSchema =
                        mapFromJsonNode(entry.getValue());
                if (propSchema != null) {
                    propsRef.put(entry.getKey(), propSchema);
                }
            });
            if (properties.isEmpty()) {
                properties = null;
            }
        }

        Object items = null;
        if (objectNode.has("items")) {
            items = mapFromJsonNode(objectNode.get("items"));
        }

        Map<String, JsonNode> extensions = null;
        Map<String, JsonNode> ext = new LinkedHashMap<>();
        objectNode.properties().forEach(e -> {
            if (e.getKey().startsWith("x-")) {
                ext.put(e.getKey(), e.getValue());
            }
        });
        if (!ext.isEmpty()) {
            extensions = ext;
        }

        return new io.ballerina.asyncapi.core.model.component.AsyncApiSchema(
                title, type, required,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                enumValues, null, null, null, null, null, null, null,
                properties, null, null, null, items, null, null,
                null, null, null, null,
                description, format, null, null, null, null,
                extensions, null
        );
    }

    /**
     * Maps a list of Apicurio {@link AsyncApiSchema} entries to a list of
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * Used for {@code oneOf} and {@code anyOf}.
     *
     * @param list the Apicurio schema list
     * @return the mapped list, or null if empty
     */
    private static List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> mapAsyncSchemaList(
            List<? extends AsyncApiSchema> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result = list.stream()
                        .map(SchemaMapper::map).filter(Objects::nonNull).toList();
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps a list of base Apicurio {@link Schema} entries (used for {@code allOf}) to a list of
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * When {@code rawSchemas} is provided, {@code $ref} entries are resolved against it and their
     * full schema content is mapped. When {@code rawSchemas} is {@code null} or the ref is not
     * found, a stub schema with only the {@code name} field set is produced as a fallback.
     * Inline schema entries are always mapped normally.
     *
     * @param list       the Apicurio schema list using the base Schema type
     * @param rawSchemas the raw Apicurio schemas map for {@code $ref} resolution, or {@code null}
     * @return the mapped list, or null if empty
     */
    private static List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> mapBaseSchemaList(
            List<? extends Schema> list, Map<String, ? extends AsyncApiSchema> rawSchemas) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result = new ArrayList<>();
        for (Schema s : list) {
            if (s instanceof AsyncApiReferenceable ref && ref.get$ref() != null) {
                String $ref = ref.get$ref();
                String refName = $ref.substring($ref.lastIndexOf('/') + 1);
                if (rawSchemas != null) {
                    AsyncApiSchema refSchema = rawSchemas.get(refName);
                    if (refSchema != null) {
                        io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped =
                                map(refSchema, rawSchemas);
                        if (mapped != null) {
                            result.add(mapped.withName(refName));
                            continue;
                        }
                    }
                }
                // Fallback: stub with name only when rawSchemas is unavailable or ref not found
                result.add(new io.ballerina.asyncapi.core.model.component.AsyncApiSchema(
                        null, null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null,
                        null, null, null, null, null, null,
                        null, refName
                ));
            } else if (s instanceof AsyncApiSchema typedSchema) {
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = map(typedSchema);
                if (mapped != null) {
                    result.add(mapped);
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

}
