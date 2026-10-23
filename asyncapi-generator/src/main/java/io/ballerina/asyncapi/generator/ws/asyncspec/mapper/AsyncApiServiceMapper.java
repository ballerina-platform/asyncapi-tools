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

import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ChannelsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationsImpl;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.List;
import java.util.NoSuchElementException;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_KEY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_STREAM_ID;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_STREAM_ID_VALUE_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.NO_ANNOTATION_PRESENT;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.NO_DISPATCHER_KEY;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_BALLERINA_WS_CLOSE_FRAME;
import static io.ballerina.asyncapi.generator.ws.asyncspec.mapper.AsyncApiRemoteMapper.containsCloseFrameSchema;
import static io.ballerina.asyncapi.generator.ws.asyncspec.mapper.AsyncApiRemoteMapper.getWsCloseFrameExtension;

/**
 * Maps a Ballerina WebSocket service definition to an AsyncAPI 3.0 document.
 * Corresponds to the legacy {@code AsyncApiServiceMapper}.
 */
public class AsyncApiServiceMapper {

    private final AsyncApi30Document asyncAPI;
    private final SemanticModel semanticModel;

    /**
     * Initializes a service mapper for AsyncAPI 3.0.
     *
     * @param semanticModel Ballerina semantic model
     * @param asyncAPI      the AsyncAPI 3.0 document to populate
     */
    public AsyncApiServiceMapper(SemanticModel semanticModel, AsyncApi30Document asyncAPI) {
        this.semanticModel = semanticModel;
        this.asyncAPI = asyncAPI;
    }

    /**
     * Converts a Ballerina service declaration to an AsyncAPI 3.0 document.
     *
     * @param service              the Ballerina service declaration node
     * @param classDefinitionNodes list of class definition nodes
     * @param asyncApi             the AsyncAPI document to populate
     * @return populated AsyncAPI 3.0 document
     */
    public AsyncApi30Document convertServiceToAsyncApi(ServiceDeclarationNode service,
                                                       List<ClassDefinitionNode> classDefinitionNodes,
                                                       AsyncApi30Document asyncApi) {
        NodeList<Node> functions = service.members();
        String dispatcherValue = extractDispatcherValue(service);
        for (Node function : functions) {
            SyntaxKind kind = function.kind();
            if (kind.equals(SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)) {
                AsyncApiRemoteMapper resourceMapper = new AsyncApiRemoteMapper(this.semanticModel, asyncApi);
                AsyncApi30ChannelsImpl generatedChannels = resourceMapper.getChannels(
                        (FunctionDefinitionNode) function, classDefinitionNodes, dispatcherValue);
                asyncApi.setChannels(generatedChannels);
                AsyncApi30OperationsImpl generatedOperations = resourceMapper.getOperations();
                asyncApi.setOperations(generatedOperations);
                AsyncApi30ComponentsImpl generatedComponents = resourceMapper.getComponents();
                asyncApi.setComponents(generatedComponents);
                if (containsCloseFrameSchema(generatedComponents)) {
                    asyncApi.addExtension(X_BALLERINA_WS_CLOSE_FRAME, getWsCloseFrameExtension());
                }
            }
        }
        return asyncApi;
    }

    private String extractDispatcherValue(ServiceDeclarationNode service) {
        String dispatcherValue = "";
        if (service.metadata().isPresent()) {
            MetadataNode serviceMetadataNode = service.metadata().get();
            NodeList<AnnotationNode> annotationNodes = serviceMetadataNode.annotations();
            AnnotationNode annotationNode = annotationNodes.get(0);
            Node node = annotationNode.annotReference();
            if (node instanceof QualifiedNameReferenceNode) {
                SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().get().fields();
                for (MappingFieldNode field : fields) {
                    if (field instanceof SpecificFieldNode) {
                        SpecificFieldNode specificFieldNode = (SpecificFieldNode) field;
                        String fieldName = specificFieldNode.fieldName().toString().trim();
                        dispatcherValue = setFieldValues(dispatcherValue, specificFieldNode, fieldName);
                        if (fieldName.equals(DISPATCHER_STREAM_ID)) {
                            String dispatcherStreamIdValue = specificFieldNode.valueExpr().get().toString().trim();
                            dispatcherStreamIdValue = dispatcherStreamIdValue.replaceAll("\"", "");
                            if (dispatcherStreamIdValue.equals("")) {
                                throw new NoSuchElementException(DISPATCHER_STREAM_ID_VALUE_CANNOT_BE_EMPTY);
                            }
                            asyncAPI.addExtension("x-dispatcherStreamId", new TextNode(dispatcherStreamIdValue));
                        }
                    }
                }
                if (!dispatcherValue.equals("")) {
                    return dispatcherValue.trim();
                }

                if (dispatcherValue.isEmpty()) {
                    throw new NoSuchElementException(NO_DISPATCHER_KEY);
                }
            }
        } else {
            throw new NoSuchElementException(NO_ANNOTATION_PRESENT);
        }
        return null;
    }

    private String setFieldValues(String dispatcherValue, SpecificFieldNode specificFieldNode, String fieldName) {
        if (fieldName.equals(DISPATCHER_KEY)) {
            dispatcherValue = specificFieldNode.valueExpr().get().toString().trim();
            dispatcherValue = dispatcherValue.replaceAll("\"", "");
            if (dispatcherValue.isEmpty()) {
                throw new NoSuchElementException(DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY);
            }
            asyncAPI.addExtension("x-dispatcherKey", new TextNode(dispatcherValue));
        }
        return dispatcherValue;
    }
}
