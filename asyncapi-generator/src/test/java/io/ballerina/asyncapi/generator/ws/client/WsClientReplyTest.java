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
package io.ballerina.asyncapi.generator.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.ws.client.model.ServiceSideSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Checks that an AsyncAPI 3.0 operation's reply is read as the reply its function returns.
 */
public class WsClientReplyTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    void testReplyOfInlineChannelMessagesIsReturned() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Rpc, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  rpc:
                    address: /rpc
                    messages:
                      Add:
                        payload: {type: object, required: [a, b], properties: {a: {type: integer}, b: {type: integer}}}
                      Sum:
                        payload: {type: object, required: [value], properties: {value: {type: integer}}}
                operations:
                  add:
                    action: send
                    channel: {$ref: '#/channels/rpc'}
                    messages: [{$ref: '#/channels/rpc/messages/Add'}]
                    reply:
                      channel: {$ref: '#/channels/rpc'}
                      messages: [{$ref: '#/channels/rpc/messages/Sum'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings, UnaryOperator.identity());
        Assert.assertTrue(client.contains("remote isolated function doAdd(Add add, decimal timeout) returns "
                + "Sum|error"), client);
        Assert.assertFalse(client.contains("function doSum("), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testReplyOfComponentMessagesReturnsEachReply() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Orders, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  orders:
                    address: /orders
                    messages:
                      PlaceOrder: {$ref: '#/components/messages/PlaceOrder'}
                      OrderAccepted: {$ref: '#/components/messages/OrderAccepted'}
                      OrderRejected: {$ref: '#/components/messages/OrderRejected'}
                      PriceUpdate: {$ref: '#/components/messages/PriceUpdate'}
                operations:
                  placeOrder:
                    action: send
                    channel: {$ref: '#/channels/orders'}
                    messages: [{$ref: '#/channels/orders/messages/PlaceOrder'}]
                    reply:
                      channel: {$ref: '#/channels/orders'}
                      messages:
                        - {$ref: '#/channels/orders/messages/OrderAccepted'}
                        - {$ref: '#/channels/orders/messages/OrderRejected'}
                  onPriceUpdate:
                    action: receive
                    channel: {$ref: '#/channels/orders'}
                    messages: [{$ref: '#/channels/orders/messages/PriceUpdate'}]
                components:
                  messages:
                    PlaceOrder:
                      payload:
                        type: object
                        required: [type, item]
                        properties: {type: {type: string, const: place_order}, item: {type: string}}
                    OrderAccepted:
                      payload:
                        type: object
                        required: [type, orderId]
                        properties: {type: {type: string, const: order_accepted}, orderId: {type: string}}
                    OrderRejected:
                      payload:
                        type: object
                        required: [type, reason]
                        properties: {type: {type: string, const: order_rejected}, reason: {type: string}}
                    PriceUpdate:
                      payload:
                        type: object
                        required: [type, price]
                        properties: {type: {type: string, const: price_update}, price: {type: number}}
                """;
        String client = generateClient(spec, new ArrayList<>(), UnaryOperator.identity());
        String signature = signature(client, "doPlaceOrder");
        Assert.assertTrue(signature.contains("OrderAccepted") && signature.contains("OrderRejected"), signature);
        Assert.assertTrue(client.contains("function doPriceUpdate("), client);
        Assert.assertFalse(client.contains("function doOrderAccepted("), client);
    }

    @Test
    void testMessageOwnXResponseWinsOverTheReply() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Rpc, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  rpc:
                    address: /rpc
                    messages:
                      Add: {$ref: '#/components/messages/Add'}
                      Sum: {$ref: '#/components/messages/Sum'}
                      Total: {$ref: '#/components/messages/Total'}
                operations:
                  add:
                    action: send
                    channel: {$ref: '#/channels/rpc'}
                    messages: [{$ref: '#/channels/rpc/messages/Add'}]
                    reply: {messages: [{$ref: '#/channels/rpc/messages/Sum'}]}
                components:
                  messages:
                    Add:
                      x-response: {$ref: '#/components/messages/Total'}
                      x-response-type: simple-rpc
                      payload: {type: object, required: [type], properties: {type: {type: string, const: add}}}
                    Sum:
                      payload: {type: object, required: [type], properties: {type: {type: string, const: sum}}}
                    Total:
                      payload: {type: object, required: [type], properties: {type: {type: string, const: total}}}
                """;
        String client = generateClient(spec, new ArrayList<>(), UnaryOperator.identity());
        Assert.assertTrue(signature(client, "doAdd").contains("returns Total|error"), client);
    }

    @Test
    void testReplyOnAnotherChannelIsNotWaitedFor() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Split, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  requests:
                    address: /requests
                    messages:
                      Ask: {payload: {type: object, required: [q], properties: {q: {type: string}}}}
                  answers:
                    address: /answers
                    messages:
                      Answer: {payload: {type: object, required: [a], properties: {a: {type: string}}}}
                operations:
                  ask:
                    action: send
                    channel: {$ref: '#/channels/requests'}
                    messages: [{$ref: '#/channels/requests/messages/Ask'}]
                    reply:
                      channel: {$ref: '#/channels/answers'}
                      messages: [{$ref: '#/channels/answers/messages/Answer'}]
                  onAnswer:
                    action: receive
                    channel: {$ref: '#/channels/answers'}
                    messages: [{$ref: '#/channels/answers/messages/Answer'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String clients = generateClient(spec, warnings, UnaryOperator.identity());
        Assert.assertTrue(signature(clients, "doAsk").contains("returns error?"), clients);
        Assert.assertTrue(clients.contains("function doAnswer("), clients);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/ask/reply",
                "operation 'ask' gets its reply on another channel, which the client connects to separately, so "
                        + "its function doesn't wait for the reply",
                "Receive the reply with the client of that channel.")));
    }

    @Test
    void testServerRequestTheClientAnswersIsReported() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Heartbeat, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  live:
                    address: /live
                    messages:
                      Ping: {payload: {type: object, required: [type], properties: {type: {type: string, const: ping}}}}
                      Pong: {payload: {type: object, required: [type], properties: {type: {type: string, const: pong}}}}
                operations:
                  onPing:
                    action: receive
                    channel: {$ref: '#/channels/live'}
                    messages: [{$ref: '#/channels/live/messages/Ping'}]
                    reply: {messages: [{$ref: '#/channels/live/messages/Pong'}]}
                  pong:
                    action: send
                    channel: {$ref: '#/channels/live'}
                    messages: [{$ref: '#/channels/live/messages/Pong'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings, UnaryOperator.identity());
        Assert.assertTrue(client.contains("function doPing("), client);
        Assert.assertTrue(signature(client, "doPong").contains("returns error?"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/onPing/reply",
                "the server expects 'Pong' in reply to 'Ping', and the client doesn't reply on its own",
                "Send the reply with its function after the client receives the message.")));
    }

    @Test
    void testReplyWithoutMessagesIsAnyOtherMessageOfItsChannel() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Echo, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  echo:
                    address: /echo
                    messages:
                      Say: {payload: {type: object, required: [text], properties: {text: {type: string}}}}
                      Echoed: {payload: {type: object, required: [echo], properties: {echo: {type: string}}}}
                operations:
                  say:
                    action: send
                    channel: {$ref: '#/channels/echo'}
                    messages: [{$ref: '#/channels/echo/messages/Say'}]
                    reply: {channel: {$ref: '#/channels/echo'}}
                """;
        String client = generateClient(spec, new ArrayList<>(), UnaryOperator.identity());
        Assert.assertTrue(signature(client, "doSay").contains("returns Echoed|error"), client);
    }

    @Test
    void testServerSideReplyIsTheClientsRequestReply() throws Exception {
        // Written from the server's side: it receives Subscribe and replies with Subscribed
        String spec = """
                asyncapi: 3.0.0
                info: {title: Prices, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  prices:
                    address: /prices
                    messages:
                      Subscribe: {payload: {type: object, required: [symbol], properties: {symbol: {type: string}}}}
                      Subscribed: {payload: {type: object, required: [id], properties: {id: {type: string}}}}
                operations:
                  onSubscribe:
                    action: receive
                    channel: {$ref: '#/channels/prices'}
                    messages: [{$ref: '#/channels/prices/messages/Subscribe'}]
                    reply: {messages: [{$ref: '#/channels/prices/messages/Subscribed'}]}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings, ServiceSideSpec::of);
        Assert.assertTrue(signature(client, "doSubscribe").contains("returns Subscribed|error"), client);
        Assert.assertEquals(warnings, List.of());
    }

    // The signature line of a function of the client
    private static String signature(String client, String function) {
        int start = client.indexOf("function " + function + "(");
        Assert.assertTrue(start >= 0, client);
        return client.substring(start, client.indexOf('{', start));
    }

    private static String generateClient(String spec, List<SpecWarning> warnings, UnaryOperator<AsyncApiSpec> side)
            throws Exception {
        Path outDir = Files.createTempDirectory("ws-reply");
        try {
            AsyncApiSpec parsed = side.apply(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()));
            new WsClientCodeGenerator(parsed, null, false).withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve("client.bal"));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
