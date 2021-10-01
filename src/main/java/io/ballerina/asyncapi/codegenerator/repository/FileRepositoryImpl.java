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

package io.ballerina.asyncapi.codegenerator.repository;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileRepositoryImpl implements FileRepository {

    @Override
    public String getFileContent(String filePath) throws BallerinaAsyncApiException {
        var mainFile = new File(filePath);
        try (InputStream inputStream = new FileInputStream(mainFile)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new BallerinaAsyncApiException("File not found in the given path: ".concat(filePath), e);
        }
    }

    @Override
    public String getFileContentFromResources(String fileName) throws BallerinaAsyncApiException {
        try (var inputStream = getFileFromResourceAsStream(fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new BallerinaAsyncApiException("File not found in the resources: ".concat(fileName), e);
        }
    }

    @Override
    public void writeToFile(String filePath, String content) throws BallerinaAsyncApiException {
        var file = new File(filePath);
        try {
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BallerinaAsyncApiException(
                    "Could not write the contents to the relevant path: ".concat(filePath), e);
        }
    }

    private InputStream getFileFromResourceAsStream(String fileName) {
        var classLoader = getClass().getClassLoader();
        var inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: ".concat(fileName));
        } else {
            return inputStream;
        }
    }
}
