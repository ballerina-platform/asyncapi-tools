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

import io.ballerina.asyncapi.websocketscore.GeneratorConstants;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.API_KEY_CONFIG;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModuleVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNamedArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * This class use for generating boilerplate codes for test cases.
 *
 * @since 1.3.0
 */
public class TestGenerator {
    private final IntermediateClientGenerator intermediateClientGenerator;
    private final List<String> remoteFunctionNameList;
    private final String configFileName;
    private final boolean isHttpOrOAuth;

    public TestGenerator(IntermediateClientGenerator intermediateClientGenerator) {
        this.intermediateClientGenerator = intermediateClientGenerator;
        this.remoteFunctionNameList = intermediateClientGenerator.getRemoteFunctionNameList();
        this.configFileName = "";
        this.isHttpOrOAuth = false;
    }

    /**
     * Generate test.bal file synatx tree.
     *
     * @return {@link SyntaxTree}
     * @throws IOException                Throws exception when syntax tree not modified
     * @throws BallerinaAsyncApiExceptionWs Throws exception if open api validation failed
     */
    public SyntaxTree generateSyntaxTree() throws IOException, BallerinaAsyncApiExceptionWs {
        List<FunctionDefinitionNode> functions = new ArrayList<>();
        getFunctionDefinitionNodes(functions);
        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>(getModuleVariableDeclarationNodes());
        NodeList<ImportDeclarationNode> imports = getImportNodes();
        nodes.addAll(functions);
        Token eofToken = createIdentifierToken("");
        ModulePartNode modulePartNode = createModulePartNode(imports, createNodeList(nodes), eofToken);
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

    /**
     * Generate import nodes for test.bal file.
     *
     * @return Import node list
     */
    private NodeList<ImportDeclarationNode> getImportNodes() {
        NodeList<ImportDeclarationNode> imports;
        ImportDeclarationNode importForTest = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA,
                GeneratorConstants.MODULE_TEST);
        ImportDeclarationNode importForHttp = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA,
                GeneratorConstants.HTTP);
        if (isHttpOrOAuth) {
            imports = AbstractNodeFactory.createNodeList(importForTest, importForHttp);
        } else {
            imports = AbstractNodeFactory.createNodeList(importForTest);
        }
        return imports;
    }

    /**
     * Returns String of the relevant config file to the generated test.bal.
     *
     * @return {@link String}
     * @throws IOException Throws an exception if file not exists
     */
    public String getConfigTomlFile() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        if (!configFileName.isBlank()) {
            if (!isHttpOrOAuth) {
                StringBuilder configFileContent = new StringBuilder("[" + API_KEY_CONFIG + "]\n");
                for (String apiKey : intermediateClientGenerator.getApiKeyNameList()) {
                    configFileContent.append(GeneratorUtils.getValidName(apiKey, false)).
                            append(" = \"<Enter Value>\"\n");
                }
                return configFileContent.toString();
            }
            InputStream inputStream = classLoader.getResourceAsStream("config_toml_files/" + configFileName);
            if (inputStream != null) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    /**
     * Provide module variable declaration nodes of the test.bal file.
     * -- ex: Variable declaration nodes of test.bal for basic auth.
     * <pre>
     *      configurable http:CredentialsConfig & readonly authConfig = ?;
     *      ClientConfig clientConfig = {authConfig : authConfig};
     *      Client baseClient = check new Client(clientConfig, serviceUrl = "https://domain/services/data");
     * </pre>
     *
     * @return {@link List<ModuleVariableDeclarationNode>} List of variable declaration nodes
     */
    private List<ModuleVariableDeclarationNode> getModuleVariableDeclarationNodes() {
        List<ModuleVariableDeclarationNode> moduleVariableDeclarationNodes = new ArrayList<>();
        moduleVariableDeclarationNodes.add(getClientInitForNoAuth());
        return moduleVariableDeclarationNodes;
    }

    /**
     * Generate client initialization node when no auth mehanism found.
     *
     * @return {@link ModuleVariableDeclarationNode}
     */
    private ModuleVariableDeclarationNode getClientInitForNoAuth() {
        String clientName = intermediateClientGenerator.getClientName();
        String serverURL = intermediateClientGenerator.getServerUrl();
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        BuiltinSimpleNameReferenceNode typeBindingPattern = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(clientName));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken("baseClient"));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                bindingPattern);
        Token openParenArg = createToken(OPEN_PAREN_TOKEN);
        Token closeParenArg = createToken(CLOSE_PAREN_TOKEN);
        Token newKeyWord = createToken(NEW_KEYWORD);
        List<Node> argumentsList = new ArrayList<>();
        ExpressionNode expressionNode = createRequiredExpressionNode(createIdentifierToken
                ("\"" + serverURL + "\""));
        NamedArgumentNode positionalArgumentNode = createNamedArgumentNode(createSimpleNameReferenceNode
                (createIdentifierToken(GeneratorConstants.SERVICE_URL)), createToken(EQUAL_TOKEN), expressionNode);
        argumentsList.add(positionalArgumentNode);
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenArg, arguments,
                closeParenArg);
        TypeDescriptorNode clientClassType = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken
                (clientName));
        ExplicitNewExpressionNode explicitNewExpressionNode = createExplicitNewExpressionNode(newKeyWord,
                clientClassType, parenthesizedArgList);
        CheckExpressionNode initializer = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                explicitNewExpressionNode);
        NodeList<Token> nodeList = createEmptyNodeList();
        return createModuleVariableDeclarationNode(metadataNode, null, nodeList, typedBindingPatternNode,
                createToken(EQUAL_TOKEN), initializer, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generate test functions for each remote function in the generated client.bal.
     * <pre>
     *      @test:Config {}
     *      isolated function  testCurrentWeatherData() {
     *      }
     * </pre>
     *
     * @param functions Empty function definition node list
     */
    private void getFunctionDefinitionNodes(List<FunctionDefinitionNode> functions) {
        if (!remoteFunctionNameList.isEmpty()) {
            for (String functionName : remoteFunctionNameList) {
                MetadataNode metadataNode = getAnnotation();
                Token functionKeyWord = createToken(FUNCTION_KEYWORD);
                IdentifierToken testFunctionName = createIdentifierToken(GeneratorConstants.PREFIX_TEST
                        + modifyFunctionName(functionName.trim()));
                FunctionSignatureNode functionSignatureNode = getFunctionSignature();
                FunctionBodyNode functionBodyNode = getFunctionBody();
                NodeList<Node> relativeResourcePath = createEmptyNodeList();
                Token isolatedQualifierNode = AbstractNodeFactory.createIdentifierToken("isolated");
                NodeList<Token> qualifierList = createNodeList(isolatedQualifierNode);
                FunctionDefinitionNode functionDefinitionNode = createFunctionDefinitionNode(FUNCTION_DEFINITION,
                        metadataNode, qualifierList, functionKeyWord, testFunctionName, relativeResourcePath,
                        functionSignatureNode, functionBodyNode);
                functions.add(functionDefinitionNode);
            }
        }
    }

    /**
     * Generate function signature node.
     * -- ex: {@code isolated function  testCurrentWeatherData()}
     *
     * @return {@link FunctionSignatureNode}
     */
    private FunctionSignatureNode getFunctionSignature() {
        List<Node> parameterList = new ArrayList<>();
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);
        return createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN),
                parameters, createToken(CLOSE_PAREN_TOKEN), null);
    }

    /**
     * Generate empty function body node.
     * -- ex: {@code isolated function  testCurrentWeatherData()}
     *
     * @return {@link FunctionBodyNode}
     */
    private FunctionBodyNode getFunctionBody() {
        List<StatementNode> statementsList = new ArrayList<>();
        NodeList<StatementNode> statementsNodeList = createNodeList(statementsList);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementsNodeList, createToken(CLOSE_BRACE_TOKEN), null);
    }

    /**
     * Generate test annotation node.
     * --ex: {@code @test:Config {}}
     *
     * @return {@link MetadataNode}
     */
    private MetadataNode getAnnotation() {
        SimpleNameReferenceNode annotateReference =
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.ANNOT_TEST));
        List<Node> fileds = new ArrayList<>();
        SeparatedNodeList<MappingFieldNode> fieldNodesList = createSeparatedNodeList(fileds);
        MappingConstructorExpressionNode annotValue = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), fieldNodesList, createToken(CLOSE_BRACE_TOKEN));
        AnnotationNode annotationNode = createAnnotationNode(createToken(SyntaxKind.AT_TOKEN),
                annotateReference, annotValue);
        return createMetadataNode(null, createNodeList(annotationNode));
    }

    /**
     * Modify function name by capitalizing the first letter.
     *
     * @param name remote function name
     * @return formatted name
     */
    private String modifyFunctionName(String name) {
        return name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
    }
}
