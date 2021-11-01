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
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Map;

/**
 * Test the generation of Ballerina Record nodes.
 */
public class GenerateModuleMemberDeclarationNodeTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(description = "Test the functionality of the generate function " +
            "when there is only one schema")
    public void testGenerateWithSingleSchema() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Map.Entry<String, Schema> entry = schemas.entrySet().iterator().next();
        Generator generateRecordNode = new GenerateModuleMemberDeclarationNode(entry);
        TypeDefinitionNode typeDefinitionNode = generateRecordNode.generate();

        Assert.assertEquals(typeDefinitionNode.typeName().text(), "GenericEventWrapper");
        Assert.assertTrue(typeDefinitionNode.typeDescriptor() instanceof RecordTypeDescriptorNode);
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                (RecordTypeDescriptorNode) typeDefinitionNode.typeDescriptor();
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(0)).fieldName().text(),
                "authed_users");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(4)).fieldName().text(),
                "event");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(4)).typeName().toSourceCode(),
                "record { #When the event was dispatchedstringevent_ts;" +
                        "#The specific name of the eventstring'type;} ");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when there are enums")
    public void testGenerateWithEnums() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema-with-enum.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Map.Entry<String, Schema> entry = schemas.entrySet().iterator().next();
        Generator generateRecordNode = new GenerateModuleMemberDeclarationNode(entry);
        EnumDeclarationNode enumDeclarationNode = generateRecordNode.generate();

        Assert.assertEquals(enumDeclarationNode.identifier().text(), "OccupancyStatus");
        Assert.assertEquals(enumDeclarationNode.enumMemberList().get(0).toSourceCode(), "EMPTY");
        Assert.assertEquals(enumDeclarationNode.enumMemberList().get(2).toSourceCode(), "FEW_SEATS_AVAILABLE");
        Assert.assertEquals(enumDeclarationNode.enumMemberList()
                .get(4).toSourceCode(), "CRUSHED_STANDING_ROOM_ONLY");
        Assert.assertEquals(enumDeclarationNode.enumMemberList()
                .get(6).toSourceCode(), "NOT_ACCEPTING_PASSENGERS");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when there are multiple schemas")
    public void testGenerateWithMultipleSchemas() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-schemas.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Iterator<Map.Entry<String, Schema>> iterator = schemas.entrySet().iterator();
        Map.Entry<String, Schema> firstEntry = iterator.next();
        Generator generateRecordNode1 = new GenerateModuleMemberDeclarationNode(firstEntry);
        TypeDefinitionNode typeDefinitionNode1 = generateRecordNode1.generate();
        Assert.assertEquals(typeDefinitionNode1.typeName().text(), "CustomTestSchema");

        Map.Entry<String, Schema> secondEntry = iterator.next();
        Generator generateRecordNode2 = new GenerateModuleMemberDeclarationNode(secondEntry);
        TypeDefinitionNode typeDefinitionNode2 = generateRecordNode2.generate();

        Assert.assertEquals(typeDefinitionNode2.typeName().text(), "GenericEventWrapper");
        Assert.assertTrue(typeDefinitionNode2.typeDescriptor() instanceof RecordTypeDescriptorNode);
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                (RecordTypeDescriptorNode) typeDefinitionNode2.typeDescriptor();
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(0)).typeName().toSourceCode(),
                "string[]");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(5)).fieldName().toSourceCode(),
                "'type");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(8)).typeName().toSourceCode(),
                "record {}[]");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(10)).typeName().toSourceCode(),
                "CustomTestSchema[]");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(12)).typeName().toSourceCode(),
                "CustomTestSchema");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(15)).typeName().toSourceCode(),
                "record { #When the event was dispatchedstringevent_ts;" +
                        "#The specific name of the eventstring'type;} ");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(17)).typeName().toSourceCode(),
                "string[][]");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(18)).typeName().toSourceCode(),
                "record {}");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(19)).typeName().toSourceCode(),
                "anydata");
        Assert.assertEquals(((RecordFieldNode) recordTypeDescriptorNode.fields().get(19)).fieldName().toSourceCode(),
                "'anydata");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the there is an unrecognized type in the spec",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Unsupported Async Api Spec data type `unknown_type`")
    public void testGenerateMissingItemAttributeInArray() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema-with-unrecognized-type.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Map.Entry<String, Schema> entry = schemas.entrySet().iterator().next();
        Generator generateRecordNode = new GenerateModuleMemberDeclarationNode(entry);
        generateRecordNode.generate();
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the there is an unrecognized number format with the type 'number'",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Unsupported Async Api Spec data type `unknown`")
    public void testGenerateWithInvalidNumberFormat() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema-with-invalid-number-format.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Map.Entry<String, Schema> entry = schemas.entrySet().iterator().next();
        Generator generateRecordNode = new GenerateModuleMemberDeclarationNode(entry);
        generateRecordNode.generate();
    }
}
