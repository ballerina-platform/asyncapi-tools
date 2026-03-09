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
import io.apicurio.datamodels.models.union.BooleanSchemaUnion;
import io.apicurio.datamodels.models.union.SchemaSchemaListUnion;
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

        Object additionalProps = schema.getAdditionalProperties() != null
                ? mapAdditionalProperties(schema.getAdditionalProperties()) : null;
        Object items = schema.getItems() != null ? mapItems(schema.getItems()) : null;
        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> properties =
                (schema.getProperties() != null && !schema.getProperties().isEmpty())
                        ? mapProperties(schema.getProperties()) : null;
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> allOf =
                mapBaseSchemaList(schema.getAllOf(), rawSchemas);
        Map<String, JsonNode> extensions = mapExtensions(schema);
        AsyncApiExternalDocumentation externalDocs = mapExternalDocs(schema);

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
     * Maps an Apicurio {@link BooleanSchemaUnion} (the {@code additionalProperties} field) to either
     * a {@link Boolean} or a {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     *
     * @param additionalPropertiesUnion the union value from {@code schema.getAdditionalProperties()}
     * @return {@link Boolean} when the union holds a boolean, a mapped schema when it holds a schema,
     *         or {@code null} otherwise
     */
    private static Object mapAdditionalProperties(BooleanSchemaUnion additionalPropertiesUnion) {
        if (additionalPropertiesUnion.isBoolean()) {
            return additionalPropertiesUnion.asBoolean();
        } else if (additionalPropertiesUnion.isSchema()
                && additionalPropertiesUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
            return map(typedSchema);
        }
        return null;
    }

    /**
     * Maps an Apicurio {@link SchemaSchemaListUnion} (the {@code items} field) to either a single
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema} or a {@code List} of them.
     *
     * @param itemsUnion the union value from {@code schema.getItems()}
     * @return a single mapped schema, a non-empty {@code List} of mapped schemas, or {@code null}
     */
    private static Object mapItems(SchemaSchemaListUnion itemsUnion) {
        if (itemsUnion.isSchema() && itemsUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
            return map(typedSchema);
        } else if (itemsUnion.isSchemaList()) {
            List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> list =
                    itemsUnion.asSchemaList().stream()
                            .filter(e -> e instanceof AsyncApiSchema)
                            .map(e -> map((AsyncApiSchema) e))
                            .filter(Objects::nonNull)
                            .toList();
            return list.isEmpty() ? null : list;
        }
        return null;
    }

    /**
     * Maps an Apicurio {@code properties} map ({@code Map<String, Schema>}) to a
     * {@code Map<String, }{@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}{@code >}.
     *
     * @param rawProperties the raw properties map from {@code schema.getProperties()}
     * @return the mapped properties map, or {@code null} if no entries could be mapped
     */
    private static Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> mapProperties(
            Map<String, ? extends Schema> rawProperties) {
        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> properties = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Schema> entry : rawProperties.entrySet()) {
            if (entry.getValue() instanceof AsyncApiSchema typedSchema) {
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = map(typedSchema);
                if (mapped != null) {
                    properties.put(entry.getKey(), mapped);
                }
            }
        }
        return properties.isEmpty() ? null : properties;
    }

    /**
     * Extracts the {@code x-*} extension map from a schema that implements
     * {@link AsyncApiExtensible}.
     *
     * @param schema the Apicurio schema object
     * @return the extensions map, or {@code null} if the schema is not extensible
     */
    private static Map<String, JsonNode> mapExtensions(AsyncApiSchema schema) {
        if (schema instanceof AsyncApiExtensible extensible) {
            return extensible.getExtensions();
        }
        return null;
    }

    /**
     * Extracts the {@code externalDocs} field from a schema, casting it to
     * {@link AsyncApiExternalDocumentation} when present.
     *
     * @param schema the Apicurio schema object
     * @return the typed external documentation, or {@code null} if absent or wrong type
     */
    private static AsyncApiExternalDocumentation mapExternalDocs(AsyncApiSchema schema) {
        if (schema.getExternalDocs() instanceof AsyncApiExternalDocumentation typedDoc) {
            return typedDoc;
        }
        return null;
    }

    /**
     * Maps a raw JSON payload {@link ObjectNode} (inline schema) to a
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * Parses JSON Schema fields directly from the Jackson node, including type
     * constraints, validation keywords, compositions ({@code allOf}, {@code oneOf},
     * {@code anyOf}), conditional schemas, and {@code x-*} extensions.
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
        JsonNode defaultValue = objectNode.has("default") ? objectNode.get("default") : null;
        String pattern = objectNode.has("pattern") ? objectNode.get("pattern").asText(null) : null;

        Number multipleOf = objectNode.has("multipleOf") && objectNode.get("multipleOf").isNumber()
                ? objectNode.get("multipleOf").numberValue() : null;
        Number maximum = objectNode.has("maximum") && objectNode.get("maximum").isNumber()
                ? objectNode.get("maximum").numberValue() : null;
        Number exclusiveMaximum = objectNode.has("exclusiveMaximum") && objectNode.get("exclusiveMaximum").isNumber()
                ? objectNode.get("exclusiveMaximum").numberValue() : null;
        Number minimum = objectNode.has("minimum") && objectNode.get("minimum").isNumber()
                ? objectNode.get("minimum").numberValue() : null;
        Number exclusiveMinimum = objectNode.has("exclusiveMinimum") && objectNode.get("exclusiveMinimum").isNumber()
                ? objectNode.get("exclusiveMinimum").numberValue() : null;
        Integer maxLength = objectNode.has("maxLength") && objectNode.get("maxLength").isInt()
                ? objectNode.get("maxLength").intValue() : null;
        Integer minLength = objectNode.has("minLength") && objectNode.get("minLength").isInt()
                ? objectNode.get("minLength").intValue() : null;
        Integer maxItems = objectNode.has("maxItems") && objectNode.get("maxItems").isInt()
                ? objectNode.get("maxItems").intValue() : null;
        Integer minItems = objectNode.has("minItems") && objectNode.get("minItems").isInt()
                ? objectNode.get("minItems").intValue() : null;
        Boolean uniqueItems = objectNode.has("uniqueItems") && objectNode.get("uniqueItems").isBoolean()
                ? objectNode.get("uniqueItems").booleanValue() : null;
        Integer maxProperties = objectNode.has("maxProperties") && objectNode.get("maxProperties").isInt()
                ? objectNode.get("maxProperties").intValue() : null;
        Integer minProperties = objectNode.has("minProperties") && objectNode.get("minProperties").isInt()
                ? objectNode.get("minProperties").intValue() : null;
        Boolean readOnly = objectNode.has("readOnly") && objectNode.get("readOnly").isBoolean()
                ? objectNode.get("readOnly").booleanValue() : null;
        Boolean writeOnly = objectNode.has("writeOnly") && objectNode.get("writeOnly").isBoolean()
                ? objectNode.get("writeOnly").booleanValue() : null;
        Boolean deprecated = objectNode.has("deprecated") && objectNode.get("deprecated").isBoolean()
                ? objectNode.get("deprecated").booleanValue() : null;
        JsonNode constValue = objectNode.has("const") ? objectNode.get("const") : null;

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

        List<JsonNode> examples = null;
        if (objectNode.has("examples") && objectNode.get("examples").isArray()) {
            examples = new ArrayList<>();
            for (JsonNode ex : objectNode.get("examples")) {
                examples.add(ex);
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

        Object additionalProperties = null;
        if (objectNode.has("additionalProperties")) {
            JsonNode apNode = objectNode.get("additionalProperties");
            if (apNode.isBoolean()) {
                additionalProperties = apNode.booleanValue();
            } else {
                additionalProperties = mapFromJsonNode(apNode);
            }
        }

        io.ballerina.asyncapi.core.model.component.AsyncApiSchema ifSchema =
                objectNode.has("if") ? mapFromJsonNode(objectNode.get("if")) : null;
        io.ballerina.asyncapi.core.model.component.AsyncApiSchema thenSchema =
                objectNode.has("then") ? mapFromJsonNode(objectNode.get("then")) : null;
        io.ballerina.asyncapi.core.model.component.AsyncApiSchema elseSchema =
                objectNode.has("else") ? mapFromJsonNode(objectNode.get("else")) : null;
        io.ballerina.asyncapi.core.model.component.AsyncApiSchema notSchema =
                objectNode.has("not") ? mapFromJsonNode(objectNode.get("not")) : null;

        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> allOf =
                mapJsonSchemaArray(objectNode.get("allOf"));
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> oneOf =
                mapJsonSchemaArray(objectNode.get("oneOf"));
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> anyOf =
                mapJsonSchemaArray(objectNode.get("anyOf"));

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
                multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum,
                maxLength, minLength, pattern,
                maxItems, minItems, uniqueItems, maxProperties, minProperties,
                enumValues, constValue, examples, ifSchema, thenSchema, elseSchema,
                readOnly, writeOnly,
                properties, null, additionalProperties, null, items, null, null,
                allOf, oneOf, anyOf, notSchema,
                description, format, defaultValue, null, null, deprecated,
                extensions, null
        );
    }

    /**
     * Maps a JSON array node to a list of
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema} by recursively
     * calling {@link #mapFromJsonNode(JsonNode)} on each element. Used for
     * {@code allOf}, {@code oneOf}, and {@code anyOf}.
     *
     * @param arrayNode the JSON array node; may be {@code null}
     * @return the mapped list, or {@code null} if the node is null, not an array, or all
     *         elements fail to map
     */
    private static List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> mapJsonSchemaArray(
            JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> list = new ArrayList<>();
        for (JsonNode element : arrayNode) {
            io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = mapFromJsonNode(element);
            if (mapped != null) {
                list.add(mapped);
            }
        }
        return list.isEmpty() ? null : list;
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
