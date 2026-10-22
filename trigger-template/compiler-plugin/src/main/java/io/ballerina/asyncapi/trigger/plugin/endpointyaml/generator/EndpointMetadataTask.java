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

import io.ballerina.asyncapi.trigger.plugin.AsyncAPIDiagnostic;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.asyncapi.trigger.plugin.Constants.ASYNCAPI_EXPORTED_ENDPOINTS;

/**
 * Hands the collected endpoints to Ballerina, which writes them to {@code target/artifact/endpoints.yaml}.
 *
 * <p>As in the {@code ballerina/http} plugin, the endpoint API (Ballerina 2201.13.6 and later) is called
 * reflectively, so the plugin still loads on older versions.
 */
public class EndpointMetadataTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {

    private static final String ENDPOINT_META_INFO_CLASS = "io.ballerina.projects.plugins.EndpointMetaInfo";
    private static final String ADD_ENDPOINT_METADATA_METHOD = "addEndpointMetadata";

    private final Map<String, Object> ctxData;

    public EndpointMetadataTask(Map<String, Object> ctxData) {
        this.ctxData = ctxData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void perform(CompilerLifecycleEventContext context) {
        List<Endpoint> endpoints = (List<Endpoint>) ctxData.get(ASYNCAPI_EXPORTED_ENDPOINTS);
        if (endpoints == null || endpoints.isEmpty()) {
            return;
        }
        // Services that share a listener and path share one endpoint.
        Set<Endpoint> unique;
        synchronized (endpoints) {
            unique = new LinkedHashSet<>(endpoints);
            endpoints.clear();
        }
        try {
            for (Endpoint endpoint : unique) {
                addEndpointMetadata(context, endpoint);
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            AsyncAPIDiagnostic diagnostic = AsyncAPIDiagnostic.ASYNCAPI_WARNING_102;
            context.reportDiagnostic(DiagnosticFactory.createDiagnostic(new DiagnosticInfo(diagnostic.getCode(),
                    diagnostic.getMessage(), diagnostic.getSeverity()), new NullLocation()));
        }
    }

    private void addEndpointMetadata(CompilerLifecycleEventContext context, Endpoint endpoint)
            throws ReflectiveOperationException {
        Class<?> endpointMetaInfoClass = Class.forName(ENDPOINT_META_INFO_CLASS);
        Constructor<?> constructor = endpointMetaInfoClass.getConstructor(String.class, int.class, String.class,
                String.class, String.class);
        Object endpointMetaInfo = constructor.newInstance(endpoint.name(), endpoint.port(), endpoint.basePath(),
                endpoint.type(), endpoint.schemaPath());
        Method method = context.getClass().getMethod(ADD_ENDPOINT_METADATA_METHOD, endpointMetaInfoClass);
        method.setAccessible(true);
        method.invoke(context, endpointMetaInfo);
    }

    /**
     * A location for diagnostics that do not belong to a source position.
     */
    private static class NullLocation implements Location {

        @Override
        public LineRange lineRange() {
            LinePosition from = LinePosition.from(0, 0);
            return LineRange.from("", from, from);
        }

        @Override
        public TextRange textRange() {
            return TextRange.from(0, 0);
        }
    }
}
