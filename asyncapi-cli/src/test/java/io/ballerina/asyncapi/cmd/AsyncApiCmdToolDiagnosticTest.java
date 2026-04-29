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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AsyncApiCmdToolDiagnostic} and {@link AsyncApiCmdToolException}.
 */
public class AsyncApiCmdToolDiagnosticTest extends CmdTestBase {

    @Test(description = "ASYNC_CLI_001 enum constant has correct code")
    void testAsyncCli001Code() {
        Assert.assertEquals(
                AsyncApiCmdToolDiagnostic.ASYNC_CLI_001.getCode(),
                "ASYNC_CLI_001");
    }

    @Test(description = "ASYNC_CLI_001 enum constant has correct message template")
    void testAsyncCli001Message() {
        Assert.assertEquals(
                AsyncApiCmdToolDiagnostic.ASYNC_CLI_001.getMessage(),
                "Code generation failed: %s");
    }

    @Test(description = "ASYNC_CLI_002 enum constant has correct code")
    void testAsyncCli002Code() {
        Assert.assertEquals(
                AsyncApiCmdToolDiagnostic.ASYNC_CLI_002.getCode(),
                "ASYNC_CLI_002");
    }

    @Test(description = "ASYNC_CLI_002 enum constant has non-blank message")
    void testAsyncCli002MessageNonBlank() {
        Assert.assertFalse(
                AsyncApiCmdToolDiagnostic.ASYNC_CLI_002.getMessage().isBlank(),
                "ASYNC_CLI_002 message must not be blank");
    }

    @Test(description = "Exception from no-args diagnostic: getMessage() has 'ERROR: ' prefix")
    void testExceptionMessageHasErrorPrefix() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002);

        Assert.assertTrue(
                ex.getMessage().startsWith("ERROR: "),
                "getMessage() must start with 'ERROR: '. Got: " + ex.getMessage());
    }

    @Test(description = "Exception from no-args diagnostic: getDiagnosticCode() returns enum code")
    void testExceptionDiagnosticCode() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002);

        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_002");
    }

    @Test(description = "Exception from no-args diagnostic: getDiagnosticMessage() has no 'ERROR: ' prefix")
    void testExceptionDiagnosticMessageNoPrefix() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002);

        Assert.assertFalse(
                ex.getDiagnosticMessage().startsWith("ERROR: "),
                "getDiagnosticMessage() must not have 'ERROR: ' prefix. Got: "
                        + ex.getDiagnosticMessage());
    }

    @Test(description = "Exception with format args: %s placeholder is substituted in getMessage()")
    void testExceptionWithFormatArgs() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(
                        AsyncApiCmdToolDiagnostic.ASYNC_CLI_001, "file not found");

        Assert.assertEquals(
                ex.getMessage(),
                "ERROR: Code generation failed: file not found");
    }

    @Test(description = "Exception with format args: getDiagnosticMessage() has no 'ERROR: ' prefix")
    void testExceptionWithFormatArgsDiagnosticMessage() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(
                        AsyncApiCmdToolDiagnostic.ASYNC_CLI_001, "file not found");

        Assert.assertEquals(
                ex.getDiagnosticMessage(),
                "Code generation failed: file not found");
    }

    @Test(description = "Exception with cause: getCause() returns wrapped throwable")
    void testExceptionWithCause() {
        IllegalStateException cause = new IllegalStateException("root cause");
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(
                        cause, AsyncApiCmdToolDiagnostic.ASYNC_CLI_001, "something failed");

        Assert.assertSame(ex.getCause(), cause,
                "getCause() must return the wrapped throwable");
    }

    @Test(description = "Exception with cause: getMessage() is correctly formatted")
    void testExceptionWithCauseMessage() {
        IllegalStateException cause = new IllegalStateException("root cause");
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(
                        cause, AsyncApiCmdToolDiagnostic.ASYNC_CLI_001, "something failed");

        Assert.assertEquals(
                ex.getMessage(),
                "ERROR: Code generation failed: something failed");
    }

    @Test(description = "Exception is a RuntimeException (unchecked)")
    void testExceptionIsUnchecked() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002);

        Assert.assertTrue(
                ex instanceof RuntimeException,
                "AsyncApiCmdToolException must extend RuntimeException");
    }
}
