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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of the FileRepository which includes the file reading and converting related tasks.
 */
public class FileRepositoryImpl implements FileRepository {

    @Override
    public String getFileContent(String filePath) throws BallerinaAsyncApiException {
        File mainFile = new File(filePath);
        try (InputStream inputStream = new FileInputStream(mainFile)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new BallerinaAsyncApiException("File not found in the given path: ".concat(filePath), e);
        }
    }

    @Override
    public String getFileContentFromResources(String fileName) throws BallerinaAsyncApiException {
        try (InputStream inputStream = getFileFromResourceAsStream(fileName)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new BallerinaAsyncApiException("File not found in the resources: ".concat(fileName), e);
        }
    }

    @Override
    public void writeToFile(String filePath, String content) throws BallerinaAsyncApiException {
        File file = new File(filePath);
        try {
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BallerinaAsyncApiException(
                    "Could not write the contents to the relevant path: ".concat(filePath), e);
        }
    }

    @Override
    public String convertYamlToJson(String yaml) throws BallerinaAsyncApiException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        try {
            Object obj = yamlReader.readValue(yaml, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            return jsonWriter.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BallerinaAsyncApiException("Error when converting the given yaml file to json, " +
                    "Please validate the yaml file", e);
        }
    }

    @Override
    public boolean validateJson(String jsonString) throws BallerinaAsyncApiException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
            objectMapper.readTree(jsonString);
            return true;
        } catch (JsonProcessingException e) {
            throw new BallerinaAsyncApiException("Error parsing the json, please validate the json file", e);
        }
    }

    private InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: ".concat(fileName));
        } else {
            return inputStream;
        }
    }
}
