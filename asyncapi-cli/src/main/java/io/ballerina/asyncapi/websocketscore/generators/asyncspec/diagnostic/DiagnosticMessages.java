/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * This {@code DiagnosticMessages} enum class for containing the error message related to ballerina to asyncapi command.
 *
 * @since 2.0.0
 */
public enum DiagnosticMessages {

    AAS_CONVERTOR_100("AAS_CONVERTOR_100", "Given Ballerina file contains compilation error(s).",
            DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_101("AAS_CONVERTOR_101", "No Ballerina META-INF.services found with name '%s' to" +
            " generate an AsyncAPI specification. These META-INF.services are available in ballerina file. %s",
            DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_102("AAS_CONVERTOR_102", "Failed to generate AsyncAPI definition due to: %s",
            DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_103("AAS_CONVERTOR_103", "AsyncAPI contract path can not be blank.",
                      DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_104("AAS_CONVERTOR_104", "Unsupported file type. Provide a valid contract " +
            "file in .yaml or .json format.", DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_105("AAS_CONVERTOR_105", "Provided AsyncAPI contract contains parsing error(s).",
                      DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_106("AAS_CONVERTOR_106", "Generated AsyncAPI definition does not contain information "
            + "for Ballerina type '%s'. ", DiagnosticSeverity.WARNING),
    AAS_CONVERTER_107("AAS_CONVERTOR_107", "Generated AsyncAPI definition contains parsing error(s)",
            DiagnosticSeverity.ERROR),
    AAS_CONVERTOR_108("AAS_CONVERTOR_108", "Unexpected value: %s", DiagnosticSeverity.ERROR);

    private final String code;
    private final String description;
    private final DiagnosticSeverity severity;

    DiagnosticMessages(String code, String description, DiagnosticSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
