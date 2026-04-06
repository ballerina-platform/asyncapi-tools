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
            + "Use 'bal asyncapi ws --service <name> --input <file.bal>' instead.");

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
