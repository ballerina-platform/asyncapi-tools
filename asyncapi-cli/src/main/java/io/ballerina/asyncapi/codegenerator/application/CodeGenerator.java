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
import io.ballerina.asyncapi.codegenerator.controller.AsyncApiSpecController;
import io.ballerina.asyncapi.codegenerator.controller.BalController;
import io.ballerina.asyncapi.codegenerator.controller.DispatcherController;
import io.ballerina.asyncapi.codegenerator.controller.ListenerController;
import io.ballerina.asyncapi.codegenerator.controller.SchemaController;
import io.ballerina.asyncapi.codegenerator.controller.ServiceTypesController;
import io.ballerina.asyncapi.codegenerator.controller.SpecController;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 *  This class controls the flow of code generation.
 *  1. read the file
 *  2. extract necessary data into defined entities
 *  3. generate ballerina code and write into files
 */
public class CodeGenerator implements Application {
    private final FileRepository fileRepository;
    private static final PrintStream outStream = System.err;

    public CodeGenerator() {
        this.fileRepository = new FileRepositoryImpl();
    }

    public CodeGenerator(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public void generate(String specPath, String outputPath) throws BallerinaAsyncApiException {
        String asyncApiSpecJson = getFileContent(fileRepository, specPath);

        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);
        Map<String, Schema> schemas = specController.getSchemas();
        List<ServiceType> serviceTypes = specController.getServiceTypes();
        String eventIdentifierPath = specController.getEventIdentifierPath();

        String dataTypesTemplate = fileRepository.getFileContentFromResources(Constants.DATA_TYPES_BAL_FILE_NAME);
        BalController schemaController = new SchemaController(schemas);
        String dataTypesBalContent = schemaController.generateBalCode(dataTypesTemplate);

        BalController serviceTypesController = new ServiceTypesController(serviceTypes);
        String serviceTypesBalContent = serviceTypesController.generateBalCode(Constants.EMPTY_BALLERINA_FILE_CONTENT);

        String listenerTemplate = fileRepository.getFileContentFromResources(Constants.LISTENER_BAL_FILE_NAME);
        BalController listenerController = new ListenerController(serviceTypes);
        String listenerBalContent = listenerController.generateBalCode(listenerTemplate);

        String dispatcherTemplate = fileRepository
                .getFileContentFromResources(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME);
        BalController dispatcherController = new DispatcherController(serviceTypes, eventIdentifierPath);
        String dispatcherContent = dispatcherController.generateBalCode(dispatcherTemplate);

        String outputDirectory = getOutputDirectory(outputPath);
        fileRepository.writeToFile(outputDirectory.concat(Constants.DATA_TYPES_BAL_FILE_NAME), dataTypesBalContent);
        fileRepository
                .writeToFile(outputDirectory.concat(Constants.SERVICE_TYPES_BAL_FILE_NAME), serviceTypesBalContent);
        fileRepository.writeToFile(outputDirectory.concat(Constants.LISTENER_BAL_FILE_NAME), listenerBalContent);
        fileRepository
                .writeToFile(outputDirectory.concat(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME), dispatcherContent);

        outStream.println("Following files were generated successfully:" +
                "\n--data_types.bal\n--service_types.bal\n--listener.bal\n--dispatcher_service.bal");
    }

    private String getOutputDirectory(String outputPath) {
        if (outputPath.endsWith("/")) {
            return outputPath;
        }
        return outputPath.concat("/");
    }

    private String getFileContent(FileRepository fileRepository, String specPath) throws BallerinaAsyncApiException {
        String asyncApiSpecYaml = fileRepository.getFileContent(specPath);
        if (specPath.endsWith(".json")) {
            fileRepository.validateJson(asyncApiSpecYaml);
            return asyncApiSpecYaml;
        } else if (specPath.endsWith("yaml") || specPath.endsWith("yml")) {
            return fileRepository.convertYamlToJson(asyncApiSpecYaml);
        } else {
            throw new BallerinaAsyncApiException("Unknown file type: ".concat(specPath));
        }
    }
}
