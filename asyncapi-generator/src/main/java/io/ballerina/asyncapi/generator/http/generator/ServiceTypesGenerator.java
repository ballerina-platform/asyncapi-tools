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
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.node.GenerateServiceTypeNode;
import io.ballerina.asyncapi.generator.http.node.GenerateUnionDescriptorNode;
import io.ballerina.asyncapi.generator.http.node.Generator;
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

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * Generates the content for {@code service_types.bal} from a list of HTTP service type definitions.
 * Uses the Ballerina Compiler API to produce:
 * One {@code public type ... service object { ... };} block per {@link HttpServiceType},
 *       each containing remote function declarations.
 * A {@code GenericServiceType} union type listing all generated service type names.
 */
public class ServiceTypesGenerator {

    public static final String GENERIC_SERVICE_TYPE = "GenericServiceType";

    private final List<HttpServiceType> serviceTypes;

    /**
     * Creates a generator for the given list of service types.
     *
     * @param serviceTypes the list of HTTP service type definitions to generate
     */
    public ServiceTypesGenerator(List<HttpServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    /**
     * Generates the full content for {@code service_types.bal}.
     *
     * @return the generated Ballerina source string
     * @throws GeneratorException if a service type has no remote functions
     */
    public String generate() throws GeneratorException {
        List<ModuleMemberDeclarationNode> serviceNodes = new ArrayList<>();
        List<TypeDescriptorNode> typeDescriptors = new ArrayList<>();

        for (HttpServiceType serviceType : serviceTypes) {
            Generator gen = new GenerateServiceTypeNode(
                    serviceType.serviceTypeName(), serviceType.remoteFunctions());
            TypeDefinitionNode typeDefNode = gen.generate();
            typeDescriptors.add(createSimpleNameReferenceNode(
                    createIdentifierToken(typeDefNode.typeName().text())));
            serviceNodes.add(typeDefNode);
        }

        Generator unionGen = new GenerateUnionDescriptorNode(typeDescriptors, GENERIC_SERVICE_TYPE,
                "The union of every service type that can be attached to this listener.");
        serviceNodes.add(unionGen.generate());

        TextDocument textDocument = TextDocuments.from("\n");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify().withMembers(oldRoot.members().addAll(serviceNodes)).apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new GeneratorException("Could not format the generated service_types.bal code", e);
        }
    }
}
