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

import io.apicurio.datamodels.models.Components;
import io.apicurio.datamodels.models.asyncapi.v25.*;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.openapi.converter.Constants;
import io.ballerina.openapi.converter.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.converter.diagnostic.IncompatibleResourceDiagnostic;
import io.ballerina.openapi.converter.diagnostic.OpenAPIConverterDiagnostic;
import io.ballerina.openapi.converter.utils.ConverterCommonUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.*;

import static io.ballerina.openapi.converter.Constants.*;
import static io.ballerina.openapi.converter.utils.ConverterCommonUtils.extractCustomMediaType;

/**
 * AsyncAPIParameterMapper provides functionality for converting ballerina parameter to OAS parameter model.
 */
public class AsyncAPIParameterMapper {
    private final FunctionDefinitionNode functionDefinitionNode;
    private final Map<String, String> apidocs;
    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();
    private final Components components;
    private final SemanticModel semanticModel;

    public List<AsyncAPIConverterDiagnostic> getErrors() {
        return errors;
    }

    public AsyncAPIParameterMapper(FunctionDefinitionNode functionDefinitionNode, Map<String, String> apidocs,
                                   Components components, SemanticModel semanticModel) {

        this.functionDefinitionNode = functionDefinitionNode;
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }



    /**
     * Create {@code Parameters} model for openAPI operation.
     */
    public void getResourceInputs(Components components, SemanticModel semanticModel) {
        AsyncApi25ParametersImpl parameters = new AsyncApi25ParametersImpl();
        //Set path parameters
        NodeList<Node> pathParams = functionDefinitionNode.relativeResourcePath();
        createPathParameters(parameters, pathParams);
//        // Set query parameters, headers and requestBody
//        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
//        SeparatedNodeList<ParameterNode> parameterList = functionSignature.parameters();
//        for (ParameterNode parameterNode : parameterList) {
//            AsyncAPIQueryParameterMapper queryParameterMapper = new AsyncAPIQueryParameterMapper(apidocs, components,
//                    semanticModel);
//            if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
//                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
//                // Handle query parameter
//                if (requiredParameterNode.typeName().kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
//                    QualifiedNameReferenceNode referenceNode =
//                            (QualifiedNameReferenceNode) requiredParameterNode.typeName();
//                    String typeName = (referenceNode).modulePrefix().text() + ":" + (referenceNode).identifier().text();
//                    if (typeName.equals(HTTP_REQUEST) &&
//                            (Constants.GET.equalsIgnoreCase(operationAdaptor.getHttpOperation()))) {
//                        DiagnosticMessages errorMessage = DiagnosticMessages.OAS_CONVERTOR_113;
//                        IncompatibleResourceDiagnostic error = new IncompatibleResourceDiagnostic(errorMessage,
//                                referenceNode.location());
//                        errors.add(error);
//                    } else if (typeName.equals(HTTP_REQUEST)) {
//                        RequestBody requestBody = new RequestBody();
//                        MediaType mediaType = new MediaType();
//                        mediaType.setSchema(new Schema<>().description(WILD_CARD_SUMMARY));
//                        requestBody.setContent(new Content().addMediaType(WILD_CARD_CONTENT_KEY, mediaType));
//                        operationAdaptor.getOperation().setRequestBody(requestBody);
//                    }
//                }
//                if (requiredParameterNode.typeName().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE &&
//                        requiredParameterNode.annotations().isEmpty()) {
//                    parameters.add(queryParameterMapper.createQueryParameter(requiredParameterNode));
//                }
//                // Handle header, payload parameter
//                if (requiredParameterNode.typeName() instanceof TypeDescriptorNode &&
//                        !requiredParameterNode.annotations().isEmpty()) {
//                    handleAnnotationParameters(components, semanticModel, parameters, requiredParameterNode);
//                }
//            } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
//                DefaultableParameterNode defaultableParameterNode = (DefaultableParameterNode) parameterNode;
//                // Handle header parameter
//                if (defaultableParameterNode.typeName() instanceof TypeDescriptorNode &&
//                        !defaultableParameterNode.annotations().isEmpty()) {
//                    parameters.addAll(handleDefaultableAnnotationParameters(defaultableParameterNode));
//                } else {
//                    parameters.add(queryParameterMapper.createQueryParameter(defaultableParameterNode));
//                }
//            }
//        }
//        if (parameters.isEmpty()) {
//            operationAdaptor.getOperation().setParameters(null);
//        } else {
//            operationAdaptor.getOperation().setParameters(parameters);
//        }
    }

