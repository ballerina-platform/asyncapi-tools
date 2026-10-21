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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Per-channel routing: when any channel declares its own {@code x-ballerina-event-identifier}, every channel gets
 * its own resource path (its {@code address}) and its own identifier, the document's one being the fallback.
 * Values that repeat across channels (for example Twilio's {@code failed} for calls and messages) then stay apart.
 */
public final class ChannelRoutes {

    private static final Pattern PATH_SEGMENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");

    private ChannelRoutes() {
    }

    /**
     * Whether the listener routes by channel: true when any channel declares its own identifier.
     *
     * @param serviceTypes the service types, one per channel
     * @return {@code true} for one resource per channel, {@code false} for the single {@code post .} resource
     */
    public static boolean isPerChannel(List<HttpServiceType> serviceTypes) {
        return serviceTypes.stream().anyMatch(serviceType -> serviceType.identifier() != null);
    }

    /**
     * Gives every channel its effective identifier (its own, else the document's) and checks the routes.
     *
     * @param serviceTypes the service types, one per channel
     * @param documentIdentifier the document's identifier, or {@code null} if it declares none
     * @return the service types with their effective identifiers
     * @throws GeneratorException if a channel has no identifier, uses an unsupported type, or its address cannot
     *                            be a resource path, or two channels share an address
     */
    public static List<HttpServiceType> resolve(List<HttpServiceType> serviceTypes,
                                                EventIdentifierConfig documentIdentifier) throws GeneratorException {
        List<HttpServiceType> resolved = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        for (HttpServiceType serviceType : serviceTypes) {
            EventIdentifierConfig identifier = serviceType.identifier() != null
                    ? serviceType.identifier() : documentIdentifier;
            if (identifier == null) {
                throw new GeneratorException(String.format("Channel '%s' has no x-ballerina-event-identifier, and "
                        + "the document declares none to fall back to", serviceType.address()));
            }
            if (!EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(identifier.type())
                    && !EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(identifier.type())) {
                throw new GeneratorException(String.format("Channel '%s' uses a '%s' event identifier; with "
                        + "identifiers per channel, each channel's must be 'body' or 'header'",
                        serviceType.address(), identifier.type()));
            }
            String path = urlPath(serviceType.address());
            if (!paths.add(path)) {
                throw new GeneratorException(String.format(
                        "Two channels share the address '%s'; with identifiers per channel, each channel needs "
                                + "its own address", serviceType.address()));
            }
            resolved.add(new HttpServiceType(serviceType.serviceTypeName(), serviceType.remoteFunctions(),
                    serviceType.address(), identifier));
        }
        return resolved;
    }

    /**
     * The URL path a channel is served at, for example {@code /callStatus}.
     *
     * @param address the channel's address
     * @return the path, starting with {@code /}
     * @throws GeneratorException if the address is empty or a segment is not a plain name
     */
    public static String urlPath(String address) throws GeneratorException {
        return "/" + String.join("/", segments(address));
    }

    /**
     * The address's path segments, each a plain name ({@code [A-Za-z_][A-Za-z0-9_-]*}).
     *
     * @param address the channel's address
     * @return the segments, without empty ones
     * @throws GeneratorException if the address is empty or a segment is not a plain name
     */
    public static List<String> segments(String address) throws GeneratorException {
        List<String> segments = new ArrayList<>();
        if (address != null) {
            for (String segment : address.split("/")) {
                if (segment.isEmpty()) {
                    continue;
                }
                if (!PATH_SEGMENT.matcher(segment).matches()) {
                    throw new GeneratorException(String.format("Channel address '%s' cannot be a resource path: "
                            + "'%s' must be letters, digits, '_' or '-', starting with a letter or '_'",
                            address, segment));
                }
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            throw new GeneratorException(String.format(
                    "Channel address '%s' cannot be a resource path; with identifiers per channel, each channel "
                            + "needs an address such as 'callStatus'", address));
        }
        return segments;
    }

    /**
     * A path segment as a Ballerina resource path identifier: hyphens escaped, keywords quoted.
     *
     * @param segment a segment returned by {@link #segments(String)}
     * @return the identifier text
     */
    public static String resourceSegment(String segment) {
        String escaped = segment.replace("-", "\\-");
        return CodegenUtils.BAL_KEYWORDS.contains(segment) ? "'" + escaped : escaped;
    }
}
