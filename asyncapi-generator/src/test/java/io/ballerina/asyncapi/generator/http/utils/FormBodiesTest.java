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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests form-encoded body detection, the conversions of typed form fields, and the {@code $formParams} token.
 */
public class FormBodiesTest {

    private static final SignaturePayloadTemplateBuilder.TokenResolver RESOLVER =
            new SignaturePayloadTemplateBuilder.TokenResolver() {
                @Override
                public String body() {
                    return "BODY";
                }

                @Override
                public String uri() {
                    return "URI";
                }

                @Override
                public String method() {
                    return "METHOD";
                }

                @Override
                public String secret() {
                    return "SECRET";
                }

                @Override
                public String header(String headerName) {
                    return "HEADER";
                }

                @Override
                public String config(String configName) {
                    return "CONFIG_" + configName;
                }

                @Override
                public String formParams() {
                    return "FORM";
                }
            };

    @Test
    void testFormContentTypeIgnoresParametersAndCase() {
        Assert.assertTrue(FormBodies.isFormContentType("application/x-www-form-urlencoded"));
        Assert.assertTrue(FormBodies.isFormContentType("Application/X-WWW-Form-Urlencoded; charset=utf-8"));
        Assert.assertFalse(FormBodies.isFormContentType("application/json"));
        Assert.assertFalse(FormBodies.isFormContentType(null));
    }

    @Test
    void testTypedFieldsConvertToTheTypeTheFieldIsGenerated() {
        Assert.assertEquals(FormBodies.conversionFor(AsyncApiSchema.builder().type("integer").build()), "int");
        Assert.assertEquals(FormBodies.conversionFor(AsyncApiSchema.builder().type("boolean").build()), "boolean");
        Assert.assertEquals(FormBodies.conversionFor(AsyncApiSchema.builder().type("number").build()), "decimal");
        Assert.assertEquals(
                FormBodies.conversionFor(AsyncApiSchema.builder().type("number").format("double").build()), "float");
        Assert.assertNull(FormBodies.conversionFor(AsyncApiSchema.builder().type("string").build()),
                "Strings arrive as strings");
        Assert.assertNull(FormBodies.conversionFor(AsyncApiSchema.builder().build()));
    }

    @Test
    void testFormParamsTokenInEveryInputSyntax() {
        Assert.assertEquals(SignaturePayloadTemplateBuilder.build("$config('webhookUrl') . $formParams", RESOLVER),
                "${CONFIG_webhookUrl}${FORM}", "Twilio's input: the URL, then the sorted parameters");
        Assert.assertEquals(SignaturePayloadTemplateBuilder.build("$uri$formParams", RESOLVER), "${URI}${FORM}");
        Assert.assertEquals(SignaturePayloadTemplateBuilder.build("${uri}${formParams}", RESOLVER), "${URI}${FORM}");
        Assert.assertTrue(FormBodies.usesFormParams("$uri . $formParams"));
        Assert.assertTrue(FormBodies.usesFormParams("${formParams}"));
        Assert.assertFalse(FormBodies.usesFormParams("$body"));
        Assert.assertFalse(FormBodies.usesFormParams("$uri . $formParamsDigest"),
                "Only the whole token counts, not a longer name that starts with it");
        Assert.assertTrue(FormBodies.usesFormParams("$formParams.$uri"));
    }
}
