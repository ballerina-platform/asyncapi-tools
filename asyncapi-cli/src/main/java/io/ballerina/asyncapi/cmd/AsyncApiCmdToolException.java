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
 * Checked exception thrown by the refactored asyncapi CLI tool when a recoverable error occurs
 * during command execution or code generation.
 *
 */
public class AsyncApiCmdToolException extends RuntimeException {

    private final String diagnosticCode;
    private final String diagnosticMessage;

    /**
     * Constructs an {@code AsyncApiCmdToolException} from a diagnostic constant and optional
     * format arguments. The diagnostic's message template is formatted with {@code args} via
     * {@link String#format}.
     *
     * @param diagnostic the diagnostic enum constant carrying the code and message template
     * @param args       format arguments applied to {@code diagnostic.getMessage()}
     */
    public AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic diagnostic, String... args) {
        super("ERROR: " + String.format(diagnostic.getMessage(), (Object[]) args));
        this.diagnosticCode = diagnostic.getCode();
        this.diagnosticMessage = String.format(diagnostic.getMessage(), (Object[]) args);
    }

    /**
     * Constructs an {@code AsyncApiCmdToolException} wrapping a cause, from a diagnostic constant
     * and optional format arguments.
     *
     * @param cause      the underlying exception that triggered this one
     * @param diagnostic the diagnostic enum constant carrying the code and message template
     * @param args       format arguments applied to {@code diagnostic.getMessage()}
     */
    public AsyncApiCmdToolException(Throwable cause, AsyncApiCmdToolDiagnostic diagnostic,
                                    String... args) {
        super("ERROR: " + String.format(diagnostic.getMessage(), (Object[]) args), cause);
        this.diagnosticCode = diagnostic.getCode();
        this.diagnosticMessage = String.format(diagnostic.getMessage(), (Object[]) args);
    }

    /**
     * Returns the stable diagnostic code for this exception, or an empty string if none was set.
     *
     * @return diagnostic code string
     */
    public String getDiagnosticCode() {
        return diagnosticCode;
    }

    /**
     * Returns the human-readable diagnostic message without the code prefix.
     *
     * @return diagnostic message string
     */
    public String getDiagnosticMessage() {
        return diagnosticMessage;
    }
}
