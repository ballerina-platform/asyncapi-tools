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
import io.apicurio.datamodels.models.asyncapi.AsyncApiCorrelationID;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.model.message.AsyncApiCorrelationId;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiCorrelationID} to {@link AsyncApiCorrelationId}
 * for AsyncAPI 2.x.
 */
public final class CorrelationIdMapperV2 {

    private CorrelationIdMapperV2() {
    }

    /**
     * Maps an Apicurio {@link AsyncApiCorrelationID} to a model
     * {@link AsyncApiCorrelationId}.
     *
     * @param correlationId the Apicurio correlation ID object
     * @return the mapped AsyncApiCorrelationId, or null if null
     */
    public static AsyncApiCorrelationId map(AsyncApiCorrelationID correlationId) {
        if (correlationId == null) {
            return null;
        }

        Map<String, JsonNode> extensions = null;
        if (correlationId instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        return new AsyncApiCorrelationId(
                correlationId.getDescription(),
                correlationId.getLocation(),
                extensions
        );
    }
}
