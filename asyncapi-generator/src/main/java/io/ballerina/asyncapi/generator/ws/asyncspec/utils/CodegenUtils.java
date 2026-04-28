/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.YAML_EXTENSION;

/**
 * File I/O and naming utilities for Ballerina-to-AsyncAPI 3.0 conversion.
 * Corresponds to the legacy {@code asyncspec/utils/CodegenUtils}.
 */
public final class CodegenUtils {

    private CodegenUtils() {
    }

    /**
     * Writes {@code content} to the file at {@code filePath} using UTF-8 encoding.
     *
     * @param filePath destination file path
     * @param content  content to write
     * @throws IOException if the file cannot be written
     */
    public static void writeFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toString(), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Resolves the output contract file name, prompting to overwrite or appending a counter suffix
     * to avoid collisions with existing files in {@code outPath}.
     *
     * @param outPath      output directory
     * @param asyncApiName requested file name (with extension)
     * @param isJson       true if the file is JSON, false if YAML
     * @return resolved file name, possibly with a numeric suffix
     */
    public static String resolveContractFileName(Path outPath, String asyncApiName, Boolean isJson) {
        if (outPath != null && Files.exists(outPath)) {
            final File[] listFiles = new File(String.valueOf(outPath)).listFiles();
            if (listFiles != null) {
                asyncApiName = checkAvailabilityOfGivenName(asyncApiName, listFiles, isJson);
            }
        }
        return asyncApiName;
    }

    private static String checkAvailabilityOfGivenName(String asyncApiName, File[] listFiles, Boolean isJson) {
        for (File file : listFiles) {
            if (System.console() != null && file.getName().equals(asyncApiName)) {
                String userInput = System.console().readLine(
                        "There is already a file named '" + file.getName()
                                + "' in the target location. Do you want to overwrite the file? [y/N] ");
                if (!Objects.equals(userInput.toLowerCase(Locale.ENGLISH), "y")) {
                    asyncApiName = setGeneratedFileName(listFiles, asyncApiName, isJson);
                }
            }
        }
        return asyncApiName;
    }

    private static String setGeneratedFileName(File[] listFiles, String fileName, boolean isJson) {
        int duplicateCount = 0;
        for (File listFile : listFiles) {
            String listFileName = listFile.getName();
            if (listFileName.contains(".") && (listFileName.split("\\.")).length >= 2
                    && listFileName.split("\\.")[0].equals(fileName.split("\\.")[0])) {
                duplicateCount++;
            }
        }
        if (isJson) {
            return fileName.split("\\.")[0] + "." + duplicateCount + JSON_EXTENSION;
        }
        return fileName.split("\\.")[0] + "." + duplicateCount + YAML_EXTENSION;
    }
}
