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
package io.ballerina.asyncapi.generator.ws.asyncspec.mapper;

import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ServerImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ServerVariable;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils;
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

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ATTR_HOST;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.PORT;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SECURE_SOCKET;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SERVER;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SERVER_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WS;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WSS_LOCALHOST;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WSS_PREFIX;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WS_LOCALHOST;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WS_PREFIX;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WS_PROTOCOL_VERSION;

/**
 * Maps Ballerina listener/endpoint definitions to AsyncAPI 3.0.0 server objects.
 * Corresponds to the legacy {@code AsyncApiEndpointMapper}.
 */
public class AsyncApiEndpointMapper {

    /** Shared singleton instance used across the conversion pipeline. */
    public static final AsyncApiEndpointMapper ENDPOINT_MAPPER = new AsyncApiEndpointMapper();

    private AsyncApiEndpointMapper() {
    }

    /**
     * Populates the servers section of the given AsyncAPI 3.0.0 document
     * from listener declarations bound to the service.
     *
     * @param asyncAPI  document to mutate
     * @param endpoints all listener declarations in the module
     * @param service   service node with bound endpoint expressions
     */
    public void getServers(AsyncApi30Document asyncAPI,
                           List<ListenerDeclarationNode> endpoints,
                           ServiceDeclarationNode service) {
        AsyncApiServers asyncApiServers = asyncAPI.createServers();
        List<AsyncApi30ServerImpl> servers =
                extractServerForExpressionNode(service.expressions(), service, asyncApiServers);
        if (!endpoints.isEmpty()) {
            for (ListenerDeclarationNode ep : endpoints) {
                SeparatedNodeList<ExpressionNode> exprNodes = service.expressions();
                for (ExpressionNode node : exprNodes) {
                    if (node.toString().trim().equals(ep.variableName().text().trim())) {
                        String serviceBasePath = getServiceBasePath(service);
                        AsyncApi30ServerImpl server = extractServer(ep, serviceBasePath, asyncApiServers);
                        servers.add(server);
                    }
                }
            }
        }
        if (servers.size() > 1) {
            AsyncApi30ServerImpl mainServer = addEnumValues(servers);
            asyncApiServers.addItem(SERVER_TYPE, mainServer);
        } else if (!servers.isEmpty()) {
            asyncApiServers.addItem(SERVER_TYPE, servers.get(0));
        }
        asyncAPI.setServers(asyncApiServers);
    }

    private AsyncApi30ServerImpl addEnumValues(List<AsyncApi30ServerImpl> servers) {
        AsyncApi30ServerImpl mainServer = servers.get(0);
        List<AsyncApi30ServerImpl> rotated = new ArrayList<>(servers);
        Map<String, ServerVariable> mainVariable = mainServer.getVariables();
        ServerVariable hostVariable = mainVariable.get(SERVER);
        ServerVariable portVariable = mainVariable.get(PORT);
        if (servers.size() > 1) {
            Collections.rotate(rotated, servers.size() - 1);
            for (AsyncApi30ServerImpl server : rotated) {
                Map<String, ServerVariable> variables = server.getVariables();
                setServerVariables(hostVariable, variables, SERVER);
                setServerVariables(portVariable, variables, PORT);
            }
        }
        return mainServer;
    }

    /**
     * Sets a server variable as an enum by appending the value from another server's variable.
     *
     * @param variable     target server variable to accumulate enum values into
     * @param variables    all server variables from the source server
     * @param variableName name of the variable to read from the source
     */
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
     * Extracts a server from a named listener declaration node.
     */
    private AsyncApi30ServerImpl extractServer(ListenerDeclarationNode ep, String serviceBasePath,
                                              AsyncApiServers asyncApiServers) {
        Optional<ParenthesizedArgList> list;
        if (ep.initializer().kind() == SyntaxKind.CHECK_EXPRESSION) {
            ExpressionNode expression = ((CheckExpressionNode) ep.initializer()).expression();
            list = extractListenerNodeType(expression);
        } else {
            list = extractListenerNodeType(ep.initializer());
        }
        return generateServer(serviceBasePath, list, asyncApiServers);
    }

