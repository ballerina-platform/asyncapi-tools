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
 * A view of a spec written from the server's side, read from the client's: what the server sends, the client
 * receives, and the other way round. Every operation's send and receive are swapped.
 */
public final class ServiceSideSpec implements AsyncApiSpec {

    private final AsyncApiSpec spec;
    private final Map<String, AsyncApiOperation> operations;

    private ServiceSideSpec(AsyncApiSpec spec, Map<String, AsyncApiOperation> operations) {
        this.spec = spec;
        this.operations = operations;
    }

    /**
     * Returns {@code spec} read from the client's side.
     *
     * @param spec a spec whose operations describe what the server does
     * @return a view of it whose operations describe what the client does
     */
    public static AsyncApiSpec of(AsyncApiSpec spec) {
        if (spec == null || spec.getAsyncApiOperations().isEmpty()) {
            return spec;
        }
        Map<String, AsyncApiOperation> swapped = new LinkedHashMap<>();
        spec.getAsyncApiOperations().get().forEach((id, op) -> swapped.put(id, op == null ? null : swap(op)));
        return new ServiceSideSpec(spec, swapped);
    }

    private static AsyncApiOperation swap(AsyncApiOperation op) {
        AsyncApiOperation.Action action = switch (op.action()) {
            case SEND -> AsyncApiOperation.Action.RECEIVE;
            case RECEIVE -> AsyncApiOperation.Action.SEND;
            case null -> null;
        };
        return new AsyncApiOperation(action, op.channelId(), op.channel(), op.title(), op.summary(),
                op.description(), op.messages(), op.security(), op.reply(), op.tags(), op.externalDocs(),
                op.bindings(), op.traits(), op.extensions());
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
        return spec.getAsyncApiChannels();
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
