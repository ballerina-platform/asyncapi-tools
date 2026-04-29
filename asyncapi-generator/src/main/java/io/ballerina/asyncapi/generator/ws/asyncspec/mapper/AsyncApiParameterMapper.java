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
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30BindingImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ChannelBindingsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ChannelImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ParameterImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ParametersImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.Constants;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils;
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

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.BINDING_VERSION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.BINDING_VERSION_VALUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.HEADERS;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.HTTP_HEADER;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.PATH_PARAM_DASH_CONTAIN_ERROR;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.QUERY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SCHEMA_REFERENCE;

/**
 * Provides functionality for converting Ballerina parameters to AsyncApiSpec parameter model.
 * Corresponds to the legacy {@code AsyncApiParameterMapper}.
 */
public class AsyncApiParameterMapper {

    private final FunctionDefinitionNode functionDefinitionNode;
    private final Map<String, String> apidocs;
    private final AsyncApi30ComponentsImpl components;
    private final SemanticModel semanticModel;

    /**
     * Creates a new parameter mapper.
     *
     * @param functionDefinitionNode the function definition node
     * @param apidocs                map of parameter name to API doc description
     * @param components             AsyncAPI components for schema registration
     * @param semanticModel          Ballerina semantic model
     */
    public AsyncApiParameterMapper(FunctionDefinitionNode functionDefinitionNode, Map<String, String> apidocs,
                                   AsyncApi30ComponentsImpl components, SemanticModel semanticModel) {
        this.functionDefinitionNode = functionDefinitionNode;
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }

    /**
     * Create parameters and bindings for an AsyncAPI channel.
     *
     * @param channelItem the channel to populate
     */
    public void getResourceInputs(AsyncApi30ChannelImpl channelItem) {
        NodeList<Node> pathParams = functionDefinitionNode.relativeResourcePath();
        if (!pathParams.isEmpty()) {
            AsyncApi30ParametersImpl pathParameters = createPathParameters(pathParams);
            if (!pathParameters.getItems().isEmpty()) {
                channelItem.setParameters(pathParameters);
            }
        }

        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameterList = functionSignature.parameters();
        if (!parameterList.isEmpty()) {
            channelItem.setBindings(createQueryParameters(parameterList));
        }
    }

