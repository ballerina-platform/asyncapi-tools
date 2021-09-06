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

import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.controller.Controller;
import io.ballerina.asyncapi.codegenerator.controller.SchemaController;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;

public class CodeGenerator implements Application {
    private final String specPath;

    public CodeGenerator(String specPath) {
        this.specPath = specPath;
    }

    @Override
    public void generate() {
        FileRepository fileRepository = new FileRepositoryImpl();
        String asyncApiSpec = fileRepository.getFileContent(specPath);
        String balTemplate = fileRepository.getFileContentFromResources(Constants.HTTP_BAL_TEMPLATE_FILE_NAME);

        Controller schemaController = new SchemaController();
        schemaController.generateBalCode(asyncApiSpec, balTemplate);
    }
}
