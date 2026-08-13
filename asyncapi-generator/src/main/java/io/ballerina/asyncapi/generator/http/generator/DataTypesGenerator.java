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

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.node.GenerateCryptoImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateHttpImportNode;
import io.ballerina.asyncapi.generator.http.node.GenerateListenerConfigNode;
import io.ballerina.asyncapi.generator.http.node.GenerateModuleMemberDeclarationNode;
import io.ballerina.asyncapi.generator.http.node.GenerateUnionDescriptorNode;
import io.ballerina.asyncapi.generator.http.node.Generator;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * Generates the content for {@code data_types.bal} from a map of AsyncAPI schema definitions.
 *
 */
public class DataTypesGenerator {

    public static final String GENERIC_DATA_TYPE = "GenericDataType";

    private final Map<String, AsyncApiSchema> schemas;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;
    private final Optional<ConnectionAuthConfig> connectionAuthConfig;

    /**
     * Creates a generator for the given schema map.
     *
     * @param schemas              map of schema name to schema object from the AsyncAPI components
     * @param webhookAuthConfig    the optional webhook authentication configuration, whose
     *                             {@code $config('name')} references become extra listener config fields
     * @param connectionAuthConfig the optional outbound (client-side) API authentication configuration,
     *                             whose type/flow determines any additional listener config fields
     *                             (e.g. {@code clientId}/{@code clientSecret}/{@code refreshToken})
     */
    public DataTypesGenerator(Map<String, AsyncApiSchema> schemas, Optional<WebhookAuthConfig> webhookAuthConfig,
            Optional<ConnectionAuthConfig> connectionAuthConfig) {
        this.schemas = schemas;
        this.webhookAuthConfig = webhookAuthConfig;
        this.connectionAuthConfig = connectionAuthConfig;
    }

    /**
     * Generates the full content for {@code data_types.bal}.
     *
     * @return the generated Ballerina source string
     * @throws GeneratorException if a schema entry cannot be converted to a valid AST node
     */
    public String generate() throws GeneratorException {
        List<String> extraConfigFields = webhookAuthConfig
                .map(WebhookAuthConfig::configFields)
                .orElseGet(List::of);
        List<ModuleMemberDeclarationNode> typeNodes = new ArrayList<>();
        typeNodes.add(GenerateListenerConfigNode.generateDefaultSecretConst());
        typeNodes.add(GenerateListenerConfigNode.generate(extraConfigFields, connectionAuthConfig));
        List<TypeDescriptorNode> typeDescriptors = new ArrayList<>();

        for (Map.Entry<String, AsyncApiSchema> entry : schemas.entrySet()) {
            Generator gen = new GenerateModuleMemberDeclarationNode(entry, schemas);
            ModuleMemberDeclarationNode node = gen.generate();
            if (node instanceof TypeDefinitionNode typeDefNode) {
                typeDescriptors.add(createSimpleNameReferenceNode(
                        createIdentifierToken(typeDefNode.typeName().text())));
            }
            typeNodes.add(node);
        }

        Generator unionGen = new GenerateUnionDescriptorNode(typeDescriptors, GENERIC_DATA_TYPE);
        typeNodes.add(unionGen.generate());

        List<ImportDeclarationNode> imports = new ArrayList<>();
        boolean x509AuthConfigured = connectionAuthConfig.isPresent()
                && ConnectionAuthConfig.TYPE_X509.equals(connectionAuthConfig.get().type());
        // X509's keyConfig field type is crypto:KeyStore|http:CertKey, so http is needed even
        // when no schema property requires an @http:Header annotation.
        if (needsHttpImport() || x509AuthConfigured) {
            imports.add(GenerateHttpImportNode.generate());
        }
        if (x509AuthConfigured) {
            // X509's cert/keyConfig fields are crypto:TrustStore|string and crypto:KeyStore|http:CertKey -
            // only pull in the crypto import when a scheme actually needs it.
            imports.add(GenerateCryptoImportNode.generate());
        }

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify()
                .withImports(createNodeList(imports))
                .withMembers(oldRoot.members().addAll(typeNodes))
                .apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new GeneratorException("Could not format the generated data_types.bal code", e);
        }
    }

    /**
     * Determines whether {@code data_types.bal} needs {@code import ballerina/http;} on account
     * of an {@code @http:Header {...}} annotation, generated for any schema property whose name
     * requires one (see {@link CodegenUtils#requiresHeaderAnnotation}). Ballerina treats an
     * unused import as a compile error, not a warning, so the import must only be added when at
     * least one property actually needs that annotation - or, separately (see {@link #generate}),
     * when X509 connection auth is configured, since {@code keyConfig}'s type also references
     * {@code http:CertKey}.
     *
     * @return {@code true} if any schema field name requires an {@code @http:Header} annotation
     */
    private boolean needsHttpImport() {
        return schemas.values().stream()
                .filter(schema -> schema.properties() != null)
                .flatMap(schema -> schema.properties().keySet().stream())
                .anyMatch(CodegenUtils::requiresHeaderAnnotation);
    }
}
