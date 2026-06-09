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
import io.ballerina.asyncapi.core.Constants;
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

        return io.ballerina.asyncapi.core.model.component.AsyncApiSchema.builder()
                .title(schema.getTitle())
                .type(schema.getType())
                .required(schema.getRequired())
                .multipleOf(schema.getMultipleOf())
                .maximum(schema.getMaximum())
                .exclusiveMaximum(schema.getExclusiveMaximum())
                .minimum(schema.getMinimum())
                .exclusiveMinimum(schema.getExclusiveMinimum())
                .maxLength(schema.getMaxLength())
                .minLength(schema.getMinLength())
                .pattern(schema.getPattern())
                .maxItems(schema.getMaxItems())
                .minItems(schema.getMinItems())
                .uniqueItems(schema.isUniqueItems())
                .maxProperties(schema.getMaxProperties())
                .minProperties(schema.getMinProperties())
                .enumValue(schema.getEnum())
                .constValue(schema.getConst())
                .examples(schema.getExamples())
                .ifBranch(map(schema.getIf()))
                .then(map(schema.getThen()))
                .elseBranch(map(schema.getElse()))
                .readOnly(schema.isReadOnly())
                .writeOnly(schema.isWriteOnly())
                .properties(properties)
                .patternProperties(schema.getPatternProperties())
                .additionalProperties(additionalProps)
                .additionalItems(map(schema.getAdditionalItems()))
                .items(items)
                .propertyNames(map(schema.getPropertyNames()))
                .contains(map(schema.getContains()))
                .allOf(allOf)
                .oneOf(mapAsyncSchemaList(schema.getOneOf()))
                .anyOf(mapAsyncSchemaList(schema.getAnyOf()))
                .not(map(schema.getNot()))
                .description(schema.getDescription())
                .format(schema.getFormat())
                .defaultValue(schema.getDefault())
                .discriminator(schema.getDiscriminator())
                .externalDocs(ExternalDocMapperV2.map(externalDocs))
                .deprecated(schema.isDeprecated())
                .extensions(extensions)
                .build();
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
        if (itemsUnion.isSchema()) {
            Schema itemSchema = itemsUnion.asSchema();
            if (itemSchema instanceof AsyncApiReferenceable refSchema && refSchema.get$ref() != null) {
                String ref = refSchema.get$ref();
                String refName = ref.substring(ref.lastIndexOf('/') + 1);
                return io.ballerina.asyncapi.core.model.component.AsyncApiSchema.refStub(refName);
            } else if (itemSchema instanceof AsyncApiSchema typedSchema) {
                return map(typedSchema);
            }
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
            if (entry.getValue() instanceof AsyncApiReferenceable refSchema && refSchema.get$ref() != null) {
                String ref = refSchema.get$ref();
                String refName = ref.substring(ref.lastIndexOf('/') + 1);
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema stub =
                        io.ballerina.asyncapi.core.model.component.AsyncApiSchema.refStub(refName);
                if (entry.getValue() instanceof AsyncApiSchema typedSchema
                        && typedSchema.getDescription() != null) {
                    stub = stub.withDescription(typedSchema.getDescription());
                }
                properties.put(entry.getKey(), stub);
            } else if (entry.getValue() instanceof AsyncApiSchema typedSchema) {
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

        String title = objectNode.has(Constants.SCHEMA_TITLE) ? objectNode.get(Constants.SCHEMA_TITLE).asText(null)
                : null;
        String type = objectNode.has(Constants.SCHEMA_TYPE) ? objectNode.get(Constants.SCHEMA_TYPE).asText(null)
                : null;
        String format = objectNode.has(Constants.SCHEMA_FORMAT) ? objectNode.get(Constants.SCHEMA_FORMAT).asText(null)
                : null;
        String description = objectNode.has(Constants.SCHEMA_DESCRIPTION)
                ? objectNode.get(Constants.SCHEMA_DESCRIPTION).asText(null) : null;
        JsonNode defaultValue = objectNode.has(Constants.SCHEMA_DEFAULT) ? objectNode.get(Constants.SCHEMA_DEFAULT)
                : null;
        String pattern = objectNode.has(Constants.SCHEMA_PATTERN)
                ? objectNode.get(Constants.SCHEMA_PATTERN).asText(null) : null;

        Number multipleOf = objectNode.has(Constants.SCHEMA_MULTIPLE_OF)
                && objectNode.get(Constants.SCHEMA_MULTIPLE_OF).isNumber()
                ? objectNode.get(Constants.SCHEMA_MULTIPLE_OF).numberValue() : null;
        Number maximum = objectNode.has(Constants.SCHEMA_MAXIMUM) && objectNode.get(Constants.SCHEMA_MAXIMUM).isNumber()
                ? objectNode.get(Constants.SCHEMA_MAXIMUM).numberValue() : null;
        Number exclusiveMaximum = objectNode.has(Constants.SCHEMA_EXCLUSIVE_MAXIMUM)
                && objectNode.get(Constants.SCHEMA_EXCLUSIVE_MAXIMUM).isNumber()
                ? objectNode.get(Constants.SCHEMA_EXCLUSIVE_MAXIMUM).numberValue() : null;
        Number minimum = objectNode.has(Constants.SCHEMA_MINIMUM) && objectNode.get(Constants.SCHEMA_MINIMUM).isNumber()
                ? objectNode.get(Constants.SCHEMA_MINIMUM).numberValue() : null;
        Number exclusiveMinimum = objectNode.has(Constants.SCHEMA_EXCLUSIVE_MINIMUM)
                && objectNode.get(Constants.SCHEMA_EXCLUSIVE_MINIMUM).isNumber()
                ? objectNode.get(Constants.SCHEMA_EXCLUSIVE_MINIMUM).numberValue() : null;
        Integer maxLength = objectNode.has(Constants.SCHEMA_MAX_LENGTH)
                && objectNode.get(Constants.SCHEMA_MAX_LENGTH).isInt()
                ? objectNode.get(Constants.SCHEMA_MAX_LENGTH).intValue() : null;
        Integer minLength = objectNode.has(Constants.SCHEMA_MIN_LENGTH)
                && objectNode.get(Constants.SCHEMA_MIN_LENGTH).isInt()
                ? objectNode.get(Constants.SCHEMA_MIN_LENGTH).intValue() : null;
        Integer maxItems = objectNode.has(Constants.SCHEMA_MAX_ITEMS)
                && objectNode.get(Constants.SCHEMA_MAX_ITEMS).isInt()
                ? objectNode.get(Constants.SCHEMA_MAX_ITEMS).intValue() : null;
        Integer minItems = objectNode.has(Constants.SCHEMA_MIN_ITEMS)
                && objectNode.get(Constants.SCHEMA_MIN_ITEMS).isInt()
                ? objectNode.get(Constants.SCHEMA_MIN_ITEMS).intValue() : null;
        Boolean uniqueItems = objectNode.has(Constants.SCHEMA_UNIQUE_ITEMS)
                && objectNode.get(Constants.SCHEMA_UNIQUE_ITEMS).isBoolean()
                ? objectNode.get(Constants.SCHEMA_UNIQUE_ITEMS).booleanValue() : null;
        Integer maxProperties = objectNode.has(Constants.SCHEMA_MAX_PROPERTIES)
                && objectNode.get(Constants.SCHEMA_MAX_PROPERTIES).isInt()
                ? objectNode.get(Constants.SCHEMA_MAX_PROPERTIES).intValue() : null;
        Integer minProperties = objectNode.has(Constants.SCHEMA_MIN_PROPERTIES)
                && objectNode.get(Constants.SCHEMA_MIN_PROPERTIES).isInt()
                ? objectNode.get(Constants.SCHEMA_MIN_PROPERTIES).intValue() : null;
        Boolean readOnly = objectNode.has(Constants.SCHEMA_READ_ONLY)
                && objectNode.get(Constants.SCHEMA_READ_ONLY).isBoolean()
                ? objectNode.get(Constants.SCHEMA_READ_ONLY).booleanValue() : null;
        Boolean writeOnly = objectNode.has(Constants.SCHEMA_WRITE_ONLY)
                && objectNode.get(Constants.SCHEMA_WRITE_ONLY).isBoolean()
                ? objectNode.get(Constants.SCHEMA_WRITE_ONLY).booleanValue() : null;
        Boolean deprecated = objectNode.has(Constants.SCHEMA_DEPRECATED)
                && objectNode.get(Constants.SCHEMA_DEPRECATED).isBoolean()
                ? objectNode.get(Constants.SCHEMA_DEPRECATED).booleanValue() : null;
        JsonNode constValue = objectNode.has(Constants.SCHEMA_CONST) ? objectNode.get(Constants.SCHEMA_CONST) : null;

        List<String> required = null;
        if (objectNode.has(Constants.SCHEMA_REQUIRED) && objectNode.get(Constants.SCHEMA_REQUIRED).isArray()) {
            required = new ArrayList<>();
            for (JsonNode req : objectNode.get(Constants.SCHEMA_REQUIRED)) {
                required.add(req.asText());
            }
        }

        List<JsonNode> enumValues = null;
        if (objectNode.has(Constants.SCHEMA_ENUM) && objectNode.get(Constants.SCHEMA_ENUM).isArray()) {
            enumValues = new ArrayList<>();
            for (JsonNode val : objectNode.get(Constants.SCHEMA_ENUM)) {
                enumValues.add(val);
            }
        }

        List<JsonNode> examples = null;
        if (objectNode.has(Constants.SCHEMA_EXAMPLES) && objectNode.get(Constants.SCHEMA_EXAMPLES).isArray()) {
            examples = new ArrayList<>();
            for (JsonNode ex : objectNode.get(Constants.SCHEMA_EXAMPLES)) {
                examples.add(ex);
            }
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> properties = null;
        if (objectNode.has(Constants.SCHEMA_PROPERTIES) && objectNode.get(Constants.SCHEMA_PROPERTIES).isObject()) {
            properties = new LinkedHashMap<>();
            final Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> propsRef = properties;
            objectNode.get(Constants.SCHEMA_PROPERTIES).properties().forEach(entry -> {
                JsonNode propNode = entry.getValue();
                if (propNode.has(Constants.SCHEMA_REF)) {
                    String ref = propNode.get(Constants.SCHEMA_REF).asText();
                    String refName = ref.substring(ref.lastIndexOf('/') + 1);
                    io.ballerina.asyncapi.core.model.component.AsyncApiSchema stub =
                            io.ballerina.asyncapi.core.model.component.AsyncApiSchema.refStub(refName);
                    if (propNode.has(Constants.SCHEMA_DESCRIPTION)) {
                        String siblingDescription = propNode.get(Constants.SCHEMA_DESCRIPTION).asText(null);
                        if (siblingDescription != null) {
                            stub = stub.withDescription(siblingDescription);
                        }
                    }
                    propsRef.put(entry.getKey(), stub);
                } else {
                    io.ballerina.asyncapi.core.model.component.AsyncApiSchema propSchema =
                            mapFromJsonNode(propNode);
                    if (propSchema != null) {
                        propsRef.put(entry.getKey(), propSchema);
                    }
                }
            });
            if (properties.isEmpty()) {
                properties = null;
            }
        }

        Object items = null;
        if (objectNode.has(Constants.SCHEMA_ITEMS)) {
            JsonNode itemsNode = objectNode.get(Constants.SCHEMA_ITEMS);
            if (itemsNode.has(Constants.SCHEMA_REF)) {
                String ref = itemsNode.get(Constants.SCHEMA_REF).asText();
                String refName = ref.substring(ref.lastIndexOf('/') + 1);
                items = io.ballerina.asyncapi.core.model.component.AsyncApiSchema.refStub(refName);
            } else {
                items = mapFromJsonNode(itemsNode);
            }
        }

        Object additionalProperties = null;
        if (objectNode.has(Constants.SCHEMA_ADDITIONAL_PROPERTIES)) {
            JsonNode apNode = objectNode.get(Constants.SCHEMA_ADDITIONAL_PROPERTIES);
            if (apNode.isBoolean()) {
                additionalProperties = apNode.booleanValue();
            } else {
                additionalProperties = mapFromJsonNode(apNode);
            }
        }

        Map<String, JsonNode> extensions = null;
        Map<String, JsonNode> ext = new LinkedHashMap<>();
        objectNode.properties().forEach(e -> {
            if (e.getKey().startsWith(Constants.EXTENSION_PREFIX)) {
                ext.put(e.getKey(), e.getValue());
            }
        });
        if (!ext.isEmpty()) {
            extensions = ext;
        }

        return io.ballerina.asyncapi.core.model.component.AsyncApiSchema.builder()
                .title(title)
                .type(type)
                .required(required)
                .multipleOf(multipleOf)
                .maximum(maximum)
                .exclusiveMaximum(exclusiveMaximum)
                .minimum(minimum)
                .exclusiveMinimum(exclusiveMinimum)
                .maxLength(maxLength)
                .minLength(minLength)
                .pattern(pattern)
                .maxItems(maxItems)
                .minItems(minItems)
                .uniqueItems(uniqueItems)
                .maxProperties(maxProperties)
                .minProperties(minProperties)
                .enumValue(enumValues)
                .constValue(constValue)
                .examples(examples)
                .ifBranch(objectNode.has(Constants.SCHEMA_IF) ? mapFromJsonNode(objectNode.get(Constants.SCHEMA_IF))
                        : null)
                .then(objectNode.has(Constants.SCHEMA_THEN)
                        ? mapFromJsonNode(objectNode.get(Constants.SCHEMA_THEN)) : null)
                .elseBranch(objectNode.has(Constants.SCHEMA_ELSE)
                        ? mapFromJsonNode(objectNode.get(Constants.SCHEMA_ELSE)) : null)
                .not(objectNode.has(Constants.SCHEMA_NOT) ? mapFromJsonNode(objectNode.get(Constants.SCHEMA_NOT))
                        : null)
                .readOnly(readOnly)
                .writeOnly(writeOnly)
                .properties(properties)
                .additionalProperties(additionalProperties)
                .items(items)
                .allOf(mapJsonSchemaArray(objectNode.get(Constants.SCHEMA_ALL_OF)))
                .oneOf(mapJsonSchemaArray(objectNode.get(Constants.SCHEMA_ONE_OF)))
                .anyOf(mapJsonSchemaArray(objectNode.get(Constants.SCHEMA_ANY_OF)))
                .description(description)
                .format(format)
                .defaultValue(defaultValue)
                .deprecated(deprecated)
                .extensions(extensions)
                .build();
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
                String refValue = ref.get$ref();
                String refName = refValue.substring(refValue.lastIndexOf('/') + 1);
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
                result.add(io.ballerina.asyncapi.core.model.component.AsyncApiSchema.refStub(refName));
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
