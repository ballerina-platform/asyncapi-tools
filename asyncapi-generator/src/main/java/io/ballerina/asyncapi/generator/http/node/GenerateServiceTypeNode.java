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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownParameterDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MARKDOWN_PARAMETER_DOCUMENTATION_LINE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MARKDOWN_RETURN_PARAMETER_DOCUMENTATION_LINE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MINUS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PLUS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SERVICE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates a {@code public type <ServiceName> service object { ... };} definition node
 * for {@code service_types.bal}.
 */
public class GenerateServiceTypeNode implements Generator {

    private final String serviceTypeName;
    private final List<HttpRemoteFunction> remoteFunctions;

    /**
     * Creates a generator for a single service type.
     *
     * @param serviceTypeName  the raw service type name (will be Pascal-cased and suffixed)
     * @param remoteFunctions  the remote functions to declare in the service type
     */
    public GenerateServiceTypeNode(String serviceTypeName, List<HttpRemoteFunction> remoteFunctions) {
        this.serviceTypeName = serviceTypeName;
        this.remoteFunctions = remoteFunctions;
    }

    @Override
    public TypeDefinitionNode generate() throws GeneratorException {
        if (remoteFunctions.isEmpty()) {
            throw new GeneratorException(String.format(
                    "Remote functions list is empty in the service type %s", serviceTypeName));
        }

        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(
                createToken(ERROR_KEYWORD), createToken(QUESTION_MARK_TOKEN));
        ReturnTypeDescriptorNode returnTypeDescriptor = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);

        List<Node> methods = new ArrayList<>();
        for (HttpRemoteFunction fn : remoteFunctions) {
            String eventType = CodegenUtils.getValidName(
                    CodegenUtils.escapeIdentifier(fn.eventType().trim()), true);
            BuiltinSimpleNameReferenceNode typeNode = createBuiltinSimpleNameReferenceNode(
                    null, createIdentifierToken(eventType));
            List<Node> params = new ArrayList<>();
            params.add(createRequiredParameterNode(createEmptyNodeList(), typeNode, createIdentifierToken("payload")));
            String namingBasis = fn.displayLabel() != null ? fn.displayLabel() : fn.functionName();
            String functionName = CodegenUtils.getFunctionNameByEventName(namingBasis);
            MethodDeclarationNode method = createMethodDeclarationNode(
                    SyntaxKind.METHOD_DECLARATION,
                    buildFunctionDoc("Triggered on " + humanize(functionName) + ".", eventType),
                    createNodeList(createToken(REMOTE_KEYWORD)),
                    createToken(SyntaxKind.FUNCTION_KEYWORD),
                    createIdentifierToken(functionName),
                    createEmptyNodeList(),
                    createFunctionSignatureNode(
                            createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(params),
                            createToken(CLOSE_PAREN_TOKEN), returnTypeDescriptor),
                    createToken(SEMICOLON_TOKEN));
            methods.add(method);
        }

        String serviceName = CodegenUtils.getServiceTypeNameByServiceName(serviceTypeName);
        IdentifierToken typeNameToken = AbstractNodeFactory.createIdentifierToken(serviceName);
        ObjectTypeDescriptorNode objectType = NodeFactory.createObjectTypeDescriptorNode(
                createNodeList(createToken(SERVICE_KEYWORD)),
                createToken(OBJECT_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                createNodeList(methods), createToken(CLOSE_BRACE_TOKEN));
        return createTypeDefinitionNode(
                buildDoc("Attachable service type exposing the " + serviceName + " family of webhook events."),
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeNameToken, objectType, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Builds a single-line doc-comment metadata node.
     *
     * @param text the doc-comment text
     * @return the metadata node carrying that one line
     */
    private MetadataNode buildDoc(String text) {
        List<Node> docLines = new ArrayList<>();
        docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                createToken(HASH_TOKEN), createNodeList(createIdentifierToken(text))));
        return createMetadataNode(createMarkdownDocumentationNode(createNodeList(docLines)), createEmptyNodeList());
    }

    /**
     * Builds a remote function's doc comment: a one-line summary plus {@code + payload} and
     * {@code + return} parameter documentation lines, so the compiler doesn't flag either as
     * undocumented.
     *
     * @param summary   the one-line doc-comment summary
     * @param eventType the payload's type name, used in the {@code payload} param description
     * @return the metadata node carrying the full doc comment
     */
    private MetadataNode buildFunctionDoc(String summary, String eventType) {
        List<Node> docLines = new ArrayList<>();
        docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                createToken(HASH_TOKEN), createNodeList(createIdentifierToken(summary))));
        docLines.add(createMarkdownParameterDocumentationLineNode(MARKDOWN_PARAMETER_DOCUMENTATION_LINE,
                createToken(HASH_TOKEN), createToken(PLUS_TOKEN), createIdentifierToken("payload"),
                createToken(MINUS_TOKEN),
                createNodeList(createIdentifierToken("the " + eventType + " webhook payload"))));
        docLines.add(createMarkdownParameterDocumentationLineNode(MARKDOWN_RETURN_PARAMETER_DOCUMENTATION_LINE,
                createToken(HASH_TOKEN), createToken(PLUS_TOKEN), createIdentifierToken("return"),
                createToken(MINUS_TOKEN),
                createNodeList(createIdentifierToken("an error if handling the event fails"))));
        return createMetadataNode(createMarkdownDocumentationNode(createNodeList(docLines)), createEmptyNodeList());
    }

    /**
     * Turns a Ballerina-safe function name like {@code onCompanyCreation} into a readable phrase
     * like {@code "Company creation"}: drops the leading {@code on}, then splits on capitalization
     * boundaries and lowercases every word after the first.
     *
     * @param functionName the {@code onXyz}-style function name
     * @return a human-readable phrase derived from it
     */
    private String humanize(String functionName) {
        String withoutOn = functionName.startsWith("on") && functionName.length() > 2
                ? functionName.substring(2) : functionName;
        StringBuilder phrase = new StringBuilder();
        for (int i = 0; i < withoutOn.length(); i++) {
            char c = withoutOn.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                phrase.append(' ');
            }
            phrase.append(i == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
        }
        return phrase.toString();
    }
}
