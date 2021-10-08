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

package io.ballerina.asyncapi.codegenerator.controller;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractIdentifierPathFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractSchemasFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractServiceTypesFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractUseCase;

import java.util.List;
import java.util.Map;

public class AsyncApiSpecController implements SpecController {
    private List<ServiceType> serviceTypes;
    private Map<String, Schema> schemas;
    private String eventIdentifierPath;

    public AsyncApiSpecController(String asyncApiSpecJson) throws BallerinaAsyncApiException {
        readSpec(asyncApiSpecJson);
    }

    private void readSpec(String asyncApiSpecJson) throws BallerinaAsyncApiException {
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);

        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        ExtractUseCase extractSchemas = new ExtractSchemasFromSpec(asyncApiSpec);
        ExtractUseCase extractIdentifierPath = new ExtractIdentifierPathFromSpec(asyncApiSpec);

        serviceTypes = extractServiceTypes.extract();
        schemas = extractSchemas.extract();
        eventIdentifierPath = extractIdentifierPath.extract();
    }

    @Override
    public List<ServiceType> getServiceTypes() {
        return serviceTypes;
    }

    @Override
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    @Override
    public String getEventIdentifierPath() {
        return eventIdentifierPath;
    }
}