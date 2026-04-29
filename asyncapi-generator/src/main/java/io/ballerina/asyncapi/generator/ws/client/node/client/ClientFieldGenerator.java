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
package io.ballerina.asyncapi.generator.ws.client.node.client;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.BallerinaAuthConfigGenerator;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds the class-level instance variable field nodes for a WebSocket client class.
 */
public class ClientFieldGenerator {

    private static final String CLIENT_EP = "clientEp";
    private static final String WRITE_MESSAGE_QUEUE = "writeMessageQueue";
    private static final String PIPES_MAP = "PipesMap";
    private static final String PIPES = "pipes";
    private static final String STREAM_GENERATORS_MAP = "StreamGeneratorsMap";
    private static final String STREAM_GENERATORS = "streamGenerators";
    private static final String BOOLEAN = "boolean";
    private static final String IS_ACTIVE = "isActive";
    private static final String RESPONSE_MAP = "responseMap";

    private final boolean isStreamPresent;
    private final BallerinaAuthConfigGenerator authGen;
    private final Map<String, String> responseMap;

    /**
     * Creates a new client field generator.
     *
     * @param isStreamPresent whether streaming return types exist
     * @param authGen         auth config generator
     * @param responseMap     response type to pipe name map
     */
    public ClientFieldGenerator(boolean isStreamPresent, BallerinaAuthConfigGenerator authGen,
                                 Map<String, String> responseMap) {
        this.isStreamPresent = isStreamPresent;
        this.authGen = authGen;
        this.responseMap = responseMap;
    }

    /**
     * Builds the list of object field nodes for the WebSocket client class.
     *
     * @return list of object field nodes
     * @throws GeneratorException if auth config field generation fails
     */
    public List<ObjectFieldNode> buildFields() throws GeneratorException {
        List<ObjectFieldNode> fields = new ArrayList<>();

        fields.add((ObjectFieldNode) NodeParser.parseObjectMember(
                String.format("private final websocket:Client %s;", CLIENT_EP)));
        fields.add((ObjectFieldNode) NodeParser.parseObjectMember(
                String.format("private final pipe:Pipe %s;", WRITE_MESSAGE_QUEUE)));
        fields.add((ObjectFieldNode) NodeParser.parseObjectMember(
                String.format("private final %s %s;", PIPES_MAP, PIPES)));
        if (isStreamPresent) {
            fields.add((ObjectFieldNode) NodeParser.parseObjectMember(
                    String.format("private final %s %s;", STREAM_GENERATORS_MAP, STREAM_GENERATORS)));
        }
        fields.add((ObjectFieldNode) NodeParser.parseObjectMember(
                String.format("private %s %s;", BOOLEAN, IS_ACTIVE)));

        ObjectFieldNode apiKeyField = authGen.getApiKeyMapClassVariable();
        if (apiKeyField != null) {
            fields.add(apiKeyField);
        }

        fields.add(buildResponseMapField());
        return fields;
    }

    /**
     * Builds the {@code responseMap} field node with its literal initializer.
     *
     * @return the object field node
     */
    private ObjectFieldNode buildResponseMapField() {
        StringBuilder mapExpr = new StringBuilder("{\n");
        Iterator<Map.Entry<String, String>> it = responseMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            mapExpr.append("    \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            if (it.hasNext()) {
                mapExpr.append(",\n");
            } else {
                mapExpr.append("\n    }");
            }
        }
        return (ObjectFieldNode) NodeParser.parseObjectMember(
                String.format("private final readonly & map<string> %s = %s;", RESPONSE_MAP, mapExpr));
    }
}
