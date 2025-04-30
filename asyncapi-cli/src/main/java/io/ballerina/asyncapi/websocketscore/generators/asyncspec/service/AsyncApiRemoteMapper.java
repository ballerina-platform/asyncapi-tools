/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.asyncspec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelItemImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25OperationImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Schema;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.model.BalAsyncApi25MessageImpl;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
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
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ANNOTATION_ATTR_DISPATCHER_VALUE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.CAMEL_CASE_PATTERN;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHER_CONFIG_ANNOTATION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ERROR;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FRAME_TYPE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FRAME_TYPE_CLOSE_NODE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FUNCTION_PARAMETERS_EXCEEDED;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FUNCTION_SIGNATURE_WRONG_TYPE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FUNCTION_WRONG_NAME;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.NO_SERVICE_CLASS;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_BINARY_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_CLOSE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_ERROR;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_OPEN;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_PING;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_PONG;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ON_TEXT_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.REMOTE_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.RETURN;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_PATH;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_PATH_FRAME_TYPE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_TYPE_BODY;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_VALUE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_VALUE_CLOSE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;

/**
 * This class will do resource mapping from ballerina to AsyncApi.
 */
public class AsyncApiRemoteMapper {
    private final AsyncApi25ChannelsImpl channelObject = new AsyncApi25ChannelsImpl();
    private final AsyncApi25ComponentsImpl components = new AsyncApi25ComponentsImpl();
    private final AsyncApiComponentMapper componentMapper = new AsyncApiComponentMapper(components);
    private final SemanticModel semanticModel;

    /**
     * Initializes a resource parser for asyncApi.
     */
    AsyncApiRemoteMapper(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    public static String createCustomRemoteFunctionName(String dispatchingValue) {
        StringBuilder builder = new StringBuilder();
        String[] words = dispatchingValue.split("[\\W_]+");
        for (String word : words) {
            word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1)
                    .toLowerCase(Locale.ENGLISH);
            builder.append(word);
        }
        return builder.toString();
    }

    public static boolean containsCloseFrameSchema(AsyncApi25Components components) {
        if (Objects.isNull(components) || Objects.isNull(components.getSchemas())) {
            return false;
        }
        return components.getSchemas().values().stream()
                .anyMatch(sch ->
                        (sch instanceof AsyncApi25Schema asyncApiSchema) && isCloseFrameSchema(asyncApiSchema));
    }

    public static boolean isCloseFrameSchema(AsyncApiSchema schema) {
        if (Objects.isNull(schema) || Objects.isNull(schema.getProperties()) ||
                !schema.getProperties().containsKey(FRAME_TYPE)) {
            return false;
        }
        if (!(schema.getProperties().get(FRAME_TYPE) instanceof AsyncApi25Schema asyncApi25Schema)) {
            return false;
        }
        return FRAME_TYPE_CLOSE_NODE.equals(asyncApi25Schema.getConst());
    }

