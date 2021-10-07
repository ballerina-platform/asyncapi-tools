/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.codegenerator.application;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.controller.*;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;

import java.util.List;
import java.util.Map;

public class CodeGenerator implements Application {
    @Override
    public void generate(String specPath, String outputPath) throws BallerinaAsyncApiException {
        FileRepository fileRepository = new FileRepositoryImpl();
        String asyncApiSpecJson = getFileContent(fileRepository, specPath);

        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);
        Map<String, Schema> schemas = specController.getSchemas();
        List<ServiceType> serviceTypes = specController.getServiceTypes();
        String eventIdentifierPath = specController.getEventIdentifierPath();

        BalController schemaController = new SchemaController(schemas);
        String dataTypesBalContent = schemaController.generateBalCode("");

        BalController serviceTypesController = new ServiceTypesController(serviceTypes);
        String serviceTypesBalContent = serviceTypesController.generateBalCode("");

        String listenerTemplate = fileRepository.getFileContentFromResources(Constants.LISTENER_BAL_FILE_NAME);
        BalController listenerController = new ListenerController(serviceTypes);
        String listenerBalContent = listenerController.generateBalCode(listenerTemplate);

        String dispatcherTemplate = fileRepository.getFileContentFromResources(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME);
        BalController dispatcherController = new DispatcherController(serviceTypes, eventIdentifierPath);
        String dispatcherContent = dispatcherController.generateBalCode(dispatcherTemplate);

        String outputDirectory = getOutputDirectory(outputPath);
        fileRepository.writeToFile(outputDirectory.concat(Constants.DATA_TYPES_BAL_FILE_NAME), dataTypesBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.SERVICE_TYPES_BAL_FILE_NAME), serviceTypesBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.LISTENER_BAL_FILE_NAME), listenerBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME), dispatcherContent);
    }

    private String getOutputDirectory(String outputPath) {
        if (outputPath.endsWith("/")) return outputPath;
        return outputPath.concat("/");
    }

    private String getFileContent(FileRepository fileRepository, String specPath) throws BallerinaAsyncApiException {
        String asyncApiSpecYaml = fileRepository.getFileContent(specPath);
        if (specPath.endsWith(".json")) {
            return asyncApiSpecYaml;
        } else if (specPath.endsWith("yaml") || specPath.endsWith("yml")) {
            return fileRepository.convertYamlToJson(asyncApiSpecYaml);
        } else {
            throw new BallerinaAsyncApiException("Unknown file type: ".concat(specPath));
        }
    }
}
