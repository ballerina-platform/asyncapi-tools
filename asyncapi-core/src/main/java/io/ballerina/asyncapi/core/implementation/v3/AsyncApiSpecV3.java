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
package io.ballerina.asyncapi.core.implementation.v3;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.component.ComponentMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.info.InfoMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.ServerMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.semver4j.Semver;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link AsyncApiSpec} for AsyncAPI 3.0 specifications.
 */
public class AsyncApiSpecV3 implements AsyncApiSpec {

    private final AsyncApiDocument asyncApiDocument;
    private Semver version;
    private URI id;
    private AsyncApiInfo info;
    private Map<String, AsyncApiServer> servers;
    private String contentType;
    private Map<String, AsyncApiChannel> channels;
    private Map<String, AsyncApiOperation> operations;
    private AsyncApiComponent components;
    private Map<String, JsonNode> extensions;

    /**
     * Creates a new AsyncAPI 3.0 specification wrapper.
     *
     * @param asyncApiDocument the parsed AsyncAPI document
     */
    public AsyncApiSpecV3(AsyncApiDocument asyncApiDocument) {
        this.asyncApiDocument = asyncApiDocument;
        extractFields();
    }

    private void extractFields() {
        this.version = new Semver(asyncApiDocument.getAsyncapi());

        String documentId = asyncApiDocument.getId();
        if (documentId != null) {
            this.id = URIUtils.toUri(documentId);
        }
        this.info = InfoMapperV3.map(asyncApiDocument.getInfo(), asyncApiDocument.getComponents());

        this.servers = ServerMapperV3.map(asyncApiDocument.getServers(), asyncApiDocument.getComponents());

        this.contentType = asyncApiDocument.getDefaultContentType();

        this.channels = ChannelMapperV3.map(asyncApiDocument.getChannels(), asyncApiDocument.getComponents(),
                this.servers
        );

        this.operations = OperationMapperV3.map(asyncApiDocument, this.channels);

        if (asyncApiDocument.getComponents() != null) {
            this.components = ComponentMapperV3.map(asyncApiDocument.getComponents());
        }

        if (asyncApiDocument instanceof AsyncApiExtensible extensible) {
            this.extensions = extensible.getExtensions();
        }

    }

    @Override
    public Semver getAsyncApiVersion() {
        return version;
    }

    @Override
    public Optional<URI> getAsyncApiId() {
        return Optional.ofNullable(id);
    }

    @Override
    public AsyncApiInfo getAsyncApiInfo() {
        return info;
    }

    @Override
    public Optional<Map<String, AsyncApiServer>> getAsyncApiServers() {
        return Optional.ofNullable(servers);
    }

    @Override
    public Optional<String> getAsyncApiContentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public Optional<Map<String, AsyncApiChannel>> getAsyncApiChannels() {
        return Optional.ofNullable(channels);
    }

    @Override
    public Optional<Map<String, AsyncApiOperation>> getAsyncApiOperations() {
        return Optional.ofNullable(operations);
    }

    @Override
    public Optional<AsyncApiComponent> getAsyncApiComponents() {
        return Optional.ofNullable(components);
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return Optional.ofNullable(extensions);
    }
}
