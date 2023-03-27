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

import io.apicurio.datamodels.models.asyncapi.v25.*;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;


/**
 * This class will do resource mapping from ballerina to asyncApi.
 *
 * @since 2.0.0
 */
public class AsyncAPIRemoteMapper {
    private final SemanticModel semanticModel;
    private final  AsyncApi25ChannelsImpl channelObject = new AsyncApi25ChannelsImpl();
    private final AsyncApi25ComponentsImpl components = new AsyncApi25ComponentsImpl();

    private final AsyncAPIComponentMapper componentMapper=new AsyncAPIComponentMapper(components);
    private final List<AsyncAPIConverterDiagnostic> errors;

    public List<AsyncAPIConverterDiagnostic> getErrors() {
        return errors;
    }

    /**
     * Initializes a resource parser for asyncApi.
     */
    AsyncAPIRemoteMapper(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.errors = new ArrayList<>();
    }

    public AsyncApi25ComponentsImpl getComponents() {
        return components;
    }
    /**
     * This method will convert ballerina resource to asyncApi Paths objects.
     *
//     * @param resources Resource list to be converted.
     * @return map of string and asyncApi path objects.
     */
    public AsyncApi25ChannelsImpl getChannels(FunctionDefinitionNode resource,List<ClassDefinitionNode> classDefinitionNodes,String dispatcherValue) {

        AsyncApi25ChannelItemImpl channelItem= (AsyncApi25ChannelItemImpl) channelObject.createChannelItem();

        //call asyncAPIParameterMapper to map parameters
        Map<String, String> apiDocs = listAPIDocumentations(resource,channelItem);
        AsyncAPIParameterMapper asyncAPIParameterMapper = new AsyncAPIParameterMapper(resource,apiDocs, components,
                semanticModel);
        asyncAPIParameterMapper.getResourceInputs(channelItem);


        String serviceClassName= getServiceClassName(resource);
        if (!serviceClassName.isEmpty()){
            for (ClassDefinitionNode node: classDefinitionNodes) {
               String testClassName1= node.className().text();
                if (testClassName1.equals(serviceClassName)) {
                    return handleRemoteFunctions(resource,node,dispatcherValue,channelItem);
                }
            }

        }else{
            throw new NoSuchElementException(NO_SERVICE_CLASS);

        }

        return channelObject;
    }

    /**
     * Resource mapper when a resource has more than 1 http method.
     *
     * @param resource The ballerina resource.
//     * @param httpMethods   Sibling methods related to operation.
     */
    private AsyncApi25ChannelsImpl handleRemoteFunctions(FunctionDefinitionNode resource, ClassDefinitionNode classDefinitionNode,String dispatcherValue,AsyncApi25ChannelItemImpl channelItem) {
        String path = ConverterCommonUtils.unescapeIdentifier(generateRelativePath(resource));
        NodeList<Node> classMethodNodes= classDefinitionNode.members();
        AsyncApi25OperationImpl publishOperationItem=new AsyncApi25OperationImpl();
        AsyncApi25OperationImpl subscribeOperationItem=new AsyncApi25OperationImpl();
        AsyncApi25MessageImpl subscribeMessage= new AsyncApi25MessageImpl();
        AsyncApi25MessageImpl publishMessage= new AsyncApi25MessageImpl();
        AsyncAPIResponseMapper responseMapper=new AsyncAPIResponseMapper(resource.location(), componentMapper,semanticModel,components);
        for(Node node: classMethodNodes){
            if (node.kind().equals(SyntaxKind.OBJECT_METHOD_DEFINITION)){
                FunctionDefinitionNode remoteFunctionNode= (FunctionDefinitionNode)node;
                String functionName= remoteFunctionNode.functionName().toString();
                if (functionName.startsWith(ON)) {
                    //TODO : have to pass this through unescape identifier
                    String remoteRequestTypeName = ConverterCommonUtils.unescapeIdentifier(functionName.substring(2));
                    RequiredParameterNode requiredParameterNode = checkParameterContainsCustomType(remoteRequestTypeName, remoteFunctionNode);
                    if (requiredParameterNode != null) {
                        String paramName=requiredParameterNode.paramName().get().toString();
                        Node parameterTypeNode =requiredParameterNode.typeName();
                        TypeSymbol remoteFunctionNameTypeSymbol = (TypeSymbol) semanticModel.symbol(parameterTypeNode).orElseThrow();
                        TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) remoteFunctionNameTypeSymbol;
                        TypeSymbol type = typeRef.typeDescriptor();
//                        Boolean testing= type.typeKind().equals(TypeDescKind.INTERSECTION);
//                        SymbolKind testing2= componentMapper.excludeReadonlyIfPresent(type).typeKind();
//                        Boolean testing1=componentMapper.excludeReadonlyIfPresent(type).kind().equals(TypeDescKind.RECORD);

                        //check if there is a readOnly & record type also
                        if (type.typeKind().equals(TypeDescKind.RECORD) || (type.typeKind().equals(TypeDescKind.INTERSECTION) && componentMapper.excludeReadonlyIfPresent(type).typeKind().equals(TypeDescKind.RECORD))){

                            FunctionSymbol remoteFunctionSymbol = (FunctionSymbol) semanticModel.symbol(remoteFunctionNode).get();
                            Map<String, String> remoteDocs = getRemoteDocumentation(remoteFunctionSymbol);

                            String paramDescription = null;
                            if (remoteDocs.containsKey(paramName)) {
                                paramDescription = remoteDocs.get(paramName);
                                remoteDocs.remove(paramName);

                            }
                            AsyncApi25MessageImpl componentMessage = responseMapper.extractMessageSchemaReference(publishMessage, remoteRequestTypeName, remoteFunctionNameTypeSymbol, dispatcherValue, paramDescription);
                            if (remoteDocs.containsKey(REMOTE_DESCRIPTION)) {
                                componentMessage.setDescription(remoteDocs.get(REMOTE_DESCRIPTION));
                                remoteDocs.remove(REMOTE_DESCRIPTION);
                            }
                            Optional<ReturnTypeDescriptorNode> optionalRemoteReturnNode = remoteFunctionNode.functionSignature().returnTypeDesc();
                            if (optionalRemoteReturnNode.isPresent()) {
                                Node remoteReturnType = optionalRemoteReturnNode.get().type();
                                String returnDescription = null;
                                if (remoteDocs.containsKey(RETURN)) {
                                    returnDescription = remoteDocs.get(RETURN);
                                    remoteDocs.remove(RETURN);
                                }
                                responseMapper.createResponse( subscribeMessage, componentMessage, remoteReturnType, returnDescription);
                            }
                            components.addMessage(remoteRequestTypeName, componentMessage);
                        }else{
                            throw new NoSuchElementException(String.format(FUNCTION_SIGNATURE_WRONG_TYPE,remoteRequestTypeName,type.typeKind().getName()));
                        }

                    } else {
                        throw new NoSuchElementException(FUNCTION_SIGNATURE_ABSENT);
                    }

                }else{
                    throw new NoSuchElementException(FUNCTION_WRONG_NAME);
                }
            }

        }
        if(publishMessage.getOneOf()!=null){
            publishOperationItem.setMessage(publishMessage);
            channelItem.setPublish(publishOperationItem);

        }
        if(subscribeMessage.getOneOf()!=null){
            subscribeOperationItem.setMessage(subscribeMessage);
            channelItem.setSubscribe(subscribeOperationItem);
        }
        channelObject.addItem(path,channelItem);

