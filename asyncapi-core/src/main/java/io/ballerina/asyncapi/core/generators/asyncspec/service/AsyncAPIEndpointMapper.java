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
import io.apicurio.datamodels.models.asyncapi.v25.*;
import io.ballerina.asyncapi.core.generators.asyncspec.Constants;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.syntax.tree.*;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.PORT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SERVER;

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
        List<AsyncApi25ServerImpl> servers = extractServerForExpressionNode(asyncAPI, service.expressions(), service);
//        AsyncApi25ServersImpl expressionServers = (AsyncApi25ServersImpl) asyncAPI.getServers();

//        List<AsyncApi25ServerImpl> servers = new ArrayList<>();
//        servers.add((AsyncApi25ServerImpl) expressionServers.getItem("development"));
        if (!endpoints.isEmpty()) {
            for (ListenerDeclarationNode ep : endpoints) {
                SeparatedNodeList<ExpressionNode> exprNodes = service.expressions();
                for (ExpressionNode node : exprNodes) {
                    if (node.toString().trim().equals(ep.variableName().text().trim())) {
                        String serviceBasePath = getServiceBasePath(service);
                        AsyncApi25ServerImpl server = extractServer(ep, serviceBasePath);
//                        servers.addItem("check",server);
                        servers.add(server);
                    }
                }
            }
        }

        if (servers.size() > 1) {
            AsyncApi25ServerImpl mainServer = addEnumValues(servers);
            AsyncApi25ServersImpl asyncApi25Servers=new AsyncApi25ServersImpl();
            asyncApi25Servers.addItem("development",mainServer);
            asyncAPI.setServers(asyncApi25Servers);
//            asyncAPI.setServers(Collections.singletonList(mainServer));
        }else{
            AsyncApi25ServersImpl asyncApi25Servers=new AsyncApi25ServersImpl();
            asyncApi25Servers.addItem("development",servers.get(0));
            asyncAPI.setServers(asyncApi25Servers);
        }
        return asyncAPI;
    }

    private AsyncApi25ServerImpl addEnumValues(List<AsyncApi25ServerImpl> servers) {
//        AsyncApi25ServerImpl mainServer= (AsyncApi25ServerImpl) servers.getItem("private");
        AsyncApi25ServerImpl mainServer = servers.get(0);
//        Map<String, AsyncApi25ServersImpl> rotated = new LinkedHashMap<>(servers);
        List<AsyncApi25ServerImpl> rotated = new ArrayList<>(servers);
        Map<String, ServerVariable> mainVariable = mainServer.getVariables();
        ServerVariable hostVariable = mainVariable.get(SERVER);
        ServerVariable portVariable = mainVariable.get(PORT);
        if (servers.size() > 1) {
            Collections.rotate(rotated, servers.size() - 1);
            for (AsyncApi25ServerImpl server: rotated) {
                Map<String, ServerVariable>  variables = server.getVariables();

                setServerVariables(hostVariable, variables, SERVER);

                setServerVariables(portVariable, variables, PORT);
            }
        }
        return mainServer;
    }

    private void setServerVariables(ServerVariable variable, Map<String, ServerVariable> variables, String variableName) {
        if (variables.get(variableName) != null){
            List<String> hostVariableEnum= variable.getEnum();
            if (hostVariableEnum == null) {
                hostVariableEnum = new ArrayList<>();
            }
           hostVariableEnum.add(variables.get(variableName).getDefault());
           variable.setEnum(hostVariableEnum);
//                    hostVariable.getEnum().add(variables.get(SERVER).getDefault());
//                    hostVariable.setEnum(variables.get(SERVER).getDefault());
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

    // Function for handle both ExplicitNewExpressionNode and ImplicitNewExpressionNode in listener.
    private List<AsyncApi25ServerImpl>  extractServerForExpressionNode(AsyncApi25DocumentImpl asyncAPI, SeparatedNodeList<ExpressionNode> bTypeExplicit,
                                                                    ServiceDeclarationNode service) {
        String serviceBasePath = getServiceBasePath(service);
        Optional<ParenthesizedArgList> list;
//        AsyncApi25ServersImpl servers = (AsyncApi25ServersImpl) asyncAPI.createServers();
        List<AsyncApi25ServerImpl> asyncApi25Servers = new ArrayList<>();
//        AsyncApi25ServersImpl asyncApi25Servers = (AsyncApi25ServersImpl) asyncAPI.createServers();
        for (ExpressionNode expressionNode: bTypeExplicit) {
            if (expressionNode.kind().equals(SyntaxKind.EXPLICIT_NEW_EXPRESSION)) {
                ExplicitNewExpressionNode explicit = (ExplicitNewExpressionNode) expressionNode;
                list = Optional.ofNullable(explicit.parenthesizedArgList());
                AsyncApi25ServerImpl server = generateServer(serviceBasePath, list);
//                asyncApi25Servers.ge
                asyncApi25Servers.add(server);
//                servers.add(server);
//                servers.addItem("private",server);
//                servers.add(server);
            } else if (expressionNode.kind().equals(SyntaxKind.IMPLICIT_NEW_EXPRESSION)) {
                ImplicitNewExpressionNode implicit = (ImplicitNewExpressionNode) expressionNode;
                list = implicit.parenthesizedArgList();
                AsyncApi25ServerImpl server = generateServer(serviceBasePath, list);
                asyncApi25Servers.add(server);
//                servers.add(server);
//                servers.addItem("public",server);
//                servers.add(server);
            }
        }
//        AsyncApi25ServersImpl asyncApi25Servers = (AsyncApi25ServersImpl) asyncAPI.createServers();
//        asyncApi25Servers.addItem("development",);
//        asyncAPI.setServers(asyncApi25Servers);
        return asyncApi25Servers;
    }

    //Assign host and port values
    private AsyncApi25ServerImpl generateServer(String serviceBasePath, Optional<ParenthesizedArgList> list) {

        String port = null;
        String host = null;
//        AsyncApi25ServerVariableImpl serverVariable = new AsyncApi25ServerVariableImpl();
//        Server server = new Server();
        AsyncApi25ServerImpl server= new AsyncApi25ServerImpl();

        if (list.isPresent()) {
            SeparatedNodeList<FunctionArgumentNode> arg = (list.get()).arguments();
            port = arg.get(0).toString();
            if (arg.size() > 1 && (arg.get(1) instanceof NamedArgumentNode)) {
                ExpressionNode bLangRecordLiteral = ((NamedArgumentNode) arg.get(1)).expression();
                if (bLangRecordLiteral instanceof MappingConstructorExpressionNode) {
                    host = extractHost((MappingConstructorExpressionNode) bLangRecordLiteral);
                }
            }
        }
        // Set default values to host and port if values are not defined
        setServerVariableValues(serviceBasePath, port, host, server);
        return server;
    }

    /**
     * Set server variables port and server.
     */
    private void setServerVariableValues(String serviceBasePath, String port, String host,
                                          AsyncApi25ServerImpl server) {

        String serverUrl;
        if (host != null && port != null) {
//            AsyncApi25ServerVariable serverVariable = new AsyncApi25ServerVariableImpl();

            AsyncApi25ServerVariable serverUrlVariable= server.createServerVariable();
            serverUrlVariable.setDefault(host);
            AsyncApi25ServerVariable portVariable = server.createServerVariable();
            portVariable.setDefault(port);
//            serverUrlVariable.setDefault(host);
//            server.addVariable();

//            ServerVariable portVariable =  new ServerVariable();
//            portVariable._default(port);
            server.addVariable(SERVER,serverUrlVariable);
            server.addVariable(PORT,portVariable);
//            serverVariables.addServerVariable(SERVER, serverUrlVariable);
//            serverVariables.addServerVariable(PORT, portVariable);
            serverUrl = String.format("{server}:{port}%s", serviceBasePath);
            server.setUrl(serverUrl);
//            server.setVariables(serverVariables);
        } else if (host != null) {
            AsyncApi25ServerVariable serverUrlVariable= server.createServerVariable();
//            ServerVariable serverUrlVariable = new ServerVariable();
            serverUrlVariable.setDefault(host);

            server.addVariable(SERVER,serverUrlVariable);
//            serverVariables.addServerVariable(SERVER, serverUrlVariable);
            serverUrl = "{server}" + serviceBasePath;
            server.setUrl(serverUrl);
//            server.setVariables(serverVariables);

        } else if (port != null) {
            if (port.equals("443")) {
                AsyncApi25ServerVariable serverUrlVariable= server.createServerVariable();
//                ServerVariable serverUrlVariable = new ServerVariable();
                serverUrlVariable.setDefault("https://localhost");
                AsyncApi25ServerVariable portVariable= server.createServerVariable();
//                ServerVariable portVariable =  new ServerVariable();
                portVariable.setDefault("443");
                server.addVariable(SERVER,serverUrlVariable);
                server.addVariable(PORT,portVariable);
//                serverVariables.addServerVariable(SERVER, serverUrlVariable);
//                serverVariables.addServerVariable(PORT, portVariable);
                serverUrl = "{server}:{port}" + serviceBasePath;
                server.setUrl(serverUrl);
//                server.setVariables(serverVariables);
            } else {
                AsyncApi25ServerVariable serverUrlVariable= server.createServerVariable();
//                ServerVariable serverUrlVariable = new ServerVariable();
                serverUrlVariable.setDefault("http://localhost");
                AsyncApi25ServerVariable portVariable= server.createServerVariable();
//                ServerVariable portVariable =  new ServerVariable();
                portVariable.setDefault(port);
                server.addVariable(SERVER,serverUrlVariable);
                server.addVariable(PORT,portVariable);

//                serverVariables.addServerVariable(SERVER, serverUrlVariable);
//                serverVariables.addServerVariable(PORT, portVariable);
                serverUrl = "{server}:{port}" + serviceBasePath;
                server.setUrl(serverUrl);
//                server.setVariables(serverVariables);
            }
        }
    }

    // Extract host value for creating URL.
    private String extractHost(MappingConstructorExpressionNode bLangRecordLiteral) {
        String host = "";
        if (bLangRecordLiteral.fields() != null && !bLangRecordLiteral.fields().isEmpty()) {
            SeparatedNodeList<MappingFieldNode> recordFields = bLangRecordLiteral.fields();
            host = concatenateServerURL(host, recordFields);
        }
        if (!host.equals("")) {
           host = host.replaceAll("\"", "");
        }
        return host;
    }

    private String concatenateServerURL(String host, SeparatedNodeList<MappingFieldNode> recordFields) {

        for (MappingFieldNode filed: recordFields) {
            if (filed instanceof SpecificFieldNode) {
                Node fieldName = ((SpecificFieldNode) filed).fieldName();
                if (fieldName.toString().equals(Constants.ATTR_HOST)) {
                    if (((SpecificFieldNode) filed).valueExpr().isPresent()) {
                          host = ((SpecificFieldNode) filed).valueExpr().get().toString();
                    }
                }
            }
        }
        return host;
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
