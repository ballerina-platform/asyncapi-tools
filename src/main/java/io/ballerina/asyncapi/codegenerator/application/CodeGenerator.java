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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.controller.Controller;
import io.ballerina.asyncapi.codegenerator.controller.ListenerController;
import io.ballerina.asyncapi.codegenerator.controller.DispatcherController;
import io.ballerina.asyncapi.codegenerator.controller.SchemaController;
import io.ballerina.asyncapi.codegenerator.controller.ServiceTypesController;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;

public class CodeGenerator implements Application {
    private final String specPath;
    private final String outputPath;

    public CodeGenerator(String specPath, String outputPath) {
        this.specPath = specPath;
        this.outputPath = outputPath;
    }

    @Override
    public void generate() throws BallerinaAsyncApiException {
        FileRepository fileRepository = new FileRepositoryImpl();
        String asyncApiSpecYaml = fileRepository.getFileContent(specPath);
        String asyncApiSpecJson;
        if (specPath.endsWith(".json")) {
            asyncApiSpecJson = asyncApiSpecYaml;
        } else if (specPath.endsWith("yaml") || specPath.endsWith("yml")) {
            try {
                asyncApiSpecJson = convertYamlToJson(asyncApiSpecYaml);
            } catch (JsonProcessingException e) {
                throw new BallerinaAsyncApiException("Error when converting the given yaml file to json", e);
            }
        } else {
            throw new BallerinaAsyncApiException("Unknown file type: ".concat(specPath));
        }

        Controller schemaController = new SchemaController();
        String dataTypesBalContent = schemaController.generateBalCode(asyncApiSpecJson, "");

        Controller serviceTypesController = new ServiceTypesController();

        String serviceTypesBalContent = serviceTypesController.generateBalCode(asyncApiSpecJson, "");

        String listenerTemplate = fileRepository.getFileContentFromResources(Constants.LISTENER_BAL_FILE_NAME);
        Controller listenerController = new ListenerController();
        String listenerBalContent = listenerController.generateBalCode(asyncApiSpecJson, listenerTemplate);

        fileRepository.writeToFile(outputPath.concat(Constants.DATA_TYPES_BAL_FILE_NAME), dataTypesBalContent);
        fileRepository.writeToFile(outputPath.concat(Constants.SERVICE_TYPES_BAL_FILE_NAME), serviceTypesBalContent);
        fileRepository.writeToFile(outputPath.concat(Constants.LISTENER_BAL_FILE_NAME), listenerBalContent);
        serviceTypesController.generateBalCode(asyncApiSpecJson, "");

        String asyncApiSpec = fileRepository.getFileContent(specPath);
        String balTemplate = fileRepository.getFileContentFromResources(Constants.HTTP_BAL_TEMPLATE_LISTENER_FILE_NAME);

//        Controller schemaController = new SchemaController();
//        schemaController.generateBalCode(asyncApiSpec, balTemplate);
//        FileRepository fileRepository = new FileRepositoryImpl();
//        String asyncApiSpec = fileRepository.getFileContent(specPath);

//        Controller schemaController = new SchemaController();
//        schemaController.generateBalCode(asyncApiSpec);

        Controller dispatcherController = new DispatcherController();
        dispatcherController.generateBalCode(asyncApiSpec, balTemplate);
    }

    String convertYamlToJson(String yaml) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);
        var jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }
}
