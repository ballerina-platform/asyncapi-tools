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
 * Unit tests for {@link DiagnosticMessages} verifying that every enum constant
 * has a well-formed code, a non-empty description, and the correct severity.
 */
class DiagnosticMessagesTest {

    @Test
    void testAllConstants_haveNonNullCode() {
        for (DiagnosticMessages msg : DiagnosticMessages.values()) {
            Assert.assertNotNull(msg.getCode(),
                    msg.name() + ".getCode() must not be null");
        }
    }

    @Test
    void testAllConstants_codeMatchesPattern() {
        // AAS_CONVERTER_107's *constant name* has a typo (ER vs OR) but its
        // getCode() string is "AAS_CONVERTOR_107" — the regex targets getCode().
        for (DiagnosticMessages msg : DiagnosticMessages.values()) {
            Assert.assertTrue(msg.getCode().matches("AAS_CONVERTOR_\\d+"),
                    msg.name() + " code must match AAS_CONVERTOR_\\d+, was: " + msg.getCode());
        }
    }

    @Test
    void testAllConstants_haveNonNullSeverity() {
        for (DiagnosticMessages msg : DiagnosticMessages.values()) {
            Assert.assertNotNull(msg.getSeverity(),
                    msg.name() + ".getSeverity() must not be null");
        }
    }

    @Test
    void testAllConstants_haveNonEmptyDescription() {
        for (DiagnosticMessages msg : DiagnosticMessages.values()) {
            String desc = msg.getDescription();
            Assert.assertNotNull(desc,
                    msg.name() + " description must not be null");
            Assert.assertFalse(desc.isEmpty(),
                    msg.name() + " description must not be empty");
        }
    }

    @Test
    void testDescriptionFormatArgs_containSentinelAfterFormatting() {
        // AAS_CONVERTOR_101: two %s (service name, available-services list)
        String result101 = String.format(DiagnosticMessages.AAS_CONVERTOR_101.getDescription(),
                "TEST_ARG", "TEST_ARG");
        Assert.assertTrue(result101.contains("TEST_ARG"),
                "AAS_CONVERTOR_101 formatted description should contain TEST_ARG");

        // AAS_CONVERTOR_102: one %s (failure reason)
        String result102 = String.format(DiagnosticMessages.AAS_CONVERTOR_102.getDescription(),
                "TEST_ARG");
        Assert.assertTrue(result102.contains("TEST_ARG"),
                "AAS_CONVERTOR_102 formatted description should contain TEST_ARG");

        // AAS_CONVERTOR_106: one %s (Ballerina type name)
        String result106 = String.format(DiagnosticMessages.AAS_CONVERTOR_106.getDescription(),
                "TEST_ARG");
        Assert.assertTrue(result106.contains("TEST_ARG"),
                "AAS_CONVERTOR_106 formatted description should contain TEST_ARG");

        // AAS_CONVERTER_107: one %s (parsing error detail)
        String result107 = String.format(DiagnosticMessages.AAS_CONVERTER_107.getDescription(),
                "TEST_ARG");
        Assert.assertTrue(result107.contains("TEST_ARG"),
                "AAS_CONVERTER_107 formatted description should contain TEST_ARG");

        // AAS_CONVERTOR_108: one %s (unexpected value)
        String result108 = String.format(DiagnosticMessages.AAS_CONVERTOR_108.getDescription(),
                "TEST_ARG");
        Assert.assertTrue(result108.contains("TEST_ARG"),
                "AAS_CONVERTOR_108 formatted description should contain TEST_ARG");
    }

    @Test
    void testKnownSeverities_errorAndWarning() {
        Assert.assertEquals(DiagnosticMessages.AAS_CONVERTOR_100.getSeverity(), DiagnosticSeverity.ERROR,
                "AAS_CONVERTOR_100 must be ERROR severity");
        Assert.assertEquals(DiagnosticMessages.AAS_CONVERTOR_106.getSeverity(), DiagnosticSeverity.WARNING,
                "AAS_CONVERTOR_106 must be WARNING severity");
        Assert.assertEquals(DiagnosticMessages.AAS_CONVERTER_107.getSeverity(), DiagnosticSeverity.WARNING,
                "AAS_CONVERTER_107 must be WARNING severity");
        Assert.assertEquals(DiagnosticMessages.AAS_CONVERTOR_108.getSeverity(), DiagnosticSeverity.ERROR,
                "AAS_CONVERTOR_108 must be ERROR severity");
    }

    @Test
    void testValueOf_roundTrip() {
        for (DiagnosticMessages msg : DiagnosticMessages.values()) {
            Assert.assertEquals(DiagnosticMessages.valueOf(msg.name()), msg,
                    "valueOf round-trip failed for " + msg.name());
        }
    }
}
