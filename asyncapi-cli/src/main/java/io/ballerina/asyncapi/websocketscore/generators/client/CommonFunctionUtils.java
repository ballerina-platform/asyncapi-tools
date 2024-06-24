/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.client;

import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorConstants;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;

import java.util.List;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_INSIDE_REQUIRED_PROPERTY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.INVALID_RESPONSE_SCHEMA;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RESPONSE_TYPE_MUST_BE_A_RECORD;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SCHEMA_MUST_BE_A_RECORD;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.extractReferenceType;

/**
 * Common function utils for request and response.
 */
public class CommonFunctionUtils {

    private AsyncApi25DocumentImpl asyncAPI;

    public CommonFunctionUtils(AsyncApi25DocumentImpl asyncAPI) {
        this.asyncAPI = asyncAPI;
    }

    /**
     * Get return data type by traversing AsyncAPI schemas.
     */
    public boolean isDispatcherPresent(String schemaName, AsyncApi25SchemaImpl schema, String dispatcherVal,
                                       boolean isParent) throws BallerinaAsyncApiExceptionWs {
        if (schema != null) {
            if (schema.getProperties() != null) {
//                type = getValidName(schemaName, true);
                if (schema.getProperties().containsKey(dispatcherVal)) {
                    if (!((AsyncApi25SchemaImpl) schema.getProperties().get(dispatcherVal)).getType().equals(STRING)) {
                        throw new BallerinaAsyncApiExceptionWs(DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_STRING);
                    }
                    if (schema.getRequired() == null || (!schema.getRequired().contains(dispatcherVal))) {
                        throw new BallerinaAsyncApiExceptionWs(
                                DISPATCHER_KEY_AND_DISPATCHER_STREAM_ID_MUST_BE_INSIDE_REQUIRED_PROPERTY);
                    }
                    return true;
                }
            } else if (schema.getOneOf() != null) {
                List<AsyncApiSchema> oneOfSchemas = schema.getOneOf();
                for (AsyncApiSchema oneOfSchema : oneOfSchemas) {
                    AsyncApi25SchemaImpl oneOf25Schema = (AsyncApi25SchemaImpl) oneOfSchema;
                    boolean oneOfContainProperties;
                    if (oneOf25Schema.get$ref() != null) {
                        String refSchemaName = extractReferenceType(oneOf25Schema.get$ref());
                        AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().
                                getSchemas().get(
                                        refSchemaName);
                        oneOfContainProperties = isDispatcherPresent(refSchemaName, refSchema, dispatcherVal, false);
                    } else {
                        oneOfContainProperties = isDispatcherPresent("", oneOf25Schema, dispatcherVal, false);
                    }
                    if (!oneOfContainProperties && isParent) {
                        TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
                        String dispatcherKey = textNode.asText();
                        if (dispatcherVal.equals(dispatcherKey)) {
                            throw new BallerinaAsyncApiExceptionWs(String.format(
                                  SCHEMA_MUST_BE_A_RECORD, schemaName));
                        }
                    }
                }
                return true;
            } else if (schema.getAllOf() != null) {
                List<Schema> allOfSchemas = schema.getAllOf();
                boolean allOfContainProperties;
                for (Schema allOfSchema : allOfSchemas) {
                    AsyncApi25SchemaImpl allOf25Schema = (AsyncApi25SchemaImpl) allOfSchema;
                    if (allOf25Schema.get$ref() != null) {
                        String refSchemaName = extractReferenceType(allOf25Schema.get$ref());
                        AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().
                                getSchemas().get(
                                        schemaName);
                        allOfContainProperties = isDispatcherPresent(refSchemaName, refSchema, dispatcherVal,
                                false);
                    } else {
                        allOfContainProperties = isDispatcherPresent("", allOf25Schema,
                                dispatcherVal, false);
                    }
                    if (allOfContainProperties) {
                        return true;

                    }
                }
                TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
                String dispatcherKey = textNode.asText();
                if (dispatcherVal.equals(dispatcherKey)) {
                    throw new BallerinaAsyncApiExceptionWs(String.format(
                            SCHEMA_MUST_BE_A_RECORD, schemaName));
                }
            } else if (!schema.getType().equals(GeneratorConstants.OBJECT)) {
                throw new BallerinaAsyncApiExceptionWs(String.format(
                        RESPONSE_TYPE_MUST_BE_A_RECORD,
                        schema.getType(), schemaName));
            } else {
                return false;
            }
        } else {
            throw new BallerinaAsyncApiExceptionWs(INVALID_RESPONSE_SCHEMA);
        }
        return false;
    }
}
