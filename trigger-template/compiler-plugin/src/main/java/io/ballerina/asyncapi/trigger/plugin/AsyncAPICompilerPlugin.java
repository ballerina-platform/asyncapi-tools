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

import io.ballerina.asyncapi.trigger.plugin.endpointyaml.generator.Endpoint;
import io.ballerina.asyncapi.trigger.plugin.endpointyaml.generator.ServiceArtifactsLifecycleListener;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.ballerina.asyncapi.trigger.plugin.Constants.ASYNCAPI_EXPORTED_ENDPOINTS;

/**
 * The compiler plugin shipped with each generated trigger package.
 *
 * <p>Modelled on the {@code ballerina/http} compiler plugin's endpoint export: on
 * {@code bal build --export-endpoints} it reports the HTTP endpoint of every service attached to the trigger's
 * listener, which the {@code ballerina/http} plugin skips because that listener is not an {@code http:Listener}.
 */
public class AsyncAPICompilerPlugin extends CompilerPlugin {

    @Override
    public void init(CompilerPluginContext context) {
        // Not context.userData(): every plugin in the build shares that map, and each trigger brings its own copy of
        // this plugin, from its own class loader, so the copies would overwrite each other's endpoints.
        Map<String, Object> ctxData = new HashMap<>();
        ctxData.put(ASYNCAPI_EXPORTED_ENDPOINTS, Collections.synchronizedList(new ArrayList<Endpoint>()));
        context.addCodeAnalyzer(new AsyncAPIServiceAnalyzer(ctxData));
        context.addCompilerLifecycleListener(new ServiceArtifactsLifecycleListener(ctxData));
    }
}
