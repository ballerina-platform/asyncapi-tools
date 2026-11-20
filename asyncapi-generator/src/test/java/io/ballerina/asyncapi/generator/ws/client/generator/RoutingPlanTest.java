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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Checks how {@link RoutingPlan} routes each incoming message.
 */
public class RoutingPlanTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // Ping is answered with Pong, and Tick arrives on its own
    private static final String SPEC = """
            asyncapi: 2.0.0
            info: {title: Chat, version: 1.0.0}
            x-dispatcherKey: type
            channels:
              ws:
                publish:
                  operationId: sendPing
                  message: {$ref: '#/components/messages/Ping'}
                subscribe:
                  operationId: onTick
                  message: {$ref: '#/components/messages/Tick'}
            components:
              messages:
                Ping:
                  payload: {$ref: '#/components/schemas/Ping'}
                  x-response: {$ref: '#/components/messages/Pong'}
                Pong:
                  payload: {$ref: '#/components/schemas/Pong'}
                Tick:
                  payload: {$ref: '#/components/schemas/Tick'}
              schemas:
                Ping: {type: object, required: [type], properties: {type: {type: string, const: ping}}}
                Pong: {type: object, required: [type], properties: {type: {type: string, const: pong}}}
                Tick: {type: object, required: [type], properties: {type: {type: string, const: tick}}}
            """;

    @Test
    void testReplyGoesToItsRequestsPipeAndAnEventToItsOwn() throws Exception {
        RoutingPlan plan = plan(SPEC);
        Assert.assertEquals(plan.dispatcherKey(), "type");
        Assert.assertEquals(plan.responseMap(), Map.of("Pong", "ping", "Tick", "tick"));
        Assert.assertEquals(plan.dispatchValueMap(), Map.of("pong", "ping", "tick", "tick"));
        Assert.assertFalse(plan.keylessReplies());
    }

    @Test
    void testTextReplyIsKeptOffTheKeyRoutes() throws Exception {
        RoutingPlan plan = plan(SPEC.replace(
                "Pong: {type: object, required: [type], properties: {type: {type: string, const: pong}}}",
                "Pong: {type: string}"));
        Assert.assertFalse(plan.responseMap().containsKey("Pong"), plan.responseMap().toString());
        Assert.assertTrue(plan.keylessReplies());
        Assert.assertFalse(plan.keylessReceiveOnly());
    }

    @Test
    void testTwoRepliesWithTheSameFixedValueAreRejected() throws Exception {
        // Tick now fixes 'pong' too, so a 'pong' message could be either one
        String spec = SPEC.replace("const: tick", "const: pong");
        GeneratorException e = Assert.expectThrows(GeneratorException.class, () -> plan(spec));
        Assert.assertTrue(e.getMessage().contains("both fix the dispatcher value 'pong'"), e.getMessage());
    }

    @Test
    void testPlanCanNotBeChangedAfterwards() throws Exception {
        RoutingPlan plan = plan(SPEC);
        Assert.expectThrows(UnsupportedOperationException.class, () -> plan.responseMap().put("X", "x"));
        Assert.expectThrows(UnsupportedOperationException.class, () -> plan.streamReturns().add("X"));
    }

    private static RoutingPlan plan(String yaml) throws Exception {
        String json = YAML.readTree(yaml).toString();
        WsClientConfig config = new WsClientConfig.Builder()
                .withAsyncApi(AsyncApiParser.parseFromJsonString(json)).build();
        return RoutingPlan.of(config.getAsyncApi());
    }
}
