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

import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ChildNodeEntry;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
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
import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES_MAP;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.STREAM_GENERATORS_MAP;
import static io.ballerina.asyncapi.core.GeneratorConstants.TYPE_INCLUSION_GENERATOR;
import static io.ballerina.asyncapi.core.GeneratorConstants.URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.XLIBB;
import static io.ballerina.asyncapi.core.GeneratorConstants.XLIBB_PIPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSingletonTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * This class is used to generate util file syntax tree according to the generated client.
 *
 * @since 1.3.0
 */
public class UtilGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilGenerator.class);
    private static final String GET_ENCODED_URI = "getEncodedUri";

    private static final String GET_PATH_FOR_QUERY_PARAM = "getPathForQueryParam";
    private static final String GET_COMBINE_HEADERS = "getCombineHeaders";
    private final ArrayList<String> streamReturns;
    //    private boolean streamFound = false;
    private boolean headersFound = false;
    private boolean queryParamsFound = false;
    private boolean pathParametersFound = false;

    public UtilGenerator(ArrayList<String> streamReturns) {
        this.streamReturns = streamReturns;
    }

    /**
     * //     * Set `queryParamsFound` flag to `true` when at least one query parameter found.
     * //     *
     * //     * @param flag Function will be called only in the occasions where flag needs to be set to `true`
     * //
     */
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

//    public void setStreamFound(boolean flag) {
//
//        this.streamFound = flag;
//    }

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
        List<ImportDeclarationNode> imports = new ArrayList<>();


        functionNameList.add(PIPES_MAP);
        ImportDeclarationNode importForXLibbPipe = GeneratorUtils.getImportDeclarationNode(XLIBB, XLIBB_PIPE);
        imports.add(importForXLibbPipe);


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


        List<ModuleMemberDeclarationNode> memberDeclarationNodes = new ArrayList<>();
        getUtilTypeDeclarationNodes(memberDeclarationNodes);

        if (!streamReturns.isEmpty()) {
            for (String returnType : streamReturns) {
                memberDeclarationNodes.add(createStreamGenerator(returnType));
            }
            functionNameList.add(TYPE_INCLUSION_GENERATOR);
            functionNameList.add(STREAM_GENERATORS_MAP);
//            functionNameList.add(STREAM_GENERATOR);
        }

        Path path = getResourceFilePath();

        Project project = ProjectLoader.loadProject(path);
        Package currentPackage = project.currentPackage();
        DocumentId docId = currentPackage.getDefaultModule().documentIds().iterator().next();
        SyntaxTree syntaxTree = currentPackage.getDefaultModule().document(docId).syntaxTree();

        ModulePartNode modulePartNode = syntaxTree.rootNode();
        NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
        for (ModuleMemberDeclarationNode node : members) {
            if (node.kind().equals(SyntaxKind.FUNCTION_DEFINITION) || node.kind().equals(SyntaxKind.CLASS_DEFINITION)
                    || node.kind().equals(SyntaxKind.TYPE_DEFINITION)) {
                for (ChildNodeEntry childNodeEntry : node.childEntries()) {
                    if (childNodeEntry.name().equals("functionName") || childNodeEntry.name().equals
                            ("className") || childNodeEntry.name().equals("typeName")) {
                        if (functionNameList.contains(childNodeEntry.node().get().toString().trim())) {

//                                if(((ClassDefinitionNode)childNodeEntry.node().get()).className()
//                                .equals("StreamGenerator")){
//                                        createStreamGenerator(memberDeclarationNodes,node);
//                                }
//
//                                changeStreamReturn(node,memberDeclarationNodes);
//                            }else{
                            memberDeclarationNodes.add(node);
                            break;


                        } else {
                            break;
                        }
                    }
                }
            }
        }

        if (functionNameList.contains(GET_ENCODED_URI)) {
            ImportDeclarationNode importForUrl = GeneratorUtils.getImportDeclarationNode(BALLERINA, URL);
            imports.add(importForUrl);
        }

