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
package io.ballerina.asyncapi.core.implementation.v3.server;

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Server;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.common.ServerMapper;
import io.ballerina.asyncapi.core.implementation.common.ServerVariableMapper;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Server models to {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}
 * for AsyncAPI 3.0.
 */
public final class ServerMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ServerMapperV3.class);

    private ServerMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApiServers} to a map of server names to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param servers    the Apicurio servers object
     * @param components the Apicurio components object (for server $ref, tag, and externalDocs
     *                   resolution)
     * @return the mapped servers map, or an empty map if servers is null
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            AsyncApiServers servers, AsyncApiComponents components) {
        return ServerMapper.map(servers, server -> mapOne(server, components));
    }

    /**
     * Builds an {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer} from an
     * Apicurio {@link AsyncApi30Server} object.
     *
     * @param server     the Apicurio AsyncAPI 3.0 server object
     * @param components the Apicurio components object (for tag and externalDocs $ref resolution)
     * @return the mapped AsyncApiServer
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServer buildServer(
            AsyncApi30Server server, AsyncApiComponents components) {
        List<AsyncApiTag> tags = null;
        if (server.getTags() != null) {
            tags = server.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, components))
                    .toList();
        }
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServer(
                server.getHost(),
                server.getProtocol(),
                server.getProtocolVersion(),
                server.getPathname(),
                server.getDescription(),
                server.getTitle(),
                server.getSummary(),
                ServerVariableMapperV3.mapVariables(server.getVariables(), components),
                SecuritySchemeMapperV3.mapSecurity(server.getSecurity(), components),
                tags,
                ExternalDocMapperV3.map(server.getExternalDocs(), components),
                ServerBindingsMapper.mapBindings(server.getBindings(), components),
                server.getExtensions()
        );
    }

    /**
     * Maps an Apicurio {@link io.apicurio.datamodels.models.ServerVariable} to an
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable}.
     *
     * @param variable the Apicurio server variable object
     * @return the mapped AsyncApiServerVariable
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable mapVariable(
            io.apicurio.datamodels.models.ServerVariable variable) {
        return ServerVariableMapper.mapVariable(variable);
    }

    /**
     * Resolves a {@code $ref} to a component server by extracting the name from the reference
     * string and looking it up in the AsyncAPI 3.0 components map.
     *
     * @param $ref       the reference string (e.g. {@code #/components/servers/MyServer})
     * @param components the Apicurio components object
     * @return the resolved server, or null if not found
     */
    private static AsyncApiServer resolveRef(String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.SERVERS_REF_PREFIX)) {
            LOG.warn("Unsupported $ref format: {}. Skipping server.", $ref);
            return null;
        }
        String name = $ref.substring(Constants.SERVERS_REF_PREFIX.length());
        if (components == null) {
            return null;
        }
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (components instanceof AsyncApi30Components typed) {
            return typed.getServers() != null ? typed.getServers().get(name) : null;
        }
        return null;
    }

    /**
     * Dispatches a single Apicurio server to {@link #buildServer}, resolving any
     * {@code $ref} references before dispatch.
     *
     * @param server     the Apicurio server object (may be a reference)
     * @param components the Apicurio components object used for ref resolution
     * @return the mapped AsyncApiServer, or null for unresolvable refs or unknown server types
     */
    private static io.ballerina.asyncapi.core.model.server.AsyncApiServer mapOne(
            AsyncApiServer server, AsyncApiComponents components) {

        String $ref = null;
        if (server instanceof AsyncApiReferenceable referenceable) {
            $ref = referenceable.get$ref();
        }
        if ($ref != null) {
            AsyncApiServer resolved = resolveRef($ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping server.", $ref);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped
                    && resolvedTyped.get$ref() != null) {
                LOG.warn("Resolved $ref points to another $ref: {}. Skipping server.",
                        resolvedTyped.get$ref());
                return null;
            }
            return mapOne(resolved, components);
        }

        if (server instanceof AsyncApi30Server typedServer) {
            return buildServer(typedServer, components);
        }
        return null;
    }
}
