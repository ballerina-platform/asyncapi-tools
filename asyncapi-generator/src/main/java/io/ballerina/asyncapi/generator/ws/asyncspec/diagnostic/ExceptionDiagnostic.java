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
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

/**
 * A diagnostic produced from a caught exception during Ballerina-to-AsyncAPI conversion.
 * Corresponds to the legacy {@code ExceptionDiagnostic}.
 */
public class ExceptionDiagnostic implements AsyncApiConverterDiagnostic {

    private final String code;
    private final String message;
    private final DiagnosticSeverity severity;

    /**
     * Creates a new exception-based diagnostic with ERROR severity, formatting the message with the given arguments.
     * The {@code location} parameter is accepted for API compatibility but not exposed.
     *
     * @param code     diagnostic code
     * @param message  human-readable message template (may contain {@code %s} placeholders)
     * @param location source location (accepted for API compatibility, may be null)
     * @param args     optional format arguments applied to {@code message}
     */
    public ExceptionDiagnostic(String code, String message, Location location, Object... args) {
        this(code, message, DiagnosticSeverity.ERROR, location, args);
    }

    /**
     * Creates a new exception-based diagnostic with an explicit severity, formatting the message with the given
     * arguments. The {@code location} parameter is accepted for API compatibility but not exposed.
     *
     * @param code     diagnostic code
     * @param message  human-readable message template (may contain {@code %s} placeholders)
     * @param severity diagnostic severity
     * @param location source location (accepted for API compatibility, may be null)
     * @param args     optional format arguments applied to {@code message}
     */
    public ExceptionDiagnostic(String code, String message, DiagnosticSeverity severity,
                                Location location, Object... args) {
        this.code = code;
        this.message = args.length > 0 ? String.format(message, args) : message;
        this.severity = severity;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public DiagnosticSeverity getDiagnosticSeverity() {
        return severity;
    }

    @Override
    public Optional<Location> getLocation() {
        return Optional.empty();
    }
}
