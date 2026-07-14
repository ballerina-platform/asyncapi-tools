/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.validator;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import org.testng.annotations.Test;

import java.util.List;

public class WebhookDslValidatorTest {

    @Test
    public void testBuiltinBracedTokensAreAccepted() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "hex", "sha256=${signature}", "{body}",
                null, List.of(), null, null);
        // Should not throw: "body" is a builtin input token, not an unrecognized placeholder.
        WebhookDslValidator.validate(config);
    }

    @Test
    public void testDollarBracedBuiltinTokenIsAccepted() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "hex", "sha256=${signature}", "${body}",
                null, List.of(), null, null);
        WebhookDslValidator.validate(config);
    }

    @Test
    public void testDollarSecretTokenIsAccepted() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "hex", "sha256=${signature}", "$secret . $body",
                "hash", List.of(), null, null);
        WebhookDslValidator.validate(config);
    }

    @Test
    public void testConfigFuncTokenIsAccepted() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "base64", "sha256=${signature}",
                "$method . $config('callbackUrl') . $body", null, List.of("callbackUrl"), null, null);
        WebhookDslValidator.validate(config);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testUnknownDollarBracedTokenIsRejected() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "hex", "sha256=${signature}", "${garbage}",
                null, List.of(), null, null);
        WebhookDslValidator.validate(config);
    }

    @Test(expectedExceptions = GeneratorException.class)
    public void testUnknownBracedTokenIsRejected() throws GeneratorException {
        WebhookAuthConfig config = new WebhookAuthConfig(
                "X-Test-Signature", "sha256", "hex", "sha256=${signature}", "{garbage}",
                null, List.of(), null, null);
        WebhookDslValidator.validate(config);
    }
}
