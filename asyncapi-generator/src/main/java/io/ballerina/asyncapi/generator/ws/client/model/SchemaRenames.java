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
package io.ballerina.asyncapi.generator.ws.client.model;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renames component schemas, and every reference to them, inside schemas and message payloads.
 */
final class SchemaRenames {

    private final Map<String, String> renames;

    /**
     * @param renames old schema name to new schema name
     */
    SchemaRenames(Map<String, String> renames) {
        this.renames = renames;
    }

    boolean isEmpty() {
        return renames.isEmpty();
    }

    /**
     * Returns {@code schemas} with the renamed keys, in the same order, and each schema's references renamed.
     */
    Map<String, AsyncApiSchema> schemas(Map<String, AsyncApiSchema> schemas) {
        Map<String, AsyncApiSchema> result = new LinkedHashMap<>();
        schemas.forEach((name, schema) -> result.put(renames.getOrDefault(name, name), schema(schema)));
        return result;
    }

    /**
     * Returns {@code message} with the references in its payload and headers renamed.
     */
    AsyncApiMessage message(AsyncApiMessage m) {
        if (m == null || isEmpty()) {
            return m;
        }
        return new AsyncApiMessage(renamed(m.headers()), renamed(m.payload()), m.correlationId(), m.contentType(),
                m.name(), m.title(), m.summary(), m.description(), m.tags(), m.externalDocs(), m.bindings(),
                m.examples(), m.traits(), m.extensions());
    }

    private Object renamed(Object value) {
        return value instanceof AsyncApiSchema s ? schema(s) : value;
    }

    private AsyncApiSchema schema(AsyncApiSchema s) {
        if (s == null || isEmpty()) {
            return s;
        }
        Map<String, AsyncApiSchema> properties = null;
        if (s.properties() != null) {
            properties = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiSchema> property : s.properties().entrySet()) {
                properties.put(property.getKey(), schema(property.getValue()));
            }
        }
        Object items = s.items() instanceof List<?> tuple
                ? tuple.stream().map(this::renamed).toList() : renamed(s.items());
        String name = s.name() == null ? null : renames.getOrDefault(s.name(), s.name());
        return new AsyncApiSchema(s.title(), s.type(), s.required(), s.multipleOf(), s.maximum(),
                s.exclusiveMaximum(), s.minimum(), s.exclusiveMinimum(), s.maxLength(), s.minLength(), s.pattern(),
                s.maxItems(), s.minItems(), s.uniqueItems(), s.maxProperties(), s.minProperties(), s.enumValue(),
                s.constValue(), s.examples(), schema(s.ifBranch()), schema(s.then()), schema(s.elseBranch()),
                s.readOnly(), s.writeOnly(), properties, s.patternProperties(), renamed(s.additionalProperties()),
                schema(s.additionalItems()), items, schema(s.propertyNames()), schema(s.contains()),
                list(s.allOf()), list(s.oneOf()), list(s.anyOf()), schema(s.not()), s.description(), s.format(),
                s.defaultValue(), s.discriminator(), s.externalDocs(), s.deprecated(), s.extensions(), name,
                s.nullable());
    }

    private List<AsyncApiSchema> list(List<AsyncApiSchema> schemas) {
        return schemas == null ? null : schemas.stream().map(this::schema).toList();
    }
}
