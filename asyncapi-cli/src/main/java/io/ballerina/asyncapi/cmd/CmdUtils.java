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
import java.util.ArrayList;
import java.util.List;

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
     * Returns {@code true} if the given path is an existing directory.
     *
     * @param inputPath path to check; may be {@code null}
     * @return {@code true} if it is an existing directory
     */
    public static boolean isDirectory(String inputPath) {
        return inputPath != null && !inputPath.isBlank() && Files.isDirectory(Path.of(inputPath));
    }

    /**
     * Returns {@code true} if {@code inputPath} is an existing {@code .bal} file or an existing directory,
     * the two shapes accepted when generating an AsyncAPI definition from Ballerina code.
     *
     * @param inputPath input path provided by the user; may be {@code null}
     * @return {@code true} if it is a Ballerina source file or a directory
     */
    public static boolean isValidBallerinaInput(String inputPath) {
        return isDirectory(inputPath) || isValidBallerinaFile(inputPath);
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
     *         {@link ProtocolCommand#generate(CmdOptions, CmdConstants.Mode, CmdContext)}
     */
    public static CmdOptions collectCmdOptions(BaseCmd baseCmd) {
        return collectCmdOptions(baseCmd, baseCmd.module);
    }

    public static CmdOptions collectCmdOptions(BaseCmd baseCmd, String module) {
        return new CmdOptions.CmdOptionsBuilder()
                .withInput(baseCmd.inputPath)
                .withOutput(baseCmd.outputPath)
                .withLicenseFilePath(baseCmd.licensePath)
                .withService(baseCmd.serviceName)
                .withJson(CmdConstants.FORMAT_JSON.equalsIgnoreCase(specFormat(baseCmd)))
                .withTests(baseCmd.withTests)
                .withModule(module)
                .withForce(baseCmd.force)
                .withDryRun(baseCmd.dryRun)
                .withServiceSide(CmdConstants.SPEC_SIDE_SERVICE.equalsIgnoreCase(baseCmd.specSide))
                .withCommandLine(commandLine(baseCmd, baseCmd.inputPath))
                .build();
    }

    /**
     * Returns the definition format asked for with {@code --spec-format}, or with the old {@code --format}.
     *
     * @param baseCmd the parsed flags
     * @return the format, or {@code null} for the default
     */
    public static String specFormat(BaseCmd baseCmd) {
        return baseCmd.specFormat != null ? baseCmd.specFormat : baseCmd.format;
    }

    /**
     * Rebuilds the command the user ran from the parsed flags, so an error can print the fixed command. Hidden and
     * removed flags are left out.
     *
     * @param baseCmd    the parsed flags
     * @param input      the input to put in the command, the user's own or a corrected one
     * @param extraFlags flags to add, such as {@code --force}
     * @return the command, runnable as printed
     */
    public static String commandLine(BaseCmd baseCmd, String input, String... extraFlags) {
        List<String> words = new ArrayList<>(List.of("bal", "asyncapi"));
        if (input != null) {
            words.add(quote(input));
        }
        addOption(words, "--protocol", baseCmd.protocol);
        addOption(words, "-o", baseCmd.outputPath);
        addOption(words, "--module", baseCmd.module);
        addOption(words, "--license", baseCmd.licensePath);
        addOption(words, "--spec-format", specFormat(baseCmd));
        addOption(words, "--service", baseCmd.serviceName);
        addOption(words, "--spec-side", baseCmd.specSide);
        addFlag(words, "--with-tests", baseCmd.withTests);
        addFlag(words, "--force", baseCmd.force);
        addFlag(words, "--dry-run", baseCmd.dryRun);
        addOption(words, "--output-format", baseCmd.outputFormat);
        for (String flag : extraFlags) {
            if (!words.contains(flag)) {
                words.add(flag);
            }
        }
        return String.join(" ", words);
    }

    private static void addOption(List<String> words, String flag, String value) {
        if (value != null) {
            words.add(flag);
            words.add(quote(value));
        }
    }

    private static void addFlag(List<String> words, String flag, boolean set) {
        if (set) {
            words.add(flag);
        }
    }

    // Quotes a word a shell would split, so the printed command runs as shown.
    static String quote(String word) {
        return word.isEmpty() || word.matches(".*[\\s'\"$`\\\\|&;<>()*?].*")
                ? "'" + word.replace("'", "'\\''") + "'" : word;
    }

    /**
     * Turns a message from elsewhere into one line that starts in lowercase and has no trailing period, as an error
     * message is written.
     *
     * @param message the message, possibly several lines
     * @return the one-line message
     */
    public static String oneLine(String message) {
        if (message == null || message.isBlank()) {
            return "an unexpected error occurred";
        }
        String line = String.join("; ", message.strip().lines().map(String::strip)
                .filter(part -> !part.isEmpty()).toList()).replace(":;", ":");
        while (line.endsWith(".")) {
            line = line.substring(0, line.length() - 1);
        }
        // A first word with more capitals is a name, such as AsyncAPI or WebSocket, and keeps its case
        String firstWord = line.split("\\s", 2)[0];
        boolean name = firstWord.chars().skip(1).anyMatch(Character::isUpperCase);
        return name ? line : Character.toLowerCase(line.charAt(0)) + line.substring(1);
    }

    /**
     * Inspects {@code baseCmd} to determine which direction of code generation the user
     * has requested. When the {@code --input} file has a {@code .bal} extension the user
     * is exporting a Ballerina service to AsyncAPI; otherwise the user is generating
     * Ballerina from an AsyncAPI definition.
     *
     * @param baseCmd the mixin holding the raw picocli-parsed values
     * @return the resolved {@link CmdConstants.Mode}
     */
    public static CmdConstants.Mode resolveMode(BaseCmd baseCmd) {
        if (isBallerinaFile(baseCmd.inputPath) || isDirectory(baseCmd.inputPath)) {
            return CmdConstants.Mode.BALLERINA_TO_ASYNCAPI;
        }
        return CmdConstants.Mode.ASYNCAPI_TO_BALLERINA;
    }
}
