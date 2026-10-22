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
package io.ballerina.asyncapi.trigger.plugin.endpointyaml.generator;

import io.ballerina.asyncapi.trigger.plugin.AsyncAPICompilerPluginUtil;
import io.ballerina.asyncapi.trigger.plugin.AsyncAPIDiagnostic;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Computes the endpoint details of a service attached to a trigger listener.
 *
 * <p>Follows the {@code ballerina/http} plugin's {@code EndpointYamlGenerator}: the same base path, the same type,
 * and the same rules for the port (a literal, a constant, a module variable or a configurable's default value;
 * an error for a configurable without a default or any other expression).
 */
public class EndpointYamlGenerator {

    private static final String REST = "REST";
    private static final String SCHEMA_PATH = "";
    // The port and its parameter in generated trigger listeners (listenTo in tool versions before 0.13).
    private static final int DEFAULT_PORT = 8090;
    private static final Set<String> PORT_PARAMS = Set.of("listenOn", "listenTo");
    private static final String HTTP_LISTENER_PORT_PARAM = "port";

    private final ServiceDeclarationNode node;
    private final ExpressionNode listener;
    private final SyntaxNodeAnalysisContext context;

    public EndpointYamlGenerator(ServiceDeclarationNode node, ExpressionNode listener,
                                 SyntaxNodeAnalysisContext context) {
        this.node = node;
        this.listener = listener;
        this.context = context;
    }

    /**
     * Returns the endpoint, or empty when its port cannot be determined, in which case an error is added.
     *
     * @param diagnostics the list to add diagnostics to
     * @return the endpoint
     */
    public Optional<Endpoint> getEndpoint(List<Diagnostic> diagnostics) {
        String basePath = getBasePath(node);
        return getListenerPort(diagnostics)
                .map(port -> new Endpoint(basePath, port, basePath, REST, SCHEMA_PATH));
    }

    /**
     * Returns the service base path the way the {@code ballerina/http} plugin computes it.
     *
     * @param node the service declaration
     * @return the base path, empty when the service has none
     */
    public static String getBasePath(ServiceDeclarationNode node) {
        StringBuilder serviceBasePath = new StringBuilder();
        NodeList<Node> resourcePathNode = node.absoluteResourcePath();
        for (Node identifierNode : resourcePathNode) {
            serviceBasePath.append(identifierNode.toString().replace("\"", "").trim());
        }
        return serviceBasePath.toString();
    }

    private Optional<Integer> getListenerPort(List<Diagnostic> diagnostics) {
        Optional<ExpressionNode> construction = resolve(listener);
        Optional<Integer> portParam = getPortParamIndex();
        if (construction.isEmpty() || !isNewExpression(construction.get()) || portParam.isEmpty()) {
            return unsupported(listener, diagnostics);
        }
        Optional<ExpressionNode> portArg = getArgument(construction.get(), portParam.get(), PORT_PARAMS);
        if (portArg.isEmpty()) {
            return Optional.of(DEFAULT_PORT);
        }
        return getPort(portArg.get(), diagnostics);
    }

    // The port of a listenOn argument: a port number, or an http:Listener to share.
    private Optional<Integer> getPort(ExpressionNode expression, List<Diagnostic> diagnostics) {
        ExpressionNode value = unwrapCheck(expression);
        if (value instanceof BasicLiteralNode literal && literal.kind() == SyntaxKind.NUMERIC_LITERAL) {
            try {
                return Optional.of(Integer.parseInt(literal.literalToken().text().trim()));
            } catch (NumberFormatException e) {
                return unsupported(value, diagnostics);
            }
        }
        if (isNewExpression(value)) {
            Optional<ExpressionNode> httpPort = getArgument(value, 0, Set.of(HTTP_LISTENER_PORT_PARAM));
            return httpPort.isPresent() ? getPort(httpPort.get(), diagnostics) : unsupported(value, diagnostics);
        }
        if (value instanceof SimpleNameReferenceNode reference) {
            Optional<ModuleMemberDeclarationNode> declaration = findDeclaration(reference.name().text());
            if (declaration.isPresent() && isConfigurable(declaration.get())) {
                ExpressionNode initializer = ((ModuleVariableDeclarationNode) declaration.get()).initializer()
                        .orElse(null);
                if (initializer == null || initializer.kind() == SyntaxKind.REQUIRED_EXPRESSION) {
                    diagnostics.add(diagnostic(AsyncAPIDiagnostic.ASYNCAPI_ERROR_101,
                            initializer != null ? initializer : value));
                    return Optional.empty();
                }
                diagnostics.add(diagnostic(AsyncAPIDiagnostic.ASYNCAPI_WARNING_101, initializer));
                return getPort(initializer, diagnostics);
            }
            Optional<ExpressionNode> initializer = declaration.flatMap(EndpointYamlGenerator::initializerOf);
            if (initializer.isPresent()) {
                return getPort(initializer.get(), diagnostics);
            }
        }
        return unsupported(value, diagnostics);
    }

