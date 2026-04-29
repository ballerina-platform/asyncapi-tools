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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30MessageImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ReadonlyTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.MapTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeParamsNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.tools.diagnostics.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CUSTOM_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.MESSAGE_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.NO_TYPE_IN_STREAM;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ONEOF;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.PAYLOAD;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.PREDEFINED_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.REF;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SERVER_STREAMING_TYPE_NODE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SIMPLE_RPC;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.UNION_STREAMING_SIMPLE_RPC_ERROR;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_REQUIRED;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.callObjectMapper;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_FIELD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;

/**
 * Processes mapping responses between Ballerina and AsyncApiSpec.
 * Corresponds to the legacy {@code AsyncApiResponseMapper}.
 */
public class AsyncApiResponseMapper {

    private static final Set<String> closeFrameTypes = Set.of(PREDEFINED_CLOSE_FRAME_TYPE, CUSTOM_CLOSE_FRAME_TYPE);
    private final Location location;
    private final SemanticModel semanticModel;
    private final AsyncApi30ComponentsImpl components;
    private final AsyncApiComponentMapper componentMapper;
    private final List<AsyncApiConverterDiagnostic> errors = new ArrayList<>();

    /**
     * Creates a new response mapper.
     *
     * @param location        source location for diagnostics
     * @param componentMapper component schema mapper
     * @param semanticModel   Ballerina semantic model
     * @param components      AsyncAPI 3.0 components
     */
    public AsyncApiResponseMapper(Location location, AsyncApiComponentMapper componentMapper,
                                  SemanticModel semanticModel, AsyncApi30ComponentsImpl components) {
        this.location = location;
        this.semanticModel = semanticModel;
        this.componentMapper = componentMapper;
        this.components = components;
    }

