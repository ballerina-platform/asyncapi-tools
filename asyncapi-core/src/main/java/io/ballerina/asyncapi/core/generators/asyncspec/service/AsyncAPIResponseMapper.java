package io.ballerina.asyncapi.core.generators.asyncspec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25MessageImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ReadonlyTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
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

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DESCRIPTION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.MESSAGE_REFERENCE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.NO_TYPE_IN_STREAM;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.ONEOF;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.PAYLOAD;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.REF;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SERVER_STREAMING;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.UNION_STREAMING_SIMPLE_RPC_ERROR;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_REQUIRED;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_RESPONSE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.callObjectMapper;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_FIELD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;


/**
 * This class processes mapping responses between Ballerina and AsyncAPISpec.
 *
 * @since 2.0.0
 */
public class AsyncAPIResponseMapper {
    private final Location location;
    private final SemanticModel semanticModel;

    private final AsyncApi25ComponentsImpl components;

    private final AsyncAPIComponentMapper componentMapper;


    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();

    public AsyncAPIResponseMapper(Location location, AsyncAPIComponentMapper componentMapper,
                                  SemanticModel semanticModel
            , AsyncApi25ComponentsImpl components) {
        this.location = location;
        this.semanticModel = semanticModel;
        this.componentMapper = componentMapper;
        this.components = components;
    }


    public void createResponse(BalAsyncApi25MessageImpl subscribeMessage, BalAsyncApi25MessageImpl componentMessage,
                               Node remoteReturnType, String returnDescription, String isOptional) {
        String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString().trim());
        ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();


        switch (remoteReturnType.kind()) {
            case FLOAT_TYPE_DESC:
            case DECIMAL_TYPE_DESC:
            case INT_TYPE_DESC:
            case STRING_TYPE_DESC:
            case BOOLEAN_TYPE_DESC:

                BalAsyncApi25SchemaImpl remoteReturnSchema = getAsyncApiSchema(remoteReturnTypeString);
                setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC, returnDescription, objectMapper,
                        remoteReturnSchema, isOptional);
                break;

            //TODO : Edit this after figure out how to do this
            //TODO : It may be an object in asyncapi but not sue
            case JSON_TYPE_DESC:
            case XML_TYPE_DESC:
                BalAsyncApi25SchemaImpl jsonSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                //TODO : Change this into true after Api-curio team change additionalProperties type into Object type
                BalAsyncApi25SchemaImpl additionalPropertyObject = new BalAsyncApi25SchemaImpl();
                jsonSchema.setAdditionalProperties(additionalPropertyObject);

