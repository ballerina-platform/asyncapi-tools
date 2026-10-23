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
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link IncompatibleRemoteDiagnostic}.
 *
 * <p>Key differences from {@link ExceptionDiagnostic}:
 * <ul>
 *   <li>The constructor accepts a {@link DiagnosticMessages} constant directly.</li>
 *   <li>{@code message} is always produced via {@code String.format(details.getDescription(), args)},
 *       so any constant whose description contains {@code %s} must be called with matching varargs.</li>
 *   <li>{@code severity} mirrors {@code details.getSeverity()} — it is not hardcoded to ERROR.</li>
 *   <li>{@code getLocation()} returns {@code Optional.ofNullable(location)}, so passing {@code null}
 *       yields {@code Optional.empty()} and a real {@link io.ballerina.tools.diagnostics.Location}
 *       would be present.</li>
 * </ul>
 */
class IncompatibleRemoteDiagnosticTest {

    @Test
    void testConstruction_withNullLocation_doesNotThrow() {
        // AAS_CONVERTOR_100 has no %s — zero varargs is safe
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_100, null);
        Assert.assertNotNull(diagnostic,
                "IncompatibleRemoteDiagnostic must construct without throwing when location is null");
    }

    @Test
    void testGetCode_returnsConstantCodeExactly() {
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_100, null);
        Assert.assertEquals(diagnostic.getCode(), "AAS_CONVERTOR_100",
                "getCode() must return the exact code from the DiagnosticMessages constant");
    }

    @Test
    void testGetDiagnosticSeverity_mirrorsConstantSeverity() {
        // Severity is NOT hardcoded — it copies details.getSeverity(). Verify for both ERROR and WARNING.
        IncompatibleRemoteDiagnostic errorDiagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_100, null);
        Assert.assertEquals(errorDiagnostic.getDiagnosticSeverity(), DiagnosticSeverity.ERROR,
                "AAS_CONVERTOR_100 is ERROR — getDiagnosticSeverity() must mirror the constant's severity");

        // AAS_CONVERTOR_106 is WARNING and has one %s — supply the required arg
        IncompatibleRemoteDiagnostic warnDiagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_106, null, "SomeType");
        Assert.assertEquals(warnDiagnostic.getDiagnosticSeverity(), DiagnosticSeverity.WARNING,
                "AAS_CONVERTOR_106 is WARNING — getDiagnosticSeverity() must mirror the constant's severity");
    }

    @Test
    void testGetMessage_containsFunctionName_whenUsedAsFormatArg() {
        // AAS_CONVERTOR_108: "Unexpected value: %s" — one %s, embed function name
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_108, null, "onMessage");
        Assert.assertTrue(diagnostic.getMessage().contains("onMessage"),
                "getMessage() must embed varargs into the description template; should contain 'onMessage'");
    }

    @Test
    void testGetLocation_isEmpty_whenNullPassed() {
        // getLocation() returns Optional.ofNullable(location) — null → Optional.empty()
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_100, null);
        Assert.assertFalse(diagnostic.getLocation().isPresent(),
                "getLocation() must return Optional.empty() when null was passed as location");
    }

    @Test
    void testGetMessage_withSpecialCharacterFunctionName_doesNotThrow() {
        // String.format treats $ and _ as literals — no format-specifier confusion
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_108, null, "on$Message_2");
        Assert.assertTrue(diagnostic.getMessage().contains("on$Message_2"),
                "getMessage() must contain 'on$Message_2' without throwing when the name contains special chars");
    }

    @Test
    void testImplementsAsyncApiConverterDiagnostic() {
        IncompatibleRemoteDiagnostic diagnostic =
                new IncompatibleRemoteDiagnostic(DiagnosticMessages.AAS_CONVERTOR_100, null);
        Assert.assertTrue(diagnostic instanceof AsyncApiConverterDiagnostic,
                "IncompatibleRemoteDiagnostic must implement AsyncApiConverterDiagnostic");
    }
}
