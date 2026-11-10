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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Utilities of the trigger compiler plugin.
 */
public final class AsyncAPICompilerPluginUtil {

    private AsyncAPICompilerPluginUtil() {
    }

    /**
     * Returns the listener expressions of the service that belong to the trigger this plugin ships with.
     *
     * <p>The counterpart of the {@code ballerina/http} plugin's listener check: that plugin keeps services on an
     * {@code http:Listener}, this one keeps services on this trigger's {@code Listener}. Each trigger ships its own
     * copy of the plugin, so each copy only claims its own trigger's services and none is exported twice.
     *
     * @param context the analysis context of a service declaration
     * @return the matching listener expressions, empty when the service is not attached to this trigger
     */
    public static List<ExpressionNode> getTriggerListeners(SyntaxNodeAnalysisContext context) {
        if (!(context.node() instanceof ServiceDeclarationNode serviceNode)) {
            return List.of();
        }
        Optional<PackageDescriptor> trigger = getTriggerPackage(context.currentPackage());
        if (trigger.isEmpty()) {
            return List.of();
        }
        SemanticModel semanticModel = context.semanticModel();
        return serviceNode.expressions().stream()
                .filter(listener -> isListenerOfPackage(semanticModel, listener, trigger.get()))
                .toList();
    }

    /**
     * Returns the type of the listener, without the {@code error} of {@code new Listener(...)}, whose init can fail.
     *
     * @param semanticModel the semantic model
     * @param listener      the listener expression
     * @return the listener type
     */
    public static Optional<TypeSymbol> getListenerType(SemanticModel semanticModel, ExpressionNode listener) {
        Optional<TypeSymbol> type = semanticModel.typeOf(listener);
        if (type.isPresent() && type.get() instanceof UnionTypeSymbol union) {
            return union.memberTypeDescriptors().stream()
                    .filter(member -> member.typeKind() != TypeDescKind.ERROR)
                    .findFirst();
        }
        return type;
    }

    // The trigger package whose bala holds the jar this plugin was loaded from.
    private static Optional<PackageDescriptor> getTriggerPackage(Package currentPackage) {
        Path pluginJar;
        try {
            pluginJar = Path.of(AsyncAPICompilerPluginUtil.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return Optional.empty();
        }
        for (ResolvedPackageDependency dependency : currentPackage.getResolution().dependencyGraph().getNodes()) {
            Package dependencyPackage = dependency.packageInstance();
            if (dependencyPackage == currentPackage) {
                continue;
            }
            if (pluginJar.startsWith(dependencyPackage.project().sourceRoot().toAbsolutePath().normalize())) {
                return Optional.of(dependencyPackage.descriptor());
            }
        }
        return Optional.empty();
    }

    private static boolean isListenerOfPackage(SemanticModel semanticModel, ExpressionNode listener,
                                               PackageDescriptor trigger) {
        Optional<ModuleSymbol> module = getListenerType(semanticModel, listener).flatMap(TypeSymbol::getModule);
        return module.isPresent()
                && module.get().id().orgName().equals(trigger.org().value())
                && module.get().id().packageName().equals(trigger.name().value());
    }
}
