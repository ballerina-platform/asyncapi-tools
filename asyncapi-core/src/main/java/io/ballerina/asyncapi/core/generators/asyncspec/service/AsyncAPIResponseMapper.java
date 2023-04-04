package io.ballerina.asyncapi.core.generators.asyncspec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.diagnostics.Location;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.callObjectMapper;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.*;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_REFERENCE;


public class AsyncAPIResponseMapper {
    private final Location location;
    private final SemanticModel semanticModel;

    private final AsyncApi25ComponentsImpl components;

    private final AsyncAPIComponentMapper componentMapper;


    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();

    public AsyncAPIResponseMapper(Location location,AsyncAPIComponentMapper componentMapper, SemanticModel semanticModel,AsyncApi25ComponentsImpl components) {
        this.location = location;
        this.semanticModel = semanticModel;
        this.componentMapper=componentMapper;
        this.components=components;
    }


    public void createResponse( AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage, Node remoteReturnType, String returnDescription) {
        String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());
        ObjectMapper objectMapper=ConverterCommonUtils.callObjectMapper();


        switch (remoteReturnType.kind()) {
            case FLOAT_TYPE_DESC:
            case DECIMAL_TYPE_DESC:
            case INT_TYPE_DESC:
            case STRING_TYPE_DESC:
            case BOOLEAN_TYPE_DESC:
//                String type = remoteReturnType.toString().toLowerCase(Locale.ENGLISH).trim();
//                AsyncApi25SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(type);
//                if (media.isPresent()) {
//                    mediaType.setSchema(schema);
//                    apiResponse.description(description);
//                    apiResponse.content(new Content().addMediaType(media.get(), mediaType));
//                    apiResponses.put(statusCode, apiResponse);
//                    return Optional.of(apiResponses);
//                } else {
//                    return Optional.empty();
//                }
                AsyncApi25SchemaImpl remoteReturnSchema= getAsyncApiSchema(remoteReturnTypeString);
                setResponseOfRequest(subscribeMessage,componentMessage,SIMPLE_RPC,returnDescription,objectMapper,remoteReturnSchema);
                break;
//                setSubscribeResponse(subscribeMessage, componentMessage, remoteReturnTypeString, SIMPLE_RPC, returnDescription);

                //TODO : Edit this after figure out how to do this
                //FIXME : It may be an object in asyncapi but not sue
            case JSON_TYPE_DESC:
            case XML_TYPE_DESC:
                AsyncApi25SchemaImpl jsonSchema= getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                //TODO : Change this into true after Apicurio team change additionalProperties type into Object type
                AsyncApi25SchemaImpl additionalPropertyObject=new AsyncApi25SchemaImpl();
                jsonSchema.setAdditionalProperties(additionalPropertyObject);

                //TODO : Set this schema into main asyncapi doc
                setResponseOfRequest(subscribeMessage,componentMessage,SIMPLE_RPC,returnDescription,objectMapper,jsonSchema);
                break;
//
//                apiResponse.content(new Content().addMediaType(mediaTypeString, mediaType));
//                apiResponse.description(description);
//                apiResponses.put(statusCode, apiResponse);
//                return Optional.of(apiResponses);
//                setCacheHeader(headers, apiResponse, statusCode);
//                mediaType.setSchema(new ObjectSchema());
//                mediaTypeString = customMediaPrefix.map(s -> APPLICATION_PREFIX + s + XML_POSTFIX)
//                        .orElse(MediaType.APPLICATION_XML);
//                apiResponse.content(new Content().addMediaType(mediaTypeString, mediaType));
//                apiResponse.description(description);
//                apiResponses.put(statusCode, apiResponse);
//                return Optional.of(apiResponses);
            case SIMPLE_NAME_REFERENCE:
                SimpleNameReferenceNode recordNode = (SimpleNameReferenceNode) remoteReturnType;
                handleReferenceResponse(subscribeMessage,componentMessage,recordNode,returnDescription);
                break;

            case UNION_TYPE_DESC:
                mapUnionReturns(subscribeMessage,componentMessage,(UnionTypeDescriptorNode) remoteReturnType,returnDescription);
                break;
            case RECORD_TYPE_DESC:
                //TODO : Problem in mapinline record return
                mapInlineRecordInReturn(subscribeMessage,componentMessage,(RecordTypeDescriptorNode) remoteReturnType,returnDescription);
                break;
            case ARRAY_TYPE_DESC:

                getApiResponsesForArrayTypes(subscribeMessage,componentMessage,(ArrayTypeDescriptorNode) remoteReturnType,returnDescription);
                break;
//            case ERROR_TYPE_DESC:
//                // Return type is given as error or error? in the ballerina it will generate 500 response.
//                apiResponse.description(HTTP_500_DESCRIPTION);
//                mediaType.setSchema(new StringSchema());
//                apiResponse.content(new Content().addMediaType(MediaType.TEXT_PLAIN, mediaType));
//                apiResponses.put(HTTP_500, apiResponse);
//                return Optional.of(apiResponses);
            case OPTIONAL_TYPE_DESC:
                createResponse(subscribeMessage,componentMessage,((OptionalTypeDescriptorNode) remoteReturnType).typeDescriptor(),returnDescription);
                break;
            case MAP_TYPE_DESC:
                MapTypeDescriptorNode mapNode = (MapTypeDescriptorNode) remoteReturnType;
                AsyncApi25SchemaImpl objectSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());

