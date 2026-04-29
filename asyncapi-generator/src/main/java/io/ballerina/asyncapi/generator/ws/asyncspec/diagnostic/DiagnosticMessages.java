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
package io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * Predefined diagnostic message codes and descriptions for Ballerina-to-AsyncAPI conversion.
 * Corresponds to the legacy {@code DiagnosticMessages}.
 */
public enum DiagnosticMessages {

    /** Ballerina file has compilation errors. */
    AAS_CONVERTOR_100("AAS_CONVERTOR_100", "Given Ballerina file contains compilation error(s).",
            DiagnosticSeverity.ERROR),
    /** No service found matching the requested name. */
    AAS_CONVERTOR_101("AAS_CONVERTOR_101",
            "No Ballerina service found with name '%s' to generate an AsyncAPI specification. "
                    + "These are the available services: %s",
            DiagnosticSeverity.ERROR),
    /** General I/O or generation failure. */
    AAS_CONVERTOR_102("AAS_CONVERTOR_102", "Failed to generate AsyncAPI definition due to: %s",
            DiagnosticSeverity.ERROR),
    /** Contract path in annotation is blank or missing. */
    AAS_CONVERTOR_103("AAS_CONVERTOR_103", "AsyncAPI contract path can not be blank.",
            DiagnosticSeverity.ERROR),
    /** File type is not YAML or JSON. */
    AAS_CONVERTOR_104("AAS_CONVERTOR_104",
            "Unsupported file type. Provide a valid contract file in .yaml or .json format.",
            DiagnosticSeverity.ERROR),
    /** Provided AsyncAPI contract has parsing errors. */
    AAS_CONVERTOR_105("AAS_CONVERTOR_105", "Provided AsyncAPI contract contains parsing error(s).",
            DiagnosticSeverity.ERROR),
    /** Generated spec missing information for a Ballerina type. */
    AAS_CONVERTOR_106("AAS_CONVERTOR_106",
            "Generated AsyncAPI definition does not contain information for Ballerina type '%s'. ",
            DiagnosticSeverity.WARNING),
    /** Generated spec has internal parsing errors. */
    AAS_CONVERTER_107("AAS_CONVERTOR_107", "Generated AsyncAPI definition contains parsing error(s): %s",
            DiagnosticSeverity.WARNING),
    /** Unexpected value encountered during generation. */
    AAS_CONVERTOR_108("AAS_CONVERTOR_108", "Unexpected value: %s", DiagnosticSeverity.ERROR);

    private final String code;
    private final String description;
    private final DiagnosticSeverity severity;

    DiagnosticMessages(String code, String description, DiagnosticSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    /**
     * Returns the diagnostic code.
     *
     * @return code string
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the diagnostic description (may contain {@code %s} format placeholders).
     *
     * @return description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the diagnostic severity.
     *
     * @return severity
     */
    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
