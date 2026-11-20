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
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.semver4j.Semver;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A view of a spec whose security schemes are ones a WebSocket client can send when it connects.
 *
 * <p>A {@code userPassword} scheme, and an {@code apiKey} sent as the user or the password, become HTTP Basic
 * credentials on the handshake. An {@code apiKey} sent in a header, query or cookie, which AsyncAPI calls an
 * {@code httpApiKey}, is read as one. Any other {@code apiKey} is left out. Each change is reported with a warning.
 */
public final class SecuritySchemeSpec implements AsyncApiSpec {

    private static final String USER_PASSWORD = "userPassword";
    private static final String API_KEY = "apiKey";
    private static final String HTTP = "http";
    private static final String HTTP_API_KEY = "httpApiKey";
    private static final String BASIC = "basic";
    private static final Set<String> CREDENTIALS = Set.of("user", "password");
    private static final Set<String> HTTP_LOCATIONS = Set.of("header", "query", "cookie");

    private final AsyncApiSpec spec;
    private final AsyncApiComponent components;

    private SecuritySchemeSpec(AsyncApiSpec spec, AsyncApiComponent components) {
        this.spec = spec;
        this.components = components;
    }

    /**
     * Returns {@code spec} with its {@code userPassword} and {@code apiKey} schemes read as schemes a WebSocket
     * client can send, or unchanged when it has neither.
     *
     * @param spec     the parsed spec
     * @param warnings where to report each scheme read differently or left out
     * @return the spec, or a view of it
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Consumer<SpecWarning> warnings) {
        AsyncApiComponent components = spec == null ? null : spec.getAsyncApiComponents().orElse(null);
        if (components == null || components.securitySchemes() == null || components.securitySchemes().values()
                .stream().noneMatch(scheme -> scheme != null
                        && (USER_PASSWORD.equals(scheme.type()) || API_KEY.equals(scheme.type())))) {
            return spec;
        }
        Map<String, AsyncApiSecurityScheme> schemes = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiSecurityScheme> entry : components.securitySchemes().entrySet()) {
            AsyncApiSecurityScheme scheme = readable(entry.getKey(), entry.getValue(), warnings);
            if (scheme != null) {
                schemes.put(entry.getKey(), scheme);
            }
        }
        return new SecuritySchemeSpec(spec, new AsyncApiComponent(components.schemas(), components.servers(),
                components.serverVariables(), components.channels(), components.operations(), components.messages(),
                schemes, components.parameters(), components.correlationIds(), components.operationTraits(),
                components.messageTraits(), components.replies(), components.replyAddresses(),
                components.externalDocs(), components.tags(), components.serverBindings(),
                components.channelBindings(), components.operationBindings(), components.messageBindings(),
                components.extensions()));
    }

    // The scheme as the client can send it, or null when it can't
    private static AsyncApiSecurityScheme readable(String name, AsyncApiSecurityScheme scheme,
                                                   Consumer<SpecWarning> warnings) {
        if (scheme == null || !USER_PASSWORD.equals(scheme.type()) && !API_KEY.equals(scheme.type())) {
            return scheme;
        }
        String location = "/components/securitySchemes/" + name.replace("~", "~0").replace("/", "~1");
        String in = scheme.in() != null ? scheme.in().toLowerCase(Locale.ROOT) : "";
        if (USER_PASSWORD.equals(scheme.type())) {
            warnings.accept(new SpecWarning(location, String.format("security scheme '%s' is userPassword, so the "
                    + "client sends the user and password as HTTP Basic credentials when it connects", name),
                    "Set them in the client's 'auth' configuration. If the server expects them another way, "
                    + "describe the scheme as 'http' or 'httpApiKey' instead."));
            return withType(scheme, HTTP, BASIC, null, null);
        }
        if (CREDENTIALS.contains(in)) {
            warnings.accept(new SpecWarning(location, String.format("security scheme '%s' is an apiKey sent as the "
                    + "%s, so the client sends it in HTTP Basic credentials when it connects", name, in),
                    String.format("Set the key as the %s in the client's 'auth' configuration.", in)));
            return withType(scheme, HTTP, BASIC, null, null);
        }
        if (HTTP_LOCATIONS.contains(in) && scheme.name() != null && !scheme.name().isBlank()) {
            warnings.accept(new SpecWarning(location, String.format("security scheme '%s' is an apiKey sent in a "
                    + "%s, which AsyncAPI calls an httpApiKey, so the client sends it as the %s '%s'", name, in, in,
                    scheme.name()), "Change its type to 'httpApiKey'."));
            return withType(scheme, HTTP_API_KEY, null, scheme.name(), in);
        }
        warnings.accept(new SpecWarning(location, String.format("security scheme '%s' is an apiKey without a "
                + "place the client can send it, so the client leaves it out", name), "Give it 'in: user' or "
                + "'in: password', or describe it as an 'httpApiKey' with 'in' and 'name'. Until then, set the "
                + "client's configuration by hand."));
        return null;
    }

    private static AsyncApiSecurityScheme withType(AsyncApiSecurityScheme scheme, String type, String httpScheme,
                                                   String keyName, String in) {
        return new AsyncApiSecurityScheme(type, scheme.description(), keyName, in, httpScheme, null, null, null,
                scheme.scopes(), scheme.extensions());
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
        return spec.getAsyncApiOperations();
    }

    @Override
    public Optional<AsyncApiComponent> getAsyncApiComponents() {
        return Optional.of(components);
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return spec.getAsyncApiExtensions();
    }
}
