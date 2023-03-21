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
import io.apicurio.datamodels.models.Components;
import io.apicurio.datamodels.models.asyncapi.v25.*;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.Constants;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.*;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;


/**
 * AsyncAPIParameterMapper provides functionality for converting ballerina parameter to AsyncApiSpec parameter model.
 */
public class AsyncAPIParameterMapper {
    private final FunctionDefinitionNode functionDefinitionNode;
    private final Map<String, String> apidocs;
    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();
    private final AsyncApi25ComponentsImpl components;
    private final SemanticModel semanticModel;

    public List<AsyncAPIConverterDiagnostic> getErrors() {
        return errors;
    }

    public AsyncAPIParameterMapper(FunctionDefinitionNode functionDefinitionNode, Map<String, String> apidocs,
                                   AsyncApi25ComponentsImpl components, SemanticModel semanticModel) {

        this.functionDefinitionNode = functionDefinitionNode;
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }


    /**
     * Create {@code Parameters} model for asyncAPI operation.
     */
    public void getResourceInputs(AsyncApi25ChannelItemImpl channelItem) {

        //Set path parameters
        NodeList<Node> pathParams = functionDefinitionNode.relativeResourcePath();
        if (!pathParams.isEmpty()) {
            AsyncApi25ParametersImpl pathParameters=createPathParameters(pathParams);
            if (!pathParameters.getItems().isEmpty()) {
                channelItem.setParameters(pathParameters);
            }
        }

        // Set query parameters, headers
        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameterList = functionSignature.parameters();
        if (!parameterList.isEmpty()){
            channelItem.setBindings(createQueryParameters(parameterList));

        }
    }

    private AsyncApi25ChannelBindingsImpl createQueryParameters(SeparatedNodeList<ParameterNode> parameterList) {
        AsyncApi25ChannelBindingsImpl channelBindings =new AsyncApi25ChannelBindingsImpl();
        AsyncApi25BindingImpl asyncApi25Binding=new AsyncApi25BindingImpl();
        AsyncApi25SchemaImpl bindingQueryObject=new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl bindingHeaderObject=new AsyncApi25SchemaImpl();
        bindingQueryObject.setType("object");
        bindingHeaderObject.setType("object");
        AsyncAPIQueryParameterMapper queryParameterMapper = new AsyncAPIQueryParameterMapper(apidocs, components,
                semanticModel);
        for (ParameterNode parameterNode : parameterList) {
            if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
                if (requiredParameterNode.typeName().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE &&
                        requiredParameterNode.annotations().isEmpty()) {

                    queryParameterMapper.createQueryParameter(requiredParameterNode,bindingQueryObject);
                }
                // Handle header, payload parameter
                if (requiredParameterNode.typeName() instanceof TypeDescriptorNode &&
                        !requiredParameterNode.annotations().isEmpty()) {
                    handleHeaderParameters(requiredParameterNode,bindingHeaderObject);
                }
            } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
                DefaultableParameterNode defaultableParameterNode = (DefaultableParameterNode) parameterNode;
//                // Handle header parameter
                if (defaultableParameterNode.typeName() instanceof TypeDescriptorNode &&
                        !defaultableParameterNode.annotations().isEmpty()) {
                    handleDefaultableHeaderParameters(defaultableParameterNode,bindingHeaderObject);
                } else {

                   queryParameterMapper.createQueryParameter(defaultableParameterNode,bindingQueryObject);
                }
            }
        }
        ObjectMapper objectMapper=ConverterCommonUtils.callObjectMapper();

        TextNode bindingVersion = new TextNode(BINDING_VERSION);
        asyncApi25Binding.addItem("bindingVersion",bindingVersion);

       if(bindingQueryObject.getProperties()!=null){
            ObjectNode queryObj = objectMapper.valueToTree(bindingQueryObject);
            asyncApi25Binding.addItem("query", queryObj);
        }
        if (bindingHeaderObject.getProperties()!=null) {
            ObjectNode headerObj = objectMapper.valueToTree(bindingHeaderObject);
            asyncApi25Binding.addItem("headers", headerObj);
        }


        channelBindings.setWs(asyncApi25Binding);
        return channelBindings;

    }

    /**
     * Map path parameter data to AsyncApiSpec path parameter.
     */
    private AsyncApi25ParametersImpl createPathParameters( NodeList<Node> pathParams) {
        AsyncApi25ParametersImpl parameters = new AsyncApi25ParametersImpl();
        for (Node param: pathParams) {
            if (param instanceof ResourcePathParameterNode) {
                AsyncApi25ParameterImpl pathParameterAAS = new AsyncApi25ParameterImpl();
                ResourcePathParameterNode pathParam = (ResourcePathParameterNode) param;
                if (pathParam.typeDescriptor().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) pathParam.typeDescriptor();
                    AsyncAPIComponentMapper componentMapper = new AsyncAPIComponentMapper(components);
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol,null);
                    AsyncApi25SchemaImpl schema = new AsyncApi25SchemaImpl();
                    schema.set$ref(SCHEMA_REFERENCE+ConverterCommonUtils.unescapeIdentifier(queryNode.name().text().trim()));
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
                String parameterItemName=ConverterCommonUtils.unescapeIdentifier(pathParam.paramName().get().text());
                parameters.addItem(parameterItemName,pathParameterAAS);
            }
        }
        return parameters;
    }

    /**
     * This function for handle the payload and header parameters with annotation @http:Payload, @http:Header.
     */
    private void handleHeaderParameters(RequiredParameterNode requiredParameterNode, AsyncApi25SchemaImpl bindingHeaderObject) {

        NodeList<AnnotationNode> annotations = requiredParameterNode.annotations();
        for (AnnotationNode annotation: annotations) {
            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
                // Handle headers.
                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(requiredParameterNode, bindingHeaderObject);
            }
        }
    }

    /**
     * This function for handle the payload and header parameters with annotation @http:Payload, @http:Header.
     */
    private void handleDefaultableHeaderParameters(DefaultableParameterNode defaultableParameterNode, AsyncApi25SchemaImpl bindingHeaderObject) {
        NodeList<AnnotationNode> annotations = defaultableParameterNode.annotations();
        for (AnnotationNode annotation: annotations) {
            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
                // Handle headers.
                AsyncAPIHeaderMapper asyncAPIHeaderMapper = new AsyncAPIHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(defaultableParameterNode,bindingHeaderObject);
            }
        }
//        return parameters;
    }
}
