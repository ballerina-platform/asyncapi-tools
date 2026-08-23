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
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.node.GenerateCloudImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateHttpImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateListenerClassNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;

/**
 * Generates the content for {@code listener.bal} from a list of HTTP service type definitions.
 *
 * <p>Builds the entire {@code Listener} class from Ballerina Compiler API AST factory
 * methods via {@link GenerateListenerClassNode}, then formats the result using the Ballerina
 * Formatter.
 */
public class ListenerGenerator {

    private final List<HttpServiceType> serviceTypes;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;
    private final Optional<ConnectionAuthConfig> connectionAuthConfig;
    private final String displayLabel;

    /**
     * Creates a generator for the given list of service types and optional webhook/connection
     * auth configuration.
     *
     * @param serviceTypes         the list of HTTP service type definitions to generate
     * @param webhookAuthConfig    the optional webhook authentication configuration
     * @param connectionAuthConfig the optional outbound (client-side) API authentication
     *                             configuration
     * @param displayLabel         the connector's display name, from the spec's {@code info.title}
     */
    public ListenerGenerator(List<HttpServiceType> serviceTypes,
            Optional<WebhookAuthConfig> webhookAuthConfig, Optional<ConnectionAuthConfig> connectionAuthConfig,
            String displayLabel) {
        this.serviceTypes = serviceTypes;
        this.webhookAuthConfig = webhookAuthConfig;
        this.connectionAuthConfig = connectionAuthConfig;
        this.displayLabel = displayLabel;
    }

    /**
     * Generates the full content for {@code listener.bal}.
     *
     * @return the generated Ballerina source string
     * @throws GeneratorException if no service types are defined or node construction fails
     */
    public String generate() throws GeneratorException {
        if (serviceTypes == null || serviceTypes.isEmpty()) {
            throw new GeneratorException("No service types defined; cannot generate listener.bal");
        }

        ClassDefinitionNode classNode = new GenerateListenerClassNode(serviceTypes, webhookAuthConfig,
                connectionAuthConfig, displayLabel).generate();
        ImportDeclarationNode httpImport = GenerateHttpImportNode.generate();
        ImportDeclarationNode cloudImport = GenerateCloudImportNode.generate();

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify()
                .withImports(createNodeList(httpImport, cloudImport))
                .withMembers(createNodeList(classNode))
                .apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new GeneratorException("Could not format the generated listener.bal code", e);
        }
    }
}
