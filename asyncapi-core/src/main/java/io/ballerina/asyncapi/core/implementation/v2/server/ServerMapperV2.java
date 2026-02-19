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
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServerBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Server;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Server;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Server;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Server;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Server;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Server;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Server;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.HttpServerBindings;
import io.ballerina.asyncapi.core.model.server.WsServerBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Server models to {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}
 * for AsyncAPI 2.x.
 */
public final class ServerMapperV2 {

    private static final String SCHEME_SEPARATOR = "://";

    private ServerMapperV2() {

    }

    /**
     * Maps Apicurio {@link AsyncApiServers} to a map of server names to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param servers         the Apicurio servers object
     * @param securitySchemes the security scheme definitions from components
     * @return the mapped servers map, or an empty map if servers is null
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            AsyncApiServers servers,
            Map<String, ? extends SecurityScheme> securitySchemes) {
        if (servers == null) {
            return Map.of();
        }
        List<String> serverNames = servers.getItemNames();
        if (serverNames == null || serverNames.isEmpty()) {
            return Map.of();
        }
        Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> result =
                new LinkedHashMap<>();
        for (String name : serverNames) {
            AsyncApiServer server = servers.getItem(name);
            if (server instanceof AsyncApi20Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        null, typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi21Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        null, typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi22Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        null, typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi23Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        null, typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi24Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        null, typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi25Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        mapTags(typedServer.getTags()),
                        typedServer.getSecurity(), securitySchemes));
            } else if (server instanceof AsyncApi26Server typedServer) {
                result.put(name, buildServer(typedServer, typedServer.getUrl(),
                        mapTags(typedServer.getTags()),
                        typedServer.getSecurity(), securitySchemes));
            }
        }
        return result;
    }

    /**
     * Maps a single Apicurio server to {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param server          the Apicurio server object
     * @param securitySchemes the security schemes map for resolution
     * @return the mapped AsyncApiServer, or null if server is null
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServer mapServer(
            AsyncApiServer server,
            Map<String, ? extends SecurityScheme> securitySchemes) {
        if (server == null) {
            return null;
        }

        return switch (server) {
            case AsyncApi26Server typed -> buildServer(typed, typed.getUrl(),
                    mapTags(typed.getTags()), typed.getSecurity(), securitySchemes);
            case AsyncApi25Server typed -> buildServer(typed, typed.getUrl(),
                    mapTags(typed.getTags()), typed.getSecurity(), securitySchemes);
            case AsyncApi24Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes);
            case AsyncApi23Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes);
            case AsyncApi22Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes);
            case AsyncApi21Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes);
            case AsyncApi20Server typed -> buildServer(typed, typed.getUrl(),
                    null, typed.getSecurity(), securitySchemes);
            default -> null;
        };
    }

    /**
     * Maps an Apicurio {@link AsyncApi20SecurityScheme} to an {@link AsyncApiSecurityScheme}.
     *
     * @param scheme the Apicurio security scheme object
     * @return the mapped AsyncApiSecurityScheme
     */
    public static AsyncApiSecurityScheme mapSecurityScheme(
            AsyncApi20SecurityScheme scheme) {
        return SecuritySchemeMapperV2.map(scheme);
    }

    /**
     * Maps Apicurio {@link AsyncApiServerBindings} to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings}.
     *
     * @param bindings the Apicurio server bindings object
     * @return the mapped AsyncApiServerBindings, or null if bindings is null or empty
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings
            mapBindings(AsyncApiServerBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpServerBindings http = bindings.getHttp() != null
                ? new HttpServerBindings() : null;
        WsServerBindings ws = bindings.getWs() != null
                ? new WsServerBindings() : null;
        if (http == null && ws == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings(
                http, ws);
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
     * @return the mapped AsyncApiServer
     */
    private static io.ballerina.asyncapi.core.model.server.AsyncApiServer buildServer(
            AsyncApiServer server, String url, List<AsyncApiTag> tags,
            List<? extends SecurityRequirement> requirements,
            Map<String, ? extends SecurityScheme> securitySchemes) {
        Map<String, JsonNode> extensions = null;
        if (server instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServer(
                parseHost(url),
                server.getProtocol(),
                server.getProtocolVersion(),
                parsePath(url),
                server.getDescription(),
                null,
                null,
                ServerVariableMapperV2.mapVariables(server.getVariables()),
                mapSecurity(requirements, securitySchemes),
                tags,
                null,
                mapBindings(server.getBindings()),
                extensions
        );
    }

    /**
     * Extracts the host portion from a server URL.
     *
     * @param url the server URL string
     * @return the host portion, or null if the URL is null
     */
    private static String parseHost(String url) {
        if (url == null) {
            return null;
        }
        int schemeEnd = url.indexOf(SCHEME_SEPARATOR);
        String afterScheme = schemeEnd >= 0 ? url.substring(schemeEnd + 3) : url;
        int pathStart = afterScheme.indexOf('/');
        return pathStart >= 0 ? afterScheme.substring(0, pathStart) : afterScheme;
    }

    /**
     * Extracts the pathname portion from a server URL.
     *
     * @param url the server URL string
     * @return the pathname portion, or null if the URL is null or has no path
     */
    private static String parsePath(String url) {
        if (url == null) {
            return null;
        }
        int schemeEnd = url.indexOf(SCHEME_SEPARATOR);
        String afterScheme = schemeEnd >= 0 ? url.substring(schemeEnd + 3) : url;
        int pathStart = afterScheme.indexOf('/');
        return pathStart >= 0 ? afterScheme.substring(pathStart) : null;
    }

    /**
     * Maps a list of Apicurio {@link Tag} objects to a list of {@link AsyncApiTag}.
     *
     * @param apicurioTags the Apicurio tag list
     * @return the mapped tag list, or null if the input is null
     */
    private static List<AsyncApiTag> mapTags(List<? extends Tag> apicurioTags) {
        if (apicurioTags == null) {
            return null;
        }
        return apicurioTags.stream()
                .map(TagMapperV2::map)
                .toList();
    }

    /**
     * Resolves security requirements against the component security scheme definitions.
     *
     * @param requirements the security requirements from a server
     * @param schemeLookup the security scheme definitions from components
     * @return the mapped security schemes list, or null if empty
     */
    private static List<AsyncApiSecurityScheme> mapSecurity(
            List<? extends SecurityRequirement> requirements,
            Map<String, ? extends SecurityScheme> schemeLookup) {
        if (requirements == null || requirements.isEmpty() || schemeLookup == null) {
            return null;
        }
        List<AsyncApiSecurityScheme> result = new ArrayList<>();
        for (SecurityRequirement requirement : requirements) {
            List<String> names = requirement.getItemNames();
            if (names == null) {
                continue;
            }
            for (String name : names) {
                SecurityScheme scheme = schemeLookup.get(name);
                if (scheme instanceof AsyncApi20SecurityScheme typedScheme) {
                    result.add(SecuritySchemeMapperV2.map(typedScheme));
                }
            }
        }
        return result.isEmpty() ? null : result;
    }
}
