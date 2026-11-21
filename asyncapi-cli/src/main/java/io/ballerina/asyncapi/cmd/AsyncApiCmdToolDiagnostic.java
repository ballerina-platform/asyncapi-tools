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

import io.ballerina.asyncapi.cmd.AsyncApiCmdToolException.Kind;

/**
 * The errors the command reports about its target and module, each with a stable code, the kind that decides the
 * exit code, a one-line message and a suggestion on how to fix it. The message and the suggestion are
 * {@link String#format} templates that share the same arguments.
 */
public enum AsyncApiCmdToolDiagnostic {

    /** General code-generation failure; {@code %s} is the underlying error. */
    ASYNC_CLI_001("ASYNC_CLI_001", Kind.RUNTIME, "code generation failed: %s", null),

    /** The protocol can't generate a definition from code; {@code %s} is the protocol name. */
    ASYNC_CLI_002("ASYNC_CLI_002", Kind.USAGE,
            "the %s protocol can't generate an AsyncAPI definition from Ballerina code",
            "Pass an AsyncAPI definition, or use --protocol ws to generate a definition from Ballerina code."),

    /** The output location is a workspace; {@code %s} is the path. */
    ASYNC_CLI_003("ASYNC_CLI_003", Kind.INVALID_TARGET, "'%s' is a Ballerina workspace",
            "Pass one of its packages with --output."),

    /** The output location is not in a package; {@code %s} is the path. */
    ASYNC_CLI_004("ASYNC_CLI_004", Kind.INVALID_TARGET, "'%s' is not a Ballerina package",
            "Create a package with 'bal new', or pass one with --output."),

    /** Ballerina.toml has no [package] table; {@code %s} is the package root. */
    ASYNC_CLI_005("ASYNC_CLI_005", Kind.INVALID_TARGET, "the Ballerina.toml in '%s' has no [package] table",
            "Add a [package] table with the package's org, name and version."),

    /** The module name has characters a module name can't have; {@code %s} is the name. */
    ASYNC_CLI_006("ASYNC_CLI_006", Kind.USAGE, "invalid module name '%s'",
            "Use only letters, digits and underscores in a module name."),

    /** The module name is too long; {@code %s} is the name. */
    ASYNC_CLI_007("ASYNC_CLI_007", Kind.USAGE, "module name '%s' is longer than 256 characters",
            "Use a shorter module name."),

    /** Ballerina.toml has no package name. */
    ASYNC_CLI_008("ASYNC_CLI_008", Kind.INVALID_TARGET, "could not read the package name from Ballerina.toml",
            "Add a name to the [package] table of Ballerina.toml."),

    /** The Ballerina code does not compile; {@code %s} is the input. */
    ASYNC_CLI_009("ASYNC_CLI_009", Kind.INVALID_INPUT, "'%s' has compilation errors",
            "Fix the errors, then re-run."),

    /** The output location is inside a package but not its root or a module; {@code %s} is the path. */
    ASYNC_CLI_010("ASYNC_CLI_010", Kind.INVALID_TARGET, "'%s' is not the root or a module of a package",
            "Pass the package root with --output, and choose a module with --module."),

    /** The input directory is not a package; {@code %s} is the path. */
    ASYNC_CLI_011("ASYNC_CLI_011", Kind.INVALID_TARGET, "'%s' is not a Ballerina package",
            "Pass a package directory or a standalone .bal file."),

    /** The input lies inside a package; the first {@code %s} is the input, the second the package root. */
    ASYNC_CLI_012("ASYNC_CLI_012", Kind.INVALID_TARGET, "'%s' belongs to the Ballerina package at '%s'",
            "Pass the package directory instead."),

    /** The package has no such module; the first {@code %s} is the module, the second the ones it has. */
    ASYNC_CLI_013("ASYNC_CLI_013", Kind.INVALID_TARGET, "module '%s' not found in the package",
            "Use one of its modules: %2$s."),

    /** The input is a workspace root; the first {@code %s} is the path, the second its packages. */
    ASYNC_CLI_014("ASYNC_CLI_014", Kind.INVALID_TARGET, "'%s' is a Ballerina workspace",
            "Pass the directory of one of its packages: %2$s."),

    /** The input is a bala; {@code %s} is the path. */
    ASYNC_CLI_015("ASYNC_CLI_015", Kind.INVALID_TARGET, "'%s' is a bala, not a Ballerina package",
            "Pass a package directory or a standalone .bal file."),

    /** The package has no Dependencies.toml; {@code %s} is the package name. */
    ASYNC_CLI_016("ASYNC_CLI_016", Kind.DEPENDENCIES_NOT_LOCKED, "dependencies are not locked for package '%s'",
            "Run 'bal build' to lock the dependencies, then re-run."),

    /** The Ballerina input does not exist; {@code %s} is the path. */
    ASYNC_CLI_017("ASYNC_CLI_017", Kind.INVALID_TARGET, "'%s' does not exist",
            "Pass a package directory or a standalone .bal file.");

    private final String code;
    private final Kind kind;
    private final String message;
    private final String suggestion;

    AsyncApiCmdToolDiagnostic(String code, Kind kind, String message, String suggestion) {
        this.code = code;
        this.kind = kind;
        this.message = message;
        this.suggestion = suggestion;
    }

    /**
     * Returns the stable diagnostic code (e.g. {@code "ASYNC_CLI_001"}).
     *
     * @return diagnostic code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the kind of error, which decides the exit code.
     *
     * @return the error kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the {@link String#format}-style message template.
     *
     * @return message template
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the {@link String#format}-style suggestion template, or {@code null} if there is none.
     *
     * @return suggestion template
     */
    public String getSuggestion() {
        return suggestion;
    }
}
