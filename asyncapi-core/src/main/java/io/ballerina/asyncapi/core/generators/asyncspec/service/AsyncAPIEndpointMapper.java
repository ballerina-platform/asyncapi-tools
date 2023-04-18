/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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


package io.ballerina.asyncapi.core.generators.asyncspec.service;


import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServersImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ATTR_HOST;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.PORT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SECURE_SOCKET;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SERVER;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SERVER_TYPE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WS;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WSS_LOCALHOST;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WSS_PREFIX;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WS_LOCALHOST;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WS_PREFIX;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WS_PROTOCOL_VERSION;

/**
 * Extract AsyncApi server information from and Ballerina endpoint.
 */
public class AsyncAPIEndpointMapper {
    public static final AsyncAPIEndpointMapper ENDPOINT_MAPPER = new AsyncAPIEndpointMapper();

    /**
     * Convert endpoints bound to {@code service} asyncapi server information.
     *
     * @param asyncAPI  asyncapi definition to attach extracted information
     * @param endpoints all endpoints defined in ballerina source
     * @param service   service node with bound endpoints
     * @return asyncapi definition with Server information
     */
    public AsyncApi25DocumentImpl getServers(AsyncApi25DocumentImpl asyncAPI, List<ListenerDeclarationNode> endpoints,
                                             ServiceDeclarationNode service) {
        List<AsyncApi25ServerImpl> servers = extractServerForExpressionNode(service.expressions(), service);
        if (!endpoints.isEmpty()) {
            for (ListenerDeclarationNode ep : endpoints) {
                SeparatedNodeList<ExpressionNode> exprNodes = service.expressions();
                for (ExpressionNode node : exprNodes) {
                    if (node.toString().trim().equals(ep.variableName().text().trim())) {
                        String serviceBasePath = getServiceBasePath(service);
                        AsyncApi25ServerImpl server = extractServer(ep, serviceBasePath);
                        servers.add(server);
                    }
                }
            }
        }

        if (servers.size() > 1) {
            AsyncApi25ServerImpl mainServer = addEnumValues(servers);
            AsyncApi25ServersImpl asyncApi25Servers = new AsyncApi25ServersImpl();
            asyncApi25Servers.addItem(SERVER_TYPE, mainServer);
            asyncAPI.setServers(asyncApi25Servers);
        } else {
            AsyncApi25ServersImpl asyncApi25Servers = new AsyncApi25ServersImpl();
            asyncApi25Servers.addItem(SERVER_TYPE, servers.get(0));
            asyncAPI.setServers(asyncApi25Servers);
        }
        return asyncAPI;
    }

    private AsyncApi25ServerImpl addEnumValues(List<AsyncApi25ServerImpl> servers) {
        AsyncApi25ServerImpl mainServer = servers.get(0);
        List<AsyncApi25ServerImpl> rotated = new ArrayList<>(servers);
        Map<String, ServerVariable> mainVariable = mainServer.getVariables();
        ServerVariable hostVariable = mainVariable.get(SERVER);
        ServerVariable portVariable = mainVariable.get(PORT);
        if (servers.size() > 1) {
            Collections.rotate(rotated, servers.size() - 1);
            for (AsyncApi25ServerImpl server : rotated) {
                Map<String, ServerVariable> variables = server.getVariables();

                setServerVariables(hostVariable, variables, SERVER);

                setServerVariables(portVariable, variables, PORT);
            }
        }
        return mainServer;
    }

    private void setServerVariables(ServerVariable variable,
                                    Map<String, ServerVariable> variables,
                                    String variableName) {
        if (variables.get(variableName) != null) {
            List<String> hostVariableEnum = variable.getEnum();
            if (hostVariableEnum == null) {
                hostVariableEnum = new ArrayList<>();
            }
            hostVariableEnum.add(variables.get(variableName).getDefault());
            variable.setEnum(hostVariableEnum);
        }
    }

    /**
     * Extract server URL from given listener node.
     */
    private AsyncApi25ServerImpl extractServer(ListenerDeclarationNode ep, String serviceBasePath) {
        Optional<ParenthesizedArgList> list;
        if (ep.initializer().kind() == SyntaxKind.CHECK_EXPRESSION) {
            ExpressionNode expression = ((CheckExpressionNode) ep.initializer()).expression();
            list = extractListenerNodeType(expression);
        } else {
            list = extractListenerNodeType(ep.initializer());
        }
        return generateServer(serviceBasePath, list);
    }

    private Optional<ParenthesizedArgList> extractListenerNodeType(Node expression2) {
        Optional<ParenthesizedArgList> list = Optional.empty();
        if (expression2.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            ExplicitNewExpressionNode bTypeExplicit = (ExplicitNewExpressionNode) expression2;
            list = Optional.ofNullable(bTypeExplicit.parenthesizedArgList());
        } else if (expression2.kind() == SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
            ImplicitNewExpressionNode bTypeInit = (ImplicitNewExpressionNode) expression2;
            list = bTypeInit.parenthesizedArgList();
        }
        return list;
    }

    // Function for handle both ExplicitNewExpressionNode in listener.
    private List<AsyncApi25ServerImpl> extractServerForExpressionNode(SeparatedNodeList<ExpressionNode> bTypeExplicit,
                                                                      ServiceDeclarationNode service) {
        String serviceBasePath = getServiceBasePath(service);
        Optional<ParenthesizedArgList> list;
        List<AsyncApi25ServerImpl> asyncApi25Servers = new ArrayList<>();
        for (ExpressionNode expressionNode : bTypeExplicit) {
            if (expressionNode.kind().equals(SyntaxKind.EXPLICIT_NEW_EXPRESSION)) {
                ExplicitNewExpressionNode explicit = (ExplicitNewExpressionNode) expressionNode;
                list = Optional.ofNullable(explicit.parenthesizedArgList());
                AsyncApi25ServerImpl server = generateServer(serviceBasePath, list);
                asyncApi25Servers.add(server);
            }

        }
        return asyncApi25Servers;
    }

