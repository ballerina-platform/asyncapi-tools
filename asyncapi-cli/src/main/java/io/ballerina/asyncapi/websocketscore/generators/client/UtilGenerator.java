/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.client;

import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.generators.document.DocCommentsGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ChildNodeEntry;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
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
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
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

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.BALLERINA;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLOSE_STREAM_STATEMENT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DECIMAL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.INIT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.NEXT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.OPTIONAL_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPES_MAP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_ID;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RESPONSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SEMICOLON;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_GENERATOR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_GENERATORS_MAP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_NEXT_CONSUME_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_NEXT_RESPONSE_CLONE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.TIMEOUT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.TRUE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.TYPE_INCLUSION_GENERATOR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.URL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WITHIN_BRACE_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.XLIBB;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.XLIBB_PIPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSingletonTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CONTINUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;

/**
 * This class is used to generate util file syntax tree according to the generated client.
 *
 */
public class UtilGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilGenerator.class);
    private static final String GET_ENCODED_URI = "getEncodedUri";
    private static final String GET_PATH_FOR_QUERY_PARAM = "getPathForQueryParam";
    private static final String GET_COMBINE_HEADERS = "getCombineHeaders";
    private static final Token openParenToken = createToken(OPEN_PAREN_TOKEN);
    private static final Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
    private static final Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
    private static final Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
    private static final Token semicolonToken = createToken(SEMICOLON_TOKEN);
    private final ArrayList<String> streamReturns;
    private boolean headersFound = false;
    private boolean queryParamsFound = false;
    private boolean pathParametersFound = false;

    public UtilGenerator(ArrayList<String> streamReturns) {
        this.streamReturns = streamReturns;
    }


    /**
     * Set `queryParamsFound` flag to `true` when at least one query parameter found.
     *
     * @param flag Function will be called only in the occasions where flag needs to be set to `true`
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
            functionNameList.addAll(Arrays.asList(GET_ENCODED_URI, GET_PATH_FOR_QUERY_PARAM));
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
                            memberDeclarationNodes.add(node);
                        }
                        break;
                    }
                }
            }
        }

        if (functionNameList.contains(GET_ENCODED_URI)) {
            ImportDeclarationNode importForUrl = GeneratorUtils.getImportDeclarationNode(BALLERINA, URL);
            imports.add(importForUrl);
        }

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
        ModulePartNode utilModulePartNode = createModulePartNode(importsList, createNodeList(memberDeclarationNodes),
                createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree utilSyntaxTree = SyntaxTree.from(textDocument);
        return utilSyntaxTree.modifyWith(utilModulePartNode);
    }

    private ClassDefinitionNode createStreamGenerator(String returnType) {
        // Collect members for class definition node
        List<Node> memberNodeList = new ArrayList<>();

        memberNodeList.addAll(createClassInstanceVariables());
        memberNodeList.add(createInitFunction());
        memberNodeList.add(createNextFunction(returnType));
        memberNodeList.add(createCloseFunction());

        MetadataNode metadataNode = getClassMetadataNode(returnType);
        returnType = GeneratorUtils.getStreamGeneratorName(returnType);
        IdentifierToken className = createIdentifierToken(returnType + STREAM_GENERATOR);
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, openBraceToken, createNodeList(memberNodeList),
                closeBraceToken, null);
    }

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
                createSimpleNameReferenceNode(createIdentifierToken(TYPE_INCLUSION_GENERATOR)), semicolonToken);

        //private final pipe:Pipe pipe;
        ObjectFieldNode pipesField = createObjectFieldNode(null, null, qualifierList,
                NodeParser.parseTypeDescriptor(PIPES_MAP), createIdentifierToken(PIPES), null, null, semicolonToken);

        //private final string pipeId;
        SimpleNameReferenceNode stringType = createSimpleNameReferenceNode(createIdentifierToken(STRING));
        IdentifierToken pipeId = createIdentifierToken(PIPE_ID);

        ObjectFieldNode pipeIdField = createObjectFieldNode(null, null, qualifierList,
                stringType, pipeId, null, null, semicolonToken);

        //private final decimal timeout;
        SimpleNameReferenceNode decimalType = createSimpleNameReferenceNode(createIdentifierToken(DECIMAL));
        IdentifierToken timeout = createIdentifierToken(TIMEOUT);

        ObjectFieldNode timeoutField = createObjectFieldNode(null, null, qualifierList,
                decimalType, timeout, null, null, semicolonToken);

        fieldNodeList.add(typeReferenceNode);
        fieldNodeList.add(pipesField);
        fieldNodeList.add(pipeIdField);
        fieldNodeList.add(timeoutField);
        return fieldNodeList;
    }



    private MetadataNode getClassMetadataNode(String returnType) {
        List<AnnotationNode> classLevelAnnotationNodes = new ArrayList<>();
        // Generate api doc
        List<Node> documentationLines = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(
                "Stream generator class for " + returnType + " return type", false));
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(createNodeList(documentationLines));
        return createMetadataNode(apiDoc, createNodeList(classLevelAnnotationNodes));
    }

    private FunctionDefinitionNode createNextFunction(String returnType) {
        FunctionSignatureNode functionSignatureNode = getNextFunctionSignatureNode(returnType);
        FunctionBodyNode functionBodyNode = getNextFunctionBodyNode(returnType);
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(NEXT);
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION, null, qualifierList,
                createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(), functionSignatureNode,
                functionBodyNode);
    }


    private FunctionDefinitionNode createCloseFunction() {
        FunctionSignatureNode functionSignatureNode = getCloseFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getCloseFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("close");
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION, null, qualifierList,
                createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(), functionSignatureNode,
                functionBodyNode);
    }

    private FunctionSignatureNode getNextFunctionSignatureNode(String returnType) {
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
                "record {|" + returnType + " value;|}|error"));
        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        return createFunctionSignatureNode(openParenToken, createSeparatedNodeList(), closeParenToken,
                returnTypeDescriptorNode);
    }

    private FunctionSignatureNode getCloseFunctionSignatureNode() {
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(
                "error?"));
        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        return createFunctionSignatureNode(openParenToken, createSeparatedNodeList(), closeParenToken,
                returnTypeDescriptorNode);
    }

    private FunctionBodyNode getCloseFunctionBodyNode() {
        List<StatementNode> assignmentNodes = new ArrayList<>();
        assignmentNodes.add(NodeParser.parseStatement(CLOSE_STREAM_STATEMENT));
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(openBraceToken, null, statementList, closeBraceToken, null);
    }

    private FunctionBodyNode getNextFunctionBodyNode(String returnType) {

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(STREAM_NEXT_CONSUME_MESSAGE));
        statements.add(createIfElseStatementNode(createToken(IF_KEYWORD), NodeParser.parseExpression(
                MESSAGE_VAR_NAME + IS + OPTIONAL_ERROR), createBlockStatementNode(openBraceToken,
                createNodeList(NodeParser.parseStatement(CONTINUE_KEYWORD.stringValue() + SEMICOLON)),
                closeBraceToken), null));
        statements.add(NodeParser.parseStatement(String.format(STREAM_NEXT_RESPONSE_CLONE, returnType)));
        statements.add(NodeParser.parseStatement(RETURN_KEYWORD.stringValue() + SPACE +
                String.format(WITHIN_BRACE_TEMPLATE, "value: " + RESPONSE) + SEMICOLON));
        WhileStatementNode whileStatementNode = createWhileStatementNode(createToken(WHILE_KEYWORD),
                NodeParser.parseExpression(TRUE), createBlockStatementNode(openBraceToken, createNodeList(statements),
                        closeBraceToken), null);
        NodeList<StatementNode> statementList = createNodeList(whileStatementNode);
        return createFunctionBodyBlockNode(openBraceToken, null, statementList, closeBraceToken, null);
    }

    private FunctionDefinitionNode createInitFunction() {
        FunctionSignatureNode functionSignatureNode = getStreamInitFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getStreamInitFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(INIT);
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION, getInitDocComment(), qualifierList,
                createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(), functionSignatureNode,
                functionBodyNode);
    }

    private MetadataNode getInitDocComment() {
        List<Node> docs = new ArrayList<>();

        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc("StreamGenerator", true));
        // Create method description
        MarkdownParameterDocumentationLineNode pipeNodeDocs = DocCommentsGenerator.createAPIParamDoc(
                SIMPLE_PIPE, "Pipe to hold stream messages");
        docs.add(pipeNodeDocs);
        MarkdownParameterDocumentationLineNode timeoutNodeDocs = DocCommentsGenerator.createAPIParamDoc(
                TIMEOUT, "Waiting time");
        docs.add(timeoutNodeDocs);
        MarkdownDocumentationNode clientInitDoc = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(clientInitDoc, createEmptyNodeList());
    }


    private FunctionSignatureNode getStreamInitFunctionSignatureNode() {
        RequiredParameterNode pipeNode = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(PIPES_MAP)), createIdentifierToken(PIPES));
        RequiredParameterNode pipeIdNode = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(STRING)), createIdentifierToken(PIPE_ID));
        RequiredParameterNode decimalNode = createRequiredParameterNode(createNodeList(), createSimpleNameReferenceNode(
                        createIdentifierToken(DECIMAL)), createIdentifierToken(TIMEOUT));
        List<Node> parameterList = new ArrayList<>();
        parameterList.add(pipeNode);
        parameterList.add(createToken(COMMA_TOKEN));
        parameterList.add(pipeIdNode);
        parameterList.add(createToken(COMMA_TOKEN));
        parameterList.add(decimalNode);
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);
        return createFunctionSignatureNode(openParenToken, parameters, closeParenToken, null);
    }

    private FunctionBodyNode getStreamInitFunctionBodyNode() {
        List<StatementNode> assignmentNodes = new ArrayList<>();
        AssignmentStatementNode selfPipes = createAssignmentStatementNode(createIdentifierToken(
                SELF + DOT + PIPES), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(PIPES)), createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode selfPipeId = createAssignmentStatementNode(createIdentifierToken(
                        SELF + DOT + PIPE_ID), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(PIPE_ID)), createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode selfTimeout = createAssignmentStatementNode(createIdentifierToken(SELF + DOT +
                        TIMEOUT), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(TIMEOUT)), createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfPipes);
        assignmentNodes.add(selfPipeId);
        assignmentNodes.add(selfTimeout);
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(openBraceToken, null, statementList, closeBraceToken, null);
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
     *
     * @return
     */
    private TypeDefinitionNode getSimpleBasicTypeDefinitionNode() {
        TypeDescriptorNode typeDescriptorNode = createSingletonTypeDescriptorNode(
                createSimpleNameReferenceNode(createIdentifierToken("string|boolean|int|float|decimal")));
        return createTypeDefinitionNode(null, null, createToken(TYPE_KEYWORD),
                createIdentifierToken("SimpleBasicType"), typeDescriptorNode, semicolonToken);
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
