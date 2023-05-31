/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.core.generators.client;

import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.compiler.syntax.tree.ChildNodeEntry;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.ballerina.asyncapi.core.GeneratorConstants.BALLERINA;
import static io.ballerina.asyncapi.core.GeneratorConstants.NUVINDU;
import static io.ballerina.asyncapi.core.GeneratorConstants.NUVINDU_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.URL;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSingletonTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * This class is used to generate util file syntax tree according to the generated client.
 *
 * @since 1.3.0
 */
public class BallerinaUtilGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BallerinaUtilGenerator.class);
    private static final String GET_ENCODED_URI = "getEncodedUri";

private static final String STREAM_GENERATOR = "StreamGenerator";
    private static final String GET_PATH_FOR_QUERY_PARAM = "getPathForQueryParam";
    private static final String GET_COMBINE_HEADERS = "getCombineHeaders";
    private static final boolean streamFound=true;
    private boolean headersFound = true;
    private boolean queryParamsFound = true;
    private boolean pathParametersFound = true;

    /**
     //     * Set `queryParamsFound` flag to `true` when at least one query parameter found.
     //     *
     //     * @param flag Function will be called only in the occasions where flag needs to be set to `true`
     //     */
    public void setQueryParamsFound(boolean flag) {
        this.queryParamsFound = flag;
    }

    /**
     * Set `headersFound` flag to `true` when at least one header found.
     *
     * @param flag Function will be called only in the occasions where flag needs to be set to `true`
     */
    public void setHeadersFound(boolean flag) {

        this.headersFound = flag;
    }

    /**
     * Set `pathParametersFound` flag to `true` when at least one path parameter found.
     *
     * @param flag Function will be called only in the occasions where flag needs to be set to `true`
     */
    public void setPathParametersFound(boolean flag) {
        this.pathParametersFound = flag;
    }


    /**
     * Generates util file syntax tree.
     *
     * @return Syntax tree of the util.bal file
     */
    public SyntaxTree generateUtilSyntaxTree() throws IOException {
        Set<String> functionNameList = new LinkedHashSet<>();
        if (queryParamsFound) {
            functionNameList.addAll(Arrays.asList(
                    GET_ENCODED_URI,
                    GET_PATH_FOR_QUERY_PARAM
            ));
        }
        if (headersFound) {
            functionNameList.add(GET_COMBINE_HEADERS);
        }
        if (pathParametersFound) {
            functionNameList.add(GET_ENCODED_URI);
        }
        if(streamFound){
            functionNameList.add(STREAM_GENERATOR);
        }

        List<ModuleMemberDeclarationNode> memberDeclarationNodes = new ArrayList<>();
        getUtilTypeDeclarationNodes(memberDeclarationNodes);

        Path path = getResourceFilePath();

        Project project =ProjectLoader.loadProject(path);
        Package currentPackage = project.currentPackage();
        DocumentId docId = currentPackage.getDefaultModule().documentIds().iterator().next();
        SyntaxTree syntaxTree = currentPackage.getDefaultModule().document(docId).syntaxTree();

        ModulePartNode modulePartNode = syntaxTree.rootNode();
        NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
        for (ModuleMemberDeclarationNode node : members) {
            if (node.kind().equals(SyntaxKind.FUNCTION_DEFINITION) || node.kind().equals(SyntaxKind.CLASS_DEFINITION)){
                for (ChildNodeEntry childNodeEntry : node.childEntries()) {
                    if (childNodeEntry.name().equals("functionName") || childNodeEntry.name().equals("className")) {
                        if (functionNameList.contains(childNodeEntry.node().get().toString())) {
//                            if(childNodeEntry.name().equals("className")){
//                                changeStreamReturn(node,memberDeclarationNodes);
//                            }else{
                                memberDeclarationNodes.add(node);

//                            }
                        }
                    }
                }
            }
        }

        List<ImportDeclarationNode> imports = new ArrayList<>();
        if (functionNameList.contains(GET_ENCODED_URI)) {
            ImportDeclarationNode importForUrl = GeneratorUtils.getImportDeclarationNode(BALLERINA, URL);
            imports.add(importForUrl);
        }
        if (functionNameList.contains(STREAM_GENERATOR)){
            ImportDeclarationNode importForUrl = GeneratorUtils.getImportDeclarationNode(NUVINDU,NUVINDU_PIPE);
            imports.add(importForUrl);
        }

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
        ModulePartNode utilModulePartNode =
                createModulePartNode(importsList, createNodeList(memberDeclarationNodes), createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree utilSyntaxTree = SyntaxTree.from(textDocument);
        return utilSyntaxTree.modifyWith(utilModulePartNode);
    }

//    private void changeStreamReturn (ModuleMemberDeclarationNode node, List<ModuleMemberDeclarationNode>
//    memberDeclarationNodes){
//        String returnName="User";
//        NodeList<Node> members=((ClassDefinitionNode)node).members();
//        for(Node member:members){
//            if( member instanceof FunctionDefinitionNode && ((FunctionDefinitionNode) member).functionName()
//            .equals("next")){
//               FunctionDefinitionNode functionDefinitionNode=  (FunctionDefinitionNode) member;
//               ReturnTypeDescriptorNode returnTypeDescriptorNode=functionDefinitionNode.functionSignature()
//               .returnTypeDesc().get();
//               UnionTypeDescriptorNode unionTypeDescriptorNode= (UnionTypeDescriptorNode) returnTypeDescriptorNode
//               .type();
//               RecordTypeDescriptorNode recordTypeDescriptorNode= (RecordTypeDescriptorNode) unionTypeDescriptorNode
//               .leftTypeDesc();
//                NodeList<Node> fields = recordTypeDescriptorNode.fields();
//                fields.remove(0);
//                RecordFieldNode returnFieldNode= createRecordFieldNode(null,null,createSimpleNameReferenceNode
//                (createIdentifierToken(returnName)),createIdentifierToken("value"),null,createToken(SEMICOLON_TOKEN));
//                fields.add(returnFieldNode);
//
//                break;
//            }
//
//        }
//        memberDeclarationNodes.add( node);
//
//    }
//

    /**
     * Set the type definition nodes related to the util functions generated.
     *
     * @param memberDeclarationNodes {@link ModuleMemberDeclarationNode}
     */
    private void getUtilTypeDeclarationNodes(List<ModuleMemberDeclarationNode> memberDeclarationNodes) {
        if ( queryParamsFound || headersFound ) {
            memberDeclarationNodes.add(getSimpleBasicTypeDefinitionNode());
        }
    }

    /**
     * Generates `SimpleBasicType` type.
     * <pre>
     *     type SimpleBasicType string|boolean|int|float|decimal;
     * </pre>
     *
     * @return
     */
    private TypeDefinitionNode getSimpleBasicTypeDefinitionNode() {

        TypeDescriptorNode typeDescriptorNode = createSingletonTypeDescriptorNode(
                createSimpleNameReferenceNode(createIdentifierToken("string|boolean|int|float|decimal")));
        return createTypeDefinitionNode(null, null,
                createToken(TYPE_KEYWORD), createIdentifierToken("SimpleBasicType"), typeDescriptorNode,
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Gets the path of the utils_asyncapi.bal template at the time of execution.
     *
     * @return Path to utils_asyncapi.bal file in the temporary directory created
     * @throws IOException When failed to get the templates/utils_asyncapi.bal file from resources
     */
    private Path getResourceFilePath() throws IOException {
        Path path = null;
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/utils_asyncapi.bal");
        if (inputStream != null) {
            String clientSyntaxTreeString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Path tmpDir = Files.createTempDirectory(".util-tmp" + System.nanoTime());
            path = tmpDir.resolve("utils.bal");
            try (PrintWriter writer = new PrintWriter(path.toString(), StandardCharsets.UTF_8)) {
                writer.print(clientSyntaxTreeString);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    FileUtils.deleteDirectory(tmpDir.toFile());
                } catch (IOException ex) {
                    LOGGER.error("Unable to delete the temporary directory : " + tmpDir, ex);
                }
            }));
        }
        return path;
    }
}
