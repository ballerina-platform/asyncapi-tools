package io.ballerina.asyncapi.core.generators.asyncspec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleResourceDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.diagnostics.Location;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SIMPLE_RPC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;

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


    public void createResponse(String dispatcherValue, AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage, Node remoteReturnType, String returnDescription) {
        AsyncApi25MessageImpl subscribeOneOf = new AsyncApi25MessageImpl();

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
                String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());
                setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnTypeString, SIMPLE_RPC, returnDescription);

                //TODO : Edit this after figure out how to do this
                //FIXME : It may be an object in asyncapi but not sue
            case JSON_TYPE_DESC:
            case XML_TYPE_DESC:
                AsyncApi25SchemaImpl jsonSchema= new AsyncApi25SchemaImpl();
                jsonSchema.setType(AsyncAPIType.OBJECT.toString());
                //TODO : Change this into true after Apicurio team change additionalProperties type into Object type
                AsyncApi25SchemaImpl additionalPropertyObject=new AsyncApi25SchemaImpl();
                additionalPropertyObject.setType(AsyncAPIType.OBJECT.toString());
                jsonSchema.setAdditionalProperties(additionalPropertyObject);
                //TODO : Set this schema into main asyncapi doc
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
                handleReferenceResponse( recordNode,subscribeMessage,subscribeOneOf,returnDescription,componentMessage,dispatcherValue);


            case UNION_TYPE_DESC:
                return mapUnionReturns(operationAdaptor,
                        (UnionTypeDescriptorNode) typeNode, customMediaPrefix, headers);
            case RECORD_TYPE_DESC:
                return mapInlineRecordInReturn(operationAdaptor, apiResponses,
                        (RecordTypeDescriptorNode) typeNode, apiResponse, mediaType, customMediaPrefix, headers);
            case ARRAY_TYPE_DESC:
                return getApiResponsesForArrayTypes(operationAdaptor, apiResponses,
                        (ArrayTypeDescriptorNode) typeNode, apiResponse, mediaType, customMediaPrefix, headers);
            case ERROR_TYPE_DESC:
                // Return type is given as error or error? in the ballerina it will generate 500 response.
                apiResponse.description(HTTP_500_DESCRIPTION);
                mediaType.setSchema(new StringSchema());
                apiResponse.content(new Content().addMediaType(MediaType.TEXT_PLAIN, mediaType));
                apiResponses.put(HTTP_500, apiResponse);
                return Optional.of(apiResponses);
            case OPTIONAL_TYPE_DESC:
                return getAPIResponses(operationAdaptor,
                        ((OptionalTypeDescriptorNode) typeNode).typeDescriptor(), customMediaPrefix, headers);
            case MAP_TYPE_DESC:
                setCacheHeader(headers, apiResponse, statusCode);
                MapTypeDescriptorNode mapNode = (MapTypeDescriptorNode) typeNode;
                ObjectSchema objectSchema = new ObjectSchema();
                Schema<?> apiSchema = getOpenApiSchema(mapNode.mapTypeParamsNode().typeNode().kind());
                objectSchema.additionalProperties(apiSchema);
                mediaType.setSchema(objectSchema);
                mediaTypeString = customMediaPrefix.map(s -> APPLICATION_PREFIX + s + JSON_POSTFIX)
                        .orElse(APPLICATION_JSON);
                apiResponse.content(new Content().addMediaType(mediaTypeString, mediaType));
                apiResponse.description(description);
                apiResponses.put(statusCode, apiResponse);
                return Optional.of(apiResponses);
            case STREAM_TYPE_DESC:
                String remoteReturnstreamTypeString = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode().toString().trim();

                setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnstreamTypeString, STREAMING, returnDescription);

            default:
                DiagnosticMessages errorMessage = DiagnosticMessages.OAS_CONVERTOR_101;
                IncompatibleResourceDiagnostic error = new IncompatibleResourceDiagnostic(errorMessage, remoteReturnType.location(),
                        remoteReturnType.kind().toString());
                errors.add(error);
                return Optional.empty();
        }



        if (remoteReturnType.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
            TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(remoteReturnType).orElseThrow();
            String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());
            //Creating return type message reference
            AsyncApi25MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage, remoteReturnTypeName, returnTypeSymbol, subscribeOneOf, dispatcherValue, null);
            components.addMessage(remoteReturnTypeName, componentReturnMessage);

            ObjectNode messageRefObject = new ObjectNode(JsonNodeFactory.instance);
            messageRefObject.put($REF, MESSAGE_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
            if (returnDescription !=null) {
                messageRefObject.put(DESCRIPTION, returnDescription);
            }
            componentMessage.addExtension(X_RESPONSE, messageRefObject);
            componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
        } else if (remoteReturnType.kind().equals(SyntaxKind.STREAM_TYPE_DESC)) {
            String remoteReturnstreamTypeString = ((StreamTypeParamsNode) ((StreamTypeDescriptorNode) remoteReturnType).streamTypeParamsNode().get()).leftTypeDescNode().toString().trim();

            setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnstreamTypeString, STREAMING, returnDescription);

        } else if (remoteReturnType instanceof BuiltinSimpleNameReferenceNode) {
            String remoteReturnTypeString = ConverterCommonUtils.unescapeIdentifier(remoteReturnType.toString());

            setSubscribeResponse(subscribeMessage, componentMessage, subscribeOneOf, remoteReturnTypeString, SIMPLE_RPC, returnDescription);
        }
    }
    private void setSubscribeResponse(AsyncApi25MessageImpl subscribeMessage, AsyncApi25MessageImpl componentMessage, AsyncApi25MessageImpl subscribeOneOf, String type, String responseType,String returnDescription) {
        ObjectMapper objMapper=ConverterCommonUtils.callObjectMapper();
        AsyncApi25SchemaImpl schema= ConverterCommonUtils.getAsyncApiSchema(type);
        subscribeOneOf.setPayload(objMapper.valueToTree(schema) );
        subscribeMessage.addOneOf(subscribeOneOf);
        ObjectNode payloadObject= new ObjectNode(JsonNodeFactory.instance);
        payloadObject.put(PAYLOAD,objMapper.valueToTree(schema) );
        if (returnDescription!=null) {
            payloadObject.put(DESCRIPTION,returnDescription);
        }

        componentMessage.addExtension(X_RESPONSE,payloadObject);
        componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(responseType));
    }
    private AsyncApi25MessageImpl extractMessageSchemaReference(AsyncApi25MessageImpl message, String typeName, TypeSymbol typeSymbol, AsyncApi25MessageImpl messageType,String dispatcherValue,String paramDescription) {

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

    private void handleReferenceResponse( SimpleNameReferenceNode referenceNode,AsyncApi25MessageImpl subscribeMessage,AsyncApi25MessageImpl subscribeOneOf,String returnDescription, AsyncApi25MessageImpl componentMessage,String dispatcherValue) {
        TypeSymbol returnTypeSymbol = (TypeSymbol) semanticModel.symbol(referenceNode).orElseThrow();
        String remoteReturnTypeName = ConverterCommonUtils.unescapeIdentifier(referenceNode.name().toString().trim());

        //Creating return type message reference
        AsyncApi25MessageImpl componentReturnMessage = extractMessageSchemaReference(subscribeMessage, remoteReturnTypeName, returnTypeSymbol, subscribeOneOf, dispatcherValue, null);

        //add created return message
        components.addMessage(remoteReturnTypeName, componentReturnMessage);

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


        //handle record for components
        AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);

        // Check typeInclusion is related to the http status code
        if (referenceNode.parent().kind().equals(ARRAY_TYPE_DESC)) {


            //create Schema
            componentMapper.createComponentSchema(returnTypeSymbol,dispatcherValue);
            errors.addAll(componentMapper.getDiagnostics());

            //create Message (there is no message reference)
            ObjectMapper objMapper=ConverterCommonUtils.callObjectMapper();
            ObjectNode refObjNode= new ObjectNode(JsonNodeFactory.instance);

            //Create schema reference
            refObjNode.put($REF, SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(remoteReturnTypeName));
            AsyncApi25SchemaImpl arraySchema = ConverterCommonUtils.getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            arraySchema.setItems(objMapper.valueToTree(refObjNode));
            subscribeOneOf.setPayload(objMapper.valueToTree(arraySchema));
            subscribeMessage.addOneOf(subscribeOneOf);


            //set message as a x-response of a request
            ObjectNode payloadObject= new ObjectNode(JsonNodeFactory.instance);
            payloadObject.put(PAYLOAD,objMapper.valueToTree(arraySchema) );

            //set return description
            if (returnDescription!=null) {
                payloadObject.put(DESCRIPTION,returnDescription);
            }

            componentMessage.addExtension(X_RESPONSE,payloadObject);
            componentMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));


        } else if (returnTypeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) returnTypeSymbol;
            TypeSymbol referredTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();
