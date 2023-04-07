/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.Constants.AsyncAPIType;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.Arrays;
import java.util.Map;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LIST_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAPPING_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NIL_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NUMERIC_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPTIONAL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;

/**
 * This class processes mapping query parameters in between Ballerina and AsyncAPISpec.
 *
 * @since 2.0.0
 */
public class AsyncAPIQueryParameterMapper {
    private final AsyncApi25ComponentsImpl components;
    private final SemanticModel semanticModel;
    private final Map<String, String> apidocs;
    private final SyntaxKind[] validExpressionKind = {STRING_LITERAL, NUMERIC_LITERAL, BOOLEAN_LITERAL,
            LIST_CONSTRUCTOR, NIL_LITERAL, MAPPING_CONSTRUCTOR};

    public AsyncAPIQueryParameterMapper(Map<String, String> apidocs, AsyncApi25ComponentsImpl components,
                                        SemanticModel semanticModel) {
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }

    /**
     * Handle function query parameters for required parameters.
     */
    public void createQueryParameter(RequiredParameterNode queryParam, AsyncApi25SchemaImpl bindingObject) {
        String queryParamName = unescapeIdentifier(queryParam.paramName().get().text());
//        boolean isQuery = !queryParam.paramName().get().text().equals(Constants.PATH)
//                && queryParam.annotations().isEmpty();
        //TODO : Check wheather do we have to check isQuery?
        if (queryParam.typeName() instanceof BuiltinSimpleNameReferenceNode) {  //int offset
//            QueryParameter queryParameter = new QueryParameter();
//            AsyncApi25SchemaImpl queryParameterSchema=new AsyncApi25SchemaImpl();
//            String queryParamName= ConverterCommonUtils.unescapeIdentifier(queryParam.paramName().get().text());
            AsyncApi25SchemaImpl asyncApiQueryParamSchema =
                    ConverterCommonUtils.getAsyncApiSchema(queryParam.typeName().toString().trim());
//            queryParameter.setSchema(asyncApiSchema);
//            queryParameter.setRequired(true);
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);

//            return queryParameter;
        } else if (queryParam.typeName().kind() == OPTIONAL_TYPE_DESC) { //int? offset // int[]? pet
            // Handle optional query parameter

//            NodeList<AnnotationNode> annotations = getAnnotationNodesFromServiceNode(queryParam);
//            String isOptional = Constants.TRUE;
//            if (!annotations.isEmpty()) {
//                Optional<String> values = ConverterCommonUtils.extractServiceAnnotationDetails(annotations,
//                        "http:ServiceConfig", "treatNilableAsOptional");
//                if (values.isPresent()) {
//                    isOptional = values.get();
//                }
//            }
//            setOptionalQueryParameter(queryParamName, ((OptionalTypeDescriptorNode) queryParam.typeName()),
//                    isOptional);
            AsyncApi25SchemaImpl asyncApiQueryParamSchema = setOptionalQueryParameter(queryParamName,
                    ((OptionalTypeDescriptorNode) queryParam.typeName()));
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);

        } else if (queryParam.typeName().kind() == SyntaxKind.ARRAY_TYPE_DESC) { //string[] //string?[]
            // Handle required array type query parameter
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) queryParam.typeName();
            // string[] //string?[]
            AsyncApi25SchemaImpl asyncApiQueryParamSchema = handleArrayTypeQueryParameter(queryParamName, arrayNode);
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);
        } else if (queryParam.typeName() instanceof SimpleNameReferenceNode) { //HeartBeat
//            QueryParameter queryParameter = new QueryParameter();
//            queryParameter.setName(ConverterCommonUtils.unescapeIdentifier(queryParamName));
            SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) queryParam.typeName();
            AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);
            TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
            componentMapper.createComponentSchema(typeSymbol, null);
            AsyncApi25SchemaImpl schema = new AsyncApi25SchemaImpl();
            schema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(queryNode.name().text().trim()));
//            queryParameter.setSchema(schema);
            //TODO: check setRequired(true) can be achieve by extensions..
//            queryParameter.setRequired(true);
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                schema.setDescription(apidocs.get(queryParamName.trim()));
            }
