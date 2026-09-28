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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the module-level {@code EVENT_PAYLOAD_TYPES} map in {@code dispatcher_service.bal},
 * associating each event identifier with the concrete record type its payload binds to.
 *
 * <p>The dispatcher needs this because the payload has to be bound to the actual event's record
 * type, not to the {@code GenericDataType} union. Resolving a union target picks the first
 * structurally matching member, and every generated event record is open with all-optional fields,
 * so every payload matches the first member declared - meaning the value handed to a handler was
 * typed by declaration order rather than by which event arrived.
 */
public class GenerateEventPayloadTypesNode implements Generator {

    public static final String EVENT_PAYLOAD_TYPES_MAP = "EVENT_PAYLOAD_TYPES";

    private final List<HttpServiceType> serviceTypes;

    /**
     * Creates a generator for the event-identifier to payload-type map.
     *
     * @param serviceTypes the service types whose remote functions supply the mapping
     */
    public GenerateEventPayloadTypesNode(List<HttpServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    @Override
    public ModuleMemberDeclarationNode generate() throws GeneratorException {
        // A LinkedHashMap keyed by event identifier: a spec may route two events to one payload
        // type, but never one identifier to two, so the last writer for a key is the only entry.
        Map<String, String> entries = new LinkedHashMap<>();
        for (HttpServiceType service : serviceTypes) {
            for (HttpRemoteFunction fn : service.remoteFunctions()) {
                entries.put(fn.functionName(), CodegenUtils.getValidName(
                        CodegenUtils.escapeIdentifier(fn.eventType().trim()), true));
            }
        }
        if (entries.isEmpty()) {
            throw new GeneratorException("No events found to build the event payload type map");
        }

        List<String> pairs = new ArrayList<>();
        entries.forEach((eventIdentifier, payloadType) ->
                pairs.add(String.format("\"%s\": %s", eventIdentifier, payloadType)));

        return NodeParser.parseModuleMemberDeclaration(String.format(
                "final readonly & map<typedesc<%s>> %s = {%s};",
                DataTypesGenerator.GENERIC_DATA_TYPE, EVENT_PAYLOAD_TYPES_MAP, String.join(", ", pairs)));
    }
}
