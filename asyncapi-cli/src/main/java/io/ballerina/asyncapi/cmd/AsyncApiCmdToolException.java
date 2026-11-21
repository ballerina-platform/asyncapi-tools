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

import java.util.ArrayList;
import java.util.List;

/**
 * The one error type of the command. Every check fails by throwing it, and the command prints it and exits with
 * the code its kind maps to (Appendix B of the Ballerina CLI Tool Design Guide).
 */
public class AsyncApiCmdToolException extends RuntimeException {

    /** What went wrong, which decides the exit code. */
    public enum Kind {
        USAGE("usage", ExitCodes.USAGE),
        INVALID_TARGET("invalid-target", ExitCodes.INVALID_TARGET),
        OVERWRITE_REFUSED("overwrite-refused", ExitCodes.OVERWRITE_REFUSED),
        INVALID_INPUT("invalid-input", ExitCodes.RUNTIME),
        RUNTIME("runtime", ExitCodes.RUNTIME),
        /** A runtime error: the package has no Dependencies.toml to compile with. */
        DEPENDENCIES_NOT_LOCKED("dependencies-not-locked", ExitCodes.RUNTIME);

        private final String label;
        private final int exitCode;

        Kind(String label, int exitCode) {
            this.label = label;
            this.exitCode = exitCode;
        }

        /**
         * Returns the kind as it appears in a JSON error, e.g. {@code invalid-target}.
         *
         * @return the label
         */
        public String label() {
            return label;
        }

        /**
         * Returns the exit code for this kind.
         *
         * @return the exit code
         */
        public int exitCode() {
            return exitCode;
        }
    }

    /**
     * A problem at a place in an input file.
     *
     * @param path     the file, as the user gave it
     * @param line     1-based line, or 0 when unknown
     * @param column   1-based column, or 0 when unknown
     * @param severity {@code error} or {@code warning}
     * @param message  what is wrong there
     */
    public record InputDiagnostic(String path, int line, int column, String severity, String message) {
    }

    private final Kind kind;
    private final String diagnosticCode;
    private String suggestion;
    private String command;
    private final List<InputDiagnostic> diagnostics = new ArrayList<>();

    /**
     * Creates an error from a diagnostic constant, formatting its message and suggestion with the arguments.
     *
     * @param diagnostic the diagnostic constant
     * @param args       arguments for the message and suggestion templates
     */
    public AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic diagnostic, String... args) {
        this(null, diagnostic, args);
    }

    /**
     * Creates an error from a diagnostic constant, keeping the exception that caused it.
     *
     * @param cause      the underlying exception
     * @param diagnostic the diagnostic constant
     * @param args       arguments for the message and suggestion templates
     */
    public AsyncApiCmdToolException(Throwable cause, AsyncApiCmdToolDiagnostic diagnostic, String... args) {
        super(String.format(diagnostic.getMessage(), (Object[]) args), cause);
        this.kind = diagnostic.getKind();
        this.diagnosticCode = diagnostic.getCode();
        this.suggestion = diagnostic.getSuggestion() == null ? null
                : String.format(diagnostic.getSuggestion(), (Object[]) args);
    }

    private AsyncApiCmdToolException(Kind kind, String message) {
        super(message);
        this.kind = kind;
        this.diagnosticCode = "";
    }

    /**
     * Creates an error of the given kind.
     *
     * @param kind    what went wrong
     * @param message one line, starting in lowercase, without a trailing period
     * @return the error
     */
    public static AsyncApiCmdToolException of(Kind kind, String message) {
        return new AsyncApiCmdToolException(kind, message);
    }

    /**
     * Adds how to fix the error, as one imperative sentence.
     *
     * @param suggestion the suggestion
     * @return this error
     */
    public AsyncApiCmdToolException withSuggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    /**
     * Adds the command that fixes the error, when exactly one fix applies.
     *
     * @param command the full command, runnable as printed
     * @return this error
     */
    public AsyncApiCmdToolException withCommand(String command) {
        this.command = command;
        return this;
    }

    /**
     * Adds problems found in an input file.
     *
     * @param inputDiagnostics the problems
     * @return this error
     */
    public AsyncApiCmdToolException withDiagnostics(List<InputDiagnostic> inputDiagnostics) {
        this.diagnostics.addAll(inputDiagnostics);
        return this;
    }

    /**
     * Returns what went wrong.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the exit code the command ends with.
     *
     * @return the exit code
     */
    public int exitCode() {
        return kind.exitCode();
    }

    /**
     * Returns the stable diagnostic code, or an empty string for an error made with {@link #of}.
     *
     * @return diagnostic code
     */
    public String getDiagnosticCode() {
        return diagnosticCode;
    }

    /**
     * Returns the message, the same as {@link #getMessage()}.
     *
     * @return the message
     */
    public String getDiagnosticMessage() {
        return getMessage();
    }

    /**
     * Returns how to fix the error, or {@code null}.
     *
     * @return the suggestion
     */
    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Returns the command that fixes the error, or {@code null}.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the problems found in an input file.
     *
     * @return the diagnostics, possibly empty
     */
    public List<InputDiagnostic> getDiagnostics() {
        return List.copyOf(diagnostics);
    }
}
