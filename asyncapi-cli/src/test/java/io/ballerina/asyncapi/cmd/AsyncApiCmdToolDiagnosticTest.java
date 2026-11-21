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
        Assert.assertEquals(AsyncApiCmdToolDiagnostic.ASYNC_CLI_001.getCode(), "ASYNC_CLI_001");
    }

    @Test(description = "Every message is one line that starts in lowercase and has no trailing period (ERR-1)")
    void testMessagesFollowTheGuide() {
        for (AsyncApiCmdToolDiagnostic diagnostic : AsyncApiCmdToolDiagnostic.values()) {
            String message = diagnostic.getMessage();
            Assert.assertFalse(message.isBlank(), diagnostic.getCode());
            Assert.assertFalse(message.contains("\n"), diagnostic.getCode() + ": " + message);
            Assert.assertFalse(message.endsWith("."), diagnostic.getCode() + ": " + message);
            char first = message.charAt(0);
            Assert.assertFalse(Character.isUpperCase(first), diagnostic.getCode() + ": " + message);
        }
    }

    @Test(description = "Every suggestion is one imperative sentence ending in a period (ERR-2)")
    void testSuggestionsAreSentences() {
        for (AsyncApiCmdToolDiagnostic diagnostic : AsyncApiCmdToolDiagnostic.values()) {
            String suggestion = diagnostic.getSuggestion();
            if (suggestion != null) {
                Assert.assertTrue(Character.isUpperCase(suggestion.charAt(0)), diagnostic.getCode());
                Assert.assertTrue(suggestion.endsWith("."), diagnostic.getCode());
            }
        }
    }

    @Test(description = "ASYNC_CLI_002 names the protocol it is raised for and is a usage error")
    void testAsyncCli002NamesProtocol() {
        AsyncApiCmdToolException ex = new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002, "http");
        Assert.assertEquals(ex.getMessage(),
                "the http protocol can't generate an AsyncAPI definition from Ballerina code");
        Assert.assertEquals(ex.exitCode(), ExitCodes.USAGE);
    }

    @Test(description = "The kind decides the exit code")
    void testKindDecidesExitCode() {
        Assert.assertEquals(new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_011, "x").exitCode(),
                ExitCodes.INVALID_TARGET);
        Assert.assertEquals(new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_006, "x").exitCode(),
                ExitCodes.USAGE);
        Assert.assertEquals(new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_009, "x").exitCode(),
                ExitCodes.RUNTIME);
        AsyncApiCmdToolException locked =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_016, "chat");
        Assert.assertEquals(locked.exitCode(), ExitCodes.RUNTIME);
        Assert.assertEquals(locked.getKind().label(), "dependencies-not-locked");
        Assert.assertEquals(AsyncApiCmdToolException.of(AsyncApiCmdToolException.Kind.OVERWRITE_REFUSED, "x")
                .exitCode(), ExitCodes.OVERWRITE_REFUSED);
    }

    @Test(description = "The message has no prefix: the command adds bal asyncapi: error:")
    void testMessageHasNoPrefix() {
        AsyncApiCmdToolException ex = new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_003, "out");
        Assert.assertEquals(ex.getMessage(), "'out' is a Ballerina workspace");
        Assert.assertEquals(ex.getDiagnosticMessage(), ex.getMessage());
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_003");
    }

    @Test(description = "The suggestion shares the message's arguments")
    void testSuggestionIsFormatted() {
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_013, "missing", "admin, chat");
        Assert.assertEquals(ex.getMessage(), "module 'missing' not found in the package");
        Assert.assertEquals(ex.getSuggestion(), "Use one of its modules: admin, chat.");
    }

    @Test(description = "Exception with cause: getCause() returns wrapped throwable")
    void testExceptionWithCause() {
        IllegalStateException cause = new IllegalStateException("root cause");
        AsyncApiCmdToolException ex =
                new AsyncApiCmdToolException(cause, AsyncApiCmdToolDiagnostic.ASYNC_CLI_001, "something failed");
        Assert.assertSame(ex.getCause(), cause, "getCause() must return the wrapped throwable");
        Assert.assertEquals(ex.getMessage(), "code generation failed: something failed");
    }
}