//        if (functionNameList.contains(STREAM_GENERATOR)) {
//            ImportDeclarationNode importForUrl = GeneratorUtils.getImportDeclarationNode(NUVINDU, NUVINDU_PIPE);
//            imports.add(importForUrl);
//        }

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
        ModulePartNode utilModulePartNode =
                createModulePartNode(importsList, createNodeList(memberDeclarationNodes), createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree utilSyntaxTree = SyntaxTree.from(textDocument);
        return utilSyntaxTree.modifyWith(utilModulePartNode);
    }

    private ClassDefinitionNode createStreamGenerator(String returnType) {

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

        //Get dispatcherKey


//        ballerinaSchemaGenerator.setDispatcherKey(dispatcherKey);
//        ballerinaSchemaGenerator.setDispatcherStreamId(dispatcherStreamId);

        // Collect members for class definition node
        List<Node> memberNodeList = new ArrayList<>();

        // Add instance variable to class definition node
        memberNodeList.addAll(createClassInstanceVariables());

        // Add init function to class definition node
        memberNodeList.add(createInitFunction());

        // Add startInterMediator function
        memberNodeList.add(createNextFunction(returnType));

        memberNodeList.add(createCloseFunction());

        // Add startMessageReading function


        // Generate the class combining members
        MetadataNode metadataNode = getClassMetadataNode(returnType);


//        //Get channel name from the specification
//        String channelName = GeneratorUtils.
//                removeNonAlphanumeric(asyncAPI.getChannels().getItemNames().get(0).trim());
//
//        //Combine class name as titleName+channelName+Client
//        String stringClassName = titleName + channelName + CLIENT_CLASS_NAME;

        returnType = GeneratorUtils.getStreamGeneratorName(returnType);


        IdentifierToken className = createIdentifierToken(returnType + "StreamGenerator");
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD),
                createToken(ISOLATED_KEYWORD));

        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, createToken(OPEN_BRACE_TOKEN),
                createNodeList(memberNodeList), createToken(CLOSE_BRACE_TOKEN), null);

    }

