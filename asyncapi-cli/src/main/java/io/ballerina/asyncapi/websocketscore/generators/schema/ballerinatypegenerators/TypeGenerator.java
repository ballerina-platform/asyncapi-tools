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
package io.ballerina.asyncapi.websocketscore.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.BALLERINA;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONSTRAINT;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Abstract class for schema types.
 *
 */
public abstract class TypeGenerator {

    final List<TypeDefinitionNode> typeDefinitionNodeList = new ArrayList<>();
    final LinkedHashSet<String> imports = new LinkedHashSet<>();
    AsyncApi25SchemaImpl schema;
    String typeName;

    public TypeGenerator(AsyncApi25SchemaImpl schema, String typeName) {
        this.schema = schema;
        this.typeName = typeName;
    }

    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {
        return typeDefinitionNodeList;
    }

    public LinkedHashSet<String> getImports() {
        return imports;
    }

    /**
     * Create Type Definition Node for a given AsyncAPI schema.
     *
     * @param typeName  IdentifierToken of the name of the type
     * @param schemaDoc Documentation of the type
     *                  //     * @param typeAnnotations Annotations related to the type. Currently, only includes
     *                  `Deprecated` annotation
     * @return {@link TypeDefinitionNode}
     * @throws BallerinaAsyncApiExceptionWs when unsupported schema type is found
     */
    public TypeDefinitionNode generateTypeDefinitionNode(IdentifierToken typeName, List<Node> schemaDoc,
                                                         List<AnnotationNode> typeAnnotations)
            throws BallerinaAsyncApiExceptionWs {
        for (AnnotationNode annotation : typeAnnotations) {
            String annotationRef = annotation.annotReference().toString();
            if (annotationRef.startsWith(CONSTRAINT)) {
                ImportDeclarationNode constraintImport = GeneratorUtils.getImportDeclarationNode(BALLERINA, CONSTRAINT);
                //Here we are unable to add ImportDeclarationNode since newly generated node has different hashcode.
                imports.add(constraintImport.toSourceCode());
            }
        }

        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createNodeList(typeAnnotations));
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, generateTypeDescriptorNode(),
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Create Type Descriptor Node for a given AsyncAPI schema.
     *
     * @return {@link TypeDescriptorNode}
     * @throws BallerinaAsyncApiExceptionWs when unsupported schema type is found
     */
    public abstract TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiExceptionWs;
}
