/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase;

import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extract the schemas from the AsyncAPI specification.
 */
public class ExtractSchemasFromSpec implements Extractor {
    private final AsyncApiDocument asyncApiSpec;

    public ExtractSchemasFromSpec(AsyncApiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public Map<String, AsyncApiSchema> extract() throws BallerinaAsyncApiException {
        if (asyncApiSpec.getComponents() != null && asyncApiSpec.getComponents().getSchemas() != null
                && !asyncApiSpec.getComponents().getSchemas().entrySet().isEmpty()) {
            return asyncApiSpec.getComponents().getSchemas().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> (AsyncApiSchema) entry.getValue()));
        }
        return new HashMap<>();
    }
}
