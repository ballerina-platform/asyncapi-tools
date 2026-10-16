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
package io.ballerina.asyncapi.trigger.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
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
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Collects the endpoint of every service attached to a listener of the trigger package this plugin ships with.
 */
class TriggerEndpointCollector implements AnalysisTask<SyntaxNodeAnalysisContext> {

    // The port generated listeners use when none is given.
    static final int DEFAULT_PORT = 8090;
    // Names of the generated listener's port parameter, across tool versions.
    private static final Set<String> PORT_PARAM_NAMES = Set.of("listenOn", "listenTo");
    private static final String PORT_UNKNOWN_CODE = "ASYNCAPI_TRIGGER_101";
    private static final String PORT_UNKNOWN_MESSAGE = "the port of this listener could not be read from the "
            + "source, so its endpoint is exported with port 0";

    private final Set<TriggerEndpoint> endpoints;

    TriggerEndpointCollector(Set<TriggerEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        Package currentPackage = context.currentPackage();
        if (!isExportEndpoints(currentPackage.project().buildOptions()) || isTestDocument(context)) {
            return;
        }
        // Each trigger ships its own copy of this plugin, so each copy only reports its own trigger's services.
        Optional<PackageDescriptor> owner = ownerPackage(currentPackage);
        if (owner.isEmpty()) {
            return;
        }
        ServiceDeclarationNode service = (ServiceDeclarationNode) context.node();
        String basePath = basePath(service);
        for (ExpressionNode listener : service.expressions()) {
            if (!isOwnListener(context.semanticModel(), listener, owner.get())) {
                continue;
            }
            int port = port(context, listener);
            if (port == 0) {
                context.reportDiagnostic(DiagnosticFactory.createDiagnostic(new DiagnosticInfo(PORT_UNKNOWN_CODE,
                        PORT_UNKNOWN_MESSAGE, DiagnosticSeverity.WARNING), listener.location()));
            }
            endpoints.add(new TriggerEndpoint(port, basePath));
        }
    }

