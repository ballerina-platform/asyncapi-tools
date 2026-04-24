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
package io.ballerina.asyncapi.generator.ws.client.utils;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * Utility methods for checking dispatcher presence in AsyncAPI schemas.
 */
public class CommonFunctionUtils {

    private static final String DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_STRING =
            "Both dispatcherKey and dispatcherStreamId type must be string";
    private static final String DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_INSIDE_REQUIRED =
            "Both dispatcherKey and dispatcherStreamId type must be inside required property";
    private static final String SCHEMA_MUST_BE_A_RECORD =
            "%s schema must be a record, and it must have properties to contain dispatcherKey as a field";
    private static final String RESPONSE_TYPE_MUST_BE_A_RECORD =
            "Response type must be a record, invalid response type %s in %s schema, schema must contain "
                    + "properties field to contain dispatcherKey";
    private static final String INVALID_RESPONSE_SCHEMA =
            "Response type must be a record, invalid response schema";
    private static final String OBJECT = "object";
    private static final String STRING = "string";

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new instance backed by the given spec.
     *
     * @param asyncApiSpec the parsed AsyncAPI spec
     */
    public CommonFunctionUtils(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Returns {@code true} if {@code dispatcherVal} is found as a string property (inside
     * {@code required}) in the given schema or any of its {@code oneOf}/{@code allOf} entries.
     *
     * @param schemaName    the schema name (used in error messages)
     * @param schema        the schema to inspect
     * @param dispatcherVal the dispatcher field name to look for
     * @param isParent      {@code true} if this is the top-level call
     * @return {@code true} if the dispatcher field is present
     * @throws GeneratorException if the schema structure is invalid
     */
    public boolean isDispatcherPresent(String schemaName, AsyncApiSchema schema,
                                       String dispatcherVal, boolean isParent) throws GeneratorException {
        if (schema == null) {
            throw new GeneratorException(INVALID_RESPONSE_SCHEMA);
        }
        Map<String, AsyncApiSchema> properties = schema.properties();
        List<AsyncApiSchema> oneOf = schema.oneOf();
        List<AsyncApiSchema> allOf = schema.allOf();

        if (properties != null) {
            if (properties.containsKey(dispatcherVal)) {
                AsyncApiSchema dispatcherSchema = properties.get(dispatcherVal);
                if (dispatcherSchema.type() == null || !dispatcherSchema.type().equals(STRING)) {
                    throw new GeneratorException(DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_STRING);
                }
                List<String> required = schema.required();
                if (required == null || !required.contains(dispatcherVal)) {
                    throw new GeneratorException(DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_INSIDE_REQUIRED);
                }
                return true;
            }
        } else if (oneOf != null) {
            Map<String, AsyncApiSchema> componentSchemas = asyncApiSpec.getAsyncApiComponents()
                    .map(c -> c.schemas()).orElse(null);
            for (AsyncApiSchema oneOfSchema : oneOf) {
                boolean oneOfContainsDispatcher;
                if (oneOfSchema.name() != null && componentSchemas != null) {
                    // This was a $ref — look up the referenced schema by name
                    AsyncApiSchema refSchema = componentSchemas.get(oneOfSchema.name());
                    oneOfContainsDispatcher = isDispatcherPresent(oneOfSchema.name(), refSchema,
                            dispatcherVal, false);
                } else {
                    oneOfContainsDispatcher = isDispatcherPresent("", oneOfSchema, dispatcherVal, false);
                }
                if (!oneOfContainsDispatcher && isParent) {
                    String specDispatcherKey = getSpecDispatcherKey();
                    if (dispatcherVal.equals(specDispatcherKey)) {
                        throw new GeneratorException(String.format(SCHEMA_MUST_BE_A_RECORD, schemaName));
                    }
                }
            }
            return true;
        } else if (allOf != null) {
            Map<String, AsyncApiSchema> componentSchemas = asyncApiSpec.getAsyncApiComponents()
                    .map(c -> c.schemas()).orElse(null);
            for (AsyncApiSchema allOfSchema : allOf) {
                boolean allOfContainsDispatcher;
                if (allOfSchema.name() != null && componentSchemas != null) {
                    AsyncApiSchema refSchema = componentSchemas.get(allOfSchema.name());
                    allOfContainsDispatcher = isDispatcherPresent(allOfSchema.name(), refSchema,
                            dispatcherVal, false);
                } else {
                    allOfContainsDispatcher = isDispatcherPresent("", allOfSchema, dispatcherVal, false);
                }
                if (allOfContainsDispatcher) {
                    return true;
                }
            }
            String specDispatcherKey = getSpecDispatcherKey();
            if (dispatcherVal.equals(specDispatcherKey)) {
                throw new GeneratorException(String.format(SCHEMA_MUST_BE_A_RECORD, schemaName));
            }
        } else if (schema.type() != null && !schema.type().equals(OBJECT)) {
            throw new GeneratorException(String.format(RESPONSE_TYPE_MUST_BE_A_RECORD,
                    schema.type(), schemaName));
        } else {
            return false;
        }
        return false;
    }

    private String getSpecDispatcherKey() {
        return asyncApiSpec.getAsyncApiExtensions()
                .flatMap(ext -> {
                    com.fasterxml.jackson.databind.JsonNode node = ext.get(X_DISPATCHER_KEY);
                    return node != null ? java.util.Optional.of(node.asText()) : java.util.Optional.empty();
                })
                .orElse("");
    }
}
