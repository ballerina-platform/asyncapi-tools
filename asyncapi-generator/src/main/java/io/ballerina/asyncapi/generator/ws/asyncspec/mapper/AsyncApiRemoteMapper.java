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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ChannelImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ChannelsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ReferenceImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Schema;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30MessageImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ANNOTATION_ATTR_DISPATCHER_VALUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CAMEL_CASE_PATTERN;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CHANNELS_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_CONFIG_ANNOTATION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ERROR;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FRAME_TYPE_CLOSE_NODE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FUNCTION_PARAMETERS_EXCEEDED;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FUNCTION_SIGNATURE_WRONG_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FUNCTION_WRONG_NAME;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.MESSAGE_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.NO_SERVICE_CLASS;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_BINARY_MESSAGE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_CLOSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_ERROR;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_MESSAGE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_OPEN;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_PING;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_PONG;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ON_TEXT_MESSAGE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.REMOTE_DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.RETURN;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.UNSUPPORTED_PARAMETER_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_PATH;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_PATH_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_TYPE_BODY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_VALUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME_VALUE_CLOSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.getServiceClassName;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;

/**
 * Maps Ballerina remote functions to AsyncAPI 3.0 channels, operations, and messages.
 * Corresponds to the legacy {@code AsyncApiRemoteMapper}.
 */
public class AsyncApiRemoteMapper {

    private AsyncApi30ChannelsImpl channelObject;
    private AsyncApi30ComponentsImpl components;
    private AsyncApi30OperationsImpl operations;
    private AsyncApiComponentMapper componentMapper;
    private final SemanticModel semanticModel;

    /**
     * Initializes a remote mapper for AsyncAPI 3.0.
     *
     * @param semanticModel Ballerina semantic model
     * @param asyncAPI      the AsyncAPI 3.0 document used to create properly-parented child nodes
     */
    AsyncApiRemoteMapper(SemanticModel semanticModel, AsyncApi30Document asyncAPI) {
        this.semanticModel = semanticModel;
        this.channelObject = (AsyncApi30ChannelsImpl) asyncAPI.createChannels();
        this.components    = (AsyncApi30ComponentsImpl) asyncAPI.createComponents();
        this.operations    = (AsyncApi30OperationsImpl) asyncAPI.createOperations();
        this.componentMapper = new AsyncApiComponentMapper(this.components);
    }

    /**
     * Converts camel-case dispatcher value to a PascalCase type name.
     *
     * @param dispatchingValue dispatcher value string
     * @return PascalCase type name
     */
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

    /**
     * Returns true if the components contain a close-frame schema.
     *
     * @param components AsyncAPI 3.0 components to check
     * @return true if a close-frame schema is present
     */
    public static boolean containsCloseFrameSchema(AsyncApi30ComponentsImpl components) {
        if (Objects.isNull(components) || Objects.isNull(components.getSchemas())) {
            return false;
        }
        return components.getSchemas().values().stream()
                .anyMatch(sch -> (sch instanceof AsyncApi30Schema asyncApiSchema)
                        && isCloseFrameSchema(asyncApiSchema));
    }

    /**
     * Returns true if the given schema is a close-frame schema.
     *
     * @param schema the schema to check
     * @return true if this is a close-frame schema
     */
    public static boolean isCloseFrameSchema(AsyncApiSchema schema) {
        if (Objects.isNull(schema) || Objects.isNull(schema.getProperties())
                || !schema.getProperties().containsKey(FRAME_TYPE)) {
            return false;
        }
        if (!(schema.getProperties().get(FRAME_TYPE) instanceof AsyncApi30Schema asyncApi30Schema)) {
            return false;
        }
        return FRAME_TYPE_CLOSE_NODE.equals(asyncApi30Schema.getConst());
    }

