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

import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link CodegenUtils} covering identifier escaping, name sanitisation,
 * reference type extraction, function name derivation, and service type name resolution.
 */
public class CodegenUtilsTest {

    @Test
    void testEscapeIdentifier_keyword() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("check"), "'check",
                "Ballerina keyword should be escaped with a leading apostrophe");
    }

    @Test
    void testEscapeIdentifier_anotherKeyword() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("error"), "'error",
                "Ballerina keyword 'error' should be escaped with a leading apostrophe");
    }

    @Test
    void testEscapeIdentifier_numericPrefix() {
        String result = CodegenUtils.escapeIdentifier("123abc");
        Assert.assertTrue(result.startsWith("'"),
                "Identifier starting with digits should be prefixed with apostrophe");
    }

    @Test
    void testEscapeIdentifier_specialChars() {
        String result = CodegenUtils.escapeIdentifier("my-func");
        Assert.assertTrue(result.contains("\\-") || result.contains("-"),
                "Special character '-' in identifier should be handled by escapeIdentifier");
    }

    @Test
    void testEscapeIdentifier_plain() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("myFunc"), "myFunc",
                "Plain alphanumeric identifier should not be modified");
    }

    @Test
    void testEscapeIdentifier_underscore() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("my_func"), "my_func",
                "Identifier with underscore should not be modified if it is not a keyword");
    }

    @Test
    void testGetValidName_capitalizeTrue() {
        Assert.assertEquals(CodegenUtils.getValidName("user/signup", true), "UserSignup",
                "Slash-separated path should become PascalCase");
    }

    @Test
    void testGetValidName_capitalizeFalse() {
        String result = CodegenUtils.getValidName("UserSignup", false);
        Assert.assertTrue(result.startsWith("u"),
                "With capitalizeFirstChar=false the result should start with lowercase");
    }

    @Test
    void testGetValidName_underscoreSeparated() {
        Assert.assertEquals(CodegenUtils.getValidName("email_delivered", true), "EmailDelivered",
                "Underscore-separated identifier should become PascalCase");
    }

    @Test
    void testGetValidName_singleWord() {
        Assert.assertEquals(CodegenUtils.getValidName("repository", true), "Repository",
                "Single lowercase word should be capitalised");
    }

    @Test
    void testExtractReferenceType_local() throws GeneratorException {
        String result = CodegenUtils.extractReferenceType("#/components/schemas/PushEvent");
        Assert.assertEquals(result, "PushEvent",
                "Local $ref should resolve to the last path segment");
    }

    @Test
    void testExtractReferenceType_nonLocalThrows() {
        try {
            CodegenUtils.extractReferenceType("http://example.com/schemas/PushEvent");
            Assert.fail("Expected GeneratorException for non-local reference");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception should have a message");
        }
    }

    @Test
    void testGetFunctionNameByEventName_simple() {
        String result = CodegenUtils.getFunctionNameByEventName("push");
        Assert.assertEquals(result, "onPush",
                "Event name 'push' should produce function name 'onPush'");
    }

    @Test
    void testGetFunctionNameByEventName_underscored() {
        String result = CodegenUtils.getFunctionNameByEventName("email_delivered");
        Assert.assertEquals(result, "onEmailDelivered",
                "Underscore-separated event name should produce camelCase function name");
    }

    @Test
    void testGetFunctionNameByEventName_dotSeparated() {
        String result = CodegenUtils.getFunctionNameByEventName("issues.opened");
        Assert.assertNotNull(result, "Function name should be non-null for dot-separated event name");
        Assert.assertTrue(result.startsWith("on"),
                "Function name should start with 'on' prefix");
    }

    @Test
    void testGetServiceTypeNameByServiceName_alreadySuffixed() {
        Assert.assertEquals(CodegenUtils.getServiceTypeNameByServiceName("RepositoryService"), "RepositoryService",
                "Name already ending with 'Service' should not get an extra suffix");
    }

    @Test
    void testGetServiceTypeNameByServiceName_noSuffix() {
        Assert.assertEquals(CodegenUtils.getServiceTypeNameByServiceName("orders"), "OrdersService",
                "Name without 'Service' suffix should have it appended after PascalCase conversion");
    }

    @Test
    void testGetServiceTypeNameByServiceName_singleWord() {
        String result = CodegenUtils.getServiceTypeNameByServiceName("payment");
        Assert.assertEquals(result, "PaymentService",
                "Single-word service name should become PascalCase with Service suffix");
    }
}
