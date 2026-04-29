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
package io.ballerina.asyncapi.generator.ws.asyncspec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30SchemaImpl;
import io.apicurio.datamodels.models.union.BooleanSchemaUnion;
import io.apicurio.datamodels.models.union.SchemaSchemaListUnion;

/**
 * Ballerina-specific extension of the Apicurio {@code AsyncApi30SchemaImpl} used during AsyncAPI 3.0
 * spec generation.
 *
 * <p>Overrides {@code isEntity()} and {@code isSchema()} so that Jackson serialises this node correctly
 * when it is embedded inside Apicurio's data-model graph. The {@code @JsonDeserialize} annotations ensure
 * that {@code items} and {@code additionalProperties} round-trip through the correct concrete type.
 */
public class BalAsyncApi30SchemaImpl extends AsyncApi30SchemaImpl {

    @JsonDeserialize(as = BalAsyncApi30SchemaImpl.class)
    private SchemaSchemaListUnion items;

    @JsonDeserialize(as = BalAsyncApi30SchemaImpl.class)
    private BooleanSchemaUnion additionalProperties;

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} so this schema node is treated as a serialisable entity.
     */
    @JsonIgnore
    @Override
    public boolean isEntity() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} so this node is recognised as a schema node by Apicurio visitors.
     */
    @JsonIgnore
    @Override
    public boolean isSchema() {
        return true;
    }
}