//    private TypeDefinitionNode createGenObject() {
//        MetadataNode metadataNode = getGenObjectMetadataNode();
//        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
//        Token semicolonToken = createToken(SEMICOLON_TOKEN);
//        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
//
//        NodeList<Token> objectTypeQualifiers = createNodeList(
//                createToken(ISOLATED_KEYWORD));
//        ArrayList<Node> methodDeclarationNodes = new ArrayList<>();
//        SimpleNameReferenceNode nextReturnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
//                "record {|anydata value;|}|error?"));
//        SimpleNameReferenceNode closeReturnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
//                "error?"));
//
//        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD),
//                createToken(ISOLATED_KEYWORD));
//        //returns
//        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
//                createToken(RETURNS_KEYWORD), createEmptyNodeList(), nextReturnTypeNode);
//        ReturnTypeDescriptorNode closeReturnTypeDescriptorNode = createReturnTypeDescriptorNode(
//                createToken(RETURNS_KEYWORD), createEmptyNodeList(), closeReturnTypeNode);
//        FunctionSignatureNode nextFunctionSignatureNode = createFunctionSignatureNode(
//                createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN),
//                returnTypeDescriptorNode);
//        FunctionSignatureNode closeFunctionSignatureNode = createFunctionSignatureNode(
//                createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN),
//                closeReturnTypeDescriptorNode);
//        MethodDeclarationNode nextMethodDeclarationNode = createMethodDeclarationNode(null, createMetadataNode(
//                        null, createNodeList()), qualifierList, createToken(FUNCTION_KEYWORD),
//                createIdentifierToken("next"), createEmptyNodeList(), nextFunctionSignatureNode, semicolonToken);
//        MethodDeclarationNode closeMethodDeclarationNode = createMethodDeclarationNode(null, createMetadataNode(
//                        null, createNodeList()), qualifierList, createToken(FUNCTION_KEYWORD),
//                createIdentifierToken("close"), createEmptyNodeList(), closeFunctionSignatureNode, semicolonToken);
//        methodDeclarationNodes.add(nextMethodDeclarationNode);
//        methodDeclarationNodes.add(closeMethodDeclarationNode);
//        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
//        createIdentifierToken("Gen"),
//                createObjectTypeDescriptorNode(objectTypeQualifiers, createToken(OBJECT_KEYWORD),
//                openBraceToken, createNodeList(methodDeclarationNodes), closeBraceToken), semicolonToken);
//    }

    private List<Node> createClassInstanceVariables() {

        List<Node> fieldNodeList = new ArrayList<>();
        Token privateKeywordToken = createToken(PRIVATE_KEYWORD);
        Token finalKeywordToken = createToken(FINAL_KEYWORD);
        ArrayList<Token> prefixTokens = new ArrayList<>();
        prefixTokens.add(privateKeywordToken);
        prefixTokens.add(finalKeywordToken);
        NodeList<Token> qualifierList = createNodeList(prefixTokens);

        //*Generator
        TypeReferenceNode typeReferenceNode = createTypeReferenceNode(createToken(ASTERISK_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(TYPE_INCLUSION_GENERATOR)),
                createToken(SEMICOLON_TOKEN));


        //private final pipe:Pipe pipe;
        QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
        IdentifierToken pipe = createIdentifierToken("pipe");
        MetadataNode pipeNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode pipeField = createObjectFieldNode(pipeNode,
                null,
                qualifierList, pipeTypeName, pipe, null, null,
                createToken(SEMICOLON_TOKEN));

        //private final pipe:Pipe pipe;
        SimpleNameReferenceNode decimalType = createSimpleNameReferenceNode(createIdentifierToken("decimal"));
        IdentifierToken timeout = createIdentifierToken("timeout");

        MetadataNode timeoutNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode timeoutField = createObjectFieldNode(timeoutNode,
                null,
                qualifierList, decimalType, timeout, null, null,
                createToken(SEMICOLON_TOKEN));

        fieldNodeList.add(typeReferenceNode);
        fieldNodeList.add(pipeField);
        fieldNodeList.add(timeoutField);
//        fieldNodeList.add(readMessageQueueClientField);
//        fieldNodeList.add(pipesField);


//        // add apiKey instance variable when API key security schema is given
//        ObjectFieldNode apiKeyFieldNode = ballerinaAuthConfigGenerator.getApiKeyMapClassVariable();
//        if (apiKeyFieldNode != null) {
//            fieldNodeList.add(apiKeyFieldNode);
//        }
        return fieldNodeList;
    }



    private MetadataNode getClassMetadataNode(String returnType) {

        List<AnnotationNode> classLevelAnnotationNodes = new ArrayList<>();

        // Generate api doc
        List<Node> documentationLines = new ArrayList<>();
//        if (asyncAPI.getInfo().getDescription() != null && !asyncAPI.getInfo().getDescription().isBlank()) {
//            documentationLines.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
//                    asyncAPI.getInfo().getDescription(), false));
//        }
        documentationLines.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                "Stream generator class for " + returnType + " return type", false));
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(createNodeList(documentationLines));
        return createMetadataNode(apiDoc, createNodeList(classLevelAnnotationNodes));
    }

    private FunctionDefinitionNode createNextFunction(String returnType) {
        FunctionSignatureNode functionSignatureNode = getNextFunctionSignatureNode(returnType);
        FunctionBodyNode functionBodyNode = getNextFunctionBodyNode(returnType);
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("next");
        return createFunctionDefinitionNode(null, getDocCommentsForNextMethod(" " +
                        "Next method to return next stream message")
                , qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);

    }


    private FunctionDefinitionNode createCloseFunction() {
        FunctionSignatureNode functionSignatureNode = getCloseFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getCloseFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("close");
        return createFunctionDefinitionNode(null, getDocCommentsForNextMethod(" " +
                        "Close method to close used pipe")
                , qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);

    }

    private MetadataNode getDocCommentsForNextMethod(String comment) {

        List<Node> docs = new ArrayList<>();
//        String clientInitDocComment = "Gets invoked to initialize the `connector`.\n";
//        Map<String, JsonNode> extensions = ((AsyncApi25InfoImpl) asyncAPI.getInfo()).getExtensions();
//        if (extensions != null && !extensions.isEmpty()) {
//            for (Map.Entry<String, JsonNode> extension : extensions.entrySet()) {
//                if (extension.getKey().trim().equals(functionType)) {
//                    comment = comment.concat(extension.getValue().toString());
//                    break;
//                }
//            }
//        }
        //todo: setInitDocComment() pass the references
        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(comment, true));
        MarkdownDocumentationNode workerDoc = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(workerDoc, createEmptyNodeList());
    }


    private FunctionSignatureNode getNextFunctionSignatureNode(String returnType) {
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
                "record {|" + returnType + " value;|}|error?"));
        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);

        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN),
                returnTypeDescriptorNode);


    }

    private FunctionSignatureNode getCloseFunctionSignatureNode() {
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
                "error?"));
