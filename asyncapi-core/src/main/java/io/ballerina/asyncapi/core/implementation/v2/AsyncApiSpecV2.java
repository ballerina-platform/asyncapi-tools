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
package io.ballerina.asyncapi.core.implementation.v2;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Extensible;
import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.implementation.v2.info.InfoMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.channel.ChannelMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.component.ComponentMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.operation.OperationMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.server.ServerMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.semver4j.Semver;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

public class AsyncApiSpecV2 implements AsyncApiSpec {

    private final AsyncApiDocument asyncApiDocument;
    private Semver version;
    private URI id;
    private AsyncApiInfo info;
    private Map<String, AsyncApiServer> servers;
    private Map<String, AsyncApiChannel> channels;
    private Map<String, AsyncApiOperation> operations;
    private AsyncApiComponent components;
    private Map<String, JsonNode> extensions;

    public AsyncApiSpecV2(AsyncApiDocument asyncApiDocument) {
        this.asyncApiDocument = asyncApiDocument;
        extractFields();
    }

    private void extractFields() {
        this.version = new Semver(asyncApiDocument.getAsyncapi());

        String documentId = asyncApiDocument.getId();
        if (documentId != null) {
            this.id = URI.create(documentId);
        }

        this.info = InfoMapperV2.map(asyncApiDocument);

        Map<String, ? extends SecurityScheme> securitySchemes = null;
        if (asyncApiDocument.getComponents() != null) {
            securitySchemes = asyncApiDocument.getComponents().getSecuritySchemes();
        }
        this.servers = ServerMapperV2.map(asyncApiDocument.getServers(), securitySchemes);

        this.channels = ChannelMapperV2.map(asyncApiDocument.getChannels(),
                asyncApiDocument.getComponents(),
                this.servers);
        this.operations = OperationMapperV2.map(asyncApiDocument.getChannels(), asyncApiDocument.getComponents());
        this.components = ComponentMapperV2.map(
                asyncApiDocument.getComponents());

        if (asyncApiDocument instanceof Extensible extensible) {
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
    public Map<String, AsyncApiServer> getAsyncApiServers() {
        return servers;
    }

    @Override
    public String getAsyncApiContentType() {
        return null;
    }

    @Override
    public Map<String, AsyncApiChannel> getAsyncApiChannels() {
        return channels;
    }

    @Override
    public Map<String, AsyncApiOperation> getAsyncApiOperations() {
        return operations;
    }

    @Override
    public AsyncApiComponent getAsyncApiComponents() {
        return components;
    }

    @Override
    public Map<String, JsonNode> getAsyncApiExtensions() {
        return extensions != null ? extensions : Map.of();
    }
}
