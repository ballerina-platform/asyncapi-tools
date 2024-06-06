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
import io.apicurio.datamodels.deref.Dereferencer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.refs.ReferenceResolverChain;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.entity.MultiChannel;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractChannelsFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractIdentifierPathFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractIdentifierTypeFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractSchemasFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.Extractor;

import java.util.List;
import java.util.Map;

/**
 * This class contains extraction of data into entities from the AsyncAPI specification.
 */
public class AsyncApiSpecController implements SpecController {
    private List<ServiceType> serviceTypes;
    private Map<String, AsyncApiSchema> schemas;
    private String eventIdentifierType;
    private String eventIdentifierPath;

    public AsyncApiSpecController(String asyncApiSpecJson) throws BallerinaAsyncApiException {
        readSpec(asyncApiSpecJson);
    }

    private void readSpec(String asyncApiSpecJson) throws BallerinaAsyncApiException {
        AsyncApiDocument asyncApiSpec = (AsyncApiDocument) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Dereferencer dereferencer = new Dereferencer(ReferenceResolverChain.getInstance(), false);
        asyncApiSpec = (AsyncApiDocument) dereferencer.dereference(asyncApiSpec);
//        Set<String> unresolvedRefs = dereferencer.getUnresolvableReferences();
//        if (!unresolvedRefs.isEmpty()) {
//            throw new BallerinaAsyncApiException("Could not resolve some Yaml paths defined in $ref attributes: "
//                    .concat(String.join(", ", unresolvedRefs)));
//        }

        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        Extractor extractSchemas = new ExtractSchemasFromSpec(asyncApiSpec);
        Extractor extractIdentifierType = new ExtractIdentifierTypeFromSpec(asyncApiSpec);
        Extractor extractIdentifierPath = new ExtractIdentifierPathFromSpec(asyncApiSpec);

        MultiChannel multiChannel = extractServiceTypes.extract();
        serviceTypes = multiChannel.getServiceTypes();
        schemas = extractSchemas.extract();
        schemas.putAll(multiChannel.getInlineSchemas());
        eventIdentifierType = extractIdentifierType.extract();
        eventIdentifierPath = extractIdentifierPath.extract();
    }

    @Override
    public List<ServiceType> getServiceTypes() {
        return serviceTypes;
    }

    @Override
    public Map<String, AsyncApiSchema> getSchemas() {
        return schemas;
    }

    @Override
    public String getEventIdentifierType() {
        return eventIdentifierType;
    }

    @Override
    public String getEventIdentifierPath() {
        return eventIdentifierPath;
    }
}
