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
package io.ballerina.asyncapi.generator.http.generator;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.node.GenerateCryptoImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateDispatcherServiceNode;
import io.ballerina.asyncapi.generator.http.node.GenerateHttpImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateLogImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateNativeHandlerImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateTimeImportNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;

/**
 * Generates the content for {@code dispatcher_service.bal} from a list of HTTP service type
 * definitions and an event identifier configuration.
 *
 * <p>Builds the entire {@code DispatcherService} class from Ballerina Compiler API AST factory
 * methods via {@link GenerateDispatcherServiceNode}, then formats the result using the Ballerina
 * Formatter. Supports {@code "body"}, {@code "header"}, and {@code "composite"} identifier types.
 */
public class DispatcherGenerator {

    private final List<HttpServiceType> serviceTypes;
    private final EventIdentifierConfig identifierConfig;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;

    /**
     * Creates a generator for the given service types, event identifier configuration, and optional
     * webhook auth configuration.
     *
     * @param serviceTypes      the list of HTTP service type definitions
     * @param identifierConfig  the resolved event identifier type and path
     * @param webhookAuthConfig the optional webhook authentication configuration
     */
    public DispatcherGenerator(List<HttpServiceType> serviceTypes, EventIdentifierConfig identifierConfig,
            Optional<WebhookAuthConfig> webhookAuthConfig) {
        this.serviceTypes = serviceTypes;
        this.identifierConfig = identifierConfig;
        this.webhookAuthConfig = webhookAuthConfig;
    }

    /**
     * Generates the full content for {@code dispatcher_service.bal}.
     *
     * @return the generated Ballerina source string
     * @throws GeneratorException if no service types are defined or the identifier type is invalid
     */
    public String generate() throws GeneratorException {
        if (serviceTypes == null || serviceTypes.isEmpty()) {
            throw new GeneratorException("No service types defined; cannot generate dispatcher_service.bal");
        }

        String identifierType = identifierConfig.type();
        if (!EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(identifierType)
                && !EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(identifierType)
                && !EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(identifierType)) {
            throw new GeneratorException(String.format(
                    "Unsupported identifier type: %s. Expected \"body\", \"header\" or \"composite\".",
                    identifierType));
        }

        ClassDefinitionNode classNode =
                new GenerateDispatcherServiceNode(serviceTypes, identifierConfig, webhookAuthConfig).generate();

        List<ImportDeclarationNode> imports = new ArrayList<>();
        imports.add(GenerateHttpImportNode.generate());
        imports.add(GenerateNativeHandlerImportNode.generate());
        if (webhookAuthConfig.isPresent()) {
            imports.add(GenerateCryptoImportNode.generate());
            imports.add(GenerateLogImportNode.generate());
            if (webhookAuthConfig.get().freshnessHeader() != null) {
                imports.add(GenerateTimeImportNode.generate());
            }
        }

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify()
                .withImports(createNodeList(imports))
                .withMembers(createNodeList(classNode))
                .apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new GeneratorException("Could not format the generated dispatcher_service.bal code", e);
        }
    }
}
