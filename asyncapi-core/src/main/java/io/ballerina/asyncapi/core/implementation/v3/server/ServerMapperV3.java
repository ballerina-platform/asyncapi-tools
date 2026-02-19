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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.ExternalDocumentation;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServerBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Server;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Tag;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import io.ballerina.asyncapi.core.model.server.HttpServerBindings;
import io.ballerina.asyncapi.core.model.server.WsServerBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Server models to {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}
 * for AsyncAPI 3.0.
 */
public final class ServerMapperV3 {

    private ServerMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApiServers} to a map of server names to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer}.
     *
     * @param servers the Apicurio servers object
     * @return the mapped servers map, or an empty map if servers is null
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            AsyncApiServers servers) {
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
            if (server instanceof AsyncApi30Server typedServer) {
                result.put(name, buildServer(typedServer));
            }
        }
        return result;
    }

    /**
     * Builds an {@link io.ballerina.asyncapi.core.model.server.AsyncApiServer} from an
     * Apicurio {@link AsyncApi30Server} object.
     *
     * @param server the Apicurio AsyncAPI 3.0 server object
     * @return the mapped AsyncApiServer
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServer buildServer(
            AsyncApi30Server server) {
        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = server.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(ServerMapperV3::mapTag)
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
                ServerVariableMapperV3.mapVariables(server.getVariables()),
                mapSecurity(server.getSecurity()),
                tags,
                mapExternalDocs(server.getExternalDocs()),
                mapBindings(server.getBindings()),
                server.getExtensions()
        );
    }

    /**
     * Maps an Apicurio {@link AsyncApi30SecurityScheme} to an {@link AsyncApiSecurityScheme}.
     *
     * @param scheme the Apicurio security scheme object
     * @return the mapped AsyncApiSecurityScheme
     */
    public static AsyncApiSecurityScheme mapSecurityScheme(
            AsyncApi30SecurityScheme scheme) {
        return SecuritySchemeMapperV3.map(scheme);
    }

    /**
     * Maps a list of Apicurio security schemes to a list of {@link AsyncApiSecurityScheme}.
     *
     * @param securitySchemes the Apicurio security schemes list
     * @return the mapped security schemes list, or null if empty
     */
    public static List<AsyncApiSecurityScheme> mapSecurityList(
            List<AsyncApi30SecurityScheme> securitySchemes) {
        if (securitySchemes == null || securitySchemes.isEmpty()) {
            return null;
        }
        return securitySchemes.stream()
                .map(ServerMapperV3::mapSecurityScheme)
                .toList();
    }

    /**
     * Maps an Apicurio {@link io.apicurio.datamodels.models.ServerVariable} to an
     * {@link AsyncApiServerVariable}.
     *
     * @param name     the variable name
     * @param variable the Apicurio server variable object
     * @return the mapped AsyncApiServerVariable
     */
    public static AsyncApiServerVariable mapVariable(
            String name, ServerVariable variable) {
        return ServerVariableMapperV3.mapVariable(name, variable);
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
     * Maps a list of Apicurio security schemes to a list of {@link AsyncApiSecurityScheme}.
     *
     * @param securitySchemes the Apicurio security schemes list
     * @return the mapped security schemes list, or null if empty
     */
    private static List<AsyncApiSecurityScheme> mapSecurity(
            List<AsyncApi30SecurityScheme> securitySchemes) {
        if (securitySchemes == null || securitySchemes.isEmpty()) {
            return null;
        }
        return securitySchemes.stream()
                .map(SecuritySchemeMapperV3::map)
                .toList();
    }

    /**
     * Maps an Apicurio {@link Tag} to an {@link AsyncApiTag}.
     *
     * @param tag the Apicurio tag object
     * @return the mapped AsyncApiTag, or null if tag is null
     */
    private static AsyncApiTag mapTag(Tag tag) {
        if (tag == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (tag instanceof AsyncApi30Tag typedTag) {
            extensions = typedTag.getExtensions();
        }
        return new AsyncApiTag(
                tag.getName(),
                tag.getDescription(),
                mapExternalDocs(tag.getExternalDocs()),
                extensions
        );
    }

    /**
     * Maps an Apicurio {@link ExternalDocumentation} to an {@link AsyncApiExternalDocs}.
     *
     * @param externalDocs the Apicurio external documentation object
     * @return the mapped AsyncApiExternalDocs, or null if externalDocs is null
     */
    private static AsyncApiExternalDocs mapExternalDocs(ExternalDocumentation externalDocs) {
        if (externalDocs == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (externalDocs instanceof AsyncApi30ExternalDocumentation typedDocs) {
            extensions = typedDocs.getExtensions();
        }
        return new AsyncApiExternalDocs(
                externalDocs.getDescription(),
                URIUtils.toUri(externalDocs.getUrl()),
                extensions
        );
    }
}
