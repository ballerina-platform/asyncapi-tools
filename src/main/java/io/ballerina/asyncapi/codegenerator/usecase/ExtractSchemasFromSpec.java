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

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.entity.SchemaDecorator;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extract the schemas from the AsyncAPI specification.
 */
public class ExtractSchemasFromSpec implements Extractor {
    private final AaiDocument asyncApiSpec;

    public ExtractSchemasFromSpec(AaiDocument asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    @Override
    public Map<String, Schema> extract() throws BallerinaAsyncApiException {
        if (asyncApiSpec.components != null && asyncApiSpec.components.schemas != null
                && !asyncApiSpec.components.schemas.entrySet().isEmpty()) {
            return asyncApiSpec.components.schemas.entrySet()
                    .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new SchemaDecorator(e.getValue())));
        } else {
            throw new BallerinaAsyncApiException("There are no schemas in the given AsyncAPI specification");
        }
    }
}
