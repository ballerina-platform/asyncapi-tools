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
 * Unit tests for {@link BalAsyncApi30MessageImpl}.
 *
 * <p>This subclass has exactly one override: {@code isEntity()} returns {@code false},
 * preventing Jackson from serialising transient message nodes as nested entities.
 * There are no other overridden methods in the source.
 *
 * <p>{@code setPayload} is not present in the production code usage of this class
 * (the codebase only calls {@code setDescription} and adds messages via component
 * references). The inherited {@code setDescription}/{@code getDescription} round-trip
 * is verified here as it is the only setter actively exercised by the mappers.
 */
class BalAsyncApi30MessageImplTest {

    @Test
    void testIsEntity_returnsFalse() {
        BalAsyncApi30MessageImpl message = new BalAsyncApi30MessageImpl();
        Assert.assertFalse(message.isEntity(),
                "BalAsyncApi30MessageImpl.isEntity() must return false");
    }

    @Test
    void testSetDescription_getDescription_roundTrip() {
        // setDescription is the only setter called on BalAsyncApi30MessageImpl in the production mappers
        BalAsyncApi30MessageImpl message = new BalAsyncApi30MessageImpl();
        message.setDescription("Dispatched when a chat message is received");
        Assert.assertEquals(message.getDescription(), "Dispatched when a chat message is received",
                "getDescription() must return the exact value passed to setDescription()");
    }

    @Test
    void testSetDescription_null_doesNotThrow() {
        BalAsyncApi30MessageImpl message = new BalAsyncApi30MessageImpl();
        message.setDescription(null);
        Assert.assertNull(message.getDescription(),
                "getDescription() must return null after setDescription(null)");
    }
}