    //Assign host and port values
    private AsyncApi25ServerImpl generateServer(String serviceBasePath, Optional<ParenthesizedArgList> list) {

        String port = null;
        String host = null;
        String secured = FALSE;

        AsyncApi25ServerImpl server = new AsyncApi25ServerImpl();

        if (list.isPresent()) {
            SeparatedNodeList<FunctionArgumentNode> arg = (list.get()).arguments();
            port = arg.get(0).toString().trim().replaceAll("\"", "");

            if (arg.size() > 1 && (arg.get(1) instanceof NamedArgumentNode)) {
                ExpressionNode bLangRecordLiteral = ((NamedArgumentNode) arg.get(1)).expression();
                if (bLangRecordLiteral instanceof MappingConstructorExpressionNode) {
                    ArrayList<String> extractedValues = extractHostAndCheckSecured(
                            (MappingConstructorExpressionNode) bLangRecordLiteral);
                    host = extractedValues.get(0);
                    secured = extractedValues.get(1);
                }
            }
        }
        server.setProtocol(WS);
        server.setProtocolVersion(WS_PROTOCOL_VERSION);
//        setServerProtocol(secured,server);
        setServerVariableValues(serviceBasePath, port, host, secured, server);
        return server;
    }

    /**
     * Set server variables port and server.
     */
    private void setServerVariableValues(String serviceBasePath, String port, String host, String secured,
                                         AsyncApi25ServerImpl server) {

        String serverUrl;
        if (host != null && port != null) {

            AsyncApi25ServerVariable serverUrlVariable = server.createServerVariable();
            if (secured.equals(TRUE)) {
                serverUrlVariable.setDefault(WSS_PREFIX + host);
            } else {
                serverUrlVariable.setDefault(WS_PREFIX + host);

            }
//            serverUrlVariable.setDefault( host);

            AsyncApi25ServerVariable portVariable = server.createServerVariable();
            portVariable.setDefault(port);
            server.addVariable(SERVER, serverUrlVariable);
            server.addVariable(PORT, portVariable);
            serverUrl = String.format("{server}:{port}%s", serviceBasePath);
            server.setUrl(serverUrl);
        } else if (host != null) {
            AsyncApi25ServerVariable serverUrlVariable = server.createServerVariable();
            serverUrlVariable.setDefault(host);
            server.addVariable(SERVER, serverUrlVariable);
            serverUrl = "{server}" + serviceBasePath;
            server.setUrl(serverUrl);

        } else if (port != null) {
            AsyncApi25ServerVariable serverUrlVariable = server.createServerVariable();
            if (secured.equals(TRUE)) {
                serverUrlVariable.setDefault(WSS_LOCALHOST);
            } else {
                serverUrlVariable.setDefault(WS_LOCALHOST);
            }
//            serverUrlVariable.setDefault(LOCALHOST);

            AsyncApi25ServerVariable portVariable = server.createServerVariable();
            portVariable.setDefault(port);
            server.addVariable(SERVER, serverUrlVariable);
            server.addVariable(PORT, portVariable);
            serverUrl = "{server}:{port}" + serviceBasePath;
            server.setUrl(serverUrl);
        }

    }

    // Extract host value for creating URL.
    private ArrayList<String> extractHostAndCheckSecured(MappingConstructorExpressionNode bLangRecordLiteral) {
        ArrayList<String> returnValues = new ArrayList<>();
        String host = null;
        String secured = FALSE;
        if (bLangRecordLiteral.fields() != null && !bLangRecordLiteral.fields().isEmpty()) {
            SeparatedNodeList<MappingFieldNode> recordFields = bLangRecordLiteral.fields();
            for (MappingFieldNode filed : recordFields) {
                if (filed instanceof SpecificFieldNode) {
                    Node fieldNode = ((SpecificFieldNode) filed).fieldName();
                    String fieldName = ConverterCommonUtils.unescapeIdentifier(fieldNode.toString().trim());
                    if (fieldName.trim().equals(ATTR_HOST)) {
                        if (((SpecificFieldNode) filed).valueExpr().isPresent()) {
                            host = ((SpecificFieldNode) filed).valueExpr().get().toString().trim();
                        }
                    } else if (fieldName.trim().equals(SECURE_SOCKET)) {
                        secured = TRUE;

                    }
                }
            }

        }
        if (host != null) {
            host = host.replaceAll("\"", "");
        }
        returnValues.add(host);
        returnValues.add(secured);

        return returnValues;
    }


    /**
     * Gets the base path of a service.
     *
     * @param serviceDefinition The service definition node.
     * @return The base path.
     */
    public String getServiceBasePath(ServiceDeclarationNode serviceDefinition) {
        StringBuilder currentServiceName = new StringBuilder();
        NodeList<Node> serviceNameNodes = serviceDefinition.absoluteResourcePath();
        for (Node serviceBasedPathNode : serviceNameNodes) {
            currentServiceName.append(ConverterCommonUtils.unescapeIdentifier(serviceBasedPathNode.toString()));
        }
        return currentServiceName.toString().trim();
    }
}