    private static boolean isExportEndpoints(BuildOptions buildOptions) {
        // exportEndpoints() exists from Ballerina 2201.13.3.
        try {
            return (boolean) BuildOptions.class.getMethod("exportEndpoints").invoke(buildOptions);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean isTestDocument(SyntaxNodeAnalysisContext context) {
        Module module = context.currentPackage().module(context.moduleId());
        return module != null && module.testDocumentIds().contains(context.documentId());
    }

    /**
     * Finds the trigger package whose bala holds the jar this plugin was loaded from.
     */
    private static Optional<PackageDescriptor> ownerPackage(Package currentPackage) {
        Path pluginJar;
        try {
            pluginJar = Path.of(TriggerEndpointCollector.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return Optional.empty();
        }
        for (ResolvedPackageDependency dependency : currentPackage.getResolution().dependencyGraph().getNodes()) {
            Package dependencyPackage = dependency.packageInstance();
            if (dependencyPackage == currentPackage) {
                continue;
            }
            Path root = dependencyPackage.project().sourceRoot().toAbsolutePath().normalize();
            if (pluginJar.startsWith(root)) {
                return Optional.of(dependencyPackage.descriptor());
            }
        }
        return Optional.empty();
    }

    /**
     * The listener's class type: {@code new Listener(...)} is typed {@code Listener|error} since init can fail.
     */
    private static Optional<TypeSymbol> listenerType(SemanticModel semanticModel, ExpressionNode listener) {
        Optional<TypeSymbol> type = semanticModel.typeOf(listener);
        if (type.isPresent() && type.get() instanceof UnionTypeSymbol union) {
            return union.memberTypeDescriptors().stream()
                    .filter(member -> member.typeKind() != TypeDescKind.ERROR)
                    .findFirst();
        }
        return type;
    }

    private static boolean isOwnListener(SemanticModel semanticModel, ExpressionNode listener,
                                         PackageDescriptor owner) {
        Optional<ModuleSymbol> module = listenerType(semanticModel, listener).flatMap(TypeSymbol::getModule);
        return module.isPresent()
                && module.get().id().orgName().equals(owner.org().value())
                && module.get().id().packageName().equals(owner.name().value());
    }

    /**
     * The path the service is attached to, or empty when it has none, as the {@code ballerina/http} plugin reports it.
     */
    static String basePath(ServiceDeclarationNode service) {
        StringBuilder path = new StringBuilder();
        for (Node segment : service.absoluteResourcePath()) {
            String text = segment.toString().trim();
            if (segment.kind() == SyntaxKind.STRING_LITERAL && text.length() >= 2) {
                text = text.substring(1, text.length() - 1);
            }
            path.append(text.replace("\\", ""));
        }
        String normalized = path.toString().replaceAll("/+", "/");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    /**
     * The port the listener serves on, or {@code 0} when the source does not say.
     */
    private static int port(SyntaxNodeAnalysisContext context, ExpressionNode listener) {
        Optional<Node> construction = construction(context, listener);
        Optional<Integer> portIndex = portParamIndex(context.semanticModel(), listener);
        if (construction.isEmpty() || portIndex.isEmpty()) {
            return 0;
        }
        Optional<ExpressionNode> portArg = argument(arguments(construction.get()), portIndex.get());
        if (portArg.isEmpty()) {
            return DEFAULT_PORT;
        }
        Optional<Integer> literal = intLiteral(portArg.get());
        if (literal.isPresent()) {
            return literal.get();
        }
        // An http:Listener to share: its port is its first argument.
        Optional<Node> httpListener = construction(context, portArg.get());
        if (httpListener.isEmpty()) {
            return 0;
        }
        return argument(arguments(httpListener.get()), 0).flatMap(TriggerEndpointCollector::intLiteral).orElse(0);
    }

    private static Optional<Integer> portParamIndex(SemanticModel semanticModel, ExpressionNode listener) {
        Optional<TypeSymbol> type = listenerType(semanticModel, listener);
        if (type.isEmpty() || !(type.get() instanceof TypeReferenceTypeSymbol reference)
                || !(reference.typeDescriptor() instanceof ClassSymbol listenerClass)) {
            return Optional.empty();
        }
        Optional<List<ParameterSymbol>> params = listenerClass.initMethod()
                .flatMap(init -> init.typeDescriptor().params());
        if (params.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; i < params.get().size(); i++) {
            if (params.get().get(i).getName().filter(PORT_PARAM_NAMES::contains).isPresent()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    /**
     * The {@code new} expression that creates the listener, following a reference to a module-level declaration.
     */
    private static Optional<Node> construction(SyntaxNodeAnalysisContext context, ExpressionNode expression) {
        ExpressionNode unwrapped = expression instanceof CheckExpressionNode check ? check.expression() : expression;
        if (unwrapped instanceof ExplicitNewExpressionNode || unwrapped instanceof ImplicitNewExpressionNode) {
            return Optional.of(unwrapped);
        }
        if (!(unwrapped instanceof SimpleNameReferenceNode reference)) {
            return Optional.empty();
        }
        String name = reference.name().text();
        Module module = context.currentPackage().module(context.moduleId());
        for (DocumentId documentId : module.documentIds()) {
            ModulePartNode root = module.document(documentId).syntaxTree().rootNode();
            for (ModuleMemberDeclarationNode member : root.members()) {
                Optional<ExpressionNode> initializer = initializerOf(member, name);
                if (initializer.isPresent()) {
                    return construction(context, initializer.get());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ExpressionNode> initializerOf(ModuleMemberDeclarationNode member, String name) {
        if (member instanceof ListenerDeclarationNode listener && listener.variableName().text().equals(name)
                && listener.initializer() instanceof ExpressionNode initializer) {
            return Optional.of(initializer);
        }
        if (member instanceof ModuleVariableDeclarationNode variable
                && variable.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode capture
                && capture.variableName().text().equals(name)) {
            return variable.initializer();
        }
        return Optional.empty();
    }

    private static List<FunctionArgumentNode> arguments(Node construction) {
        Optional<ParenthesizedArgList> argList = construction instanceof ExplicitNewExpressionNode explicit
                ? Optional.of(explicit.parenthesizedArgList())
                : ((ImplicitNewExpressionNode) construction).parenthesizedArgList();
        List<FunctionArgumentNode> arguments = new ArrayList<>();
        argList.ifPresent(list -> list.arguments().forEach(arguments::add));
        return arguments;
    }

    /**
     * The argument for the parameter at {@code index}, given by name or by position.
     */
    private static Optional<ExpressionNode> argument(List<FunctionArgumentNode> arguments, int index) {
        for (FunctionArgumentNode argument : arguments) {
            if (argument instanceof NamedArgumentNode named
                    && PORT_PARAM_NAMES.contains(named.argumentName().name().text())) {
                return Optional.of(named.expression());
            }
        }
        if (index < arguments.size() && arguments.get(index) instanceof PositionalArgumentNode positional) {
            return Optional.of(positional.expression());
        }
        return Optional.empty();
    }

    private static Optional<Integer> intLiteral(ExpressionNode expression) {
        if (expression instanceof BasicLiteralNode literal && literal.kind() == SyntaxKind.NUMERIC_LITERAL) {
            try {
                return Optional.of(Integer.parseInt(literal.literalToken().text().trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
