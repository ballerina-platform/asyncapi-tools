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

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.*;


import java.util.ArrayList;
import java.util.List;

/**
 * AsyncAPIServiceMapper provides functionality for reading and writing OpenApi, either to and from ballerina service, or
 * to, as well as related functionality for performing conversions between asyncapi and ballerina.
 *
 * @since 2.0.0
 */
public class AsyncAPIServiceMapper {
    private final SemanticModel semanticModel;
    private final List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();

    public List<AsyncAPIConverterDiagnostic> getErrors() {
        return errors;
    }

    /**
     * Initializes a service parser for OpenApi.
     */
    public AsyncAPIServiceMapper(SemanticModel semanticModel) {
        // Default object mapper is JSON mapper available in openApi utils.
        this.semanticModel = semanticModel;
    }

    /**
     * This method will convert ballerina @Service to openApi @OpenApi object.
     *
     * @param service   - Ballerina @Service object to be map to openApi definition
     * @param asyncApi   - OpenApi model to populate
     * @return OpenApi object which represent current service.
     */
    public AsyncApi25DocumentImpl convertServiceToOpenAPI(ServiceDeclarationNode service,List<ClassDefinitionNode>classDefinitionNodes, AsyncApi25DocumentImpl asyncApi) {
        NodeList<Node> functions = service.members();
//        List<FunctionDefinitionNode> resource = new ArrayList<>();
//        for (Node function: functions) {
        Node function=functions.get(0);
        SyntaxKind kind = function.kind();
        if (kind.equals(SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)) {
            AsyncAPIRemoteMapper resourceMapper = new AsyncAPIRemoteMapper(this.semanticModel);


            asyncApi.setChannels(resourceMapper.getChannels((FunctionDefinitionNode)function,classDefinitionNodes));
//            asyncApi.setComponents(resourceMapper.getComponents());
            errors.addAll(resourceMapper.getErrors());
        }


        return asyncApi;
    }
}
