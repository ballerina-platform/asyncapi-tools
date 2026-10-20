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

/**
 * Diagnostic constants for the asyncapi CLI tool.
 * Each constant pairs a stable code string with a {@link String#format}-style message template.
 * Instances are passed to {@link AsyncApiCmdToolException} constructors; the exception formats
 * and stores the final message at construction time.
 *
 */
public enum AsyncApiCmdToolDiagnostic {

    /** General code-generation failure; {@code %s} is replaced with the underlying error message. */
    ASYNC_CLI_001("ASYNC_CLI_001", "Code generation failed: %s"),

    /** The protocol does not support Ballerina-to-AsyncAPI generation; {@code %s} is the protocol name. */
    ASYNC_CLI_002("ASYNC_CLI_002",
            "The %s protocol does not support Ballerina-to-AsyncAPI generation."),

    /** Ballerina workspace detected during project resolution; use --output to target a package. */
    ASYNC_CLI_003("ASYNC_CLI_003",
            "Ballerina workspace detected. "
            + "Use --output to point to a package directory inside the workspace."),

    /** No Ballerina project found at or above the target output directory. */
    ASYNC_CLI_004("ASYNC_CLI_004",
            "No Ballerina project found. Run 'bal new <project-name>' first, then retry."),

    /** Ballerina.toml exists but has no valid [package] table; {@code %s} is the resolved path. */
    ASYNC_CLI_005("ASYNC_CLI_005",
            "No valid Ballerina package found at: %s. "
            + "Ensure Ballerina.toml contains a [package] table."),

    /** The supplied module name contains illegal characters; {@code %s} is the name. */
    ASYNC_CLI_006("ASYNC_CLI_006",
            "Invalid module name '%s'. "
            + "Module names can only contain alphanumerics and underscores."),

    /** The supplied module name exceeds the 256-character limit; {@code %s} is the name. */
    ASYNC_CLI_007("ASYNC_CLI_007",
            "Module name '%s' is too long. Maximum length is 256 characters."),

    /** Ballerina.toml is present but is missing the name field under [package]. */
    ASYNC_CLI_008("ASYNC_CLI_008", "Could not find package name in Ballerina.toml."),

    /** The supplied .bal file has compilation errors; {@code %s} is the newline-separated list. */
    ASYNC_CLI_009("ASYNC_CLI_009", "The .bal file has compilation errors:%s"),

    /** Output path is inside project but not root or modules/<name>; {@code %s} is the path. */
    ASYNC_CLI_010("ASYNC_CLI_010",
            "Unrecognized module location: %s. "
            + "Use the project root, modules/<module>, or pass --module to target a specific module."),

    /** Input directory has no Ballerina.toml; {@code %s} is the path. */
    ASYNC_CLI_011("ASYNC_CLI_011",
            "'%s' is not a Ballerina package: it has no Ballerina.toml. "
            + "Pass a package directory or a standalone .bal file."),

    /** Input .bal file sits inside a package; first {@code %s} is the file, second the package root. */
    ASYNC_CLI_012("ASYNC_CLI_012",
            "'%s' belongs to the Ballerina package at '%s'. "
            + "Pass the package directory instead of a file inside it, as 'bal run' does."),

    /** Requested module is not in the package; first {@code %s} is the module, second the available ones. */
    ASYNC_CLI_013("ASYNC_CLI_013",
            "Module '%s' not found in the package. Available modules: %s."),

    /** Input directory is a workspace root; {@code %s} is the path. */
    ASYNC_CLI_014("ASYNC_CLI_014",
            "'%s' is a Ballerina workspace. Pass the directory of one of its packages.");

    private final String code;
    private final String message;

    AsyncApiCmdToolDiagnostic(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the stable diagnostic code string (e.g. {@code "ASYNC_CLI_001"}).
     *
     * @return diagnostic code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the {@link String#format}-style message template for this diagnostic.
     *
     * @return message template
     */
    public String getMessage() {
        return message;
    }
}
