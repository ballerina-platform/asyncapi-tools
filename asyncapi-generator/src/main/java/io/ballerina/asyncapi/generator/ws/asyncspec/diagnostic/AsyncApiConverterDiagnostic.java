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
 * Represents a diagnostic produced during Ballerina-to-AsyncAPI conversion.
 * Corresponds to the legacy {@code AsyncApiConverterDiagnostic}.
 */
public interface AsyncApiConverterDiagnostic {

    /**
     * Returns the diagnostic code.
     *
     * @return diagnostic code string
     */
    String getCode();

    /**
     * Returns the human-readable diagnostic message.
     *
     * @return diagnostic message
     */
    String getMessage();

    /**
     * Returns the severity of this diagnostic.
     *
     * @return diagnostic severity
     */
    DiagnosticSeverity getDiagnosticSeverity();

    /**
     * Returns the source location associated with this diagnostic, if any.
     *
     * @return an {@link Optional} containing the location, or empty if not available
     */
    Optional<Location> getLocation();
}