                //TODO : Set this schema into main asyncapi doc
                setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC,
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
                            (UnionTypeDescriptorNode) remoteReturnType, returnDescription, isOptional);
                }
                break;
            case RECORD_TYPE_DESC:
                //TODO : Problem in map inline record return
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
                    createResponse(subscribeMessage, componentMessage,
                            ((OptionalTypeDescriptorNode) remoteReturnType).typeDescriptor(),
                            returnDescription, TRUE);
                }
                break;
            case MAP_TYPE_DESC:
                if (remoteReturnType instanceof MapTypeDescriptorNode) {
                    MapTypeDescriptorNode mapNode = (MapTypeDescriptorNode) remoteReturnType;
                    BalAsyncApi25SchemaImpl objectSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());

                    //TODO : I changed this kind to string
                    BalAsyncApi25SchemaImpl apiSchema = getAsyncApiSchema(mapNode.mapTypeParamsNode().
                            typeNode().kind());
                    objectSchema.setAdditionalProperties(apiSchema);
                    setResponseOfRequest(subscribeMessage, componentMessage,
                            SIMPLE_RPC, returnDescription, objectMapper, objectSchema, isOptional);
                }
                break;

            case STREAM_TYPE_DESC:
                if (remoteReturnType instanceof StreamTypeDescriptorNode) {
                    if (((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().isPresent()) {
                        String remoteReturnStream = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode)
                                remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode().toString().trim();

                        BalAsyncApi25SchemaImpl remoteReturnStreamSchema = getAsyncApiSchema(remoteReturnStream);
//                        remoteReturnStreamSchema.addExtension(X_RESPONSE_TYPE,new TextNode(SERVER_STREAMING));
                        setResponseOfRequest(subscribeMessage, componentMessage, SERVER_STREAMING, returnDescription,
                                objectMapper, remoteReturnStreamSchema, isOptional);
                    } else {
                        throw new NoSuchElementException(NO_TYPE_IN_STREAM);
                    }
                }
                break;

            case QUALIFIED_NAME_REFERENCE:
//                TypeSymbol qualifiedNameReferenceSymbol = (TypeSymbol) semanticModel.symbol(remoteReturnType).get();

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

    private void handleQualifiedNameTypeReference(BalAsyncApi25MessageImpl subscribeMessage,
                                                  BalAsyncApi25MessageImpl componentMessage,
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
                        //TODO : address the rest of the simple name reference types
                        typeSymbol = memberType;
                        break;
                    }
                }
            }

            if (remoteReturnType.parent().kind().equals(ARRAY_TYPE_DESC)) {

                //create Schema
                updateArraySchema(subscribeMessage, componentMessage, returnDescription,
                        qualifiedNameReferenceSymbol, remoteReturnTypeName, isOptional);


            } else if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
                handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription,
                        qualifiedNameReferenceSymbol,
                        qualifiedNameReferenceSymbol.getName().get(), isOptional);
            }
        }
    }

    private void setResponseOfRequest(BalAsyncApi25MessageImpl subscribeMessage,
                                      BalAsyncApi25MessageImpl componentMessage,
                                      String responseType, String returnDescription, ObjectMapper objMapper,
                                      BalAsyncApi25SchemaImpl schema, String isOptional) {
        //create new subscribe oneOf message
        BalAsyncApi25MessageImpl subscribeOneOf = new BalAsyncApi25MessageImpl();

        //set payload of the oneOf message(but this is not a reference)
        subscribeOneOf.setPayload(objMapper.valueToTree(schema));

        //set oneOf message into Subscribe channels
        setSchemasForChannelsAsOneOfSchema(subscribeMessage, subscribeOneOf);

        //create message response with its description
        ObjectNode payloadObject = ConverterCommonUtils.createObjectNode();
        payloadObject.set(PAYLOAD, objMapper.valueToTree(schema));

        //If there exist previous x-response for same request
        Map<String, JsonNode> xResponses = componentMessage.getExtensions();
        if (xResponses != null && xResponses.get(X_RESPONSE) != null) {

            if (xResponses.get(X_RESPONSE_TYPE).equals(new TextNode(SERVER_STREAMING))
                    && responseType.equals(SIMPLE_RPC) ||
                    (xResponses.get(X_RESPONSE_TYPE).equals(new TextNode(SIMPLE_RPC))
                            && responseType.equals(SERVER_STREAMING))) {
                throw new NoSuchElementException(UNION_STREAMING_SIMPLE_RPC_ERROR);
            }

            //Create oneOfSchema
            BalAsyncApi25MessageImpl oneOfSchema = new BalAsyncApi25MessageImpl();

            if (xResponses.get(X_RESPONSE).get(PAYLOAD) != null || xResponses.get(X_RESPONSE).get(REF) != null) {

                setRefPayloadAsOneOfSchemaForPreviousOneResponse(componentMessage, oneOfSchema);

                //Get newly created x-response and add it to oneOf section
                setSchemaForOneOfSchema(oneOfSchema, subscribeOneOf);

//                //Set x-response and x-response type as extensions to request
//                object = oneOfSchema;

                //If there are more than two responses have
            } else if (xResponses.get(X_RESPONSE).get(ONEOF) != null) {


                //Take all previous oneOf responses
                setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(componentMessage, oneOfSchema);


                setSchemaForOneOfSchema(oneOfSchema, subscribeOneOf);

            }
            //Set the description for oneOfSchema
            setDescriptionAndXResponsesForOneOf(componentMessage, returnDescription, objMapper,
                    oneOfSchema, responseType, isOptional);
        } else {

            setDescriptionForOneResponse(returnDescription, payloadObject, componentMessage, responseType, isOptional);
        }

    }

    public BalAsyncApi25MessageImpl extractMessageSchemaReference(BalAsyncApi25MessageImpl message, String typeName,
                                                                  TypeSymbol typeSymbol, String dispatcherValue,
                                                                  String paramDescription) {

        BalAsyncApi25MessageImpl messageType = new BalAsyncApi25MessageImpl();

        //create Schema in schema section
        componentMapper.createComponentSchema(typeSymbol, dispatcherValue);

        //create SchemaReference message section
        ObjectNode objNode1 = ConverterCommonUtils.createObjectNode();
        objNode1.put(REF, SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeName));

        //Set description
        if (paramDescription != null) {
            objNode1.put(DESCRIPTION, paramDescription);
        }

        //create Message in component section
        BalAsyncApi25MessageImpl componentMessage = new BalAsyncApi25MessageImpl();
        componentMessage.setPayload(objNode1);

        //create MessageReference in channel section
        messageType.set$ref(MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeName));
        setSchemasForChannelsAsOneOfSchema(message, messageType);

        return componentMessage;
    }

    private void setSchemasForChannelsAsOneOfSchema(BalAsyncApi25MessageImpl oneOfSchema,
                                                    BalAsyncApi25MessageImpl schema) {

        if (oneOfSchema.getOneOf() == null) {
            oneOfSchema.addOneOf(schema);

        } else {
            if (schema.get$ref() != null) {
                boolean check = false;
                for (AsyncApiMessage s : oneOfSchema.getOneOf()) {
                    if (((BalAsyncApi25MessageImpl) s).get$ref() != null) {
                        if (((BalAsyncApi25MessageImpl) s).get$ref().equals(schema.get$ref())) {
                            check = true;

                        }
                    }
                }
                if (!check) {
                    oneOfSchema.addOneOf(schema);
                }
            } else if (schema.getPayload() != null) {
                boolean check = false;
                for (AsyncApiMessage s : oneOfSchema.getOneOf()) {
                    if (s.getPayload() != null) {
                        if (s.getPayload().equals(schema.getPayload())) {
                            check = true;
                        }
                    }
                }
                if (!check) {
                    oneOfSchema.addOneOf(schema);
                }

            }
        }
    }

    private void handleReferenceResponse(BalAsyncApi25MessageImpl subscribeMessage,
                                         BalAsyncApi25MessageImpl componentMessage,
                                         SimpleNameReferenceNode referenceNode,
                                         String returnDescription, String isOptional) {
        TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(referenceNode).orElseThrow();
        String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(referenceNode.name().toString().trim());

        if (referenceNode.parent().kind().equals(ARRAY_TYPE_DESC)) {

            updateArraySchema(subscribeMessage, componentMessage, returnDescription,
                    returnTypeSymbol, remoteReturnTypeName, isOptional);


        } else if (returnTypeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {


//            if (referredTypeSymbol.typeKind() == TypeDescKind.RECORD) {
//                componentMapper.createComponentSchema(referredTypeSymbol, null);
            handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription,
                    returnTypeSymbol, remoteReturnTypeName, isOptional);


            //handle record for components


//            } else if (referredTypeSymbol.typeKind() == TypeDescKind.ERROR) {
//                io.swagger.v3.oas.models.media.MediaType mediaType = new io.swagger.v3.oas.models.media.MediaType();
//                apiResponse.description(HTTP_500_DESCRIPTION);
//                mediaType.setSchema(new StringSchema());
//                apiResponse.content(new Content().addMediaType(MediaType.TEXT_PLAIN, mediaType));
//                apiResponses.put(HTTP_500, apiResponse);
//            } else if (referredTypeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE &&
//                    generateApiResponseCode(referredTypeName).isPresent()) {
//                Optional<String> code = generateApiResponseCode(referredTypeName);
//                apiResponse.description(referredTypeName);
//                setCacheHeader(headers, apiResponse, code.get());
//                apiResponses.put(code.get(), apiResponse);
            //TODO : Need to implement this

        }

    }

    private void updateArraySchema(BalAsyncApi25MessageImpl subscribeMessage,
                                   BalAsyncApi25MessageImpl componentMessage,
                                   String returnDescription, TypeSymbol returnTypeSymbol,
                                   String remoteReturnTypeName, String isOptional) {
        //create Schema
        componentMapper.createComponentSchema(returnTypeSymbol, null);
        errors.addAll(componentMapper.getDiagnostics());

        //create Message (there is no message reference)
        ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();
//        ObjectNode refObjNode = ConverterCommonUtils.createObjectNode();

        //Create schema reference
        BalAsyncApi25SchemaImpl itemSchema= new BalAsyncApi25SchemaImpl();
        itemSchema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
//        refObjNode.put(REF, SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
        BalAsyncApi25SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
        arraySchema.setItems(itemSchema);
        setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC,
                returnDescription, objMapper, arraySchema, isOptional);
    }

    private void handleRecordTypeSymbol(BalAsyncApi25MessageImpl subscribeMessage,
                                        BalAsyncApi25MessageImpl componentMessage,
                                        String returnDescription,
                                        TypeSymbol returnTypeSymbol,
                                        String remoteReturnTypeName,
                                        String isOptional) {
        //Creating return type message reference

        BalAsyncApi25MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage,
                remoteReturnTypeName, returnTypeSymbol, null, null);


        if (!(components.getMessages() != null && components.getMessages().get(remoteReturnTypeName) != null)) {
            //add created return message
            components.addMessage(remoteReturnTypeName, componentReturnMessage);
        }
        //set message reference as a x-response of a request
        ObjectNode messageRefObject = ConverterCommonUtils.createObjectNode();
        messageRefObject.put(REF, MESSAGE_REFERENCE +
                ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));


        //If there exist previous x-response for same request then start adding them to a oneOf schema
        Map<String, JsonNode> xResponses = componentMessage.getExtensions();
        if (xResponses != null && xResponses.get(X_RESPONSE) != null) {

            if (xResponses.get(X_RESPONSE_TYPE).equals(new TextNode(SERVER_STREAMING))) {
                throw new NoSuchElementException(UNION_STREAMING_SIMPLE_RPC_ERROR);
            }

            ObjectMapper objMapper = ConverterCommonUtils.callObjectMapper();
            //Create oneOfSchema
            BalAsyncApi25MessageImpl oneOfSchema = new BalAsyncApi25MessageImpl();

            if (xResponses.get(X_RESPONSE).get(PAYLOAD) != null || xResponses.get(X_RESPONSE).get(REF) != null) {

                setRefPayloadAsOneOfSchemaForPreviousOneResponse(componentMessage, oneOfSchema);

                //set newly created x-response to a schema and add it to oneOf section
                BalAsyncApi25MessageImpl schemaObject;
                try {
                    schemaObject = objMapper.treeToValue(messageRefObject, BalAsyncApi25MessageImpl.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                setSchemaForOneOfSchema(oneOfSchema, schemaObject);


                //If there are more than two responses have
            } else if (xResponses.get(X_RESPONSE).get(ONEOF) != null) {


                //Take all previous oneOf responses
                setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(componentMessage, oneOfSchema);

                //create schema reference for newly created x-response
                BalAsyncApi25MessageImpl schema = new BalAsyncApi25MessageImpl();

                //Get newly created x-response and add it to oneOf section of schema object
                schema.set$ref(MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));

                setSchemaForOneOfSchema(oneOfSchema, schema);

            }
            //Set the description for oneOfSchema
            setDescriptionAndXResponsesForOneOf(componentMessage, returnDescription, objMapper,
                    oneOfSchema, SIMPLE_RPC, isOptional);

        } else {
            setDescriptionForOneResponse(returnDescription, messageRefObject, componentMessage,
                    SIMPLE_RPC, isOptional);
        }

    }


    private void setDescriptionForOneResponse(String returnDescription,
                                              ObjectNode messageRefObject,
                                              BalAsyncApi25MessageImpl componentMessage,
                                              String responseType, String isOptional) {
        //Set the description
        if (returnDescription != null) {
            messageRefObject.put(DESCRIPTION, returnDescription);
        }
        if (isOptional.equals(TRUE)) {
            messageRefObject.put(X_REQUIRED, BooleanNode.FALSE);
        }
        //Set x-response and x-response type of the request
        componentMessage.addExtension(X_RESPONSE, messageRefObject);
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));

    }

    private void setDescriptionAndXResponsesForOneOf(BalAsyncApi25MessageImpl componentMessage,
                                                     String returnDescription,
                                                     ObjectMapper objMapper,
                                                     BalAsyncApi25MessageImpl oneOfSchema,
                                                     String responseType, String isOptional) {
        //Set the description
        if (returnDescription != null) {
            oneOfSchema.setDescription(returnDescription);
        }
        if (isOptional.equals(TRUE)) {
//            componentMessage.addExtension(X_OPTIONAL,new TextNode(isOptional.toString()));
            oneOfSchema.addExtension(X_REQUIRED, BooleanNode.FALSE);
        }
        //Set x-response and x-response type as extensions to request
        componentMessage.addExtension(X_RESPONSE, objMapper.valueToTree(oneOfSchema));
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));


