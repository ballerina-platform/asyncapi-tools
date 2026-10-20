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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * File I/O utilities for Ballerina-to-AsyncAPI 3.0 conversion. Whether an existing file is overwritten is decided
 * by the caller (the CLI's output stage), not here.
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
}
