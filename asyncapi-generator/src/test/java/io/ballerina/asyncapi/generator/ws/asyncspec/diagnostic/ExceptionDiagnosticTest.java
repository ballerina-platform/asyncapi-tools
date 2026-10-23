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
 * Unit tests for {@link ExceptionDiagnostic}.
 *
 * <p>NOTE: The constructor signature is {@code (String code, String message, Location location, Object... args)}.
 * There is no constructor that accepts a {@link DiagnosticMessages} directly; callers pass
 * {@code constant.getCode()} and {@code constant.getDescription()} as the first two arguments.
 * {@code getLocation()} always returns {@code Optional.empty()} — the location parameter is accepted
 * for API compatibility but is never stored.
 */
class ExceptionDiagnosticTest {

    @Test
    void testConstruction_doesNotThrow() {
        // AAS_CONVERTOR_100 has no %s — extra vararg is silently ignored by String.format
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null,
                new RuntimeException("boom"));
        Assert.assertNotNull(diagnostic,
                "ExceptionDiagnostic must construct without throwing");
    }

    @Test
    void testGetDiagnosticSeverity_defaultsToError() {
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null);
        Assert.assertEquals(diagnostic.getDiagnosticSeverity(), DiagnosticSeverity.ERROR,
                "Single-arg-severity constructor must default to DiagnosticSeverity.ERROR");
    }

    @Test
    void testGetCode_returnsExactConstantCode() {
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null);
        Assert.assertEquals(diagnostic.getCode(), "AAS_CONVERTOR_100",
                "getCode() must return the exact code passed to the constructor");
    }

    @Test
    void testGetMessage_containsExceptionMessage_whenExceptionIsFormatArg() {
        // AAS_CONVERTOR_102: "Failed to generate AsyncAPI definition due to: %s"
        // RuntimeException.toString() produces "java.lang.RuntimeException: boom" which contains "boom"
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_102.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_102.getDescription(),
                null,
                new RuntimeException("boom"));
        Assert.assertTrue(diagnostic.getMessage().contains("boom"),
                "getMessage() should contain the exception's message text when the exception is the format arg");
    }

    @Test
    void testGetLocation_alwaysEmpty_whenNullPassed() {
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null);
        Assert.assertFalse(diagnostic.getLocation().isPresent(),
                "getLocation() must always return Optional.empty() — location is not stored");
    }

    @Test
    void testGetMessage_nonNull_whenExceptionHasNullMessage() {
        // RuntimeException(null).toString() → "java.lang.RuntimeException" (no message suffix)
        // String.format("... %s", nullMsgException) produces a non-null string
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_102.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_102.getDescription(),
                null,
                new RuntimeException((String) null));
        Assert.assertNotNull(diagnostic.getMessage(),
                "getMessage() must not be null even when the format-arg exception carries a null message");
        Assert.assertFalse(diagnostic.getMessage().isEmpty(),
                "getMessage() must not be empty when constructed with a non-empty template");
    }

    @Test
    void testGetMessage_containsFormatArg_whenTemplateHasPlaceholder() {
        // AAS_CONVERTOR_101: "No Ballerina service found with name '%s' ... available services: %s"
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_101.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_101.getDescription(),
                null,
                "MY_SERVICE", "[/chat, /events]");
        Assert.assertTrue(diagnostic.getMessage().contains("MY_SERVICE"),
                "getMessage() must embed varargs into the description template via String.format");
    }

    @Test
    void testImplementsAsyncApiConverterDiagnostic() {
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null);
        Assert.assertTrue(diagnostic instanceof AsyncApiConverterDiagnostic,
                "ExceptionDiagnostic must implement AsyncApiConverterDiagnostic");
    }

    @Test
    void testExplicitSeverity_warningConstructor() {
        // Second constructor allows overriding severity — exercise the WARNING path
        ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_106.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_106.getDescription(),
                DiagnosticSeverity.WARNING,
                null,
                "SomeType");
        Assert.assertEquals(diagnostic.getDiagnosticSeverity(), DiagnosticSeverity.WARNING,
                "Explicit-severity constructor must honour the passed DiagnosticSeverity");
        Assert.assertEquals(diagnostic.getCode(), "AAS_CONVERTOR_106",
                "getCode() must reflect the code passed to the explicit-severity constructor");
    }
}
