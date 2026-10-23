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
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (servers == null) {
            return Collections.emptyMap();
        }
        List<String> serverNames = servers.getItemNames();
        if (serverNames == null || serverNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> result = new LinkedHashMap<>();
        for (String name : serverNames) {
            io.ballerina.asyncapi.core.model.server.AsyncApiServer mapped =
                    mapServerItem(servers.getItem(name), components);
            if (mapped != null) {
                result.put(name, mapped);
            }
        }
        return result;
    }

    /**
     * Maps a single Apicurio {@link AsyncApiServer} to an
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer},
     * resolving any {@code $ref} references before building the model.
     *
     * @param server     the Apicurio server object (may be a reference)
     * @param components the Apicurio components object used for ref resolution
     * @return the mapped AsyncApiServer, or null for unresolvable refs or unknown server types
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServer mapServerItem(AsyncApiServer server,
            AsyncApiComponents components) {
        String ref = server instanceof AsyncApiReferenceable referenceable ? referenceable.get$ref() : null;
        if (ref != null) {
            AsyncApiServer resolved = resolveRef(ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping server.", ref);
                return null;
            }
            return mapServerItem(resolved, components);
        }
        if (server instanceof AsyncApi30Server typedServer) {
            List<AsyncApiTag> tags = null;
            if (typedServer.getTags() != null) {
                tags = typedServer.getTags().stream()
                        .map(tag -> TagMapperV3.map(tag, components))
                        .toList();
            }
            return new io.ballerina.asyncapi.core.model.server.AsyncApiServer(
                    typedServer.getHost(),
                    typedServer.getProtocol(),
                    typedServer.getProtocolVersion(),
                    typedServer.getPathname(),
                    typedServer.getDescription(),
                    typedServer.getTitle(),
                    typedServer.getSummary(),
                    ServerVariableMapperV3.map(typedServer.getVariables(), components),
                    SecuritySchemeMapperV3.map(typedServer.getSecurity(), components),
                    tags,
                    ExternalDocMapperV3.map(typedServer.getExternalDocs(), components),
                    ServerBindingsMapper.mapBindings(typedServer.getBindings(), components),
                    typedServer.getExtensions()
            );
        }
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        // e.g. if (server instanceof AsyncApi31Server typedServer) { ... }
        return null;
    }

    /**
     * Resolves a {@code $ref} to a component server by extracting the name from the reference
     * string and looking it up in the AsyncAPI 3.0 components map.
     *
     * @param ref        the reference string (e.g. {@code #/components/servers/MyServer})
     * @param components the Apicurio components object
     * @return the resolved server, or null if not found
     */
    private static AsyncApiServer resolveRef(String ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = ref;
        while (current != null) {
            if (!current.startsWith(Constants.SERVERS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping server.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping server.", current);
                return null;
            }
            String name = current.substring(Constants.SERVERS_REF_PREFIX.length());
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            AsyncApiServer resolved = null;
            if (components instanceof AsyncApi30Components typed) {
                resolved = typed.getServers() != null ? typed.getServers().get(name) : null;
            }
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching server found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                current = resolvedTyped.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