                //TODO : I changed this kind to string
                AsyncApi25SchemaImpl apiSchema = getAsyncApiSchema(mapNode.mapTypeParamsNode().typeNode().kind().toString());
                objectSchema.setAdditionalProperties(apiSchema);
                setResponseOfRequest(subscribeMessage,componentMessage,SIMPLE_RPC,returnDescription,objectMapper,objectSchema);
                break;
//                return Optional.of(apiResponses);
            case STREAM_TYPE_DESC:
                if(((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().isPresent()) {
                    String remoteReturnStream = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode().toString().trim();

                    AsyncApi25SchemaImpl remoteReturnStreamSchema= getAsyncApiSchema(remoteReturnStream);
                    setResponseOfRequest(subscribeMessage,componentMessage,remoteReturnStream,returnDescription,objectMapper,remoteReturnStreamSchema);
//                    setSubscribeResponse(subscribeMessage, componentMessage, remoteReturnstreamTypeString, STREAMING, returnDescription);
                }else {
                    throw new NoSuchElementException(NO_TYPE_IN_STREAM);
                }
                break;

            case QUALIFIED_NAME_REFERENCE:
                TypeSymbol qualifiedNameReferenceSymbol=(TypeSymbol) semanticModel.symbol(remoteReturnType).get();

                handleQualifiedNameTypeReference(subscribeMessage,componentMessage,returnDescription,qualifiedNameReferenceSymbol);





//                handleReferenceResponse(subscribeMessage,componentMessage,reco,returnDescription);
//                break;
                break;

            default:
                DiagnosticMessages errorMessage = DiagnosticMessages.AAS_CONVERTOR_108;
                IncompatibleRemoteDiagnostic error = new IncompatibleRemoteDiagnostic(errorMessage, location,
                        remoteReturnType.kind().toString());
                errors.add(error);
                break;

        }
//
//        if (remoteReturnType.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
//            TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(remoteReturnType).orElseThrow();
//            String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());
//            //Creating return type message reference
//            AsyncApi25MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage, remoteReturnTypeName, returnTypeSymbol, subscribeOneOf, dispatcherValue, null);
//            components.addMessage(remoteReturnTypeName, componentReturnMessage);
//
//            ObjectNode messageRefObject = new ObjectNode(JsonNodeFactory.instance);
//            messageRefObject.put($REF, MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
//            if (returnDescription !=null) {
//                messageRefObject.put(DESCRIPTION, returnDescription);
//            }
//            componentMessage.addExtension(X_RESPONSE, messageRefObject);
//            componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
//        } else if (remoteReturnType.kind().equals(SyntaxKind.STREAM_TYPE_DESC)) {
//            String remoteReturnstreamTypeString = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode().toString().trim();
//
//            setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnstreamTypeString, STREAMING, returnDescription);
//
//        } else if (remoteReturnType instanceof BuiltinSimpleNameReferenceNode) {
//            String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());
//
//            setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnTypeString, SIMPLE_RPC, returnDescription);
//        }
    }
//    private void setSubscribeResponse(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage,  String type, String responseType,String returnDescription) {
//        ObjectMapper objMapper=ConverterCommonUtils.callObjectMapper();
//        AsyncApi25SchemaImpl schema= getAsyncApiSchema(type);
//        setResponseOfRequest(subscribeMessage, componentMessage, responseType, returnDescription, objMapper, schema);
//    }