    public static ObjectNode getWsCloseFrameExtension() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode closeFrameExtension = objectMapper.createObjectNode();
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_TYPE, X_BALLERINA_WS_CLOSE_FRAME_TYPE_BODY);
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_PATH, X_BALLERINA_WS_CLOSE_FRAME_PATH_FRAME_TYPE);
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_VALUE, X_BALLERINA_WS_CLOSE_FRAME_VALUE_CLOSE);
        return closeFrameExtension;
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
        AsyncApiParameterMapper asyncAPIParameterMapper = new AsyncApiParameterMapper(resource, apiDocs, components,
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
     * @param resource            functionDefinitionNode which contains resource function
     * @param classDefinitionNode classDefinitionNode which contains class definition
     * @param dispatcherValue     dispatcher key value
     * @param channelItem         AsyncAPI channel
     * @return AsyncAPI channel object
     */
    private AsyncApi25ChannelsImpl handleRemoteFunctions(FunctionDefinitionNode resource,
                                                         ClassDefinitionNode classDefinitionNode,
                                                         String dispatcherValue,
                                                         AsyncApi25ChannelItemImpl channelItem) {
        String path = unescapeIdentifier(generateRelativePath(resource));
        NodeList<Node> classMethodNodes = classDefinitionNode.members();
        AsyncApi25OperationImpl publishOperationItem = new AsyncApi25OperationImpl();
        AsyncApi25OperationImpl subscribeOperationItem = new AsyncApi25OperationImpl();
        BalAsyncApi25MessageImpl subscribeMessage = new BalAsyncApi25MessageImpl();
        BalAsyncApi25MessageImpl publishMessage = new BalAsyncApi25MessageImpl();
        AsyncApiResponseMapper responseMapper = new AsyncApiResponseMapper(resource.location(), componentMapper,
                semanticModel, components);
        Map<String, ReturnTypeDescriptorNode> onErrorReturnTypes = getReturnTypesFromOnErrorMethods(classMethodNodes);
        for (Node node : classMethodNodes) {
            if (node.kind().equals(SyntaxKind.OBJECT_METHOD_DEFINITION)) {
                FunctionDefinitionNode remoteFunctionNode = (FunctionDefinitionNode) node;
                if (remoteFunctionNode.functionSignature().parameters().size() <= 2) {
                    String functionName = remoteFunctionNode.functionName().toString().trim();
                    if (functionName.matches(CAMEL_CASE_PATTERN)) {
                        if (isRemoteFunctionNameValid(functionName)) {
                            String remoteRequestTypeName =
                                    getRequestTypeNameFromDispatcherConfigAnnotation(remoteFunctionNode)
                                            .orElse(unescapeIdentifier(functionName.substring(2)));
                            RequiredParameterNode requiredParameterNode =
                                    checkParameterContainsCustomType(remoteRequestTypeName, remoteFunctionNode);
                            //TODO: uncomment after handle onError is capable to have in the server side
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
                                    if (!functionName.endsWith(ERROR)) {
                                        ReturnTypeDescriptorNode customErrorReturnType = null;
                                        if (onErrorReturnTypes.containsKey(functionName + ERROR)) {
                                            customErrorReturnType = onErrorReturnTypes.get(functionName + ERROR);
                                        } else if (onErrorReturnTypes.containsKey(ON_ERROR)) {
                                            customErrorReturnType = onErrorReturnTypes.get(ON_ERROR);
                                        }
                                        if (Objects.nonNull(customErrorReturnType)) {
                                            responseMapper.createResponse(subscribeMessage, componentMessage,
                                                    customErrorReturnType.type(), null, FALSE, null);
                                        }
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
                                                remoteReturnType, returnDescription, FALSE, null);
                                    }
                                    //Add publish message related to remote method
                                    components.addMessage(remoteRequestTypeName, componentMessage);
                                } else {
                                    throw new NoSuchElementException(String.format(FUNCTION_SIGNATURE_WRONG_TYPE,
                                            remoteRequestTypeName, type.typeKind().getName()));
                                }
                            }
                            //TODO: Change because onError and onIdleTimeout in graphql over websocket
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
        String[] invalidRemoteFunctionNames = {ON_MESSAGE, ON_TEXT_MESSAGE, ON_BINARY_MESSAGE, ON_CLOSE, ON_OPEN,
                ON_PING, ON_PONG};
        return Arrays.stream(invalidRemoteFunctionNames).noneMatch(remoteFunctionName ->
                remoteFunctionName.equals(providedFunctionName));
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

    private Optional<String> getRequestTypeNameFromDispatcherConfigAnnotation(FunctionDefinitionNode node) {
        if (node.metadata().isEmpty()) {
            return Optional.empty();
        }
        for (AnnotationNode annotationNode : node.metadata().get().annotations()) {
            Optional<Symbol> annotationType = this.semanticModel.symbol(annotationNode);
            if (annotationType.isEmpty()) {
                continue;
            }
            if (!annotationType.get().getModule().flatMap(Symbol::getName).orElse("").equals(WEBSOCKET) ||
                    !annotationType.get().getName().orElse("").equals(DISPATCHER_CONFIG_ANNOTATION)) {
                continue;
            }
            if (annotationNode.annotValue().isEmpty()) {
                return Optional.empty();
            }
            MappingConstructorExpressionNode annotationValue = annotationNode.annotValue().get();
            for (Node field : annotationValue.fields()) {
                if (!SyntaxKind.SPECIFIC_FIELD.equals(field.kind())) {
                    continue;
                }
                String fieldName = ((SpecificFieldNode) field).fieldName().toString().strip();
                Optional<ExpressionNode> filedValue = ((SpecificFieldNode) field).valueExpr();
                if (!fieldName.equals(ANNOTATION_ATTR_DISPATCHER_VALUE) || filedValue.isEmpty()) {
                    continue;
                }
                return Optional.of(createCustomRemoteFunctionName(filedValue.get().toString()
                        .replaceAll("\"", "").strip()));
            }
        }
        return Optional.empty();
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

    private Map<String, ReturnTypeDescriptorNode> getReturnTypesFromOnErrorMethods(
            NodeList<Node> classMethodNodes) {
        return classMethodNodes.stream()
                .filter(node -> SyntaxKind.OBJECT_METHOD_DEFINITION.equals(node.kind()))
                .map(node -> (FunctionDefinitionNode) node)
                .filter(functionDefNode -> functionDefNode.functionName().toString().trim().endsWith(ERROR))
                .filter(functionDefNode -> functionDefNode.functionSignature().returnTypeDesc().isPresent())
                .collect(Collectors.toMap(
                        functionDefNode -> functionDefNode.functionName().toString().trim(),
                        functionDefNode -> functionDefNode.functionSignature().returnTypeDesc().get(),
                        (existing, replacement) -> existing));
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
