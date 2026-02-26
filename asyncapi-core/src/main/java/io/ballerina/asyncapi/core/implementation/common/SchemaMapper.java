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
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;

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

        // allOf: List<Schema> — filter to AsyncApiSchema before mapping
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> allOf = mapBaseSchemaList(schema.getAllOf());

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
                extensions
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
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema},
     * filtering out non-{@link AsyncApiSchema} entries.
     *
     * @param list the Apicurio schema list using the base Schema type
     * @return the mapped list, or null if empty
     */
    private static List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> mapBaseSchemaList(
            List<? extends Schema> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result = list.stream()
                        .filter(s -> s instanceof AsyncApiSchema)
                        .map(s -> map((AsyncApiSchema) s))
                        .filter(Objects::nonNull).toList();
        return result.isEmpty() ? null : result;
    }

}