    /**
     * Returns true if the given type symbol represents a WebSocket close-frame record type.
     *
     * @param typeSymbol the type symbol to check
     * @return true if it is a close-frame record type
     */
    public static boolean isCloseFrameRecordType(TypeSymbol typeSymbol) {
        String moduleName = typeSymbol.getModule().flatMap(Symbol::getName).orElse("");
        String symbolName = typeSymbol.getName().orElse("");
        if (WEBSOCKET.equals(moduleName) && closeFrameTypes.contains(symbolName)) {
            return true;
        }

        TypeDescKind typeDescKind = typeSymbol.typeKind();
        if (TypeDescKind.TYPE_REFERENCE.equals(typeDescKind)) {
            return isCloseFrameRecordType(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        } else if (TypeDescKind.RECORD.equals(typeDescKind)) {
            RecordTypeSymbol bRecordTypeSymbol = (RecordTypeSymbol) typeSymbol;
            if (bRecordTypeSymbol.fieldDescriptors().containsKey(CLOSE_FRAME_TYPE)) {
                return isCloseFrameRecordType(bRecordTypeSymbol.fieldDescriptors().get(CLOSE_FRAME_TYPE)
                        .typeDescriptor());
            }
        } else if (TypeDescKind.INTERSECTION.equals(typeDescKind)) {
            return isCloseFrameRecordType(((IntersectionTypeSymbol) typeSymbol).effectiveTypeDescriptor());
        }
        return false;
    }

    /**
     * Maps a Ballerina remote function return type to AsyncAPI subscribe and component messages.
     *
     * @param subscribeMessage  channel-level message accumulating oneOf entries
     * @param componentMessage  component-level message accumulating x-response extensions
     * @param remoteReturnType  return type syntax node
     * @param returnDescription doc comment for the return type
     * @param isOptional        {@code "true"} if the return is optional
     * @param responseType      response mode (simple-rpc or server-streaming)
     */
    public void createResponse(BalAsyncApi30MessageImpl subscribeMessage, BalAsyncApi30MessageImpl componentMessage,
                               Node remoteReturnType, String returnDescription, String isOptional,
                               String responseType) {
        responseType = Objects.isNull(responseType) ? SIMPLE_RPC : responseType;
        String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString().trim());
        ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();
        switch (remoteReturnType.kind()) {
            case FLOAT_TYPE_DESC:
            case DECIMAL_TYPE_DESC:
            case INT_TYPE_DESC:
            case STRING_TYPE_DESC:
            case BOOLEAN_TYPE_DESC:
                BalAsyncApi30SchemaImpl remoteReturnSchema = getAsyncApiSchema(remoteReturnTypeString);
                setResponseOfRequest(subscribeMessage, componentMessage, responseType, returnDescription, objectMapper,
                        remoteReturnSchema, isOptional);
                break;
            case JSON_TYPE_DESC:
            case XML_TYPE_DESC:
                BalAsyncApi30SchemaImpl jsonSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                BalAsyncApi30SchemaImpl additionalPropertyObject = new BalAsyncApi30SchemaImpl();
                jsonSchema.setAdditionalProperties(additionalPropertyObject);
                setResponseOfRequest(subscribeMessage, componentMessage, responseType,
                        returnDescription, objectMapper, jsonSchema, isOptional);
                break;
            case SIMPLE_NAME_REFERENCE:
                if (remoteReturnType instanceof SimpleNameReferenceNode) {
                    SimpleNameReferenceNode recordNode = (SimpleNameReferenceNode) remoteReturnType;
                    handleReferenceResponse(subscribeMessage, componentMessage, recordNode,
                            returnDescription, isOptional);
                }
                break;
            case UNION_TYPE_DESC:
                if (remoteReturnType instanceof UnionTypeDescriptorNode) {
                    mapUnionReturns(subscribeMessage, componentMessage,
                            (UnionTypeDescriptorNode) remoteReturnType, returnDescription, isOptional, responseType);
                }
                break;
            case RECORD_TYPE_DESC:
                if (remoteReturnType instanceof RecordTypeDescriptorNode) {
                    mapInlineRecordInReturn(subscribeMessage, componentMessage,
                            (RecordTypeDescriptorNode) remoteReturnType, returnDescription, isOptional);
                }
                break;
            case ARRAY_TYPE_DESC:
                if (remoteReturnType instanceof ArrayTypeDescriptorNode) {
                    getApiResponsesForArrayTypes(subscribeMessage, componentMessage,
                            (ArrayTypeDescriptorNode) remoteReturnType, returnDescription, isOptional);
                }
                break;
            case OPTIONAL_TYPE_DESC:
                if (remoteReturnType instanceof OptionalTypeDescriptorNode) {
                    createResponse(subscribeMessage, componentMessage, ((OptionalTypeDescriptorNode) remoteReturnType)
                            .typeDescriptor(), returnDescription, TRUE, responseType);
                }
                break;
            case MAP_TYPE_DESC:
                if (remoteReturnType instanceof MapTypeDescriptorNode) {
                    MapTypeDescriptorNode mapNode = (MapTypeDescriptorNode) remoteReturnType;
                    BalAsyncApi30SchemaImpl objectSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                    BalAsyncApi30SchemaImpl apiSchema = getAsyncApiSchema(mapNode.mapTypeParamsNode()
                            .typeNode().kind());
                    if (apiSchema.getType() == null) {
                        objectSchema.setAdditionalProperties(new BalAsyncApi30SchemaImpl());
                    } else {
                        objectSchema.setAdditionalProperties(apiSchema);
                    }
                    setResponseOfRequest(subscribeMessage, componentMessage, responseType, returnDescription,
                            objectMapper, objectSchema, isOptional);
                }
                break;
            case STREAM_TYPE_DESC:
                if (remoteReturnType instanceof StreamTypeDescriptorNode) {
                    if (((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().isPresent()) {
                        Node remoteReturnStream = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode)
                                remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode();
                        if (remoteReturnStream instanceof UnionTypeDescriptorNode) {
                            mapUnionReturns(subscribeMessage, componentMessage,
                                    (UnionTypeDescriptorNode) remoteReturnStream, returnDescription, isOptional,
                                    SERVER_STREAMING);
                        } else {
                            createResponse(subscribeMessage, componentMessage, remoteReturnStream, returnDescription,
                                    isOptional, SERVER_STREAMING);
                        }
                        componentMessage.addExtension(X_RESPONSE_TYPE, SERVER_STREAMING_TYPE_NODE);
                    } else {
                        throw new NoSuchElementException(NO_TYPE_IN_STREAM);
                    }
                }
                break;
            case QUALIFIED_NAME_REFERENCE:
                handleQualifiedNameTypeReference(subscribeMessage, componentMessage,
                        returnDescription, remoteReturnType, isOptional);
                break;
            default:
                DiagnosticMessages errorMessage = DiagnosticMessages.AAS_CONVERTOR_108;
                IncompatibleRemoteDiagnostic error = new IncompatibleRemoteDiagnostic(errorMessage, location,
                        remoteReturnType.kind().toString());
                errors.add(error);
                break;
        }
    }

    /**
     * Creates a schema reference message entry for the given type, registers it in components,
     * adds a message reference to the channel's oneOf list, and returns the component message.
     *
     * @param message          channel-level message to add oneOf reference to
     * @param typeName         the type name for schema and message reference
     * @param typeSymbol       type symbol used to generate the component schema
     * @param dispatcherValue  dispatcher value passed to the component mapper
     * @param paramDescription optional description for the payload node
     * @return the component-level message with the payload set
     */
    public BalAsyncApi30MessageImpl extractMessageSchemaReference(BalAsyncApi30MessageImpl message, String typeName,
                                                                   TypeSymbol typeSymbol, String dispatcherValue,
                                                                   String paramDescription) {
        BalAsyncApi30MessageImpl messageType = new BalAsyncApi30MessageImpl();

        componentMapper.createComponentSchema(typeSymbol, dispatcherValue);

        ObjectNode objNode1 = ConverterCommonUtils.createObjectNode();
        objNode1.put(REF, SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeName));

        if (paramDescription != null) {
            objNode1.put(DESCRIPTION, paramDescription);
        }

        BalAsyncApi30MessageImpl componentMessage = new BalAsyncApi30MessageImpl();
        componentMessage.setParent(components);
        componentMessage.addExtension(PAYLOAD, objNode1);

        messageType.set$ref(MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeName));
        setSchemasForChannelsAsOneOfSchema(message, messageType);
        return componentMessage;
    }

    private void setResponseOfRequest(BalAsyncApi30MessageImpl subscribeMessage,
                                      BalAsyncApi30MessageImpl componentMessage, String responseType,
                                      String returnDescription, ObjectMapper objMapper,
                                      BalAsyncApi30SchemaImpl schema, String isOptional) {
        BalAsyncApi30MessageImpl subscribeOneOf = new BalAsyncApi30MessageImpl();

        subscribeOneOf.addExtension(PAYLOAD, objMapper.valueToTree(schema));

        setSchemasForChannelsAsOneOfSchema(subscribeMessage, subscribeOneOf);

        ObjectNode payloadObject = ConverterCommonUtils.createObjectNode();
        payloadObject.set(PAYLOAD, objMapper.valueToTree(schema));

        Map<String, JsonNode> xResponses = componentMessage.getExtensions();
        if (xResponses != null && xResponses.get(X_RESPONSE) != null) {
            if (SERVER_STREAMING_TYPE_NODE.equals(xResponses.get(X_RESPONSE_TYPE))
                    && responseType.equals(SIMPLE_RPC)
                    || (xResponses.get(X_RESPONSE_TYPE).equals(new TextNode(SIMPLE_RPC))
                    && responseType.equals(SERVER_STREAMING))) {
                throw new NoSuchElementException(UNION_STREAMING_SIMPLE_RPC_ERROR);
            }
            BalAsyncApi30MessageImpl oneOfSchema = new BalAsyncApi30MessageImpl();
            if (xResponses.get(X_RESPONSE).get(PAYLOAD) != null || xResponses.get(X_RESPONSE).get(REF) != null) {
                setRefPayloadAsOneOfSchemaForPreviousOneResponse(componentMessage, oneOfSchema);
                setSchemaForOneOfSchema(oneOfSchema, subscribeOneOf);
            } else if (xResponses.get(X_RESPONSE).get(ONEOF) != null) {
                setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(componentMessage, oneOfSchema);
                setSchemaForOneOfSchema(oneOfSchema, subscribeOneOf);
            }
            setDescriptionAndXResponsesForOneOf(componentMessage, returnDescription, objMapper,
                    oneOfSchema, responseType, isOptional);
        } else {
            setDescriptionForOneResponse(returnDescription, payloadObject, componentMessage, responseType, isOptional);
        }
    }

    private void handleQualifiedNameTypeReference(BalAsyncApi30MessageImpl subscribeMessage,
                                                  BalAsyncApi30MessageImpl componentMessage,
                                                  String returnDescription,
                                                  Node remoteReturnType, String isOptional) {
        TypeSymbol qualifiedNameReferenceSymbol = (TypeSymbol) semanticModel.symbol(remoteReturnType).get();
        String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(
                qualifiedNameReferenceSymbol.getName().get());
        if (qualifiedNameReferenceSymbol instanceof TypeReferenceTypeSymbol) {
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) qualifiedNameReferenceSymbol;
            TypeSymbol typeSymbol = typeRef.typeDescriptor();
            if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
                List<TypeSymbol> memberTypes = ((IntersectionTypeSymbol) typeSymbol).memberTypeDescriptors();
                for (TypeSymbol memberType : memberTypes) {
                    if (!(memberType instanceof ReadonlyTypeSymbol)) {
                        typeSymbol = memberType;
                        break;
                    }
                }
            }

            if (remoteReturnType.parent().kind().equals(ARRAY_TYPE_DESC)) {
                updateArraySchema(subscribeMessage, componentMessage, returnDescription,
                        qualifiedNameReferenceSymbol, remoteReturnTypeName, isOptional);
            } else if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
                handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription,
                        qualifiedNameReferenceSymbol,
                        qualifiedNameReferenceSymbol.getName().get(), isOptional);
            } else if (typeSymbol.typeKind() == TypeDescKind.UNION) {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                for (TypeSymbol memberType : unionTypeSymbol.memberTypeDescriptors()) {
                    if (memberType instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol
                            && typeReferenceTypeSymbol.getName().isPresent()) {
                        handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription,
                                typeReferenceTypeSymbol, typeReferenceTypeSymbol.getName().get(), isOptional);
                    }
                }
            }
        }
    }

    private void handleReferenceResponse(BalAsyncApi30MessageImpl subscribeMessage,
                                         BalAsyncApi30MessageImpl componentMessage,
                                         SimpleNameReferenceNode referenceNode,
                                         String returnDescription, String isOptional) {
        TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(referenceNode).orElseThrow();
        String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(referenceNode.name().toString().trim());

        if (referenceNode.parent().kind().equals(ARRAY_TYPE_DESC)) {
            updateArraySchema(subscribeMessage, componentMessage, returnDescription,
                    returnTypeSymbol, remoteReturnTypeName, isOptional);
        } else if (returnTypeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription,
                    returnTypeSymbol, remoteReturnTypeName, isOptional);
        }
    }

    private void handleRecordTypeSymbol(BalAsyncApi30MessageImpl subscribeMessage,
                                        BalAsyncApi30MessageImpl componentMessage, String returnDescription,
                                        TypeSymbol returnTypeSymbol, String remoteReturnTypeName, String isOptional) {
        BalAsyncApi30MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage,
                remoteReturnTypeName, returnTypeSymbol, null, null);
        if (!(components.getMessages() != null && components.getMessages().get(remoteReturnTypeName) != null)) {
            components.addMessage(remoteReturnTypeName, componentReturnMessage);
        }
        ObjectNode messageRefObject = ConverterCommonUtils.createObjectNode();
        messageRefObject.put(REF, MESSAGE_REFERENCE
                + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
        Map<String, JsonNode> xResponses = componentMessage.getExtensions();
        if (xResponses != null && xResponses.get(X_RESPONSE) != null) {
            if (SERVER_STREAMING_TYPE_NODE.equals(xResponses.get(X_RESPONSE_TYPE))
                    && !isCloseFrameRecordType(returnTypeSymbol)) {
                throw new NoSuchElementException(UNION_STREAMING_SIMPLE_RPC_ERROR);
            }
            ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();
            BalAsyncApi30MessageImpl oneOfSchema = new BalAsyncApi30MessageImpl();
            if (xResponses.get(X_RESPONSE).get(PAYLOAD) != null || xResponses.get(X_RESPONSE).get(REF) != null) {
                setRefPayloadAsOneOfSchemaForPreviousOneResponse(componentMessage, oneOfSchema);
                BalAsyncApi30MessageImpl schemaObject = new BalAsyncApi30MessageImpl();
                schemaObject.set$ref(messageRefObject.get(REF).asText());
                setSchemaForOneOfSchema(oneOfSchema, schemaObject);
            } else if (xResponses.get(X_RESPONSE).get(ONEOF) != null) {
                setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(componentMessage, oneOfSchema);
                BalAsyncApi30MessageImpl schema = new BalAsyncApi30MessageImpl();
                schema.set$ref(MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
                setSchemaForOneOfSchema(oneOfSchema, schema);
            }
            String responseType = isCloseFrameRecordType(returnTypeSymbol)
                    ? xResponses.get(X_RESPONSE_TYPE).asText() : SIMPLE_RPC;
            setDescriptionAndXResponsesForOneOf(componentMessage, returnDescription, objMapper,
                    oneOfSchema, responseType, isOptional);
        } else {
            setDescriptionForOneResponse(returnDescription, messageRefObject, componentMessage,
                    SIMPLE_RPC, isOptional);
        }
    }

    private void updateArraySchema(BalAsyncApi30MessageImpl subscribeMessage,
                                   BalAsyncApi30MessageImpl componentMessage,
                                   String returnDescription, TypeSymbol returnTypeSymbol,
                                   String remoteReturnTypeName, String isOptional) {
        componentMapper.createComponentSchema(returnTypeSymbol, null);
        errors.addAll(componentMapper.getDiagnostics());

        ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();

        BalAsyncApi30SchemaImpl itemSchema = new BalAsyncApi30SchemaImpl();
        itemSchema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
        BalAsyncApi30SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
        arraySchema.setItems(itemSchema);
        setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC, returnDescription, objMapper,
                arraySchema, isOptional);
    }

    private void setSchemasForChannelsAsOneOfSchema(BalAsyncApi30MessageImpl oneOfSchema,
                                                    BalAsyncApi30MessageImpl schema) {
        ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();
        JsonNode schemaNode = objMapper.valueToTree(schema);
        Map<String, JsonNode> extensions = oneOfSchema.getExtensions();

        if (extensions == null || extensions.get(ONEOF) == null) {
            ArrayNode oneOfArray = objMapper.createArrayNode();
            oneOfArray.add(schemaNode);
            oneOfSchema.addExtension(ONEOF, oneOfArray);
        } else {
            ArrayNode oneOfArray = (ArrayNode) extensions.get(ONEOF);
            if (schema.get$ref() != null) {
                boolean check = false;
                for (JsonNode node : oneOfArray) {
                    if (node.get(REF) != null && node.get(REF).asText().equals(schema.get$ref())) {
                        check = true;
                        break;
                    }
                }
                if (!check) {
                    oneOfArray.add(schemaNode);
                    oneOfSchema.addExtension(ONEOF, oneOfArray);
                }
            } else {
                Map<String, JsonNode> schemaExtensions = schema.getExtensions();
                JsonNode schemaPayload = schemaExtensions != null ? schemaExtensions.get(PAYLOAD) : null;
                if (schemaPayload != null) {
                    boolean check = false;
                    for (JsonNode node : oneOfArray) {
                        if (node.get(PAYLOAD) != null && node.get(PAYLOAD).equals(schemaPayload)) {
                            check = true;
                            break;
                        }
                    }
                    if (!check) {
                        oneOfArray.add(schemaNode);
                        oneOfSchema.addExtension(ONEOF, oneOfArray);
                    }
                }
            }
        }
    }

    private void setDescriptionForOneResponse(String returnDescription, ObjectNode messageRefObject,
                                              BalAsyncApi30MessageImpl componentMessage, String responseType,
                                              String isOptional) {
        if (returnDescription != null) {
            messageRefObject.put(DESCRIPTION, returnDescription);
        }
        if (isOptional.equals(TRUE)) {
            messageRefObject.put(X_REQUIRED, BooleanNode.FALSE);
        }
        componentMessage.addExtension(X_RESPONSE, messageRefObject);
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));
    }

    private void setDescriptionAndXResponsesForOneOf(BalAsyncApi30MessageImpl componentMessage,
                                                     String returnDescription, ObjectMapper objMapper,
                                                     BalAsyncApi30MessageImpl oneOfSchema, String responseType,
                                                     String isOptional) {
        if (returnDescription != null) {
            oneOfSchema.setDescription(returnDescription);
        }
        if (isOptional.equals(TRUE)) {
            oneOfSchema.addExtension(X_REQUIRED, BooleanNode.FALSE);
        }
        componentMessage.addExtension(X_RESPONSE, objMapper.valueToTree(oneOfSchema));
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));
    }

    private void setSchemaForOneOfSchema(BalAsyncApi30MessageImpl oneOfSchema, BalAsyncApi30MessageImpl schema) {
        ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();
        Map<String, JsonNode> extensions = oneOfSchema.getExtensions();
        JsonNode oneOfNode = extensions != null ? extensions.get(ONEOF) : null;
        if (Objects.isNull(oneOfNode) || Objects.isNull(schema.get$ref())
                || ((ArrayNode) oneOfNode).findValues(REF).stream()
                .noneMatch(ref -> schema.get$ref().equals(ref.asText()))) {
            ArrayNode oneOfArray = oneOfNode instanceof ArrayNode
                    ? (ArrayNode) oneOfNode : objMapper.createArrayNode();
            oneOfArray.add(objMapper.valueToTree(schema));
            oneOfSchema.addExtension(ONEOF, oneOfArray);
        }
    }

    private void setRefPayloadAsOneOfSchemaForPreviousOneResponse(BalAsyncApi30MessageImpl componentMessage,
                                                                  BalAsyncApi30MessageImpl oneOfSchema) {
        if (componentMessage.getExtensions().get(X_RESPONSE).get(REF) != null) {
            TextNode reference = (TextNode) componentMessage.getExtensions().get(X_RESPONSE).get(REF);
            BalAsyncApi30MessageImpl testObject = new BalAsyncApi30MessageImpl();
            testObject.set$ref(reference.textValue());
            setSchemaForOneOfSchema(oneOfSchema, testObject);
        } else if (componentMessage.getExtensions().get(X_RESPONSE).get(PAYLOAD) != null) {
            ObjectNode reference = (ObjectNode) componentMessage.getExtensions().get(X_RESPONSE).get(PAYLOAD);
            BalAsyncApi30MessageImpl testObject = new BalAsyncApi30MessageImpl();
            testObject.addExtension(PAYLOAD, reference);
            setSchemaForOneOfSchema(oneOfSchema, testObject);
        }
    }

    private void setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(BalAsyncApi30MessageImpl componentMessage,
                                                                     BalAsyncApi30MessageImpl oneOfSchema) {
        ArrayNode oneOfNode = (ArrayNode) componentMessage.getExtensions().get(X_RESPONSE).get(ONEOF);
        for (int i = 0; i < oneOfNode.size(); i++) {
            BalAsyncApi30MessageImpl refSchema = new BalAsyncApi30MessageImpl();
            if (oneOfNode.get(i).get(PAYLOAD) != null) {
                refSchema.addExtension(PAYLOAD, oneOfNode.get(i).get(PAYLOAD));
            } else if (oneOfNode.get(i).get(REF) != null) {
                refSchema.set$ref(oneOfNode.get(i).get(REF).asText());
            }
            setSchemaForOneOfSchema(oneOfSchema, refSchema);
        }
    }

    /**
     * Handle the response has union type.
     */
    private void mapUnionReturns(BalAsyncApi30MessageImpl subscribeMessage, BalAsyncApi30MessageImpl componentMessage,
                                 UnionTypeDescriptorNode typeNode, String returnDescription, String isOptional,
                                 String responseType) {
        TypeDescriptorNode rightNode = typeNode.rightTypeDesc();
        TypeDescriptorNode leftNode = typeNode.leftTypeDesc();
        createResponse(subscribeMessage, componentMessage, leftNode, returnDescription, isOptional, responseType);
        if (rightNode instanceof UnionTypeDescriptorNode) {
            UnionTypeDescriptorNode traversRightNode = (UnionTypeDescriptorNode) rightNode;
            while (traversRightNode.rightTypeDesc() != null) {
                if (leftNode.kind() == QUALIFIED_NAME_REFERENCE) {
                    leftNode = ((UnionTypeDescriptorNode) rightNode).leftTypeDesc();
                    createResponse(subscribeMessage, componentMessage, leftNode, returnDescription, isOptional,
                            responseType);
                }
            }
        } else {
            createResponse(subscribeMessage, componentMessage, rightNode, returnDescription, isOptional, responseType);
        }
    }

    /**
     * Handle response has inline record as return type.
     */
    private void mapInlineRecordInReturn(BalAsyncApi30MessageImpl subscribeMessage,
                                         BalAsyncApi30MessageImpl componentMessage,
                                         RecordTypeDescriptorNode typeNode,
                                         String returnDescription, String isOptional) {
        NodeList<Node> fields = typeNode.fields();
        BalAsyncApi30SchemaImpl inlineSchema = new BalAsyncApi30SchemaImpl();
        inlineSchema.setType(AsyncAPIType.OBJECT.toString());
        for (Node field : fields) {
            if (field.kind() == RECORD_FIELD) {
                RecordFieldNode recordField = (RecordFieldNode) field;
                Node type01 = recordField.typeName();
                if (recordField.typeName().kind() == SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode nameRefNode = (SimpleNameReferenceNode) type01;
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(nameRefNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol, null);
                    BalAsyncApi30SchemaImpl referenceSchema = new BalAsyncApi30SchemaImpl();
                    referenceSchema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(
                            recordField.typeName().toString().trim()));
                    inlineSchema.addProperty(recordField.fieldName().text(), referenceSchema);
                } else {
                    BalAsyncApi30SchemaImpl propertySchema = ConverterCommonUtils.getAsyncApiSchema(
                            recordField.typeName().toString().trim());
                    inlineSchema.addProperty(recordField.fieldName().text(), propertySchema);
                }
            }
        }
        setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC,
                returnDescription, callObjectMapper(), inlineSchema, isOptional);
    }

    /**
     * Handle return has array types.
     */
    private void getApiResponsesForArrayTypes(BalAsyncApi30MessageImpl subscribeMessage,
                                              BalAsyncApi30MessageImpl componentMessage,
                                              ArrayTypeDescriptorNode array,
                                              String returnDescription, String isOptional) {
        if (array.memberTypeDesc().kind() == SIMPLE_NAME_REFERENCE) {
            handleReferenceResponse(subscribeMessage, componentMessage,
                    (SimpleNameReferenceNode) array.memberTypeDesc(), returnDescription, isOptional);
        } else if (array.memberTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
            handleQualifiedNameTypeReference(subscribeMessage, componentMessage, returnDescription,
                    array.memberTypeDesc(), isOptional);
        } else {
            BalAsyncApi30SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            String type02 = array.memberTypeDesc().kind().toString().trim().split("_")[0].toLowerCase(Locale.ENGLISH);
            BalAsyncApi30SchemaImpl asyncApiSchema = getAsyncApiSchema(type02);
            ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();
            arraySchema.setItems(asyncApiSchema);
            setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC, returnDescription, objectMapper,
                    arraySchema, isOptional);
        }
    }
}
