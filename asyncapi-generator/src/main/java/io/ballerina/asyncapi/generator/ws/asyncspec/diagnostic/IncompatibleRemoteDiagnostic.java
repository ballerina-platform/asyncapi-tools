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
 * A diagnostic reporting an incompatible remote function found during
 * Ballerina-to-AsyncAPI conversion.
 */
public class IncompatibleRemoteDiagnostic implements AsyncApiConverterDiagnostic {

    private final String code;
    private final String message;
    private final Location location;
    private final DiagnosticSeverity severity;

    /**
     * Creates a new incompatible-remote diagnostic.
     *
     * @param details  the diagnostic message template
     * @param location source location, or null
     * @param args     format arguments for the message template
     */
    public IncompatibleRemoteDiagnostic(DiagnosticMessages details,
                                        Location location,
                                        Object... args) {
        this.code = details.getCode();
        this.message = String.format(details.getDescription(), args);
        this.location = location;
        this.severity = details.getSeverity();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public DiagnosticSeverity getDiagnosticSeverity() {
        return severity;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }
}
