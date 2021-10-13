/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * Test the generation of Ballerina Union Descriptor nodes.
 */
public class GenerateUnionDescriptorNodeTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(description = "Test the functionality of the generate function " +
            "when there is only one node")
    public void testGenerateWithOneNode() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Map.Entry<String, Schema> entry = schemas.entrySet().iterator().next();
        Generator generateRecordNode = new GenerateRecordNode(schemas, entry);
        TypeDefinitionNode typeDefinitionNode = generateRecordNode.generate();
        List<TypeDescriptorNode> typeDescriptorNodes = new ArrayList<>();
        typeDescriptorNodes.add(
                createSimpleNameReferenceNode(createIdentifierToken(typeDefinitionNode.typeName().text())));

        Generator generateUnionDescriptorNode = new GenerateUnionDescriptorNode(typeDescriptorNodes,
                "GenericDataType");
        TypeDefinitionNode unionDefinitionNode = generateUnionDescriptorNode.generate();
        Assert.assertTrue(unionDefinitionNode.visibilityQualifier().isPresent());
        Assert.assertEquals(unionDefinitionNode.visibilityQualifier().get().text(), "public");
        Assert.assertEquals(unionDefinitionNode.typeName().text(), "GenericDataType");
        Assert.assertEquals(unionDefinitionNode.typeDescriptor().toSourceCode(), "GenericEventWrapper");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when there are multiple nodes")
    public void testGenerateWithMultipleNode() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-schemas.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        List<TypeDescriptorNode> typeDescriptorNodes = new ArrayList<>();
        for (Map.Entry<String, Schema> fields : schemas.entrySet()) {
            Generator generateRecordNode = new GenerateRecordNode(schemas, fields);
            TypeDefinitionNode typeDefinitionNode = generateRecordNode.generate();
            typeDescriptorNodes.add(
                    createSimpleNameReferenceNode(createIdentifierToken(typeDefinitionNode.typeName().text())));
        }

        Generator generateUnionDescriptorNode = new GenerateUnionDescriptorNode(typeDescriptorNodes,
                "GenericDataType");
        TypeDefinitionNode unionDefinitionNode = generateUnionDescriptorNode.generate();
        Assert.assertTrue(unionDefinitionNode.visibilityQualifier().isPresent());
        Assert.assertEquals(unionDefinitionNode.visibilityQualifier().get().text(), "public");
        Assert.assertEquals(unionDefinitionNode.typeName().text(), "GenericDataType");
        Assert.assertEquals(unionDefinitionNode.typeDescriptor().toSourceCode(),
                "CustomTestSchema|GenericEventWrapper");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the nodes list is empty",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp =
                    "Nodes list is empty, hence can't generate the Union Node")
    public void testGenerateWithEmptyRemoteFunctionsList() throws BallerinaAsyncApiException {
        Generator generateUnionDescriptorNode = new GenerateUnionDescriptorNode(new ArrayList<>(),
                "GenericDataType");
        generateUnionDescriptorNode.generate();
    }
}