    /**
     * Returns the close-frame extension object node.
     *
     * @return ObjectNode with close-frame extension fields
     */
    public static ObjectNode getWsCloseFrameExtension() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode closeFrameExtension = objectMapper.createObjectNode();
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_TYPE, X_BALLERINA_WS_CLOSE_FRAME_TYPE_BODY);
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_PATH, X_BALLERINA_WS_CLOSE_FRAME_PATH_FRAME_TYPE);
        closeFrameExtension.put(X_BALLERINA_WS_CLOSE_FRAME_VALUE, X_BALLERINA_WS_CLOSE_FRAME_VALUE_CLOSE);
        return closeFrameExtension;
    }

    /**
     * Returns the AsyncAPI 3.0 components built during mapping.
     *
     * @return components
     */
    public AsyncApi30ComponentsImpl getComponents() {
        return components;
    }

    /**
     * Returns the AsyncAPI 3.0 operations map built during mapping.
     *
     * @return operations
     */
    public AsyncApi30OperationsImpl getOperations() {
        return operations;
    }

    /**
     * Converts a Ballerina resource function and its associated service class into
     * AsyncAPI 3.0 channels, messages, and operations.
     *
     * @param resource             resource function definition node
     * @param classDefinitionNodes list of class definition nodes to search for service class
     * @param dispatcherValue      dispatcher key value
     * @return populated AsyncAPI 3.0 channels map
     */
    public AsyncApi30ChannelsImpl getChannels(FunctionDefinitionNode resource,
                                              List<ClassDefinitionNode> classDefinitionNodes,
                                              String dispatcherValue) {
        AsyncApi30ChannelImpl channelItem = (AsyncApi30ChannelImpl) channelObject.createChannel();
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
     * Builds channel messages and operations for all remote methods in the service class.
     */
    private AsyncApi30ChannelsImpl handleRemoteFunctions(FunctionDefinitionNode resource,
                                                         ClassDefinitionNode classDefinitionNode,
                                                         String dispatcherValue,
                                                         AsyncApi30ChannelImpl channelItem) {
        String path = unescapeIdentifier(generateRelativePath(resource));
        channelItem.setAddress(path);
        String channelKey = channelKeyFromPath(path);
        NodeList<Node> classMethodNodes = classDefinitionNode.members();

        BalAsyncApi30MessageImpl publishMessage = new BalAsyncApi30MessageImpl();
        BalAsyncApi30MessageImpl subscribeMessage = new BalAsyncApi30MessageImpl();

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
                            Optional<NodeList<AnnotationNode>> annotationNodes =
                                    remoteFunctionNode.metadata().map(MetadataNode::annotations);
                            Optional<String> dispatcherTypeOverride =
                                    annotationNodes.flatMap(this::getDispatcherTypeFromAnnotation);
                            FunctionSymbol remoteFunctionSymbol = (FunctionSymbol) semanticModel.
                                    symbol(remoteFunctionNode).get();
                            Map<String, String> remoteDocs = getRemoteDocumentation(remoteFunctionSymbol);

                            RequiredParameterNode requiredParameterNode = findMessageParameter(remoteFunctionNode);
                            String remoteRequestTypeName;
                            BalAsyncApi30MessageImpl componentMessage;
                            if (requiredParameterNode == null) {
                                if (!remoteFunctionNode.functionSignature().parameters().isEmpty()) {
                                    // Has a parameter, but its type is none of the shapes findMessageParameter
                                    // recognizes (e.g. an array or map) - fail loudly rather than silently
                                    // dropping the function, per #7669.
                                    throw new NoSuchElementException(
                                            String.format(UNSUPPORTED_PARAMETER_TYPE, functionName));
                                }
                                // No parameters at all: a valid dispatch target with no request payload
                                // beyond the dispatcher key value itself (e.g. onSubscription() in #7669).
                                remoteRequestTypeName = dispatcherTypeOverride.orElseGet(() ->
                                        functionName.substring(2));
                                BalAsyncApi30SchemaImpl emptySchema = getAsyncApiSchema(AsyncAPIType.OBJECT
                                        .toString());
                                componentMessage = responseMapper.extractInlineMessageSchema(
                                        publishMessage, remoteRequestTypeName, emptySchema, null);
                            } else {
                                String paramName = requiredParameterNode.paramName().get().toString().trim();
                                Node parameterTypeNode = requiredParameterNode.typeName();
                                String paramDescription = null;
                                if (remoteDocs.containsKey(paramName)) {
                                    paramDescription = remoteDocs.get(paramName);
                                    remoteDocs.remove(paramName);
                                }
                                if (isPrimitiveTypeDesc(parameterTypeNode.kind())) {
                                    // A primitive-typed parameter (e.g. `string`) has no named type to
                                    // derive a message name from, unlike a record/class reference - fall
                                    // back to the function name, same as the zero-parameter case above.
                                    remoteRequestTypeName = dispatcherTypeOverride.orElseGet(() ->
                                            functionName.substring(2));
                                    BalAsyncApi30SchemaImpl primitiveSchema =
                                            getAsyncApiSchema(parameterTypeNode.kind());
                                    componentMessage = responseMapper.extractInlineMessageSchema(
                                            publishMessage, remoteRequestTypeName, primitiveSchema,
                                            paramDescription);
                                } else {
                                    remoteRequestTypeName = dispatcherTypeOverride.orElseGet(() ->
                                            unescapeIdentifier(resolveParameterTypeName(requiredParameterNode)));
                                    TypeSymbol remoteFunctionNameTypeSymbol = (TypeSymbol) semanticModel.
                                            symbol(parameterTypeNode).orElseThrow();
                                    TypeReferenceTypeSymbol typeRef =
                                            (TypeReferenceTypeSymbol) remoteFunctionNameTypeSymbol;
                                    TypeSymbol type = typeRef.typeDescriptor();
                                    if (type.typeKind().equals(TypeDescKind.RECORD)
                                            || (type.typeKind().equals(TypeDescKind.INTERSECTION)
                                            && componentMapper.excludeReadonlyIfPresent(type).typeKind()
                                            .equals(TypeDescKind.RECORD))) {
                                        componentMessage = responseMapper.extractMessageSchemaReference(
                                                publishMessage, remoteRequestTypeName, remoteFunctionNameTypeSymbol,
                                                dispatcherValue, paramDescription);
                                    } else {
                                        throw new NoSuchElementException(String.format(
                                                FUNCTION_SIGNATURE_WRONG_TYPE, remoteRequestTypeName,
                                                type.typeKind().getName()));
                                    }
                                }
                            }

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
                                responseMapper.createResponse(subscribeMessage, componentMessage,
                                        remoteReturnType, returnDescription, FALSE, null);
                            }
                            components.addMessage(remoteRequestTypeName, componentMessage);

                            // Register request message on channel and create send operation
                            BalAsyncApi30MessageImpl channelMsgRef = new BalAsyncApi30MessageImpl();
                            channelMsgRef.setParent(channelItem);
                            channelMsgRef.set$ref(MESSAGE_REFERENCE + remoteRequestTypeName);
                            channelItem.addMessage(remoteRequestTypeName, channelMsgRef);

                            AsyncApi30OperationImpl sendOp =
                                    (AsyncApi30OperationImpl) operations.createOperation();
                            sendOp.setAction("send");
                            AsyncApi30ReferenceImpl channelRef =
                                    (AsyncApi30ReferenceImpl) sendOp.createReference();
                            channelRef.set$ref(CHANNELS_REFERENCE + channelKey);
                            sendOp.setChannel(channelRef);
                            AsyncApi30ReferenceImpl sendMsgRef =
                                    (AsyncApi30ReferenceImpl) sendOp.createReference();
                            sendMsgRef.set$ref(CHANNELS_REFERENCE + channelKey
                                    + "/messages/" + remoteRequestTypeName);
                            sendOp.addMessage(sendMsgRef);
                            operations.addItem("send" + remoteRequestTypeName, sendOp);
                        }
                    } else {
                        throw new NoSuchElementException(FUNCTION_WRONG_NAME);
                    }
                } else {
                    throw new NoSuchElementException(FUNCTION_PARAMETERS_EXCEEDED);
                }
            }
        }

        // Build receive operations from subscribeMessage oneOf entries
        Map<String, io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30MessageImpl>
                registeredMessages = new HashMap<>();
        if (components.getMessages() != null) {
            components.getMessages().forEach((name, msg) -> {
                if (msg instanceof BalAsyncApi30MessageImpl bal30Msg) {
                    registeredMessages.put(name, bal30Msg);
                }
            });
        }

        if (subscribeMessage.getExtensions() != null
                && subscribeMessage.getExtensions().get("oneOf") != null) {
            com.fasterxml.jackson.databind.JsonNode oneOfNode = subscribeMessage.getExtensions().get("oneOf");
            if (oneOfNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode entry : oneOfNode) {
                    String responseRef = entry.has("$ref") ? entry.get("$ref").asText() : null;
                    if (responseRef != null) {
                        // Extract the message name from the $ref (last segment)
                        String[] refParts = responseRef.split("/");
                        String responseTypeName = refParts[refParts.length - 1];

                        BalAsyncApi30MessageImpl respChannelMsgRef = new BalAsyncApi30MessageImpl();
                        respChannelMsgRef.setParent(channelItem);
                        respChannelMsgRef.set$ref(MESSAGE_REFERENCE + responseTypeName);
                        channelItem.addMessage(responseTypeName, respChannelMsgRef);

                        AsyncApi30OperationImpl receiveOp =
                                (AsyncApi30OperationImpl) operations.createOperation();
                        receiveOp.setAction("receive");
                        AsyncApi30ReferenceImpl channelRef =
                                (AsyncApi30ReferenceImpl) receiveOp.createReference();
                        channelRef.set$ref(CHANNELS_REFERENCE + channelKey);
                        receiveOp.setChannel(channelRef);
                        AsyncApi30ReferenceImpl respMsgRef =
                                (AsyncApi30ReferenceImpl) receiveOp.createReference();
                        respMsgRef.set$ref(CHANNELS_REFERENCE + channelKey
                                + "/messages/" + responseTypeName);
                        receiveOp.addMessage(respMsgRef);
                        operations.addItem("receive" + responseTypeName, receiveOp);
                    }
                }
            }
        }

        channelObject.addItem(channelKey, channelItem);
        return channelObject;
    }

    /**
     * Derives a channel map key from a path by stripping the leading slash and replacing
     * inner slashes with underscores so the key never requires JSON Pointer encoding.
     *
     * @param path the channel address path (e.g. {@code "/"} or {@code "/users/profile"})
     * @return a key safe for use in JSON Pointer {@code $ref}s (e.g. {@code "root"} or
     *         {@code "users_profile"})
     */
    private static String channelKeyFromPath(String path) {
        String key = path.startsWith("/") ? path.substring(1) : path;
        key = key.replace("/", "_");
        return key.isEmpty() ? "root" : key;
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
                Optional<String> description = documentation.get().description();
                if (description.isPresent() && !description.get().trim().isEmpty()) {
                    apiDocs.put(REMOTE_DESCRIPTION, description.get().trim());
                }
            }
        }
        return apiDocs;
    }

    /**
     * Finds the remote function's data parameter - the first required parameter whose type is
     * either a named reference to a custom (record or class) type, or a primitive scalar type
     * (see {@link #isPrimitiveTypeDesc}). The function's own name plays no part in this; only the
     * parameter's actual declared type determines what gets generated.
     *
     * <p>Returning {@code null} here does not by itself mean the function is skipped - callers
     * distinguish "no parameters at all" (a valid, payload-less remote function - see #7669) from
     * "has a parameter, but its type isn't one of the shapes above" (genuinely unsupported, and
     * reported rather than silently dropped).
     *
     * @param remoteFunctionNode the remote function definition node
     * @return the resolved parameter node, or {@code null} if there are no required parameters,
     *         or none of them reference a named custom type or a primitive scalar type
     */
    private RequiredParameterNode findMessageParameter(FunctionDefinitionNode remoteFunctionNode) {
        SeparatedNodeList<ParameterNode> remoteParameters = remoteFunctionNode.functionSignature().parameters();
        for (ParameterNode remoteParameterNode : remoteParameters) {
            if (remoteParameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) remoteParameterNode;
                Node parameterTypeNode = requiredParameterNode.typeName();
                if (parameterTypeNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE
                        || parameterTypeNode.kind() == QUALIFIED_NAME_REFERENCE
                        || isPrimitiveTypeDesc(parameterTypeNode.kind())) {
                    return requiredParameterNode;
                }
            }
        }
        return null;
    }

    /**
     * Reads the actual type name off a parameter resolved by {@link #findMessageParameter} whose
     * type is a named record/class reference (not a primitive - see {@link #isPrimitiveTypeDesc}).
     *
     * @param requiredParameterNode the resolved parameter node
     * @return the parameter's declared type name
     */
    private String resolveParameterTypeName(RequiredParameterNode requiredParameterNode) {
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (parameterTypeNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            return ((SimpleNameReferenceNode) parameterTypeNode).name().toString().trim();
        }
        return ((QualifiedNameReferenceNode) parameterTypeNode).identifier().text();
    }

    /**
     * True for the built-in scalar type-descriptor kinds that {@link ConverterCommonUtils#
     * getAsyncApiSchema(SyntaxKind)} can turn directly into an inline AsyncAPI schema - the same
     * set already supported on the return-type side (see {@code AsyncApiResponseMapper#
     * createResponse}). Deliberately narrower than every kind that method accepts (e.g. excludes
     * {@code ARRAY_TYPE_DESC}/{@code MAP_TYPE_DESC}): this only needs to cover what a remote
     * function *parameter* can validly be shaped as for #7669, not every return-type shape.
     *
     * @param kind the parameter's type-descriptor syntax kind
     * @return {@code true} if it's a supported primitive scalar type
     */
    private static boolean isPrimitiveTypeDesc(SyntaxKind kind) {
        return switch (kind) {
            case SyntaxKind.STRING_TYPE_DESC, SyntaxKind.INT_TYPE_DESC, SyntaxKind.BOOLEAN_TYPE_DESC,
                    SyntaxKind.DECIMAL_TYPE_DESC, SyntaxKind.FLOAT_TYPE_DESC -> true;
            default -> false;
        };
    }

    private Optional<String> getDispatcherTypeFromAnnotation(NodeList<AnnotationNode> annotationNodes) {
        if (Objects.isNull(annotationNodes) || annotationNodes.isEmpty()) {
            return Optional.empty();
        }
        for (AnnotationNode annotationNode : annotationNodes) {
            Optional<Symbol> annotationType = this.semanticModel.symbol(annotationNode);
            if (annotationType.isEmpty()) {
                continue;
            }
            if (!annotationType.get().getModule().flatMap(Symbol::getName).orElse("").equals(WEBSOCKET)
                    || !annotationType.get().getName().orElse("").equals(DISPATCHER_CONFIG_ANNOTATION)) {
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
                Optional<ExpressionNode> fieldValue = ((SpecificFieldNode) field).valueExpr();
                if (!fieldName.equals(ANNOTATION_ATTR_DISPATCHER_VALUE) || fieldValue.isEmpty()) {
                    continue;
                }
                return Optional.of(createCustomRemoteFunctionName(fieldValue.get().toString()
                        .replaceAll("\"", "").strip()));
            }
        }
        return Optional.empty();
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
     * Filters the API documentations from a resource function node.
     *
     * @param resource    the resource function
     * @param channelItem the channel to set description on
     * @return map of parameter name to doc description
     */
    private Map<String, String> listAPIDocumentations(FunctionDefinitionNode resource,
                                                      AsyncApi30ChannelImpl channelItem) {
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
     * Generates the relative path for the channel from the resource function's path segments.
     *
     * @param resource the resource function
     * @return the relative path string
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
