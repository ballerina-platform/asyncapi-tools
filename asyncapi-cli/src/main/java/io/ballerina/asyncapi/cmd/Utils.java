/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package io.ballerina.asyncapi.cmd;

import java.io.File;
import java.util.Locale;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.YAML_EXTENSION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.YML_EXTENSION;

/**
 * Utility functions for AsyncAPI CMD.
 */
public class Utils {
    private static final String FILE_EXTENSION_SEPARATOR = ".";
    private static final String BAL_EXTENSION = "bal";
    private static final String JSON_EXTENSION = "json";
    private static final String YAML_EXTENSION = "yaml";
    private static final String YML_EXTENSION = "yml";

    static boolean isAsyncApiSpecFile(String extension) {
        return isYamlFile(extension) || isJsonFile(extension);
    }

    public static boolean isYamlFile(String filePath) {
        String extension = getFileExtension(filePath);
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase(Locale.ROOT);
        return extension.endsWith(YAML_EXTENSION) || extension.endsWith(YML_EXTENSION);
    }

    public static boolean isJsonFile(String filePath) {
        String extension = getFileExtension(filePath);
        if (extension == null) {
            return false;
        }
        return extension.toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION);
    }

    public static boolean isBallerinaFile(String extension) {
        return extension.endsWith(BAL_EXTENSION);
    }

    private static String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR) < fileName.lastIndexOf(File.separatorChar)) {
            return null;
        }
        int i = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        return fileName.substring(i + 1);
    }
}