    private AsyncApi30ChannelBindingsImpl createQueryParameters(SeparatedNodeList<ParameterNode> parameterList) {
        AsyncApi30ChannelBindingsImpl channelBindings = new AsyncApi30ChannelBindingsImpl();
        AsyncApi30BindingImpl asyncApi30Binding = new AsyncApi30BindingImpl();
        BalAsyncApi30SchemaImpl bindingQueryObject = new BalAsyncApi30SchemaImpl();
        BalAsyncApi30SchemaImpl bindingHeaderObject = new BalAsyncApi30SchemaImpl();
        bindingQueryObject.setType(AsyncAPIType.OBJECT.toString());
        bindingHeaderObject.setType(AsyncAPIType.OBJECT.toString());
        AsyncApiQueryParameterMapper queryParameterMapper = new AsyncApiQueryParameterMapper(apidocs, components,
                semanticModel);
        for (ParameterNode parameterNode : parameterList) {
            if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
                if (requiredParameterNode.typeName().kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE
                        && requiredParameterNode.annotations().isEmpty()) {
                    queryParameterMapper.createQueryParameter(requiredParameterNode, bindingQueryObject);
                }
                if (requiredParameterNode.typeName() instanceof TypeDescriptorNode
                        && !requiredParameterNode.annotations().isEmpty()) {
                    handleHeaderParameters(requiredParameterNode, bindingHeaderObject);
                }
            } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
                DefaultableParameterNode defaultableParameterNode = (DefaultableParameterNode) parameterNode;
                if (defaultableParameterNode.typeName() instanceof TypeDescriptorNode
                        && !defaultableParameterNode.annotations().isEmpty()) {
                    handleDefaultableHeaderParameters(defaultableParameterNode, bindingHeaderObject);
                } else {
                    queryParameterMapper.createQueryParameter(defaultableParameterNode, bindingQueryObject);
                }
            }
        }
        ObjectMapper objectMapper = ConverterCommonUtils.callObjectMapper();

        TextNode bindingVersion = new TextNode(BINDING_VERSION_VALUE);
        asyncApi30Binding.addItem(BINDING_VERSION, bindingVersion);

        if (bindingQueryObject.getProperties() != null) {
            ObjectNode queryObj = objectMapper.valueToTree(bindingQueryObject);
            asyncApi30Binding.addItem(QUERY, queryObj);
        }
        if (bindingHeaderObject.getProperties() != null) {
            ObjectNode headerObj = objectMapper.valueToTree(bindingHeaderObject);
            asyncApi30Binding.addItem(HEADERS, headerObj);
        }
        channelBindings.setWs(asyncApi30Binding);
        return channelBindings;
    }

    /**
     * Map path parameter data to AsyncApiSpec path parameter.
     */
    private AsyncApi30ParametersImpl createPathParameters(NodeList<Node> pathParams) {
        AsyncApi30ParametersImpl parameters = new AsyncApi30ParametersImpl();
        for (Node param : pathParams) {
            if (param instanceof ResourcePathParameterNode) {
                AsyncApi30ParameterImpl pathParameterAAS = new AsyncApi30ParameterImpl();
                ResourcePathParameterNode pathParam = (ResourcePathParameterNode) param;
                String parameterItemName = ConverterCommonUtils.unescapeIdentifier(pathParam.paramName().get().text());
                if (parameterItemName.contains("-")) {
                    throw new NoSuchElementException(PATH_PARAM_DASH_CONTAIN_ERROR);
                }
                if (pathParam.typeDescriptor().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) pathParam.typeDescriptor();
                    AsyncApiComponentMapper componentMapper = new AsyncApiComponentMapper(components);
                    TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
                    componentMapper.createComponentSchema(typeSymbol, null);
                    pathParameterAAS.set$ref(SCHEMA_REFERENCE
                            + ConverterCommonUtils.unescapeIdentifier(queryNode.name().text().trim()));
                }
                if (!apidocs.isEmpty() && apidocs.containsKey(pathParam.paramName().get().text().trim())) {
                    pathParameterAAS.setDescription(apidocs.get(pathParam.paramName().get().text().trim()));
                }
                parameters.addItem(parameterItemName, pathParameterAAS);
            }
        }
        return parameters;
    }

    /**
     * Handle header parameters with annotation @http:Header.
     */
    private void handleHeaderParameters(RequiredParameterNode requiredParameterNode,
                                        BalAsyncApi30SchemaImpl bindingHeaderObject) {
        NodeList<AnnotationNode> annotations = requiredParameterNode.annotations();
        for (AnnotationNode annotation : annotations) {
            if ((annotation.annotReference().toString()).trim().equals(Constants.HTTP_HEADER)) {
                AsyncApiHeaderMapper asyncAPIHeaderMapper = new AsyncApiHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(requiredParameterNode, bindingHeaderObject);
            }
        }
    }

    /**
     * Handle defaultable header parameters with annotation @http:Header.
     */
    private void handleDefaultableHeaderParameters(DefaultableParameterNode defaultableParameterNode,
                                                   BalAsyncApi30SchemaImpl bindingHeaderObject) {
        NodeList<AnnotationNode> annotations = defaultableParameterNode.annotations();
        for (AnnotationNode annotation : annotations) {
            if ((annotation.annotReference().toString()).trim().equals(HTTP_HEADER)) {
                AsyncApiHeaderMapper asyncAPIHeaderMapper = new AsyncApiHeaderMapper(apidocs);
                asyncAPIHeaderMapper.setHeaderParameter(defaultableParameterNode, bindingHeaderObject);
            }
        }
    }
}
