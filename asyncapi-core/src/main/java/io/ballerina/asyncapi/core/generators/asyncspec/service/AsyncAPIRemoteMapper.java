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

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelItemImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25OperationImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25MessageImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.CAMEL_CASE_PATTERN;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FUNCTION_DEFAULT_NAME_CONTAINS_ERROR;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FUNCTION_PARAMETERS_EXCEEDED;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FUNCTION_SIGNATURE_ABSENT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FUNCTION_SIGNATURE_WRONG_TYPE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FUNCTION_WRONG_NAME;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.NO_SERVICE_CLASS;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_BINARY_MESSAGE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_CLOSE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_ERROR;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_IDLE_TIME_OUT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_MESSAGE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_OPEN;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_PING;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_PONG;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ON_TEXT_MESSAGE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.REMOTE_DESCRIPTION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.RETURN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;


/**
 * This class will do resource mapping from ballerina to asyncApi.
 *
 * @since 2.0.0
 */
public class AsyncAPIRemoteMapper {
    private final SemanticModel semanticModel;
    private final AsyncApi25ChannelsImpl channelObject = new AsyncApi25ChannelsImpl();
    private final AsyncApi25ComponentsImpl components = new AsyncApi25ComponentsImpl();

    private final AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);
    private final List<AsyncAPIConverterDiagnostic> errors;

    /**
     * Initializes a resource parser for asyncApi.
     */
    AsyncAPIRemoteMapper(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.errors = new ArrayList<>();
    }

    public List<AsyncAPIConverterDiagnostic> getErrors() {
        return errors;
    }

    public AsyncApi25ComponentsImpl getComponents() {
        return components;
    }

    /**
     * This method will convert ballerina resource to asyncApi Paths objects.
     * <p>
     * //     * @param resources Resource list to be converted.
     *
     * @return map of string and asyncApi path objects.
     */
    public AsyncApi25ChannelsImpl getChannels(FunctionDefinitionNode resource,
                                              List<ClassDefinitionNode> classDefinitionNodes,
                                              String dispatcherValue) {

        AsyncApi25ChannelItemImpl channelItem = (AsyncApi25ChannelItemImpl) channelObject.createChannelItem();

        //call asyncAPIParameterMapper to map parameters
        Map<String, String> apiDocs = listAPIDocumentations(resource, channelItem);
        AsyncAPIParameterMapper asyncAPIParameterMapper = new AsyncAPIParameterMapper(resource, apiDocs, components,
                semanticModel);
        asyncAPIParameterMapper.getResourceInputs(channelItem);


        String serviceClassName = getServiceClassName(resource);
        if (!serviceClassName.isEmpty()) {
            for (ClassDefinitionNode node : classDefinitionNodes) {
                String testClassName1 = node.className().text();
                if (testClassName1.equals(serviceClassName)) {
                    return handleRemoteFunctions(resource, node, dispatcherValue, channelItem);
                }
            }

        } else {
            throw new NoSuchElementException(NO_SERVICE_CLASS);

        }

        return channelObject;
    }

    /**
     * Remote mapper when there have multiple remote methods.
     *
     * @param // * @param httpMethods   Sibling methods related to operation.
     */
    private AsyncApi25ChannelsImpl handleRemoteFunctions(FunctionDefinitionNode resource,
                                                         ClassDefinitionNode classDefinitionNode,
                                                         String dispatcherValue,
                                                         AsyncApi25ChannelItemImpl channelItem) {
        String path = ConverterCommonUtils.unescapeIdentifier(generateRelativePath(resource));
        NodeList<Node> classMethodNodes = classDefinitionNode.members();
        AsyncApi25OperationImpl publishOperationItem = new AsyncApi25OperationImpl();
        AsyncApi25OperationImpl subscribeOperationItem = new AsyncApi25OperationImpl();
        BalAsyncApi25MessageImpl subscribeMessage = new BalAsyncApi25MessageImpl();
        BalAsyncApi25MessageImpl publishMessage = new BalAsyncApi25MessageImpl();
        AsyncAPIResponseMapper responseMapper = new AsyncAPIResponseMapper(resource.location(), componentMapper,
                semanticModel, components);
        for (Node node : classMethodNodes) {
            if (node.kind().equals(SyntaxKind.OBJECT_METHOD_DEFINITION)) {
                FunctionDefinitionNode remoteFunctionNode = (FunctionDefinitionNode) node;
                if (remoteFunctionNode.functionSignature().parameters().size() <= 2) {
                    String functionName = remoteFunctionNode.functionName().toString().trim();
                    if (functionName.matches(CAMEL_CASE_PATTERN)) {
                        if (isRemoteFunctionNameValid(functionName)) {
                            //TODO : have to pass this through unescape identifier
                            String remoteRequestTypeName = ConverterCommonUtils.
                                    unescapeIdentifier(functionName.substring(2));
                            RequiredParameterNode requiredParameterNode =
                                    checkParameterContainsCustomType(remoteRequestTypeName, remoteFunctionNode);
                            if (requiredParameterNode != null) {
                                String paramName = requiredParameterNode.paramName().get().toString().trim();
                                Node parameterTypeNode = requiredParameterNode.typeName();
                                TypeSymbol remoteFunctionNameTypeSymbol = (TypeSymbol) semanticModel.
                                        symbol(parameterTypeNode).orElseThrow();
                                TypeReferenceTypeSymbol typeRef =
                                        (TypeReferenceTypeSymbol) remoteFunctionNameTypeSymbol;
                                TypeSymbol type = typeRef.typeDescriptor();

                                //check if there is a readOnly & record type also
                                if (type.typeKind().equals(TypeDescKind.RECORD) ||
                                        (type.typeKind().equals(TypeDescKind.INTERSECTION) &&
                                                componentMapper.excludeReadonlyIfPresent(type).typeKind().
                                                        equals(TypeDescKind.RECORD))) {

                                    FunctionSymbol remoteFunctionSymbol = (FunctionSymbol) semanticModel.
                                            symbol(remoteFunctionNode).get();
                                    Map<String, String> remoteDocs = getRemoteDocumentation(remoteFunctionSymbol);

                                    String paramDescription = null;
                                    if (remoteDocs.containsKey(paramName)) {
                                        paramDescription = remoteDocs.get(paramName);
                                        remoteDocs.remove(paramName);

                                    }
                                    BalAsyncApi25MessageImpl componentMessage = responseMapper.
                                            extractMessageSchemaReference(publishMessage, remoteRequestTypeName,
                                                    remoteFunctionNameTypeSymbol, dispatcherValue, paramDescription);
                                    if (remoteDocs.containsKey(REMOTE_DESCRIPTION)) {
                                        componentMessage.setDescription(remoteDocs.get(REMOTE_DESCRIPTION));
                                        remoteDocs.remove(REMOTE_DESCRIPTION);
                                    }
                                    Optional<ReturnTypeDescriptorNode> optionalRemoteReturnNode = remoteFunctionNode.
                                            functionSignature().returnTypeDesc();
                                    if (optionalRemoteReturnNode.isPresent()) {
                                        Node remoteReturnType = optionalRemoteReturnNode.get().type();
                                        String returnDescription = null;
                                        if (remoteDocs.containsKey(RETURN)) {
                                            returnDescription = remoteDocs.get(RETURN);
                                            remoteDocs.remove(RETURN);
                                        }
                                        //Call createResponse method to create the response
                                        responseMapper.createResponse(subscribeMessage, componentMessage,
                                                remoteReturnType, returnDescription,FALSE);
                                    }
                                    //Add publish message related to remote method
                                    components.addMessage(remoteRequestTypeName, componentMessage);
                                } else {

                                    throw new NoSuchElementException(String.format(FUNCTION_SIGNATURE_WRONG_TYPE,
                                            remoteRequestTypeName, type.typeKind().getName()));
                                }

                            } else {
                                throw new NoSuchElementException(FUNCTION_SIGNATURE_ABSENT);
                            }
                        } else {
                            throw new NoSuchElementException(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR);
                        }

                    } else {
                        throw new NoSuchElementException(FUNCTION_WRONG_NAME);
                    }
                } else {
                    throw new NoSuchElementException(FUNCTION_PARAMETERS_EXCEEDED);
                }
            }

        }
        if (publishMessage.getOneOf() != null) {
            if (publishMessage.getOneOf().size() == 1) {
                BalAsyncApi25MessageImpl publishOneMessage = new BalAsyncApi25MessageImpl();
                publishOneMessage.set$ref(((BalAsyncApi25MessageImpl) publishMessage.getOneOf().get(0)).get$ref());
                publishOperationItem.setMessage(publishOneMessage);

            } else {
                publishOperationItem.setMessage(publishMessage);
            }
            channelItem.setPublish(publishOperationItem);

        }
        if (subscribeMessage.getOneOf() != null) {
            if (subscribeMessage.getOneOf().size() == 1) {
                BalAsyncApi25MessageImpl subscribeOneMessage = new BalAsyncApi25MessageImpl();
                subscribeOneMessage.set$ref(((BalAsyncApi25MessageImpl) subscribeMessage.getOneOf().get(0)).get$ref());
                if (subscribeOneMessage.get$ref() == null) {
                    subscribeOneMessage.setPayload((subscribeMessage.getOneOf().get(0)).getPayload());
                }
                subscribeOperationItem.setMessage(subscribeOneMessage);

            } else {
                subscribeOperationItem.setMessage(subscribeMessage);
            }
            channelItem.setSubscribe(subscribeOperationItem);
        }
        channelObject.addItem(path, channelItem);

        return channelObject;
    }

    private Boolean isRemoteFunctionNameValid(String providedFunctionName) {
        String[] invalidRemoteFunctionNames = {ON_IDLE_TIME_OUT,
                ON_MESSAGE, ON_TEXT_MESSAGE,
                ON_BINARY_MESSAGE, ON_CLOSE,
                ON_OPEN, ON_ERROR, ON_PING, ON_PONG};
        return !(Arrays.stream(invalidRemoteFunctionNames).anyMatch(
                remoteFunctionName -> remoteFunctionName.equals(providedFunctionName)));
    }


    private Map<String, String> getRemoteDocumentation(FunctionSymbol remoteFunctionSymbol) {
        Map<String, String> apiDocs = new HashMap<>();
        Optional<Documentation> documentation = remoteFunctionSymbol.documentation();
        if (documentation.isPresent()) {
            apiDocs = documentation.get().parameterMap();
            if (documentation.get().returnDescription().isPresent()) {
                apiDocs.put(RETURN, documentation.get().returnDescription().get());
            }

            if (documentation.get().description().isPresent()) {
                Optional<String> description = (documentation.get().description());
                if (description.isPresent() && !description.get().trim().isEmpty()) {
                    apiDocs.put(REMOTE_DESCRIPTION, (description.get().trim()));
                }
            }
        }
        return apiDocs;
    }


    private RequiredParameterNode checkParameterContainsCustomType(String customTypeName,
                                                                   FunctionDefinitionNode remoteFunctionNode) {
        SeparatedNodeList<ParameterNode> remoteParameters = remoteFunctionNode.functionSignature().parameters();
        for (ParameterNode remoteParameterNode : remoteParameters) {


            if (remoteParameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) remoteParameterNode;

                Node parameterTypeNode = requiredParameterNode.typeName();
                if (parameterTypeNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) parameterTypeNode;
                    String simpleType = simpleNameReferenceNode.name().toString().trim();
                    if (simpleType.equals(customTypeName)) {
                        return requiredParameterNode;
                    }
                } else if (parameterTypeNode.kind() == QUALIFIED_NAME_REFERENCE) {
                    QualifiedNameReferenceNode qualifiedNameReferenceNode =
                            (QualifiedNameReferenceNode) parameterTypeNode;
                    String identifier = qualifiedNameReferenceNode.identifier().text();
                    if (identifier.equals(customTypeName)) {
                        return requiredParameterNode;
                    }

                }
            }

        }
        return null;
    }

    private String getServiceClassName(FunctionDefinitionNode resource) {
        String serviceClassName = "";
        FunctionBodyNode functionBodyNode = resource.functionBody();
        ChildNodeList childNodeList = functionBodyNode.children();
        for (Node node : childNodeList) {
            if (node instanceof ReturnStatementNode) {
                ReturnStatementNode returnStatementNode = (ReturnStatementNode) node;
                Optional<ExpressionNode> expression = returnStatementNode.expression();
                if (expression.get() instanceof ExplicitNewExpressionNode) {
                    ExplicitNewExpressionNode explicitNewExpressionNode = (ExplicitNewExpressionNode) expression.get();
                    TypeDescriptorNode typeDescriptorNode = explicitNewExpressionNode.typeDescriptor();
                    serviceClassName = typeDescriptorNode.toString().trim();
                }
            }
        }
        return serviceClassName;
    }

    /**
     * Filter the API documentations from resource function node.
     */
    private Map<String, String> listAPIDocumentations(FunctionDefinitionNode resource,
                                                      AsyncApi25ChannelItemImpl channelItem) {

        Map<String, String> apiDocs = new HashMap<>();
        if (resource.metadata().isPresent()) {
            Optional<Symbol> resourceSymbol = semanticModel.symbol(resource);
            if (resourceSymbol.isPresent()) {
                Symbol symbol = resourceSymbol.get();
                Optional<Documentation> documentation = ((Documentable) symbol).documentation();
                if (documentation.isPresent()) {
                    Documentation documentation1 = documentation.get();
                    Optional<String> description = documentation1.description();
                    if (description.isPresent()) {
                        String resourceFunctionAPI = description.get().trim();
                        apiDocs = documentation1.parameterMap();
                        channelItem.setDescription(resourceFunctionAPI);
                    }
                }
            }
        }
        return apiDocs;
    }

    /**
     * Gets the http methods of a resource.
     *
     * @param resource The ballerina resource.
     * @return A list of http methods.
     */
    private String generateRelativePath(FunctionDefinitionNode resource) {

        StringBuilder relativePath = new StringBuilder();
        relativePath.append("/");
        if (!resource.relativeResourcePath().isEmpty()) {
            for (Node node : resource.relativeResourcePath()) {
                if (node instanceof ResourcePathParameterNode) {
                    ResourcePathParameterNode pathNode = (ResourcePathParameterNode) node;
                    relativePath.append("{");
                    relativePath.append(pathNode.paramName().get());
                    relativePath.append("}");
                } else if ((resource.relativeResourcePath().size() == 1) && (node.toString().trim().equals("."))) {
                    return relativePath.toString().trim();
                } else {
                    relativePath.append(node.toString().trim());
                }
            }
        }
        return relativePath.toString().trim();
    }

}