    /**
     * Map path parameter data to OAS path parameter.
     */
    private void createPathParameters(AsyncApi25Parameters parameters, NodeList<Node> pathParams) {
        for (Node param: pathParams) {
            if (param instanceof ResourcePathParameterNode) {
                AsyncApi25ParameterImpl pathParameterOAS = new AsyncApi25ParameterImpl();
                ResourcePathParameterNode pathParam = (ResourcePathParameterNode) param;
                if (pathParam.typeDescriptor().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) pathParam.typeDescriptor();
                    AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
                    componentMapper.createComponentSchema(components.getSchemas(), typeSymbol);
                    AsyncApi25SchemaImpl schema = new AsyncApi25SchemaImpl();
                    schema.set$ref(ConverterCommonUtils.unescapeIdentifier(queryNode.name().text().trim()));
                    pathParameterOAS.setSchema(schema);
                } else {
                    pathParameterOAS.setSchema(ConverterCommonUtils.getAsyncApiSchema(
                            pathParam.typeDescriptor().toString().trim()));
                }

                pathParameterOAS.setName(ConverterCommonUtils.unescapeIdentifier(pathParam.paramName().get().text()));

                // Check the parameter has doc
                if (!apidocs.isEmpty() && apidocs.containsKey(pathParam.paramName().get().text().trim())) {
                    pathParameterOAS.setDescription(apidocs.get(pathParam.paramName().get().text().trim()));
                }
                // Set param description
                pathParameterOAS.setRequired(true);
                parameters.add(pathParameterOAS);
            }
        }
    }

//    /**
//     * This function for handle the payload and header parameters with annotation @http:Payload, @http:Header.
//     */
//    private void handleAnnotationParameters(Components components,
//                                            SemanticModel semanticModel,
//                                            List<Parameter> parameters,
//                                            RequiredParameterNode requiredParameterNode) {
//
//        NodeList<AnnotationNode> annotations = requiredParameterNode.annotations();
//        for (AnnotationNode annotation: annotations) {
//            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
//                // Handle headers.
//                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
//                parameters.addAll(asyncAPIHeaderMapper.setHeaderParameter(requiredParameterNode));
//            } else if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_PAYLOAD) &&
//                    (!Constants.GET.toLowerCase(Locale.ENGLISH).equalsIgnoreCase(
//                            operationAdaptor.getHttpOperation()))) {
//                Map<String, Schema> schema = components.getSchemas();
//                // Handle request payload.
//                Optional<String> customMediaType = extractCustomMediaType(functionDefinitionNode);
//                AsyncAPIRequestBodyMapper asyncAPIRequestBodyMapper = customMediaType.map(
//                        value -> new AsyncAPIRequestBodyMapper(components,
//                        operationAdaptor, semanticModel, value)).orElse(new AsyncAPIRequestBodyMapper(components,
//                        operationAdaptor, semanticModel));
//                asyncAPIRequestBodyMapper.handlePayloadAnnotation(requiredParameterNode, schema, annotation, apidocs);
//                errors.addAll(asyncAPIRequestBodyMapper.getDiagnostics());
//            } else if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_PAYLOAD) &&
//                    (Constants.GET.toLowerCase(Locale.ENGLISH).equalsIgnoreCase(operationAdaptor.getHttpOperation()))) {
//                DiagnosticMessages errorMessage = DiagnosticMessages.OAS_CONVERTOR_113;
//                IncompatibleResourceDiagnostic error = new IncompatibleResourceDiagnostic(errorMessage,
//                        annotation.location());
//                errors.add(error);
//            }
//        }
//    }
//
//    /**
//     * This function for handle the payload and header parameters with annotation @http:Payload, @http:Header.
//     */
//    private List<Parameter> handleDefaultableAnnotationParameters(DefaultableParameterNode defaultableParameterNode) {
//        List<Parameter> parameters = new ArrayList<>();
//        NodeList<AnnotationNode> annotations = defaultableParameterNode.annotations();
//        for (AnnotationNode annotation: annotations) {
//            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
//                // Handle headers.
//                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
//                parameters = asyncAPIHeaderMapper.setHeaderParameter(defaultableParameterNode);
//            }
//        }
//        return parameters;
//    }
}
