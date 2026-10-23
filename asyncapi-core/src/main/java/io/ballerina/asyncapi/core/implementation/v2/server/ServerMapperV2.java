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
package io.ballerina.asyncapi.core.implementation.v2.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.SecurityRequirement;
import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Server;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Server;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Server;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Server;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Server;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Server;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Server;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
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
 * for AsyncAPI 2.x.
 */
public final class ServerMapperV2 {

    private static final Logger LOG = LogManager.getLogger(ServerMapperV2.class);

    private ServerMapperV2() {
    }

    /**
     * Maps Apicurio {@link AsyncApiServers} to a map of server names to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param servers     the Apicurio servers object
     * @param components  the Apicurio components object
     * @return the mapped servers map, or an empty map if servers is null
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(AsyncApiServers servers,
            AsyncApiComponents components) {
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
     * @param components the Apicurio components object used for ref resolution and security lookup
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
        String url;
        List<AsyncApiTag> tags;
        List<SecurityRequirement> requirements;
        switch (server) {
            case AsyncApi25Server typed -> {
                url = typed.getUrl();
                tags = typed.getTags() == null ? null : typed.getTags().stream().map(TagMapperV2::map).toList();
                requirements = typed.getSecurity();
            }
            case AsyncApi26Server typed -> {
                url = typed.getUrl();
                tags = typed.getTags() == null ? null : typed.getTags().stream().map(TagMapperV2::map).toList();
                requirements = typed.getSecurity();
            }
            case AsyncApi20Server typed -> {
                url = typed.getUrl();
                tags = null;
                requirements = typed.getSecurity();
            }
            case AsyncApi21Server typed -> {
                url = typed.getUrl();
                tags = null;
                requirements = typed.getSecurity();
            }
            case AsyncApi22Server typed -> {
                url = typed.getUrl();
                tags = null;
                requirements = typed.getSecurity();
            }
            case AsyncApi23Server typed -> {
                url = typed.getUrl();
                tags = null;
                requirements = typed.getSecurity();
            }
            case AsyncApi24Server typed -> {
                url = typed.getUrl();
                tags = null;
                requirements = typed.getSecurity();
            }
            default -> throw new IllegalArgumentException("Unsupported AsyncAPI server version: "
                    + server.getClass().getName());
        }
        Map<String, JsonNode> extensions = ((AsyncApiExtensible) server).getExtensions();
        Map<String, SecurityScheme> securitySchemes = components != null ? components.getSecuritySchemes() : null;
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServer(
                URIUtils.parseHost(url),
                server.getProtocol(),
                server.getProtocolVersion(),
                URIUtils.parsePath(url),
                server.getDescription(),
                null, //title is not available in 2.x
                null, //summary is not available in 2.x
                ServerVariableMapperV2.map(server.getVariables(), components),
                SecuritySchemeMapperV2.map(requirements, securitySchemes),
                tags,
                null, //external docs is not available in 2.x
                ServerBindingsMapper.mapBindings(server.getBindings(), components),
                extensions
        );
    }

    /**
     * Resolves a {@code $ref} to a component server by extracting the name from the reference
     * string and looking it up in the version-specific components map.
     *
     * @param ref        the reference string (e.g. {@code #/components/servers/MyServer})
     * @param components the Apicurio components object
     * @return the resolved server, or null if not found or unsupported version
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
            // Component servers are only available in AsyncAPI 2.3+.
            Map<String, ? extends AsyncApiServer> serversMap = switch (components) {
                case AsyncApi26Components typed -> typed.getServers();
                case AsyncApi25Components typed -> typed.getServers();
                case AsyncApi24Components typed -> typed.getServers();
                case AsyncApi23Components typed -> typed.getServers();
                default -> null;
            };
            AsyncApiServer resolved = serversMap != null ? serversMap.get(name) : null;
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
