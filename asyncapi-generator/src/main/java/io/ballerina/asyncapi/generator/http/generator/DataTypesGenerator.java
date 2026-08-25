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
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Creates a generator for the given schema map.
     *
     * @param schemas           map of schema name to schema object from the AsyncAPI components
     * @param webhookAuthConfig the optional webhook authentication configuration, whose
     *                          {@code $config('name')} references become extra listener config fields
     */
    public DataTypesGenerator(Map<String, AsyncApiSchema> schemas, Optional<WebhookAuthConfig> webhookAuthConfig) {
        this.schemas = schemas;
        this.webhookAuthConfig = webhookAuthConfig;
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
        typeNodes.add(GenerateListenerConfigNode.generate(extraConfigFields));

        // Loose schemas (see isLooseObjectSchema) are ordered last in the union.
        List<TypeDescriptorNode> strictTypeDescriptors = new ArrayList<>();
        List<TypeDescriptorNode> looseTypeDescriptors = new ArrayList<>();

        // Shared across entries so hoisted inline types can't collide on name.
        Set<String> claimedTypeNames = new HashSet<>();
        for (String schemaName : schemas.keySet()) {
            claimedTypeNames.add(CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(schemaName.trim()), true));
        }

        for (Map.Entry<String, AsyncApiSchema> entry : schemas.entrySet()) {
            GenerateModuleMemberDeclarationNode gen =
                    new GenerateModuleMemberDeclarationNode(entry, schemas, claimedTypeNames);
            ModuleMemberDeclarationNode node = gen.generate();
            if (node instanceof TypeDefinitionNode typeDefNode) {
                TypeDescriptorNode reference = createSimpleNameReferenceNode(
                        createIdentifierToken(typeDefNode.typeName().text()));
                if (isLooseObjectSchema(entry.getValue())) {
                    looseTypeDescriptors.add(reference);
                } else {
                    strictTypeDescriptors.add(reference);
                }
            }
            typeNodes.add(node);
            typeNodes.addAll(gen.getHoistedTypes());
        }

        List<TypeDescriptorNode> typeDescriptors = new ArrayList<>(strictTypeDescriptors);
        typeDescriptors.addAll(looseTypeDescriptors);

        Generator unionGen = new GenerateUnionDescriptorNode(typeDescriptors, GENERIC_DATA_TYPE,
                "The union of every possible webhook payload type this listener can receive.");
        typeNodes.add(unionGen.generate());

        List<ImportDeclarationNode> imports = new ArrayList<>();
        if (needsHttpImport()) {
            imports.add(GenerateHttpImportNode.generate());
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
     * Determines whether {@code data_types.bal} needs {@code import ballerina/http;}. The only
     * thing in this file that ever references {@code http:} is an {@code @http:Header {...}}
     * annotation, generated for any schema property whose name requires one (see
     * {@link CodegenUtils#requiresHeaderAnnotation}). Ballerina treats an unused import as a
     * compile error, not a warning, so the import must only be added when at least one property
     * actually needs that annotation.
     *
     * @return {@code true} if any schema field name requires an {@code @http:Header} annotation
     */
    private boolean needsHttpImport() {
        return schemas.values().stream()
                .filter(schema -> schema.properties() != null)
                .flatMap(schema -> schema.properties().keySet().stream())
                .anyMatch(CodegenUtils::requiresHeaderAnnotation);
    }

    /**
     * A top-level object schema is "loose" if it declares properties but requires none of them
     * (e.g. {@code Installation}: {@code record { int id?; string node_id?; }}). Ballerina's
     * {@code cloneWithType} resolves a union target by a first-match policy - if a member's shape
     * is permissive enough to structurally accept almost any object, it can capture a payload that
     * was really meant for a more specific, concrete member listed later in the union. Ordering
     * loose members last in {@code GenericDataType} avoids that, without needing to guess at
     * exactly which other payload a given loose schema might collide with.
     *
     * <p>Only applies to schemas that generate as a plain record (mirrors the
     * {@code generateRecord} branch in {@link GenerateModuleMemberDeclarationNode#generate()}) -
     * enums, type aliases, and {@code allOf}-merged records have different {@code cloneWithType}
     * matching semantics and aren't reordered by this rule.
     *
     * @param schema the top-level schema to classify
     * @return {@code true} if the schema has properties but none are required
     */
    private boolean isLooseObjectSchema(AsyncApiSchema schema) {
        if (schema.properties() == null || schema.properties().isEmpty()) {
            return false;
        }
        return schema.required() == null || schema.required().isEmpty();
    }
}