        return channelObject;
    }



    private Map<String, String> getRemoteDocumentation(FunctionSymbol remoteFunctionSymbol) {
        Map<String, String> apiDocs = new HashMap<>();
        Optional<Documentation> documentation=remoteFunctionSymbol.documentation();
        if(documentation.isPresent()) {
            apiDocs = documentation.get().parameterMap();
            if(documentation.get().returnDescription().isPresent()) {
                apiDocs.put(RETURN, documentation.get().returnDescription().get());
            }

            if (documentation.get().description().isPresent()) {
                Optional<String> description = (documentation.get().description());
                if(description.isPresent() && !description.get().toString().isEmpty()) {
                    apiDocs.put(REMOTE_DESCRIPTION, (description.get().trim()));
                }
            }
        }
        return apiDocs;
    }




    private RequiredParameterNode checkParameterContainsCustomType(String customTypeName,FunctionDefinitionNode remoteFunctionNode) {
        SeparatedNodeList<ParameterNode> remoteParameters = remoteFunctionNode.functionSignature().parameters();
        for (ParameterNode remoteParameterNode : remoteParameters) {
            if (remoteParameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) remoteParameterNode;

                Node parameterTypeNode =requiredParameterNode.typeName();
                if (parameterTypeNode.kind()==SyntaxKind.SIMPLE_NAME_REFERENCE) {
                    SimpleNameReferenceNode simpleNameReferenceNode=(SimpleNameReferenceNode) parameterTypeNode;
                    String simpleType= simpleNameReferenceNode.name().toString().trim();
                    if (simpleType.equals(customTypeName)) {
                        return requiredParameterNode;
                    }
                }
            }
        }
        return null;
    }

    private String getServiceClassName(FunctionDefinitionNode resource) {
        String serviceClassName="";
        FunctionBodyNode functionBodyNode = resource.functionBody();
        ChildNodeList childNodeList = functionBodyNode.children();
        for (Node node : childNodeList) {
            if (node instanceof ReturnStatementNode) {
                ReturnStatementNode returnStatementNode = (ReturnStatementNode) node;
                Optional<ExpressionNode> expression = returnStatementNode.expression();
                if (expression.get() instanceof ExplicitNewExpressionNode) {
                    ExplicitNewExpressionNode explicitNewExpressionNode = (ExplicitNewExpressionNode) expression.get();
                    TypeDescriptorNode typeDescriptorNode = explicitNewExpressionNode.typeDescriptor();
                    serviceClassName=typeDescriptorNode.toString();
                }
            }
        }
        return serviceClassName;
    }
                /**
                 * Filter the API documentations from resource function node.
                 */
    private Map<String, String> listAPIDocumentations(FunctionDefinitionNode resource,AsyncApi25ChannelItemImpl channelItem) {

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
         * Gets the http methods of a resource.
         *
         * @param resource    The ballerina resource.
         * @return A list of http methods.
         */
        private String generateRelativePath(FunctionDefinitionNode resource) {

            StringBuilder relativePath = new StringBuilder();
            relativePath.append("/");
            if (!resource.relativeResourcePath().isEmpty()) {
                for (Node node: resource.relativeResourcePath()) {
                    if (node instanceof ResourcePathParameterNode) {
                        ResourcePathParameterNode pathNode = (ResourcePathParameterNode) node;
                        relativePath.append("{");
                        relativePath.append(pathNode.paramName().get());
                        relativePath.append("}");
                    } else if ((resource.relativeResourcePath().size() == 1) && (node.toString().trim().equals("."))) {
                        return relativePath.toString();
                    } else {
                        relativePath.append(node.toString().trim());
                    }
                }
            }
            return relativePath.toString();
        }

}
