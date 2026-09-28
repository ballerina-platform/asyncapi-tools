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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeParser;

/**
 * Generates the {@code parseEventPayload} helper in {@code dispatcher_service.bal}, which binds a
 * delivery to the concrete record type registered for its event identifier.
 *
 * <p>Binding to the concrete type rather than to the {@code GenericDataType} union is what makes
 * the value handed to a handler reflect the event that actually arrived. A union target resolves by
 * first structural match, and every generated event record is open with all-optional fields, so
 * every payload matches the first member declared.
 *
 * <p>A composite identifier is looked up by its compound value first, falling back to the bare
 * event type, mirroring how the dispatch match statements are ordered for the same events.
 */
public class GenerateParseEventPayloadFuncNode implements Generator {

    public static final String PARSE_EVENT_PAYLOAD_FUNC = "parseEventPayload";

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        String functionText = String.format(
                "private isolated function %s(json payload, string eventKey, string? fallbackKey = ())"
                        + " returns %s|error {"
                        + " typedesc<%s>? targetType = %s[eventKey];"
                        + " if targetType is () && fallbackKey is string {"
                        + " targetType = %s[fallbackKey]; }"
                        + " if targetType is () {"
                        + " return error(string `Unrecognized event identifier: ${eventKey}`); }"
                        + " return jsondata:parseAsType(payload, {}, targetType);"
                        + " }",
                PARSE_EVENT_PAYLOAD_FUNC,
                DataTypesGenerator.GENERIC_DATA_TYPE,
                DataTypesGenerator.GENERIC_DATA_TYPE,
                GenerateEventPayloadTypesNode.EVENT_PAYLOAD_TYPES_MAP,
                GenerateEventPayloadTypesNode.EVENT_PAYLOAD_TYPES_MAP);

        return (FunctionDefinitionNode) NodeParser.parseObjectMember(functionText);
    }
}
