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
 * User-facing error and warning message constants for the refactored asyncapi CLI commands.
 *
 */
public final class ErrorMessages {

    public static final String INVALID_INPUT_FILE =
            "Invalid input file: only .yaml, .yml, and .json AsyncAPI spec files are supported.";

    public static final String INVALID_INPUT_PATH =
            "error: invalid input path '%s': file does not exist or is not a valid AsyncAPI spec file.";

    public static final String INVALID_BAL_INPUT_PATH =
            "error: invalid input path '%s': file does not exist or is not a valid Ballerina source file.";

    public static final String MISSING_INPUT_FLAG =
            "An input AsyncAPI definition file must be provided with the --input/-i flag.";

    public static final String INVALID_OUTPUT_DIRECTORY =
            "Provided output path is not a valid directory.";

    public static final String INVALID_LICENSE_FILE =
            "Provided license file path is invalid or unreadable.";

    public static final String CLIENT_GENERATION_FAILED =
            "Client code generation failed: {0}";

    public static final String SERVICE_GENERATION_FAILED =
            "Service code generation failed: {0}";

    public static final String SUCCESS_MESSAGE =
            "Ballerina code generation completed successfully.";

    public static final String CODE_TO_SPEC_SUCCESS =
            "AsyncAPI spec generation completed successfully.";

    private ErrorMessages() {
    }
}
