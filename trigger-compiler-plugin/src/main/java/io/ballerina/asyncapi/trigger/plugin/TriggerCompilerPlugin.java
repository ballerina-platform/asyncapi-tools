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

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilerLifecycleContext;
import io.ballerina.projects.plugins.CompilerLifecycleListener;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Compiler plugin shipped with each generated trigger package. On {@code bal build --export-endpoints} it reports
 * the HTTP endpoint of every service attached to that trigger's listener, which the {@code ballerina/http} plugin
 * skips because the listener is not an {@code http:Listener}.
 */
public class TriggerCompilerPlugin extends CompilerPlugin {

    @Override
    public void init(CompilerPluginContext context) {
        Set<TriggerEndpoint> endpoints = Collections.synchronizedSet(new LinkedHashSet<>());
        context.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext analysisContext) {
                analysisContext.addSyntaxNodeAnalysisTask(new TriggerEndpointCollector(endpoints),
                        SyntaxKind.SERVICE_DECLARATION);
            }
        });
        context.addCompilerLifecycleListener(new CompilerLifecycleListener() {
            @Override
            public void init(CompilerLifecycleContext lifecycleContext) {
                lifecycleContext.addCodeGenerationCompletedTask(new TriggerEndpointReporter(endpoints));
            }
        });
    }
}