//            String referenceName = referenceNode.name().toString().trim();
            String referredTypeName = referredTypeSymbol.getName().isPresent() ?
                    referredTypeSymbol.getName().get() : "";

            if (referredTypeSymbol.typeKind() == TypeDescKind.RECORD) {
                componentMapper.createComponentSchema(referredTypeSymbol, dispatcherValue);

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
            } else {
                createResponseForTypeReferenceTypeReturns(remoteReturnTypeName,typeReferenceTypeSymbol, componentMapper);
            }
        }
        //Check content and status code if it is in 200 range then add the header
//        operationAdaptor.getOperation().setResponses(apiResponses);
    }

    /**
     * Create API responses when return type is type reference.
     */
    private void createResponseForTypeReferenceTypeReturns(String referenceName,TypeReferenceTypeSymbol typeReferenceTypeSymbol,AsyncAPIComponentMapper componentMapper) {

        TypeSymbol referredTypeSymbol = typeReferenceTypeSymbol.typeDescriptor();
        TypeDescKind typeDescKind = referredTypeSymbol.typeKind();


        if (referredTypeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            referredTypeSymbol = componentMapper.excludeReadonlyIfPresent(referredTypeSymbol);
            typeDescKind = referredTypeSymbol.typeKind();
        }

        if (typeDescKind == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) referredTypeSymbol;
            createResponsesForErrorsInUnion(unionTypeSymbol, headers).ifPresent(apiResponses::putAll);
        }

        componentMapper.createComponentSchema(schema, typeReferenceTypeSymbol);
        errors.addAll(componentMapper.getDiagnostics());
        media.setSchema(new Schema<>().$ref(ConverterCommonUtils.unescapeIdentifier(referenceName)));

        ImmutablePair<String, String> mediaTypePair = getMediaTypeForTypeReferenceTypeReturns(referredTypeSymbol,
                typeDescKind, customMediaPrefix);

        String mediaTypeString = mediaTypePair.getLeft();
        if (customMediaPrefix.isPresent()) {
            mediaTypeString = APPLICATION_PREFIX + customMediaPrefix.get() + mediaTypePair.getRight();
        }

        ApiResponse apiResponse = new ApiResponse();
        setCacheHeader(headers, apiResponse, statusCode);
        apiResponse.content(new Content().addMediaType(mediaTypeString, media));
        apiResponse.description(description);
        apiResponses.put(statusCode, apiResponse);

        return apiResponses;
    }



}
