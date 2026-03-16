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
 *
 * <p>Union-typed fields that can hold more than one Java type are represented as {@code Object}:
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
 * @param enumValue            Restricts the value to a fixed set of values.
 * @param constValue           Restricts the value to a single constant value.
 * @param examples             Non-validating examples for the schema.
 * @param ifBranch             Conditional validation: if-schema.
 * @param then                 Conditional validation: then-schema.
 * @param elseBranch           Conditional validation: else-schema.
 * @param readOnly             Marks the value as read-only.
 * @param writeOnly            Marks the value as write-only.
 * @param properties           Property definitions for object schemas.
 * @param patternProperties    Schema definitions keyed by a regex pattern.
 * @param additionalProperties {@code Boolean} or {@link AsyncApiSchema}: controls additional
 *                             object properties.
 * @param additionalItems      Validates items beyond those matched by a tuple-form
 *                             {@code items}.
 * @param items                AsyncApiSchema or list of AsyncApiSchema for array items.
 * @param propertyNames        Schema for validating property names of an object.
 * @param contains             Array is valid if at least one item matches this schema.
 * @param allOf                Validates against all of the given sub-schemas.
 * @param oneOf                Validates against exactly one of the given sub-schemas.
 * @param anyOf                Validates against any of the given sub-schemas.
 * @param not                  Validates against the negation of the given schema.
 * @param description          Descriptive text about the schema.
 * @param format               Semantic format hint (e.g. {@code "date-time"}, {@code "email"}).
 * @param defaultValue         Default value to use when the field is absent.
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
        List<JsonNode> enumValue,
        JsonNode constValue,
        List<JsonNode> examples,
        AsyncApiSchema ifBranch,
        AsyncApiSchema then,
        AsyncApiSchema elseBranch,
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
        JsonNode defaultValue,
        String discriminator,
        AsyncApiExternalDocs externalDocs,
        Boolean deprecated,
        Map<String, JsonNode> extensions,
        String name
) {

    /**
     * Returns a new {@link Builder} for constructing an {@link AsyncApiSchema}.
     * All fields default to {@code null}; set only the fields you need.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a minimal stub schema whose only populated field is {@code name}.
     * Used as a fallback when a {@code $ref} cannot be resolved against the raw schemas map,
     * so that code generators can still recover the component key.
     *
     * @param name the component key (e.g. {@code "GenericEventWrapper"})
     * @return a stub {@link AsyncApiSchema} with only {@code name} set
     */
    public static AsyncApiSchema refStub(String name) {
        return builder().name(name).build();
    }

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
                enumValue(), constValue(), examples(), ifBranch(), then(), elseBranch(), readOnly(),
                writeOnly(), properties(), patternProperties(), additionalProperties(),
                additionalItems(), items(), propertyNames(), contains(), allOf(),
                oneOf(), anyOf(), not(), description(), format(), defaultValue(),
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
                enumValue(), constValue(), examples(), ifBranch(), then(), elseBranch(), readOnly(),
                writeOnly(), properties(), patternProperties(), additionalProperties(),
                additionalItems(), items(), propertyNames(), contains(), allOf(),
                oneOf(), anyOf(), not(), description(), format(), defaultValue(),
                discriminator(), externalDocs(), deprecated(), extensions(), newName
        );
    }

    /**
     * Builder for {@link AsyncApiSchema}.
     *
     * <p>All fields default to {@code null}. Set only the fields relevant to the schema
     * being constructed, then call {@link #build()}.
     *
     * <pre>{@code
     * AsyncApiSchema schema = AsyncApiSchema.builder()
     *         .type("object")
     *         .description("A user object")
     *         .properties(props)
     *         .build();
     * }</pre>
     */
    public static final class Builder {

        String title;
        String type;
        List<String> required;
        Number multipleOf;
        Number maximum;
        Number exclusiveMaximum;
        Number minimum;
        Number exclusiveMinimum;
        Integer maxLength;
        Integer minLength;
        String pattern;
        Integer maxItems;
        Integer minItems;
        Boolean uniqueItems;
        Integer maxProperties;
        Integer minProperties;
        List<JsonNode> enumValue;
        JsonNode constValue;
        List<JsonNode> examples;
        AsyncApiSchema ifBranch;
        AsyncApiSchema then;
        AsyncApiSchema elseBranch;
        Boolean readOnly;
        Boolean writeOnly;
        Map<String, AsyncApiSchema> properties;
        Map<String, String> patternProperties;
        Object additionalProperties;
        AsyncApiSchema additionalItems;
        Object items;
        AsyncApiSchema propertyNames;
        AsyncApiSchema contains;
        List<AsyncApiSchema> allOf;
        List<AsyncApiSchema> oneOf;
        List<AsyncApiSchema> anyOf;
        AsyncApiSchema not;
        String description;
        String format;
        JsonNode defaultValue;
        String discriminator;
        AsyncApiExternalDocs externalDocs;
        Boolean deprecated;
        Map<String, JsonNode> extensions;
        String name;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder required(List<String> required) {
            this.required = required;
            return this;
        }

        public Builder multipleOf(Number multipleOf) {
            this.multipleOf = multipleOf;
            return this;
        }

        public Builder maximum(Number maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder exclusiveMaximum(Number exclusiveMaximum) {
            this.exclusiveMaximum = exclusiveMaximum;
            return this;
        }

        public Builder minimum(Number minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder exclusiveMinimum(Number exclusiveMinimum) {
            this.exclusiveMinimum = exclusiveMinimum;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder maxItems(Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder minItems(Integer minItems) {
            this.minItems = minItems;
            return this;
        }

        public Builder uniqueItems(Boolean uniqueItems) {
            this.uniqueItems = uniqueItems;
            return this;
        }

        public Builder maxProperties(Integer maxProperties) {
            this.maxProperties = maxProperties;
            return this;
        }

        public Builder minProperties(Integer minProperties) {
            this.minProperties = minProperties;
            return this;
        }

        public Builder enumValue(List<JsonNode> enumValue) {
            this.enumValue = enumValue;
            return this;
        }

        public Builder constValue(JsonNode constValue) {
            this.constValue = constValue;
            return this;
        }

        public Builder examples(List<JsonNode> examples) {
            this.examples = examples;
            return this;
        }

        public Builder ifBranch(AsyncApiSchema ifBranch) {
            this.ifBranch = ifBranch;
            return this;
        }

        public Builder then(AsyncApiSchema then) {
            this.then = then;
            return this;
        }

        public Builder elseBranch(AsyncApiSchema elseBranch) {
            this.elseBranch = elseBranch;
            return this;
        }

        public Builder readOnly(Boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder writeOnly(Boolean writeOnly) {
            this.writeOnly = writeOnly;
            return this;
        }

        public Builder properties(Map<String, AsyncApiSchema> properties) {
            this.properties = properties;
            return this;
        }

        public Builder patternProperties(Map<String, String> patternProperties) {
            this.patternProperties = patternProperties;
            return this;
        }

        public Builder additionalProperties(Object additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public Builder additionalItems(AsyncApiSchema additionalItems) {
            this.additionalItems = additionalItems;
            return this;
        }

        public Builder items(Object items) {
            this.items = items;
            return this;
        }

        public Builder propertyNames(AsyncApiSchema propertyNames) {
            this.propertyNames = propertyNames;
            return this;
        }

        public Builder contains(AsyncApiSchema contains) {
            this.contains = contains;
            return this;
        }

        public Builder allOf(List<AsyncApiSchema> allOf) {
            this.allOf = allOf;
            return this;
        }

        public Builder oneOf(List<AsyncApiSchema> oneOf) {
            this.oneOf = oneOf;
            return this;
        }

        public Builder anyOf(List<AsyncApiSchema> anyOf) {
            this.anyOf = anyOf;
            return this;
        }

        public Builder not(AsyncApiSchema not) {
            this.not = not;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder defaultValue(JsonNode defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder discriminator(String discriminator) {
            this.discriminator = discriminator;
            return this;
        }

        public Builder externalDocs(AsyncApiExternalDocs externalDocs) {
            this.externalDocs = externalDocs;
            return this;
        }

        public Builder deprecated(Boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder extensions(Map<String, JsonNode> extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Constructs the {@link AsyncApiSchema} from the values set on this builder.
         *
         * @return a new {@link AsyncApiSchema}
         */
        public AsyncApiSchema build() {
            return new AsyncApiSchema(
                    title, type, required,
                    multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum,
                    maxLength, minLength, pattern,
                    maxItems, minItems, uniqueItems, maxProperties, minProperties,
                    enumValue, constValue, examples, ifBranch, then, elseBranch,
                    readOnly, writeOnly,
                    properties, patternProperties, additionalProperties,
                    additionalItems, items, propertyNames, contains,
                    allOf, oneOf, anyOf, not,
                    description, format, defaultValue, discriminator, externalDocs,
                    deprecated, extensions, name
            );
        }
    }
}
