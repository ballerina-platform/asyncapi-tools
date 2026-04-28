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

    /** The http sub-command does not support Ballerina-to-AsyncAPI export. */
    ASYNC_CLI_002("ASYNC_CLI_002",
            "The http subcommand does not support Ballerina-to-AsyncAPI generation. "
            + "Use 'bal asyncapi ws --service <name> --input <file.bal>' instead."),

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
    ASYNC_CLI_009("ASYNC_CLI_009", "The .bal file has compilation errors:%s");

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