//            return queryParameter;
            bindingObject.addProperty(queryParamName, schema);

            //TODO : Try below after figure out how to map map<json>? offset={"x":{"id":"sss"}} into asyncapi..
            // here setAdditionalProperties() have to give an asyncapischema ,not boolean value
        } else {
            AsyncApi25SchemaImpl schema = createContentTypeForMapJson(queryParamName, false);
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                schema.setDescription(apidocs.get(queryParamName.trim()));
            }
            bindingObject.addProperty(queryParamName, schema);
        }
    }

    /**
     * Create AsyncAPISpec query parameter for default query parameters.
     */
    public void createQueryParameter(DefaultableParameterNode defaultableQueryParam,
                                     AsyncApi25SchemaImpl bindingQueryObject) {

        String queryParamName = defaultableQueryParam.paramName().get().text();
//        boolean isQuery = !defaultableQueryParam.paramName().get().text().equals(Constants.PATH) &&
//                defaultableQueryParam.annotations().isEmpty();

        AsyncApi25SchemaImpl asyncApiQueryParamDefaultSchema = null;
//        QueryParameter queryParameter = new QueryParameter();
        if (defaultableQueryParam.typeName() instanceof BuiltinSimpleNameReferenceNode) {
//            queryParameter.setName(ConverterCommonUtils.unescapeIdentifier(queryParamName));

            asyncApiQueryParamDefaultSchema = ConverterCommonUtils.getAsyncApiSchema(
                    defaultableQueryParam.typeName().toString().trim());
//            queryParameter.setSchema(asyncApiSchema);
            if (!apidocs.isEmpty() && defaultableQueryParam.paramName().isPresent() &&
                    apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamDefaultSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
//            bindingObject.addProperty(queryParamName,asyncApiQueryDefaultSchema);

        } else if (defaultableQueryParam.typeName().kind() == OPTIONAL_TYPE_DESC) {
            // Handle optional query parameter
            asyncApiQueryParamDefaultSchema = setOptionalQueryParameter(queryParamName,
                    ((OptionalTypeDescriptorNode) defaultableQueryParam.typeName()));
//            bindingObject.addProperty(queryParamName,asyncApiQueryParamSchema);
        } else if (defaultableQueryParam.typeName() instanceof ArrayTypeDescriptorNode) {
            // Handle required array type query parameter
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) defaultableQueryParam.typeName();
            asyncApiQueryParamDefaultSchema = handleArrayTypeQueryParameter(queryParamName, arrayNode);
            //TODO : Try below after figure out how to map map<json>? offset={"x":{"id":"sss"}} into asyncapi..
            // here setAdditionalProperties() have to give an asyncapischema ,not boolean value
        } else {
            asyncApiQueryParamDefaultSchema = createContentTypeForMapJson(queryParamName, false);
            if (!apidocs.isEmpty() && defaultableQueryParam.paramName().isPresent() &&
                    apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamDefaultSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
        }

        if (Arrays.stream(validExpressionKind).anyMatch(syntaxKind -> syntaxKind ==
                defaultableQueryParam.expression().kind())) {
            String defaultValue = defaultableQueryParam.expression().toString().trim().
                    replaceAll("\"", "");
            if (defaultableQueryParam.expression().kind() == NIL_LITERAL) {
                defaultValue = null;
            }
//            if (queryParameter.getContent() != null) {
//                Content content = queryParameter.getContent();
//                for (Map.Entry<String, MediaType> stringMediaTypeEntry : content.entrySet()) {
//                    Schema schema = stringMediaTypeEntry.getValue().getSchema();
//                    schema.setDefault(defaultValue);
//                    io.swagger.v3.oas.models.media.MediaType media = new io.swagger.v3.oas.models.media.MediaType();
//                    media.setSchema(schema);
//                    content.addMediaType(stringMediaTypeEntry.getKey(), media);
//                }
//            else {
            if (asyncApiQueryParamDefaultSchema != null) {
                asyncApiQueryParamDefaultSchema.setDefault(new TextNode(defaultValue));
                bindingQueryObject.addProperty(queryParamName, asyncApiQueryParamDefaultSchema);

            }
//                Schema schema = queryParameter.getSchema();
//                schema.setDefault(defaultValue);
//                queryParameter.setSchema(schema);
//            }
        } else {
            bindingQueryObject.addProperty(queryParamName, asyncApiQueryParamDefaultSchema);
        }
    }

    /**
     * Handle array type query parameter.
     */
    private AsyncApi25SchemaImpl handleArrayTypeQueryParameter(String queryParamName,
                                                               ArrayTypeDescriptorNode arrayNode) {
//        QueryParameter queryParameter = new QueryParameter();
        AsyncApi25SchemaImpl arraySchema = new AsyncApi25SchemaImpl();
        arraySchema.setType(AsyncAPIType.ARRAY.toString());
//        queryParameter.setName(ConverterCommonUtils.unescapeIdentifier(queryParamName));
        TypeDescriptorNode itemTypeNode = arrayNode.memberTypeDesc();
        AsyncApi25SchemaImpl itemSchema;
        if (arrayNode.memberTypeDesc().kind() == OPTIONAL_TYPE_DESC) {
            itemSchema = ConverterCommonUtils.getAsyncApiSchema(
                    ((OptionalTypeDescriptorNode) itemTypeNode).typeDescriptor().toString().trim());
//            itemSchema.setNullable(true);
            itemSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);

        } else {
            itemSchema = ConverterCommonUtils.getAsyncApiSchema(itemTypeNode.toString().trim());
        }

        ObjectNode obj = ConverterCommonUtils.callObjectMapper().valueToTree(itemSchema);

        arraySchema.setItems(obj);

        //TODO : setRequired(true) , check this is in asyncapi schema
//        queryParameter.setRequired(true);

        if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
            arraySchema.setDescription(apidocs.get(queryParamName));
        }
        return arraySchema;
    }

    /**
     * Handle optional query parameter.
     */
    private AsyncApi25SchemaImpl setOptionalQueryParameter(String queryParamName, OptionalTypeDescriptorNode typeNode) {
        //TODO : If treatNilableAsOptional got fixed then add this also
//        QueryParameter queryParameter = new QueryParameter();
//        if (isOptional.equals(Constants.FALSE)) {
//            queryParameter.setRequired(true);
//        }
//        queryParameter.setName(ConverterCommonUtils.unescapeIdentifier(queryParamName));
        Node node = typeNode.typeDescriptor();
        if (node.kind() == SyntaxKind.ARRAY_TYPE_DESC) { //int[]? offset
//            ArraySchema arraySchema = new ArraySchema();
            AsyncApi25SchemaImpl arraySchema = ConverterCommonUtils.getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            arraySchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) node;
            TypeDescriptorNode itemTypeNode = arrayNode.memberTypeDesc();
            AsyncApi25SchemaImpl itemSchema = ConverterCommonUtils.getAsyncApiSchema(itemTypeNode.toString().trim());
            //TODO : Decide whether this will be another object , because of the field entity:true
            ObjectNode obj = ConverterCommonUtils.callObjectMapper().valueToTree(itemSchema);
            arraySchema.setItems(obj);
//            queryParameter.schema(arraySchema);
//            queryParameter.setName(ConverterCommonUtils.unescapeIdentifier(queryParamName));
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                arraySchema.setDescription(apidocs.get(queryParamName));
            }
            return arraySchema;
            //TODO : Try below after figure out how to map map<json>? offset={"x":{"id":"sss"}} into asyncapi..
            // here setAdditionalProperties() have to give an asyncapischema ,not boolean value
        } else if (node.kind() == SyntaxKind.MAP_TYPE_DESC) { //map<json>? offset={"x":{"id":"sss"}}
            AsyncApi25SchemaImpl mapJsonSchema = createContentTypeForMapJson(queryParamName, true);
            //TODO : If treatNilableAsOptional got fixed then add this also
//            if (isOptional.equals(Constants.FALSE)) {
//                queryParameter.setRequired(true);
//            }
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                mapJsonSchema.setDescription(apidocs.get(queryParamName));
            }
            return mapJsonSchema;
        } else { //int? offset
            AsyncApi25SchemaImpl asyncApiSchema = ConverterCommonUtils.getAsyncApiSchema(node.toString().trim());
            asyncApiSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);

//            asyncApiSchema.setNullable(true);

//            queryParameter.setSchema(asyncApiSchema);
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                asyncApiSchema.setDescription(apidocs.get(queryParamName));
            }
            return asyncApiSchema;
        }
    }

    private AsyncApi25SchemaImpl createContentTypeForMapJson(String queryParamName, boolean nullable) {

        AsyncApi25SchemaImpl objectSchema = ConverterCommonUtils.getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
        AsyncApi25SchemaImpl emptyObjectSchema = ConverterCommonUtils.getAsyncApiSchema(queryParamName);


//        String queryParamString=queryParam.typeName().toString().trim();
        //TODO
//        AsyncApi25SchemaImpl objectSchema=ConverterCommonUtils.getAsyncApiSchema(queryParamName);
        if (nullable) {
            objectSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
        }
        objectSchema.setAdditionalProperties(emptyObjectSchema);

        return objectSchema;

    }
}
