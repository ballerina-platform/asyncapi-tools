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
package io.ballerina.asyncapi.generator.ws.client.utils;

import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises specs for protocols and framings a plain WebSocket client can't speak.
 *
 * <p>A spec whose servers all use another protocol, such as STOMP or MQTT, or whose WebSocket server is a Socket.IO
 * endpoint, stops with the reason. A spec whose description names a framing, such as SignalR or Phoenix Channels,
 * gets a warning, as the client sends and reads plain JSON frames.
 */
public final class OtherProtocols {

    private static final Set<String> WEBSOCKET = Set.of("ws", "wss", "websocket", "websockets");
    // Protocols that aren't a plain WebSocket; http and https aren't here, as WebSocket servers are often labelled so
    private static final Set<String> OTHER = Set.of("stomp", "stomps", "mqtt", "mqtts", "secure-mqtt", "mqtt5",
            "amqp", "amqps", "amqp1", "kafka", "kafka-secure", "nats", "jms", "ibmmq", "solace", "redis", "sns",
            "sqs", "pulsar", "googlepubsub", "anypointmq", "ros2", "eftl", "sse");
    private static final Pattern SOCKET_IO_PATH = Pattern.compile("/socket\\.io(/|$|\\?)");
    // Framings over a WebSocket, as a spec's title or description names them
    private static final Map<Pattern, String> FRAMINGS = new LinkedHashMap<>();

    static {
        FRAMINGS.put(Pattern.compile("\\bsocket\\.?io\\b|\\bengine\\.io\\b", Pattern.CASE_INSENSITIVE), "Socket.IO");
        FRAMINGS.put(Pattern.compile("\\bstomp\\b", Pattern.CASE_INSENSITIVE), "STOMP");
        FRAMINGS.put(Pattern.compile("\\bsignalr\\b", Pattern.CASE_INSENSITIVE), "SignalR");
        FRAMINGS.put(Pattern.compile("\\bphoenix channels?\\b|\\bphoenix channel protocol\\b",
                Pattern.CASE_INSENSITIVE), "Phoenix Channels");
        FRAMINGS.put(Pattern.compile("\\bsockjs\\b", Pattern.CASE_INSENSITIVE), "SockJS");
        FRAMINGS.put(Pattern.compile("\\bwamp\\b", Pattern.CASE_INSENSITIVE), "WAMP");
        FRAMINGS.put(Pattern.compile("\\baction ?cable\\b", Pattern.CASE_INSENSITIVE), "Action Cable");
    }

    private OtherProtocols() {
    }

    /**
     * Stops when the client can't talk to the spec's servers, and warns about a framing the spec names.
     *
     * @param spec     the parsed spec
     * @param warnings where to report a framing the spec names
     * @throws GeneratorException if every server uses another protocol, or a WebSocket server is a Socket.IO endpoint
     */
    public static void check(AsyncApiSpec spec, Consumer<SpecWarning> warnings) throws GeneratorException {
        Map<String, AsyncApiServer> servers = spec.getAsyncApiServers().orElse(Map.of());
        Set<String> others = new LinkedHashSet<>();
        boolean webSocket = false;
        for (Map.Entry<String, AsyncApiServer> server : servers.entrySet()) {
            String protocol = protocol(server.getValue());
            if (WEBSOCKET.contains(protocol)) {
                webSocket = true;
                if (isSocketIo(server.getValue())) {
                    throw new GeneratorException(String.format("server '%s' is a Socket.IO endpoint (%s). Socket.IO "
                            + "wraps every message in its own framing, which a WebSocket client doesn't speak, so "
                            + "the client couldn't talk to it. If the server also takes plain WebSocket "
                            + "connections, point the server at that endpoint instead.", server.getKey(),
                            address(server.getValue())));
                }
            } else if (OTHER.contains(protocol)) {
                others.add(protocol);
            }
        }
        if (!webSocket && !others.isEmpty()) {
            throw new GeneratorException(String.format("the spec's servers use %s, not ws or wss, so a WebSocket "
                    + "client can't talk to them. If a server is a plain WebSocket endpoint, set its protocol to ws "
                    + "or wss.", String.join(", ", others)));
        }
        String framing = namedFraming(spec.getAsyncApiInfo());
        if (framing != null) {
            warnings.accept(new SpecWarning("/info", String.format("the spec's description names %s, and the "
                    + "client sends and reads plain JSON frames, so it can't talk to a server that speaks %s",
                    framing, framing), String.format("Generate from a spec of the server's plain WebSocket "
                    + "endpoint, if it has one; %s itself isn't supported.", framing)));
        }
    }

    /**
     * Returns the servers a WebSocket client can connect to first, followed by the rest, keeping their order.
     *
     * @param servers the spec's servers
     * @return the servers, WebSocket ones first
     */
    public static List<AsyncApiServer> webSocketFirst(Map<String, AsyncApiServer> servers) {
        List<AsyncApiServer> ordered = new ArrayList<>();
        List<AsyncApiServer> rest = new ArrayList<>();
        for (AsyncApiServer server : servers.values()) {
            (WEBSOCKET.contains(protocol(server)) ? ordered : rest).add(server);
        }
        ordered.addAll(rest);
        return ordered;
    }

    private static String protocol(AsyncApiServer server) {
        return server != null && server.protocol() != null ? server.protocol().toLowerCase(Locale.ROOT) : "";
    }

    private static String address(AsyncApiServer server) {
        return (server.host() != null ? server.host() : "") + (server.pathname() != null ? server.pathname() : "");
    }

    private static boolean isSocketIo(AsyncApiServer server) {
        return SOCKET_IO_PATH.matcher(address(server).toLowerCase(Locale.ROOT)).find();
    }

    // The first framing the spec's title or description names
    private static String namedFraming(AsyncApiInfo info) {
        if (info == null) {
            return null;
        }
        String text = (info.title() != null ? info.title() : "") + "\n"
                + (info.description() != null ? info.description() : "");
        for (Map.Entry<Pattern, String> framing : FRAMINGS.entrySet()) {
            Matcher matcher = framing.getKey().matcher(text);
            if (matcher.find()) {
                return framing.getValue();
            }
        }
        return null;
    }
}
