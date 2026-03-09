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
package io.ballerina.asyncapi.core.model.component;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;

import java.util.List;
import java.util.Map;

/**
 * Represents a Schema Object in an AsyncAPI document (JSON Schema Draft-07 subset).
 *
 * <p>Fields whose names conflict with Java keywords use a leading underscore
 * (e.g. {@code _enum}, {@code _if}), following the same convention used by the Apicurio library.
 *
 * <p>Union-typed fields that can hold more than one Java type are represented as {@code Object}:
 * <ul>
 *   <li>{@code additionalProperties} holds a {@code Boolean} or an {@link AsyncApiSchema}.</li>
 *   <li>{@code items} holds an {@link AsyncApiSchema} or a {@code List<AsyncApiSchema>}.</li>
 * </ul>
 *
 * @param title                Annotates the schema with a short title (from the JSON Schema
 *                             {@code title} keyword).
 * @param type                 The JSON type (e.g. {@code "object"}, {@code "string"},
 *                             {@code "array"}).
 * @param required             Required property names for object schemas.
 * @param multipleOf           A numeric instance is valid if dividing its value by this yields
 *                             an integer.
 * @param maximum              Inclusive upper bound for numeric values.
 * @param exclusiveMaximum     Exclusive upper bound for numeric values.
 * @param minimum              Inclusive lower bound for numeric values.
 * @param exclusiveMinimum     Exclusive lower bound for numeric values.
 * @param maxLength            Maximum length of a string instance.
 * @param minLength            Minimum length of a string instance.
 * @param pattern              ECMA-262 regular expression the string must match.
 * @param maxItems             Maximum number of items in an array.
 * @param minItems             Minimum number of items in an array.
 * @param uniqueItems          All items in an array instance must be unique.
 * @param maxProperties        Maximum number of properties in an object.
 * @param minProperties        Minimum number of properties in an object.
 * @param _enum                Restricts the value to a fixed set of values.
 * @param _const               Restricts the value to a single constant value.
 * @param examples             Non-validating examples for the schema.
 * @param _if                  Conditional validation: if-schema.
 * @param then                 Conditional validation: then-schema.
 * @param _else                Conditional validation: else-schema.
 * @param readOnly             Marks the value as read-only.
 * @param writeOnly            Marks the value as write-only.
 * @param properties           Property definitions for object schemas.
 * @param patternProperties    Schema definitions keyed by a regex pattern.
 * @param additionalProperties {@code Boolean} or {@link AsyncApiSchema}: controls additional
 *                             object properties.
 * @param additionalItems      Validates items beyond those matched by a tuple-form
 *                             {@code items}.
 * @param propertyNames        Schema for validating property names of an object.
 * @param contains             Array is valid if at least one item matches this schema.
 * @param allOf                Validates against all of the given sub-schemas.
 * @param oneOf                Validates against exactly one of the given sub-schemas.
 * @param anyOf                Validates against any of the given sub-schemas.
 * @param not                  Validates against the negation of the given schema.
 * @param description          Descriptive text about the schema.
 * @param format               Semantic format hint (e.g. {@code "date-time"}, {@code "email"}).
 * @param _default             Default value to use when the field is absent.
 * @param discriminator        Polymorphism discriminator field name.
 * @param externalDocs         Additional external documentation.
 * @param deprecated           Marks the schema as deprecated.
 * @param extensions           Specification extensions (fields prefixed with {@code "x-"}).
 * @param name                 The component key used to identify this schema in
 *                             {@code #/components/schemas}. Not part of JSON Schema; stamped by
 *                             the mapper when a {@code $ref} payload is resolved so that code
 *                             generators can use the component name as the Ballerina type name.
 */
public record AsyncApiSchema(
        String title,
        String type,
        List<String> required,
        Number multipleOf,
        Number maximum,
        Number exclusiveMaximum,
        Number minimum,
        Number exclusiveMinimum,
        Integer maxLength,
        Integer minLength,
        String pattern,
        Integer maxItems,
        Integer minItems,
        Boolean uniqueItems,
        Integer maxProperties,
        Integer minProperties,
        List<JsonNode> _enum,
        JsonNode _const,
        List<JsonNode> examples,
        AsyncApiSchema _if,
        AsyncApiSchema then,
        AsyncApiSchema _else,
        Boolean readOnly,
        Boolean writeOnly,
        Map<String, AsyncApiSchema> properties,
        Map<String, String> patternProperties,
        Object additionalProperties,
        AsyncApiSchema additionalItems,
        Object items,
        AsyncApiSchema propertyNames,
        AsyncApiSchema contains,
        List<AsyncApiSchema> allOf,
        List<AsyncApiSchema> oneOf,
        List<AsyncApiSchema> anyOf,
        AsyncApiSchema not,
        String description,
        String format,
        JsonNode _default,
        String discriminator,
        AsyncApiExternalDocs externalDocs,
        Boolean deprecated,
        Map<String, JsonNode> extensions,
        String name
) {

    /**
     * Returns a copy of this schema with the given {@code title}, preserving all other fields.
     *
     * @param newTitle the title to set
     * @return a new {@link AsyncApiSchema} with the given title
     */
    public AsyncApiSchema withTitle(String newTitle) {
        return new AsyncApiSchema(
                newTitle, type(), required(), multipleOf(), maximum(), exclusiveMaximum(),
                minimum(), exclusiveMinimum(), maxLength(), minLength(), pattern(),
                maxItems(), minItems(), uniqueItems(), maxProperties(), minProperties(),
                _enum(), _const(), examples(), _if(), then(), _else(), readOnly(),
                writeOnly(), properties(), patternProperties(), additionalProperties(),
                additionalItems(), items(), propertyNames(), contains(), allOf(),
                oneOf(), anyOf(), not(), description(), format(), _default(),
                discriminator(), externalDocs(), deprecated(), extensions(), name()
        );
    }

    /**
     * Returns a copy of this schema with the given {@code name}, preserving all other fields.
     * Used by the mapper to stamp the component key onto a resolved payload schema so that
     * code generators can use it as the Ballerina type name.
     *
     * @param newName the component key name to set (e.g. {@code "GenericEventWrapper"})
     * @return a new {@link AsyncApiSchema} with the given name
     */
    public AsyncApiSchema withName(String newName) {
        return new AsyncApiSchema(
                title(), type(), required(), multipleOf(), maximum(), exclusiveMaximum(),
                minimum(), exclusiveMinimum(), maxLength(), minLength(), pattern(),
                maxItems(), minItems(), uniqueItems(), maxProperties(), minProperties(),
                _enum(), _const(), examples(), _if(), then(), _else(), readOnly(),
                writeOnly(), properties(), patternProperties(), additionalProperties(),
                additionalItems(), items(), propertyNames(), contains(), allOf(),
                oneOf(), anyOf(), not(), description(), format(), _default(),
                discriminator(), externalDocs(), deprecated(), extensions(), newName
        );
    }
}
