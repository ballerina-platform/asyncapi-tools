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

import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hands the collected endpoints to Ballerina, which writes them to {@code target/artifact/endpoints.yaml}.
 *
 * <p>The endpoint API only exists from Ballerina 2201.13.6, so it is called reflectively, as the
 * {@code ballerina/http} plugin does; on older versions nothing is reported.
 */
class TriggerEndpointReporter implements CompilerLifecycleTask<CompilerLifecycleEventContext> {

    private static final String ENDPOINT_META_INFO_CLASS = "io.ballerina.projects.plugins.EndpointMetaInfo";
    private static final String ADD_ENDPOINT_METADATA_METHOD = "addEndpointMetadata";

    private final Set<TriggerEndpoint> endpoints;

    TriggerEndpointReporter(Set<TriggerEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public void perform(CompilerLifecycleEventContext context) {
        List<TriggerEndpoint> toReport;
        synchronized (endpoints) {
            toReport = new ArrayList<>(endpoints);
            endpoints.clear();
        }
        if (toReport.isEmpty()) {
            return;
        }
        try {
            Class<?> metaInfoClass = Class.forName(ENDPOINT_META_INFO_CLASS);
            Constructor<?> constructor = metaInfoClass.getConstructor(String.class, int.class, String.class,
                    String.class, String.class);
            Method addMethod = context.getClass().getMethod(ADD_ENDPOINT_METADATA_METHOD, metaInfoClass);
            addMethod.setAccessible(true);
            for (TriggerEndpoint endpoint : toReport) {
                addMethod.invoke(context, constructor.newInstance(endpoint.basePath(), endpoint.port(),
                        endpoint.basePath(), TriggerEndpoint.TYPE, TriggerEndpoint.SCHEMA_PATH));
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            // Ballerina before 2201.13.6: --export-endpoints has no endpoint API to report to.
        }
    }
}
