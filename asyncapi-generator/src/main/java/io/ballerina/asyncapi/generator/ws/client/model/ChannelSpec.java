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
package io.ballerina.asyncapi.generator.ws.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.semver4j.Semver;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A view of a spec with only one of its channels and the operations on it, so a client is generated for that
 * channel alone. The rest of the spec, such as its message types and security schemes, is shared.
 */
public final class ChannelSpec implements AsyncApiSpec {

    private static final String ROOT_CHANNEL = "Root";

    private final AsyncApiSpec spec;
    private final Map<String, AsyncApiChannel> channels;
    private final Map<String, AsyncApiOperation> operations;

    private ChannelSpec(AsyncApiSpec spec, String channelName, AsyncApiChannel channel) {
        this.spec = spec;
        this.channels = Map.of(channelName, channel);
        this.operations = operationsOn(spec, channelName, channel);
    }

    /**
     * Creates the view of {@code spec} for the channel {@code channelName}.
     *
     * @param spec        the spec
     * @param channelName the channel's key in the spec
     * @param channel     the channel
     * @return the view with only that channel and its operations
     */
    public static ChannelSpec of(AsyncApiSpec spec, String channelName, AsyncApiChannel channel) {
        return new ChannelSpec(spec, channelName, channel);
    }

    /**
     * Returns the operations of {@code spec} on the channel {@code channelName}, in spec order.
     *
     * @param spec        the spec
     * @param channelName the channel's key in the spec
     * @param channel     the channel
     * @return the operations on that channel
     */
    public static Map<String, AsyncApiOperation> operationsOn(AsyncApiSpec spec, String channelName,
                                                             AsyncApiChannel channel) {
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiOperation> entry : spec.getAsyncApiOperations().orElse(Map.of()).entrySet()) {
            if (isOn(entry.getValue(), channelName, channel)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    // A 2.x operation on the root channel '/' has no channel object, and that channel is keyed Root
    private static boolean isOn(AsyncApiOperation operation, String channelName, AsyncApiChannel channel) {
        if (operation.channel() != null) {
            return operation.channel() == channel;
        }
        String channelId = operation.channelId();
        return channelName.equals(channelId) || ROOT_CHANNEL.equals(channelName) && "".equals(channelId);
    }

    @Override
    public Semver getAsyncApiVersion() {
        return spec.getAsyncApiVersion();
    }

    @Override
    public Optional<URI> getAsyncApiId() {
        return spec.getAsyncApiId();
    }

    @Override
    public AsyncApiInfo getAsyncApiInfo() {
        return spec.getAsyncApiInfo();
    }

    @Override
    public Optional<Map<String, AsyncApiServer>> getAsyncApiServers() {
        return spec.getAsyncApiServers();
    }

    @Override
    public Optional<String> getAsyncApiContentType() {
        return spec.getAsyncApiContentType();
    }

    @Override
    public Optional<Map<String, AsyncApiChannel>> getAsyncApiChannels() {
        return Optional.of(channels);
    }

    @Override
    public Optional<Map<String, AsyncApiOperation>> getAsyncApiOperations() {
        return Optional.of(operations);
    }

    @Override
    public Optional<AsyncApiComponent> getAsyncApiComponents() {
        return spec.getAsyncApiComponents();
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return spec.getAsyncApiExtensions();
    }
}
