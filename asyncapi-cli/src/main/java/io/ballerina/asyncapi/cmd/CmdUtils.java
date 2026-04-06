/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods shared across the refactored asyncapi CLI commands.
 *
 */
public final class CmdUtils {

    private CmdUtils() {
    }

    /**
     * Returns {@code true} if the given file path ends with {@code .json}.
     *
     * @param inputPath file path to check
     * @return {@code true} if the file is a JSON file
     */
    public static boolean isJsonFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        return inputPath.trim().toLowerCase().endsWith(CmdConstants.JSON_EXTENSION);
    }

    /**
     * Returns {@code true} if the given file path ends with {@code .yaml} or {@code .yml}.
     *
     * @param inputPath file path to check
     * @return {@code true} if the file is a YAML file
     */
    public static boolean isYamlFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        String name = inputPath.trim().toLowerCase();
        return name.endsWith(CmdConstants.YAML_EXTENSION) || name.endsWith(CmdConstants.YML_EXTENSION);
    }

    public static boolean isAsyncApiSpecFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        String name = inputPath.trim().toLowerCase();
        return name.endsWith(CmdConstants.YAML_EXTENSION)
                || name.endsWith(CmdConstants.YML_EXTENSION)
                || name.endsWith(CmdConstants.JSON_EXTENSION);
    }

    /**
     * Returns {@code true} if the given file path refers to a Ballerina source file ({@code .bal}).
     *
     * @param inputPath file path to check
     * @return {@code true} if the extension is {@code .bal}
     */
    public static boolean isBallerinaFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        return inputPath.trim().toLowerCase().endsWith(".bal");
    }

    /**
     * Returns {@code true} if {@code inputPath} is non-null, non-blank, the file exists on the
     * filesystem, and its name ends with a supported AsyncAPI spec extension
     * ({@code .yaml}, {@code .yml}, or {@code .json}).
     *
     * @param inputPath input file path provided by the user; may be {@code null}
     * @return {@code true} if the path is a valid, readable AsyncAPI spec file
     */
    public static boolean isValidInputFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        Path path = Path.of(inputPath);
        return Files.exists(path) && isAsyncApiSpecFile(path.getFileName().toString());
    }

    /**
     * Returns {@code true} if {@code inputPath} is non-null, non-blank, the file exists on the
     * filesystem, and its name ends with {@code .bal}.
     *
     * @param inputPath input file path provided by the user; may be {@code null}
     * @return {@code true} if the path is a valid, readable Ballerina source file
     */
    public static boolean isValidBallerinaFile(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return false;
        }
        Path path = Path.of(inputPath);
        if (!Files.exists(path)) {
            return false;
        }
        return path.getFileName().toString().toLowerCase()
                .endsWith(CmdConstants.BAL_EXTENSION);
    }

    /**
     * Transfers the raw flag values from {@code baseCmd} into a new {@link CmdOptions} value
     * object via the builder, applying any normalization or default-filling required before
     * generation.
     *
     * @param baseCmd the mixin holding the raw picocli-parsed values
     * @return a fully populated {@link CmdOptions} ready to pass to
     *         {@link SubCmdBase#generate(CmdOptions, CmdConstants.Mode)}
     */
    public static CmdOptions collectCmdOptions(BaseCmd baseCmd) {
        return new CmdOptions.CmdOptionsBuilder()
                .withInput(baseCmd.inputPath)
                .withOutput(baseCmd.outputPath)
                .withLicenseFilePath(baseCmd.licensePath)
                .withService(baseCmd.serviceName)
                .withJson(baseCmd.jsonFlag)
                .withTests(baseCmd.withTests)
                .build();
    }

    /**
     * Inspects {@code baseCmd} to determine which direction of code generation the user
     * has requested. When a {@code --service} value is present the user is exporting a
     * Ballerina service to AsyncAPI; otherwise the user is generating Ballerina from an
     * AsyncAPI definition.
     *
     * @param baseCmd the mixin holding the raw picocli-parsed values
     * @return the resolved {@link CmdConstants.Mode}
     */
    public static CmdConstants.Mode resolveMode(BaseCmd baseCmd) {
        if (baseCmd.serviceName != null && !baseCmd.serviceName.isBlank()) {
            return CmdConstants.Mode.BALLERINA_TO_ASYNCAPI;
        }
        return CmdConstants.Mode.ASYNCAPI_TO_BALLERINA;
    }
}