    private void handleQualifiedNameTypeReference(AsyncApi25MessageImpl subscribeMessage,AsyncApi25MessageImpl componentMessage,String returnDescription,TypeSymbol qualifiedNameReferenceSymbol){
//        Symbol symbol=semanticModel.symbol(qualifiedNameReferenceNode).get();
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
            if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
                handleRecordTypeSymbol(subscribeMessage,componentMessage,returnDescription,qualifiedNameReferenceSymbol,qualifiedNameReferenceSymbol.getName().get());
//                ApiResponses responses = handleRecordTypeSymbo(qNode.identifier().text().trim(),
//                        components.getSchemas(), customMediaPrefix, typeRef, new OpenAPIComponentMapper(components),
//                        headers);
//                apiResponses.putAll(responses);
//                return Optional.of(apiResponses);
            }
        }
    }
    private void setResponseOfRequest(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage, String responseType, String returnDescription, ObjectMapper objMapper, AsyncApi25SchemaImpl schema) {
        //create new oneOf message
        AsyncApi25MessageImpl subscribeOneOf = new AsyncApi25MessageImpl();

        //set payload of the oneOf message(but this is not a reference)
        subscribeOneOf.setPayload(objMapper.valueToTree(schema));

        //set oneOf message into Subscribe channels
        subscribeMessage.addOneOf(subscribeOneOf);

        //create message response with its description
        ObjectNode payloadObject= new ObjectNode(JsonNodeFactory.instance);
        payloadObject.set(PAYLOAD,objMapper.valueToTree(schema));
        if (returnDescription!=null) {
            payloadObject.put(DESCRIPTION,returnDescription);
        }

        //set message response as x-response for the request
        componentMessage.addExtension(X_RESPONSE,payloadObject);
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));
    }

    public AsyncApi25MessageImpl extractMessageSchemaReference(AsyncApi25MessageImpl message, String typeName, TypeSymbol typeSymbol,String dispatcherValue,String paramDescription) {

        AsyncApi25MessageImpl messageType = new AsyncApi25MessageImpl();

        //create Schema
        componentMapper.createComponentSchema( typeSymbol,dispatcherValue);

        //create SchemaReference
        ObjectNode objNode1=new ObjectNode(JsonNodeFactory.instance);
        objNode1.put($REF,SCHEMA_REFERENCE+ ConverterCommonUtils.unescapeIdentifier(typeName));

        //Set description
        if(paramDescription!=null) {
            objNode1.put(DESCRIPTION, paramDescription);
        }

        //create Message
        AsyncApi25MessageImpl componentMessage= (AsyncApi25MessageImpl) components.createMessage();
        componentMessage.setPayload(objNode1);

        //create MessageReference
        messageType.set$ref(MESSAGE_REFERENCE+ ConverterCommonUtils.unescapeIdentifier(typeName));
        message.addOneOf(messageType);

        return componentMessage;
    }

    private void handleReferenceResponse( AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage,SimpleNameReferenceNode referenceNode,String returnDescription) {
        TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(referenceNode).orElseThrow();
        String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(referenceNode.name().toString().trim());

        if (referenceNode.parent().kind().equals(ARRAY_TYPE_DESC)) {

            //create Schema
            componentMapper.createComponentSchema(returnTypeSymbol,null);
            errors.addAll(componentMapper.getDiagnostics());

            //create Message (there is no message reference)
            ObjectMapper objMapper=ConverterCommonUtils.callObjectMapper();
            ObjectNode refObjNode= new ObjectNode(JsonNodeFactory.instance);

            //Create schema reference
            refObjNode.put($REF, SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
            AsyncApi25SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            arraySchema.setItems(objMapper.valueToTree(refObjNode));
            setResponseOfRequest(subscribeMessage, componentMessage, SIMPLE_RPC, returnDescription, objMapper, arraySchema);


        } else if (returnTypeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) returnTypeSymbol;
            TypeSymbol referredTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();


//            if (referredTypeSymbol.typeKind() == TypeDescKind.RECORD) {
//                componentMapper.createComponentSchema(referredTypeSymbol, null);
            handleRecordTypeSymbol(subscribeMessage, componentMessage, returnDescription, returnTypeSymbol, remoteReturnTypeName);


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
            //TODO : Uncomment if there is type reference
//            } else {
//                createResponseForTypeReferenceTypeReturns(remoteReturnTypeName,typeReferenceTypeSymbol, componentMapper);
//            }
        }
        //Check content and status code if it is in 200 range then add the header
//        operationAdaptor.getOperation().setResponses(apiResponses);
    }

    private void handleRecordTypeSymbol(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage, String returnDescription, TypeSymbol returnTypeSymbol, String remoteReturnTypeName) {
        //Creating return type message reference
        AsyncApi25MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage, remoteReturnTypeName, returnTypeSymbol, null, null);

        //set message reference as a x-response of a request
        ObjectNode messageRefObject = new ObjectNode(JsonNodeFactory.instance);
        messageRefObject.put($REF, MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));

        //Set return description
        if (returnDescription !=null) {
            messageRefObject.put(DESCRIPTION, returnDescription);
        }
        //Set x-response and x-response type of the request
        componentMessage.addExtension(X_RESPONSE, messageRefObject);
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));

        //add created return message
        components.addMessage(remoteReturnTypeName, componentReturnMessage);


    }

    /**
     * Create API responses when return type is type reference.
     */
    private void createResponseForTypeReferenceTypeReturns(String referenceName,TypeReferenceTypeSymbol typeReferenceTypeSymbol,AsyncAPIComponentMapper componentMapper) {

        TypeSymbol referredTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();
        TypeDescKind typeDescKind = referredTypeSymbol.typeKind();


        if (referredTypeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            List<TypeSymbol> typeSymbols = ((IntersectionTypeSymbol)referredTypeSymbol).memberTypeDescriptors();
            for (TypeSymbol symbol : typeSymbols) {
                if (!(symbol instanceof ReadonlyTypeSymbol)) {
                    referredTypeSymbol = symbol;
                    break;
                }
            }
            typeDescKind = referredTypeSymbol.typeKind();
        }

//        if (typeDescKind == TypeDescKind.UNION) {
//            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) referredTypeSymbol;
//            createResponsesForErrorsInUnion(unionTypeSymbol).ifPresent(apiResponses::putAll);
//        }

        componentMapper.createComponentSchema(typeReferenceTypeSymbol,null);
        errors.addAll(componentMapper.getDiagnostics());
//        media.setSchema(new Schema<>().$ref(ConverterCommonUtils.unescapeIdentifier(referenceName)));

//        ImmutablePair<String, String> mediaTypePair = getMediaTypeForTypeReferenceTypeReturns(referredTypeSymbol,
//                typeDescKind, customMediaPrefix);

//        String mediaTypeString = mediaTypePair.getLeft();
//        if (customMediaPrefix.isPresent()) {
//            mediaTypeString = APPLICATION_PREFIX + customMediaPrefix.get() + mediaTypePair.getRight();
//        }

//        ApiResponse apiResponse = new ApiResponse();
//        setCacheHeader(headers, apiResponse, statusCode);
//        apiResponse.content(new Content().addMediaType(mediaTypeString, media));
//        apiResponse.description(description);
//        apiResponses.put(statusCode, apiResponse);

//        return apiResponses;
    }

