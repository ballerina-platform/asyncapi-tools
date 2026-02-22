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
import io.ballerina.asyncapi.core.implementation.common.ServerMapper;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            AsyncApiServers servers,
            AsyncApiComponents components) {
        return ServerMapper.map(servers, server -> mapOne(server, components));
    }

    /**
     * Maps a raw map of Apicurio server entries (as found in AsyncAPI 2.x components)
     * to a map of server names to {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param rawServers  a map of server name to Apicurio server (may be null)
     * @param components  the Apicurio components object
     * @return the mapped servers map, or null if rawServers is null or empty
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            Map<String, ? extends AsyncApiServer> rawServers,
            AsyncApiComponents components) {

        if (rawServers == null || rawServers.isEmpty()) {
            return null;
        }
        Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> result = new LinkedHashMap<>();
        rawServers.forEach((name, server) -> {
            if (server != null) {
                io.ballerina.asyncapi.core.model.server.AsyncApiServer mapped =
                        mapOne(server, components);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        });
        return result.isEmpty() ? null : result;
    }

    /**
     * Resolves a {@code $ref} to a component server by extracting the name from the reference
     * string and looking it up in the version-specific components map.
     *
     * @param $ref       the reference string (e.g. {@code #/components/servers/MyServer})
     * @param components the Apicurio components object
     * @return the resolved server, or null if not found or unsupported version
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
        // Component servers are only available in AsyncAPI 2.3+.
        Map<String, ? extends AsyncApiServer> serversMap = switch (components) {
            case AsyncApi26Components typed -> typed.getServers();
            case AsyncApi25Components typed -> typed.getServers();
            case AsyncApi24Components typed -> typed.getServers();
            case AsyncApi23Components typed -> typed.getServers();
            default -> null;
        };
        return serversMap != null ? serversMap.get(name) : null;
    }

    /**
     * Dispatches a single Apicurio server to the version-specific {@link #buildServer} call,
     * resolving any {@code $ref} references before dispatch.
     *
     * @param server     the Apicurio server object (may be a reference)
     * @param components the Apicurio components object used for ref resolution and security lookup
     * @return the mapped AsyncApiServer, or null for unresolvable refs or unknown server types
     */
    private static io.ballerina.asyncapi.core.model.server.AsyncApiServer mapOne(
            AsyncApiServer server,
            AsyncApiComponents components) {

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

        Map<String, SecurityScheme> securitySchemes =
                components != null ? components.getSecuritySchemes() : null;
        return switch (server) {
            case AsyncApi20Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes, components);
            case AsyncApi21Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes, components);
            case AsyncApi22Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes, components);
            case AsyncApi23Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes, components);
            case AsyncApi24Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes, components);
            case AsyncApi25Server typed -> buildServer(typed, typed.getUrl(),
                    typed.getTags() == null ? null
                            : typed.getTags().stream().map(TagMapperV2::map).toList(),
                    typed.getSecurity(), securitySchemes, components);
            case AsyncApi26Server typed -> buildServer(typed, typed.getUrl(),
                    typed.getTags() == null ? null
                            : typed.getTags().stream().map(TagMapperV2::map).toList(),
                    typed.getSecurity(), securitySchemes, components);
            default -> null;
        };
    }

    /**
     * Builds an {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer} from an
     * Apicurio {@link AsyncApiServer} object, a URL string, and optional tags.
     *
     * @param server          the Apicurio server object
     * @param url             the server URL to parse into host and pathname
     * @param tags            the mapped tags, or null if not available for this version
     * @param requirements    the security requirements from this server
     * @param securitySchemes the security scheme definitions from components
     * @param components      the Apicurio components object for server variable $ref resolution
     * @return the mapped AsyncApiServer
     */
    private static io.ballerina.asyncapi.core.model.server.AsyncApiServer buildServer(
            AsyncApiServer server, String url, List<AsyncApiTag> tags,
            List<SecurityRequirement> requirements,
            Map<String, SecurityScheme> securitySchemes, AsyncApiComponents components) {
        Map<String, JsonNode> extensions = null;
        if (server instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServer(
                URIUtils.parseHost(url),
                server.getProtocol(),
                server.getProtocolVersion(),
                URIUtils.parsePath(url),
                server.getDescription(),
                null,
                null,
                ServerVariableMapperV2.mapVariables(server.getVariables(), components),
                SecuritySchemeMapperV2.mapSecurity(requirements, securitySchemes),
                tags,
                null,
                ServerBindingsMapper.mapBindings(server.getBindings(), components),
                extensions
        );
    }


}
