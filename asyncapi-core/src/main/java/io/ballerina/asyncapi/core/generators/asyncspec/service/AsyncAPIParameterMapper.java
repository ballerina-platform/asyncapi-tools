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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25BindingImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelBindingsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelItemImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ParameterImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ParametersImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.Constants;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.Map;
import java.util.NoSuchElementException;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.BINDING_VERSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.BINDING_VERSION_VALUE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.HEADERS;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.PATH_PARAM_DASH_CONTAIN_ERROR;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.QUERY;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SCHEMA_REFERENCE;


/**
 * AsyncAPIParameterMapper provides functionality for converting ballerina parameter to AsyncApiSpec parameter model.
 */
public class AsyncAPIParameterMapper {
    private final FunctionDefinitionNode functionDefinitionNode;
    private final Map<String, String> apidocs;
//    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();
    private final AsyncApi25ComponentsImpl components;
    private final SemanticModel semanticModel;

    public AsyncAPIParameterMapper(FunctionDefinitionNode functionDefinitionNode, Map<String, String> apidocs,
                                   AsyncApi25ComponentsImpl components, SemanticModel semanticModel) {

        this.functionDefinitionNode = functionDefinitionNode;
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }

//    public List<AsyncAPIConverterDiagnostic> getErrors() {
//        return errors;
//    }

    /**
     * Create {@code Parameters} model for asyncAPI operation.
     */
    public void getResourceInputs(AsyncApi25ChannelItemImpl channelItem) {

        //Set path parameters
        NodeList<Node> pathParams = functionDefinitionNode.relativeResourcePath();
        if (!pathParams.isEmpty()) {
            AsyncApi25ParametersImpl pathParameters = createPathParameters(pathParams);
            if (!pathParameters.getItems().isEmpty()) {
                channelItem.setParameters(pathParameters);
            }
        }

        // Set query parameters, headers
        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameterList = functionSignature.parameters();
        if (!parameterList.isEmpty()) {
            channelItem.setBindings(createQueryParameters(parameterList));

        }
    }

    private AsyncApi25ChannelBindingsImpl createQueryParameters(SeparatedNodeList<ParameterNode> parameterList) {
        AsyncApi25ChannelBindingsImpl channelBindings = new AsyncApi25ChannelBindingsImpl();
        AsyncApi25BindingImpl asyncApi25Binding = new AsyncApi25BindingImpl();
        BalAsyncApi25SchemaImpl bindingQueryObject = new BalAsyncApi25SchemaImpl();
        BalAsyncApi25SchemaImpl bindingHeaderObject = new BalAsyncApi25SchemaImpl();
        bindingQueryObject.setType(AsyncAPIType.OBJECT.toString());
        bindingHeaderObject.setType(AsyncAPIType.OBJECT.toString());
        AsyncAPIQueryParameterMapper queryParameterMapper = new AsyncAPIQueryParameterMapper(apidocs, components,
                semanticModel);
        for (ParameterNode parameterNode : parameterList) {
            if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
                if (requiredParameterNode.typeName().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE &&
                        requiredParameterNode.annotations().isEmpty()) {

                    queryParameterMapper.createQueryParameter(requiredParameterNode, bindingQueryObject);
                }
                // Handle header, payload parameter
                if (requiredParameterNode.typeName() instanceof TypeDescriptorNode &&
                        !requiredParameterNode.annotations().isEmpty()) {
                    handleHeaderParameters(requiredParameterNode, bindingHeaderObject);
                }
            } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
                DefaultableParameterNode defaultableParameterNode = (DefaultableParameterNode) parameterNode;
//                // Handle header parameter
                if (defaultableParameterNode.typeName() instanceof TypeDescriptorNode &&
                        !defaultableParameterNode.annotations().isEmpty()) {
                    handleDefaultableHeaderParameters(defaultableParameterNode, bindingHeaderObject);
                } else {

                    queryParameterMapper.createQueryParameter(defaultableParameterNode, bindingQueryObject);
                }
            }
        }
        ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();

        TextNode bindingVersion = new TextNode(BINDING_VERSION_VALUE);
        asyncApi25Binding.addItem(BINDING_VERSION, bindingVersion);

        if (bindingQueryObject.getProperties() != null) {
            ObjectNode queryObj = objectMapper.valueToTree(bindingQueryObject);
            asyncApi25Binding.addItem(QUERY, queryObj);
        }
        if (bindingHeaderObject.getProperties() != null) {
            ObjectNode headerObj = objectMapper.valueToTree(bindingHeaderObject);
            asyncApi25Binding.addItem(HEADERS, headerObj);
        }


        channelBindings.setWs(asyncApi25Binding);
        return channelBindings;

    }

    /**
     * Map path parameter data to AsyncApiSpec path parameter.
     */
    private AsyncApi25ParametersImpl createPathParameters(NodeList<Node> pathParams) {
        AsyncApi25ParametersImpl parameters = new AsyncApi25ParametersImpl();
        for (Node param : pathParams) {
            if (param instanceof ResourcePathParameterNode) {
                AsyncApi25ParameterImpl pathParameterAAS = new AsyncApi25ParameterImpl();
                ResourcePathParameterNode pathParam = (ResourcePathParameterNode) param;
                String parameterItemName = ConverterCommonUtils.unescapeIdentifier(pathParam.paramName().get().text());
                if(parameterItemName.contains("-")){
                    throw new NoSuchElementException(PATH_PARAM_DASH_CONTAIN_ERROR);
                }
                if (pathParam.typeDescriptor().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) pathParam.typeDescriptor();
                    AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol, null);
                    BalAsyncApi25SchemaImpl schema = new BalAsyncApi25SchemaImpl();
                    schema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(queryNode.name().
                            text().trim()));
                    pathParameterAAS.setSchema(schema);
                } else {
                    pathParameterAAS.setSchema(ConverterCommonUtils.getAsyncApiSchema(
                            pathParam.typeDescriptor().toString().trim()));
                }

                // Check the parameter has doc
                if (!apidocs.isEmpty() && apidocs.containsKey(pathParam.paramName().get().text().trim())) {
                    pathParameterAAS.setDescription(apidocs.get(pathParam.paramName().get().text().trim()));
                }
                // Set param description
                //TODO : Do we have to set required:true?
//                pathParameterAAS.setRequired(true);
                parameters.addItem(parameterItemName, pathParameterAAS);
            }
        }
        return parameters;
    }

    /**
     * This function for handle the payload and header parameters with annotation @http:Payload, @http:Header.
     */
    private void handleHeaderParameters(RequiredParameterNode requiredParameterNode,
                                        BalAsyncApi25SchemaImpl bindingHeaderObject) {

        NodeList<AnnotationNode> annotations = requiredParameterNode.annotations();
        for (AnnotationNode annotation : annotations) {
            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
                // Handle headers.
                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(requiredParameterNode, bindingHeaderObject);
            }
        }
    }

    /**
     * This function for handle the payload and header parameters with annotation @http:Header.
     */
    private void handleDefaultableHeaderParameters(DefaultableParameterNode defaultableParameterNode,
                                                   BalAsyncApi25SchemaImpl bindingHeaderObject) {
        NodeList<AnnotationNode> annotations = defaultableParameterNode.annotations();
        for (AnnotationNode annotation : annotations) {
            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
                // Handle headers.
                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(defaultableParameterNode, bindingHeaderObject);
            }
        }
    }
}