//        if(componentMessage.getExtensions()!=null && componentMessage.getExtensions().get(X_RESPONSE_TYPE).
//                equals(new TextNode(SIMPLE_RPC+"/"+ SERVER_STREAMING))){
//            componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC+"/"+ SERVER_STREAMING));
//
//        }else {
//            if (responseType == SERVER_STREAMING || (componentMessage.getExtensions()!=null &&
//                    componentMessage.getExtensions().get(X_RESPONSE_TYPE).equals(new TextNode(SERVER_STREAMING)))) {
//                componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC + "/" + SERVER_STREAMING));
//            } else {
//                componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
//            }
//        }

//        if(isOptional){
//            componentMessage.addExtension(X_OPTIONAL,new TextNode(isOptional.toString()));
//        }
    }

    private void setSchemaForOneOfSchema(BalAsyncApi25MessageImpl oneOfSchema, BalAsyncApi25MessageImpl schema) {

        oneOfSchema.addOneOf(schema);

    }


    private void setRefPayloadAsOneOfSchemaForPreviousOneOfResponses(BalAsyncApi25MessageImpl componentMessage,
                                                                     BalAsyncApi25MessageImpl oneOfSchema) {
        ArrayNode oneOfNode = (ArrayNode) componentMessage.getExtensions().get(X_RESPONSE).get(ONEOF);


        //Add all of them into oneOf
        for (int i = 0; i < oneOfNode.size(); i++) {
            BalAsyncApi25MessageImpl refSchema = new BalAsyncApi25MessageImpl();
            if (oneOfNode.get(i).get(PAYLOAD) != null) {
                refSchema.setPayload(oneOfNode.get(i).get(PAYLOAD));
            } else if (oneOfNode.get(i).get(REF) != null) {
                refSchema.set$ref(oneOfNode.get(i).get(REF).asText());
            }
            setSchemaForOneOfSchema(oneOfSchema, refSchema);
        }
    }

    private void setRefPayloadAsOneOfSchemaForPreviousOneResponse(BalAsyncApi25MessageImpl componentMessage,
                                                                  BalAsyncApi25MessageImpl oneOfSchema) {
        if (componentMessage.getExtensions().get(X_RESPONSE).get(REF) != null) {
            //Get existing only one x-response
            TextNode reference = (TextNode) componentMessage.getExtensions().get(X_RESPONSE).get(REF);

            //add it to oneOf section
            BalAsyncApi25MessageImpl testObject = new BalAsyncApi25MessageImpl();
            testObject.set$ref(reference.textValue());

            //add schema into oneOf section
            setSchemaForOneOfSchema(oneOfSchema, testObject);
        } else if (componentMessage.getExtensions().get(X_RESPONSE).get(PAYLOAD) != null) {

            ObjectNode reference = (ObjectNode) componentMessage.getExtensions().get(X_RESPONSE).get(PAYLOAD);

            //add it to oneOf section
            BalAsyncApi25MessageImpl testObject = new BalAsyncApi25MessageImpl();
            testObject.setPayload(reference);

            //add schema into oneOf section
            setSchemaForOneOfSchema(oneOfSchema, testObject);

        }
    }


    /**
     * Handle the response has union type.
     *
     * <pre>
     *     resource function post reservation(@http:Payload Reservation reservation)
     *                 returns ReservationCreated|ReservationConflict {
     *         ReservationCreated created = createReservation(reservation);
     *         return created;
     *     }
     * </pre>
     */
    private void mapUnionReturns(BalAsyncApi25MessageImpl subscribeMessage, BalAsyncApi25MessageImpl componentMessage,
                                 UnionTypeDescriptorNode typeNode, String returnDescription, String isOptional) {

        TypeDescriptorNode rightNode = typeNode.rightTypeDesc();
        TypeDescriptorNode leftNode = typeNode.leftTypeDesc();
        // Handle leftNode because it is main node
        createResponse(subscribeMessage, componentMessage, leftNode, returnDescription, isOptional);
        // Handle rest of the union type
        if (rightNode instanceof UnionTypeDescriptorNode) {
            UnionTypeDescriptorNode traversRightNode = (UnionTypeDescriptorNode) rightNode;
            while (traversRightNode.rightTypeDesc() != null) {
                if (leftNode.kind() == QUALIFIED_NAME_REFERENCE) {
                    leftNode = ((UnionTypeDescriptorNode) rightNode).leftTypeDesc();
                    createResponse(subscribeMessage, componentMessage, leftNode, returnDescription, isOptional);
                }
            }
        } else {
            createResponse(subscribeMessage, componentMessage, rightNode, returnDescription, isOptional);
        }
    }


    /**
     * Handle response has inline record as return type.
     */
    private void mapInlineRecordInReturn(BalAsyncApi25MessageImpl subscribeMessage,
                                         BalAsyncApi25MessageImpl componentMessage,
                                         RecordTypeDescriptorNode typeNode, String returnDescription,
                                         String isOptional) {

        NodeList<Node> fields = typeNode.fields();


//        Map<String,BalAsyncApi25SchemaImpl> properties = new HashMap<>();
        BalAsyncApi25SchemaImpl inlineSchema = new BalAsyncApi25SchemaImpl();
        inlineSchema.setType(AsyncAPIType.OBJECT.toString());


        // 1- scenarios returns record {| int id; string body;|} - done
        // 2- scenarios returns record {| int id; Person body;|} - done

        for (Node field : fields) {

            //TODO : Have to implement type inclusion part uncomment below code if after editing it
//            if (field.kind() == TYPE_REFERENCE) {
//                TypeReferenceNode typeFieldNode = (TypeReferenceNode) field;
//                if (typeFieldNode.typeName().kind() == SIMPLE_NAME_REFERENCE) {
//                   SimpleNameReferenceNode simpleNameReferenceNode=(SimpleNameReferenceNode) typeFieldNode.typeName();
//                   TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(simpleNameReferenceNode).orElseThrow();
//                   componentMapper.createComponentSchema(typeSymbol,null);
////                 QualifiedNameReferenceNode identifierNode = (QualifiedNameReferenceNode) typeFieldNode.typeName();
//                }
//            }
            if (field.kind() == RECORD_FIELD) {
                RecordFieldNode recordField = (RecordFieldNode) field;
                Node type01 = recordField.typeName();
//        TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(typeNode).orElseThrow();

                if (recordField.typeName().kind() == SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode nameRefNode = (SimpleNameReferenceNode) type01;
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(nameRefNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol, null);
////                    handleReferenceResponse(subscribeMessage,componentMessage,nameRefNode,returnDescription);
                    BalAsyncApi25SchemaImpl referenceSchema = new BalAsyncApi25SchemaImpl();
//
                    referenceSchema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(
                            recordField.typeName().toString().trim()));
                    inlineSchema.addProperty(recordField.fieldName().text(), referenceSchema);
                } else {
//                    //TODO array fields handling

                    BalAsyncApi25SchemaImpl propertySchema = ConverterCommonUtils.getAsyncApiSchema(
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
    private void getApiResponsesForArrayTypes(BalAsyncApi25MessageImpl subscribeMessage,
                                              BalAsyncApi25MessageImpl componentMessage,
                                              ArrayTypeDescriptorNode array,
                                              String returnDescription, String isOptional) {


        if (array.memberTypeDesc().kind() == SIMPLE_NAME_REFERENCE) {
            handleReferenceResponse(subscribeMessage, componentMessage,
                    (SimpleNameReferenceNode) array.memberTypeDesc(), returnDescription, isOptional);

        } else if (array.memberTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
//            TypeSymbol qualifiedNameReferenceSymbol = (TypeSymbol) semanticModel.symbol(array.memberTypeDesc()).get();
            handleQualifiedNameTypeReference(subscribeMessage, componentMessage, returnDescription,
                    array.memberTypeDesc(), isOptional);
//            if (optionalAPIResponses.isPresent()) {
//                ApiResponses responses = optionalAPIResponses.get();
//            updateResponseWithArraySchema(responses);
////            }
        } else {
            BalAsyncApi25SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            String type02 = array.memberTypeDesc().kind().toString().trim().split("_")[0].
                    toLowerCase(Locale.ENGLISH);
            BalAsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(type02);
            ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();
//            ObjectNode obj = objectMapper.valueToTree(asyncApiSchema);
            arraySchema.setItems(asyncApiSchema);

            setResponseOfRequest(subscribeMessage, componentMessage,
                    SIMPLE_RPC, returnDescription, objectMapper, arraySchema, isOptional);

        }
    }


}
