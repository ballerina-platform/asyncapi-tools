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
package io.ballerina.asyncapi.core.implementation.v2.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Components;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Components;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Maps a message payload {@link JsonNode} to a
 * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema} for AsyncAPI 2.x.
 */
final class MessagePayloadMapperV2 {

    private static final Logger LOG = LogManager.getLogger(MessagePayloadMapperV2.class);

    private MessagePayloadMapperV2() {

    }

    /**
     * Maps a message payload {@link JsonNode} to a
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * If the node is a schema {@code $ref}, resolves it from {@code components.schemas};
     * otherwise delegates to {@link SchemaMapper#mapFromJsonNode(JsonNode)}.
     *
     * @param payloadNode the raw payload node from Apicurio
     * @param components  the AsyncAPI components (for schema $ref resolution)
     * @return the mapped schema, or null if unresolvable or node is null
     */
    static io.ballerina.asyncapi.core.model.component.AsyncApiSchema map(
            JsonNode payloadNode, AsyncApiComponents components) {
        if (payloadNode == null) {
            return null;
        }
        JsonNode refNode = payloadNode.get("$ref");
        if (refNode != null) {
            String $ref = refNode.asText();
            if ($ref.startsWith(Constants.SCHEMAS_REF_PREFIX) && components != null) {
                String schemaName = $ref.substring(Constants.SCHEMAS_REF_PREFIX.length());
                Map<String, ? extends AsyncApiSchema> schemas = switch (components) {
                    case AsyncApi26Components typed -> typed.getSchemas();
                    case AsyncApi25Components typed -> typed.getSchemas();
                    case AsyncApi24Components typed -> typed.getSchemas();
                    case AsyncApi23Components typed -> typed.getSchemas();
                    case AsyncApi22Components typed -> typed.getSchemas();
                    case AsyncApi21Components typed -> typed.getSchemas();
                    case AsyncApi20Components typed -> typed.getSchemas();
                    default -> null;
                };
                AsyncApiSchema resolved = schemas != null ? schemas.get(schemaName) : null;
                if (resolved != null) {
                    io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = SchemaMapper.map(resolved);
                    if (mapped != null) {
                        mapped = mapped.withName(schemaName);
                    }
                    return mapped;
                }
            }
            LOG.warn("Could not resolve payload $ref: {}. Skipping payload.", $ref);
            return null;
        }
        return SchemaMapper.mapFromJsonNode(payloadNode);
    }
}
