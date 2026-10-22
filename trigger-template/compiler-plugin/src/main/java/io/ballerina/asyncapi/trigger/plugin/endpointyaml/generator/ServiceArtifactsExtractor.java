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

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.asyncapi.trigger.plugin.Constants.ASYNCAPI_EXPORTED_ENDPOINTS;
import static io.ballerina.asyncapi.trigger.plugin.AsyncAPICompilerPluginUtil.getTriggerListeners;

/**
 * Collects the endpoint details of a service attached to a trigger listener into the package-wide list that
 * {@link ServiceArtifactsLifecycleListener} publishes once code generation completes.
 */
public class ServiceArtifactsExtractor implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private final Map<String, Object> ctxData;

    public ServiceArtifactsExtractor(Map<String, Object> ctxData) {
        this.ctxData = ctxData;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        List<ExpressionNode> listeners = getTriggerListeners(context);
        if (listeners.isEmpty() || isTestDocument(context)
                || !isExportEndpoints(context.currentPackage().project().buildOptions())) {
            return;
        }
        // As for HTTP services, nothing is exported from a package that does not compile.
        boolean hasErrors = context.semanticModel().diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
        if (hasErrors) {
            return;
        }
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ExpressionNode listener : listeners) {
            Optional<Endpoint> endpoint = new EndpointYamlGenerator(serviceNode, listener, context)
                    .getEndpoint(diagnostics);
            endpoint.ifPresent(this::collectEndpoint);
        }
        diagnostics.forEach(context::reportDiagnostic);
    }

    @SuppressWarnings("unchecked")
    private void collectEndpoint(Endpoint endpoint) {
        ((List<Endpoint>) ctxData.get(ASYNCAPI_EXPORTED_ENDPOINTS)).add(endpoint);
    }

    private static boolean isTestDocument(SyntaxNodeAnalysisContext context) {
        Module module = context.currentPackage().module(context.moduleId());
        return module != null && module.testDocumentIds().contains(context.documentId());
    }

    private static boolean isExportEndpoints(BuildOptions buildOptions) {
        // BuildOptions.exportEndpoints() exists from Ballerina 2201.13.3, after the version this plugin builds with.
        try {
            return (boolean) BuildOptions.class.getMethod("exportEndpoints").invoke(buildOptions);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