    private Optional<ParenthesizedArgList> extractListenerNodeType(Node expression) {
        Optional<ParenthesizedArgList> list = Optional.empty();
        if (expression.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
            ExplicitNewExpressionNode bTypeExplicit = (ExplicitNewExpressionNode) expression;
            list = Optional.ofNullable(bTypeExplicit.parenthesizedArgList());
        } else if (expression.kind() == SyntaxKind.IMPLICIT_NEW_EXPRESSION) {
            ImplicitNewExpressionNode bTypeInit = (ImplicitNewExpressionNode) expression;
            list = bTypeInit.parenthesizedArgList();
        }
        return list;
    }

    /**
     * Extracts servers from inline {@code new} expressions in the service's expression list.
     */
    private List<AsyncApi30ServerImpl> extractServerForExpressionNode(
            SeparatedNodeList<ExpressionNode> expressions,
            ServiceDeclarationNode service,
            AsyncApiServers asyncApiServers) {
        String serviceBasePath = getServiceBasePath(service);
        List<AsyncApi30ServerImpl> servers = new ArrayList<>();
        for (ExpressionNode expressionNode : expressions) {
            if (expressionNode.kind().equals(SyntaxKind.EXPLICIT_NEW_EXPRESSION)) {
                ExplicitNewExpressionNode explicit = (ExplicitNewExpressionNode) expressionNode;
                Optional<ParenthesizedArgList> list = Optional.ofNullable(explicit.parenthesizedArgList());
                AsyncApi30ServerImpl server = generateServer(serviceBasePath, list, asyncApiServers);
                servers.add(server);
            }
        }
        return servers;
    }

    /**
     * Builds an {@link AsyncApi30ServerImpl} from the listener argument list and service base path.
     */
    private AsyncApi30ServerImpl generateServer(String serviceBasePath,
                                                Optional<ParenthesizedArgList> list,
                                                AsyncApiServers asyncApiServers) {
        String port = null;
        String host = null;
        String secured = FALSE;

        AsyncApi30ServerImpl server = (AsyncApi30ServerImpl) asyncApiServers.createServer();

        if (list.isPresent()) {
            SeparatedNodeList<FunctionArgumentNode> arg = list.get().arguments();
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
        setServerVariableValues(serviceBasePath, port, host, secured, server);
        return server;
    }

    /**
     * Sets host, port and pathname fields on the server using AsyncAPI 3.0 host/pathname split.
     */
    private void setServerVariableValues(String serviceBasePath, String port, String host,
                                         String secured, AsyncApi30ServerImpl server) {
        if (host != null && port != null) {
            AsyncApi30ServerVariable serverUrlVariable = server.createServerVariable();
            if (secured.equals(TRUE)) {
                serverUrlVariable.setDefault(WSS_PREFIX + host);
            } else {
                serverUrlVariable.setDefault(WS_PREFIX + host);
            }
            AsyncApi30ServerVariable portVariable = server.createServerVariable();
            portVariable.setDefault(port);
            server.addVariable(SERVER, serverUrlVariable);
            server.addVariable(PORT, portVariable);
            server.setHost("{server}:{port}");
            server.setPathname(serviceBasePath);
        } else if (host != null) {
            AsyncApi30ServerVariable serverUrlVariable = server.createServerVariable();
            serverUrlVariable.setDefault(host);
            server.addVariable(SERVER, serverUrlVariable);
            server.setHost("{server}");
            server.setPathname(serviceBasePath);
        } else if (port != null) {
            AsyncApi30ServerVariable serverUrlVariable = server.createServerVariable();
            if (secured.equals(TRUE)) {
                serverUrlVariable.setDefault(WSS_LOCALHOST);
            } else {
                serverUrlVariable.setDefault(WS_LOCALHOST);
            }
            AsyncApi30ServerVariable portVariable = server.createServerVariable();
            portVariable.setDefault(port);
            server.addVariable(SERVER, serverUrlVariable);
            server.addVariable(PORT, portVariable);
            server.setHost("{server}:{port}");
            server.setPathname(serviceBasePath);
        }
    }

    /**
     * Extracts the host value and checks whether a secure socket is configured.
     */
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
     * Returns the base path of a service by reading its absolute resource path nodes.
     *
     * @param serviceDefinition the service declaration node
     * @return service base path string (e.g. {@code "/chat"})
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
