/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.trigger.plugin;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import static io.ballerina.tools.diagnostics.DiagnosticSeverity.ERROR;
import static io.ballerina.tools.diagnostics.DiagnosticSeverity.WARNING;

/**
 * Diagnostics of the trigger compiler plugin. The messages match the {@code ballerina/http} plugin's for the same
 * situations, so trigger and HTTP services report a port problem the same way.
 */
public enum AsyncAPIDiagnostic {

    ASYNCAPI_ERROR_101("ASYNCAPI_ERROR_101", "The configurable value provided for the port should have a "
            + "default value to generate the server details", ERROR),
    ASYNCAPI_ERROR_102("ASYNCAPI_ERROR_102", "Unsupported expression found for the server port value", ERROR),

    ASYNCAPI_WARNING_101("ASYNCAPI_WARNING_101", "The server port is defined as a configurable. Hence, using the "
            + "default value to generate the server information", WARNING),
    ASYNCAPI_WARNING_102("ASYNCAPI_WARNING_102", "The Ballerina version is not supported for endpoints.yaml. "
            + "Try using Ballerina 2201.13.6 or above", WARNING);

    private final String code;
    private final String message;
    private final DiagnosticSeverity severity;

    AsyncAPIDiagnostic(String code, String message, DiagnosticSeverity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
