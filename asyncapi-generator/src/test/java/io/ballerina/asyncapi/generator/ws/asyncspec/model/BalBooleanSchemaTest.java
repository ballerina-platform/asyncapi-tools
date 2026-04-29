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
package io.ballerina.asyncapi.generator.ws.asyncspec.model;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link BalBooleanSchema}, a plain wrapper for boolean schema values
 * used to represent {@code additionalProperties: true/false} in generated AsyncAPI specs.
 */
class BalBooleanSchemaTest {

    @Test
    void testGetValue_returnsTrue_whenConstructedWithTrue() {
        BalBooleanSchema schema = new BalBooleanSchema(true);
        Assert.assertTrue(schema.getValue(),
                "getValue() must return true when constructed with true");
    }

    @Test
    void testGetValue_returnsFalse_whenConstructedWithFalse() {
        BalBooleanSchema schema = new BalBooleanSchema(false);
        Assert.assertFalse(schema.getValue(),
                "getValue() must return false when constructed with false");
    }

    @Test
    void testTwoInstances_areIndependent() {
        BalBooleanSchema trueSchema = new BalBooleanSchema(true);
        BalBooleanSchema falseSchema = new BalBooleanSchema(false);
        Assert.assertTrue(trueSchema.getValue(),
                "trueSchema.getValue() must return true independently of falseSchema");
        Assert.assertFalse(falseSchema.getValue(),
                "falseSchema.getValue() must return false independently of trueSchema");
    }
}