//    /**
//     * Create responses when http errors are present in the union type.
//     *
//     * @param unionTypeSymbol union type symbol
//     * @param headers         headers
//     * @return api responses
//     */
//    public Optional<ApiResponses> createResponsesForErrorsInUnion(UnionTypeSymbol unionTypeSymbol) {
//
//        for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
//            memberTypeDescriptor.getName().ifPresent(name -> {
//                if (HTTP_CODES.containsKey(name)) {
//                    ApiResponse apiResponse = new ApiResponse();
//                    Optional<String> code = generateApiResponseCode(name);
//                    apiResponse.description(name);
//                    setCacheHeader(headers, apiResponse, code.get());
//                    apiResponses.put(code.get(), apiResponse);
//                }
//            });
//        }
//        return apiResponses.isEmpty() ? Optional.empty() : Optional.of(apiResponses);
//    }


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
    private void mapUnionReturns(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage,UnionTypeDescriptorNode typeNode,String returnDescription) {

        TypeDescriptorNode rightNode = typeNode.rightTypeDesc();
        TypeDescriptorNode leftNode = typeNode.leftTypeDesc();
        // Handle leftNode because it is main node
         createResponse(subscribeMessage,componentMessage,leftNode,returnDescription);
        // Handle rest of the union type
        if (rightNode instanceof UnionTypeDescriptorNode) {
            UnionTypeDescriptorNode traversRightNode = (UnionTypeDescriptorNode) rightNode;
            while (traversRightNode.rightTypeDesc() != null) {
                if (leftNode.kind() == QUALIFIED_NAME_REFERENCE) {
                    leftNode = ((UnionTypeDescriptorNode) rightNode).leftTypeDesc();
                     createResponse( subscribeMessage,componentMessage,leftNode,returnDescription);
                }
            }
        } else {
            createResponse( subscribeMessage,componentMessage,rightNode,returnDescription);
        }
    }


    /**
     * Handle response has inline record as return type.
     */
    private void mapInlineRecordInReturn(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage,RecordTypeDescriptorNode typeNode,String returnDescription) {

        NodeList<Node> fields = typeNode.fields();
        boolean ishttpTypeInclusion = false;

//        Map<String,AsyncApi25SchemaImpl> properties = new HashMap<>();
        AsyncApi25SchemaImpl inlineSchema = new AsyncApi25SchemaImpl();
        inlineSchema.setType(AsyncAPIType.OBJECT.toString());
        if (fields.stream().anyMatch(module -> module.kind() == TYPE_REFERENCE)) {
            ishttpTypeInclusion = true;
        }

        // 1- scenarios returns record {| int id; string body;|} - done
        // 2- scenarios returns record {| int id; Person body;|} - done

        for (Node field : fields) {

            //TODO : Have to implement type inclusion part
//            if (field.kind() == TYPE_REFERENCE) {
//                TypeReferenceNode typeFieldNode = (TypeReferenceNode) field;
//                if (typeFieldNode.typeName().kind() == SIMPLE_NAME_REFERENCE) {
//                    SimpleNameReferenceNode simpleNameReferenceNode=(SimpleNameReferenceNode) typeFieldNode.typeName();
//                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(simpleNameReferenceNode).orElseThrow();
//                    componentMapper.createComponentSchema(typeSymbol,null);
////                    QualifiedNameReferenceNode identifierNode = (QualifiedNameReferenceNode) typeFieldNode.typeName();
//
//////                    httpCode = generateApiResponseCode(identifierNode.identifier().text().trim());
////                    description = identifierNode.identifier().text().trim();
////                    apiResponse.description(identifierNode.identifier().text().trim());
//                }
//            }
            if (field.kind() == RECORD_FIELD) {
                RecordFieldNode recordField = (RecordFieldNode) field;
                Node type01 = recordField.typeName();
//        TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(typeNode).orElseThrow();

//        componentMapper.generateObjectSchemaFromRecordFields(null,((RecordTypeSymbol)typeSymbol).fieldDescriptors(),null,null);
                if (recordField.typeName().kind() == SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode nameRefNode = (SimpleNameReferenceNode) type01;
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(nameRefNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol,null);
////                    handleReferenceResponse(subscribeMessage,componentMessage,nameRefNode,returnDescription);
                    AsyncApi25SchemaImpl referenceSchema = new AsyncApi25SchemaImpl();
//
                    referenceSchema.set$ref(SCHEMA_REFERENCE+ConverterCommonUtils.unescapeIdentifier(
                            recordField.typeName().toString().trim()));
                    inlineSchema.addProperty(recordField.fieldName().text(), referenceSchema);
                } else {
//                    //TODO array fields handling
////                    mediaTypeResponse = convertBallerinaMIMEToOASMIMETypes(recordField.typeName().toString().trim(),
////                            customMediaPrefix);
                    AsyncApi25SchemaImpl propertySchema = ConverterCommonUtils.getAsyncApiSchema(
                            recordField.typeName().toString().trim());
                    inlineSchema.addProperty(recordField.fieldName().text(), propertySchema);
                }
            }
        }
        setResponseOfRequest(subscribeMessage,componentMessage,SIMPLE_RPC,returnDescription,callObjectMapper(),inlineSchema);

//        if (!ishttpTypeInclusion) {
//            inlineSchema = new ObjectSchema();
//            inlineSchema.setProperties(properties);
//        }
//        if (mediaTypeResponse.isPresent() && httpCode.isPresent()) {
//            apiResponse.content(new Content().addMediaType(mediaTypeResponse.get(), mediaType));
//            setCacheHeader(headers, apiResponse, httpCode.get());
//            apiResponses.put(httpCode.get(), apiResponse);
//            return Optional.of(apiResponses);
//        } else {
//            return Optional.empty();
//        }
    }

    /**
     * Handle return has array types.
     */
    private void getApiResponsesForArrayTypes(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage,ArrayTypeDescriptorNode array,String returnDescription) {


        if (array.memberTypeDesc().kind() == SIMPLE_NAME_REFERENCE) {
            handleReferenceResponse(subscribeMessage, componentMessage, (SimpleNameReferenceNode) array.memberTypeDesc(), returnDescription);

        //TODO : Not sure yet...  is this like http:request []
//        } else if (array.memberTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
//            Optional<ApiResponses> optionalAPIResponses =
//                    handleQualifiedNameType((QualifiedNameReferenceNode) array.memberTypeDesc());
//            if (optionalAPIResponses.isPresent()) {
////                ApiResponses responses = optionalAPIResponses.get();
//                updateResponseWithArraySchema(responses);
//                apiResponses.putAll(responses);
//            }
        } else {
            AsyncApi25SchemaImpl arraySchema = getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            String type02 = array.memberTypeDesc().kind().toString().trim().split("_")[0].
                    toLowerCase(Locale.ENGLISH);
            AsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(type02);
//            Optional<String> mimeType = convertBallerinaMIMEToOASMIMETypes(type02, customMediaPrefix);
//            if (mimeType.isEmpty()) {
//                return Optional.empty();
//            }
            ObjectMapper objectMapper= ConverterCommonUtils.callObjectMapper();
            ObjectNode obj=objectMapper.valueToTree(asyncApiSchema);
            arraySchema.setItems(obj);
//            mediaType.setSchema(arraySchema);
            setResponseOfRequest(subscribeMessage,componentMessage,SIMPLE_RPC,returnDescription,objectMapper,arraySchema);

        }
    }



}
