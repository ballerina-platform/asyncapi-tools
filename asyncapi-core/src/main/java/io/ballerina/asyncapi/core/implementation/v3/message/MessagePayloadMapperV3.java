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
package io.ballerina.asyncapi.core.implementation.v3.message;

import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MultiFormatSchema;
import io.apicurio.datamodels.models.union.AnySchemaUnion;
import io.apicurio.datamodels.models.union.MultiFormatSchemaSchemaUnion;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Maps a message payload to a
 * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}
 */
final class MessagePayloadMapperV3 {

    private static final Logger LOG = LogManager.getLogger(MessagePayloadMapperV3.class);

    private MessagePayloadMapperV3() {
    }

    /**
     * Maps a {@link MultiFormatSchemaSchemaUnion} payload to an
     * {@link io.ballerina.asyncapi.core.model.component.AsyncApiSchema}.
     * Handles both direct JSON Schema entries and schema {@code $ref}s resolved
     * from {@code components.schemas}.
     * Non-JSON schema formats (Avro, Protobuf, etc.) are skipped and return {@code null}.
     *
     * @param payloadUnion the payload union from an {@link AsyncApi30Message}
     * @param components   the AsyncAPI components (for schema {@code $ref} resolution)
     * @return the mapped schema, or {@code null} if unresolvable or non-JSON format
     */
    static io.ballerina.asyncapi.core.model.component.AsyncApiSchema map(
            MultiFormatSchemaSchemaUnion payloadUnion, AsyncApiComponents components) {
        if (payloadUnion == null) {
            return null;
        }
        if (payloadUnion.isMultiFormatSchema()) {
            AsyncApi30MultiFormatSchema multiFormat = payloadUnion.asMultiFormatSchema();
            String $ref = multiFormat.get$ref();
            if ($ref != null) {
                if ($ref.startsWith(Constants.SCHEMAS_REF_PREFIX) && components instanceof AsyncApi30Components v3) {
                    String schemaName = $ref.substring(Constants.SCHEMAS_REF_PREFIX.length());
                    Map<String, MultiFormatSchemaSchemaUnion> schemas = v3.getSchemas();
                    MultiFormatSchemaSchemaUnion schemaUnion = schemas != null ? schemas.get(schemaName) : null;
                    if (schemaUnion != null && schemaUnion.isSchema()
                            && schemaUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
                        io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = SchemaMapper.map(typedSchema);
                        if (mapped != null) {
                            mapped = mapped.withName(schemaName);
                        }
                        return mapped;
                    }
                }
                LOG.warn("Could not resolve payload $ref: {}. Skipping payload.", $ref);
                return null;
            }
            AnySchemaUnion schemaContent = multiFormat.getSchema();
            if (schemaContent != null && schemaContent.isSchema()
                    && schemaContent.asSchema() instanceof AsyncApiSchema typedSchema) {
                return SchemaMapper.map(typedSchema);
            }
            return null;
        }
        if (!payloadUnion.isSchema()) {
            return null;
        }
        Schema rawSchema = payloadUnion.asSchema();
        if (rawSchema instanceof AsyncApiReferenceable ref && ref.get$ref() != null) {
            String $ref = ref.get$ref();
            if ($ref.startsWith(Constants.SCHEMAS_REF_PREFIX) && components instanceof AsyncApi30Components v3) {
                String schemaName = $ref.substring(Constants.SCHEMAS_REF_PREFIX.length());
                Map<String, MultiFormatSchemaSchemaUnion> schemas = v3.getSchemas();
                MultiFormatSchemaSchemaUnion schemaUnion = schemas != null ? schemas.get(schemaName) : null;
                if (schemaUnion != null && schemaUnion.isSchema()
                        && schemaUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
                    io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped = SchemaMapper.map(typedSchema);
                    if (mapped != null) {
                        mapped = mapped.withName(schemaName);
                    }
                    return mapped;
                }
            }
            LOG.warn("Could not resolve payload $ref: {}. Skipping payload.", $ref);
            return null;
        }
        if (rawSchema instanceof AsyncApiSchema typedSchema) {
            return SchemaMapper.map(typedSchema);
        }
        return null;
    }

}