    // The index of the generated listener's port parameter in its init method.
    private Optional<Integer> getPortParamIndex() {
        Optional<TypeSymbol> type = AsyncAPICompilerPluginUtil.getListenerType(context.semanticModel(), listener);
        if (type.isEmpty() || !(type.get() instanceof TypeReferenceTypeSymbol reference)
                || !(reference.typeDescriptor() instanceof ClassSymbol listenerClass)) {
            return Optional.empty();
        }
        List<ParameterSymbol> params = listenerClass.initMethod()
                .flatMap(init -> init.typeDescriptor().params())
                .orElse(List.of());
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getName().filter(PORT_PARAMS::contains).isPresent()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    // Follows a reference to a module-level listener or variable to the expression it is initialized with.
    private Optional<ExpressionNode> resolve(ExpressionNode expression) {
        ExpressionNode value = unwrapCheck(expression);
        if (value instanceof SimpleNameReferenceNode reference) {
            return findDeclaration(reference.name().text())
                    .flatMap(EndpointYamlGenerator::initializerOf)
                    .flatMap(this::resolve);
        }
        return Optional.of(value);
    }

    private Optional<ModuleMemberDeclarationNode> findDeclaration(String name) {
        Module module = context.currentPackage().module(context.moduleId());
        for (DocumentId documentId : module.documentIds()) {
            ModulePartNode root = module.document(documentId).syntaxTree().rootNode();
            for (ModuleMemberDeclarationNode member : root.members()) {
                if (name.equals(declaredName(member))) {
                    return Optional.of(member);
                }
            }
        }
        return Optional.empty();
    }

    private static String declaredName(ModuleMemberDeclarationNode member) {
        if (member instanceof ListenerDeclarationNode listenerDeclaration) {
            return listenerDeclaration.variableName().text();
        }
        if (member instanceof ConstantDeclarationNode constant) {
            return constant.variableName().text();
        }
        if (member instanceof ModuleVariableDeclarationNode variable
                && variable.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode capture) {
            return capture.variableName().text();
        }
        return null;
    }

    private static Optional<ExpressionNode> initializerOf(ModuleMemberDeclarationNode member) {
        if (member instanceof ListenerDeclarationNode listenerDeclaration
                && listenerDeclaration.initializer() instanceof ExpressionNode initializer) {
            return Optional.of(initializer);
        }
        if (member instanceof ConstantDeclarationNode constant
                && constant.initializer() instanceof ExpressionNode initializer) {
            return Optional.of(initializer);
        }
        if (member instanceof ModuleVariableDeclarationNode variable) {
            return variable.initializer();
        }
        return Optional.empty();
    }

    private static boolean isConfigurable(ModuleMemberDeclarationNode member) {
        return member instanceof ModuleVariableDeclarationNode variable && variable.qualifiers().stream()
                .anyMatch(qualifier -> qualifier.kind() == SyntaxKind.CONFIGURABLE_KEYWORD);
    }

    private static boolean isNewExpression(ExpressionNode expression) {
        return expression instanceof ExplicitNewExpressionNode || expression instanceof ImplicitNewExpressionNode;
    }

    private static ExpressionNode unwrapCheck(ExpressionNode expression) {
        return expression instanceof CheckExpressionNode check ? check.expression() : expression;
    }

    // The argument for a parameter of a new expression, given by one of its names or at its position.
    private static Optional<ExpressionNode> getArgument(ExpressionNode newExpression, int position,
                                                        Set<String> names) {
        Optional<ParenthesizedArgList> argList = newExpression instanceof ExplicitNewExpressionNode explicit
                ? Optional.of(explicit.parenthesizedArgList())
                : ((ImplicitNewExpressionNode) newExpression).parenthesizedArgList();
        List<FunctionArgumentNode> arguments = new ArrayList<>();
        argList.ifPresent(list -> list.arguments().forEach(arguments::add));
        for (FunctionArgumentNode argument : arguments) {
            if (argument instanceof NamedArgumentNode named && names.contains(named.argumentName().name().text())) {
                return Optional.of(named.expression());
            }
        }
        if (position < arguments.size() && arguments.get(position) instanceof PositionalArgumentNode positional) {
            return Optional.of(positional.expression());
        }
        return Optional.empty();
    }

    private static Optional<Integer> unsupported(Node node, List<Diagnostic> diagnostics) {
        diagnostics.add(diagnostic(AsyncAPIDiagnostic.ASYNCAPI_ERROR_102, node));
        return Optional.empty();
    }

    private static Diagnostic diagnostic(AsyncAPIDiagnostic diagnostic, Node node) {
        DiagnosticInfo info = new DiagnosticInfo(diagnostic.getCode(), diagnostic.getMessage(),
                diagnostic.getSeverity());
        return DiagnosticFactory.createDiagnostic(info, node.location());
    }
}
