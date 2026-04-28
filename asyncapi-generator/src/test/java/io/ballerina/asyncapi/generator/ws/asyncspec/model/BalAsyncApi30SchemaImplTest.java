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
 * Unit tests for {@link BalAsyncApi30SchemaImpl} verifying the two override methods
 * that justify the subclass and confirming that inherited Apicurio schema fields
 * round-trip correctly through the same instance.
 */
class BalAsyncApi30SchemaImplTest {

    @Test
    void testIsEntity_returnsTrue() {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
        Assert.assertTrue(schema.isEntity(),
                "BalAsyncApi30SchemaImpl.isEntity() must return true");
    }

    @Test
    void testIsSchema_returnsTrue() {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
        Assert.assertTrue(schema.isSchema(),
                "BalAsyncApi30SchemaImpl.isSchema() must return true");
    }

    @Test
    void testSetType_getType_roundTrip() {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
        schema.setType("integer");
        Assert.assertEquals(schema.getType(), "integer",
                "getType() must return the exact value passed to setType()");
    }

    @Test
    void testSetFormat_getFormat_roundTrip() {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
        schema.setFormat("int64");
        Assert.assertEquals(schema.getFormat(), "int64",
                "getFormat() must return the exact value passed to setFormat()");
    }

    @Test
    void testTwoInstances_areIndependent() {
        BalAsyncApi30SchemaImpl schemaA = new BalAsyncApi30SchemaImpl();
        BalAsyncApi30SchemaImpl schemaB = new BalAsyncApi30SchemaImpl();
        schemaA.setType("string");
        schemaB.setType("boolean");
        Assert.assertEquals(schemaA.getType(), "string",
                "schemaA.getType() must not be affected by changes to schemaB");
        Assert.assertEquals(schemaB.getType(), "boolean",
                "schemaB.getType() must not be affected by changes to schemaA");
    }
}