//        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);

        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN),
                returnTypeDescriptorNode);


    }

    private FunctionBodyNode getCloseFunctionBodyNode() {
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);

        FieldAccessExpressionNode pipeAccessNode = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(XLIBB_PIPE)));
        MethodCallExpressionNode gracefulMethodCallNode = createMethodCallExpressionNode(
                pipeAccessNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("gracefulClose")),
                openParenToken, createSeparatedNodeList(), closeParenToken);
        CheckExpressionNode graceFulCheckNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD), gracefulMethodCallNode);
        ExpressionStatementNode graceFulCheckExpressionNode = createExpressionStatementNode(
                null, graceFulCheckNode, semicolonToken);

        List<StatementNode> assignmentNodes = new ArrayList<>();
        assignmentNodes.add(graceFulCheckExpressionNode);
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);

        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN), null);

    }

    private FunctionBodyNode getNextFunctionBodyNode(String returnType) {
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);


        List<StatementNode> assignmentNodes = new ArrayList<>();

        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        SimpleNameReferenceNode responseVarNode = createSimpleNameReferenceNode(
                createIdentifierToken("response"));
        SimpleNameReferenceNode responseTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(returnType));
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                responseMessageVarNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
                openParanToken, createSeparatedNodeList(), closeParanToken);

        CheckExpressionNode cloneWithTypeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                cloneWithTypeMethodCallExpressionNode);


        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        responseTypeNode,
                        createFieldBindingPatternVarnameNode(
                                responseVarNode)),
                equalToken, cloneWithTypeCheck, semicolonToken);

        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken("self.timeout")));
        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken("self.pipe"));
        SimpleNameReferenceNode anydata = createSimpleNameReferenceNode(createIdentifierToken("anydata"));
        CheckExpressionNode callRelevantPipeConsumeNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(requestTypePipeNode, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),
                        openParanToken, createSeparatedNodeList(
                                responseTypeTimeOut
                        ), closeParanToken));
        VariableDeclarationNode responseMessageNode = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        anydata,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)),
                equalToken, callRelevantPipeConsumeNode, semicolonToken);

        assignmentNodes.add(responseMessageNode);
        assignmentNodes.add(responseTypeCloneStatement);

        ReturnStatementNode valueReturnStatementNode = createReturnStatementNode(createToken(RETURN_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(
                        "{value: response}")), semicolonToken);
        assignmentNodes.add(valueReturnStatementNode);

        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN), null);


    }

    private FunctionDefinitionNode createInitFunction() {

        FunctionSignatureNode functionSignatureNode = getStreamInitFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getStreamInitFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("init");
        return createFunctionDefinitionNode(null, getInitDocComment(), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private MetadataNode getInitDocComment() {
        List<Node> docs = new ArrayList<>();

        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc("StreamGenerator", true));

        // Create method description
        MarkdownParameterDocumentationLineNode pipeNodeDocs = DocCommentsGenerator.createAPIParamDoc(
                "pipe",
                "Pipe to hold stream messages");
        docs.add(pipeNodeDocs);
        MarkdownParameterDocumentationLineNode timeoutNodeDocs = DocCommentsGenerator.createAPIParamDoc(
                "timeout",
                "Waiting time");
        docs.add(timeoutNodeDocs);
//        MarkdownParameterDocumentationLineNode returnDoc = DocCommentsGenerator.createAPIParamDoc("return",
//                "An error if connector initialization failed");
//        docs.add(returnDoc);
        MarkdownDocumentationNode clientInitDoc = createMarkdownDocumentationNode(createNodeList(docs));

        return createMetadataNode(clientInitDoc, createEmptyNodeList());
    }


    private FunctionSignatureNode getStreamInitFunctionSignatureNode() {
        RequiredParameterNode pipeNode = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken("pipe:Pipe")),
                createIdentifierToken("pipe"));
        RequiredParameterNode decimalNode = createRequiredParameterNode(createNodeList(), createSimpleNameReferenceNode(
                        createIdentifierToken("decimal")),
                createIdentifierToken("timeout"));
        List<Node> parameterList = new ArrayList<>();
        parameterList.add(pipeNode);
        parameterList.add(createToken(COMMA_TOKEN));
        parameterList.add(decimalNode);
        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(createToken(ERROR_KEYWORD),
                createToken(QUESTION_MARK_TOKEN));
        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);

        return createFunctionSignatureNode(

                createToken(OPEN_PAREN_TOKEN), parameters, createToken(CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);
    }

    private FunctionBodyNode getStreamInitFunctionBodyNode() {
        List<StatementNode> assignmentNodes = new ArrayList<>();

        AssignmentStatementNode selfPipe = createAssignmentStatementNode(createIdentifierToken("self.pipe"),
                createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("pipe")), createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode selfTimeout = createAssignmentStatementNode(createIdentifierToken("self.timeout"),
                createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("timeout")), createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfPipe);
        assignmentNodes.add(selfTimeout);

        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN), null);

    }


    /**
     * Set the type definition nodes related to the util functions generated.
     *
     * @param memberDeclarationNodes {@link ModuleMemberDeclarationNode}
     */
    private void getUtilTypeDeclarationNodes(List<ModuleMemberDeclarationNode> memberDeclarationNodes) {
        if (queryParamsFound || headersFound) {
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
