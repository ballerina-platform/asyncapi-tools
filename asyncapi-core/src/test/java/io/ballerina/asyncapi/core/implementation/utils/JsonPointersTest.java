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
package io.ballerina.asyncapi.core.implementation.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for {@link JsonPointers}.
 */
public class JsonPointersTest {

    @DataProvider(name = "tokens")
    Object[][] tokens() {
        return new Object[][] {
            {"ws", "ws"},
            {"~1stream", "/stream"},
            {"~0~1", "~/"},
            {"~01", "~1"},
            {"%7Broom%7D", "{room}"},
            {"a+b", "a+b"},
            {"100%", "100%"},
        };
    }

    @Test(dataProvider = "tokens")
    void decode_readsEscapes(String token, String key) {
        Assert.assertEquals(JsonPointers.decode(token), key);
    }

    @Test
    void keyAfter_isNullForAnotherPrefix() {
        Assert.assertEquals(JsonPointers.keyAfter("#/channels/~1ws", "#/channels/"), "/ws");
        Assert.assertNull(JsonPointers.keyAfter("#/components/channels/ws", "#/channels/"));
    }

    @Test
    void resolve_followsReferencesOnTheWay() throws Exception {
        JsonNode root = new ObjectMapper().readTree("{\"channels\":{\"/ws\":{\"$ref\":\"#/components/channels/ws\"}},"
                + "\"components\":{\"channels\":{\"ws\":{\"messages\":{\"ping\":{\"name\":\"ping\"}}}}},"
                + "\"loop\":{\"$ref\":\"#/loop\"}}");
        Assert.assertEquals(JsonPointers.resolve(root, "#/channels/~1ws/messages/ping").path("name").asText(), "ping");
        Assert.assertNull(JsonPointers.resolve(root, "#/channels/~1ws/messages/pong"));
        Assert.assertNull(JsonPointers.resolve(root, "#/loop"));
        Assert.assertNull(JsonPointers.resolve(root, "other.yaml#/channels/ws"));
    }

    @Test
    void resolve_rejectsAnIndexTooLargeForAnInt() throws Exception {
        JsonNode root = new ObjectMapper().readTree("{\"servers\":[{\"host\":\"a\"}]}");
        Assert.assertEquals(JsonPointers.resolve(root, "#/servers/0/host").asText(), "a");
        Assert.assertNull(JsonPointers.resolve(root, "#/servers/99999999999"));
    }

    @Test
    void resolve_findsAKeyWithALiteralPercentSequence() throws Exception {
        JsonNode root = new ObjectMapper().readTree("{\"a%41\":1,\"a b\":2}");
        // A literal % is written %25, as the mappers decode every %xx too
        Assert.assertEquals(JsonPointers.resolve(root, "#/a%2541").asInt(), 1);
        Assert.assertNull(JsonPointers.resolve(root, "#/a%41"));
        Assert.assertEquals(JsonPointers.resolve(root, "#/a%20b").asInt(), 2);
    }

    @Test
    void resolve_stopsAtAReferenceIntoAnotherFile() throws Exception {
        JsonNode root = new ObjectMapper().readTree("{\"m\":{\"$ref\":\"shared.yaml#/Pong\"}}");
        Assert.assertEquals(JsonPointers.resolve(root, "#/m").path("$ref").asText(), "shared.yaml#/Pong");
    }
}
