/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package io.ballerina.asyncapi.core.generators.asyncspec.model;

import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import sttp.apispec.asyncapi.AsyncAPI;
import sttp.apispec.asyncapi.circe.yaml.SttpAsyncAPICirceYaml;
import sttp.apispec.asyncapi.circe.yaml.package$;
import sttp.apispec.asyncapi.circe.yaml.SttpAsyncAPICirceYaml.RichAsyncAPI;
import java.util.List;
import java.util.Optional;


/**
 * This {@code AsyncAPIResult} is used to contain OpenAPI definition in string format and error list.
 *
 * @since 2.0.0
 */
public class AsyncAPIResult {

    private AsyncAPI asyncAPI;
    private String serviceName; // added base path for key to definition
    private final List<AsyncAPIConverterDiagnostic> diagnostics;
    private SttpAsyncAPICirceYaml sttpAsyncAPICirceYaml = new SttpAsyncAPICirceYaml(){};

    /**
     * This constructor is used to store the details that Map of {@code OpenAPI} objects and diagnostic list.
     */
    public AsyncAPIResult(AsyncAPI asyncAPI, List<AsyncAPIConverterDiagnostic> diagnostics) {
        this.asyncAPI = asyncAPI;
        this.diagnostics = diagnostics;
    }

    public List<AsyncAPIConverterDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public Optional<AsyncAPI> getAsyncAPI() {
        return Optional.ofNullable(asyncAPI);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public Optional<String> getYaml() {

        return Optional.ofNullable(sttpAsyncAPICirceYaml.RichAsyncAPI(asyncAPI).toYaml());
    }

//    public Optional<String> getJson() {
//        return Optional.ofNullable(Json.pretty(this.openAPI));
//    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setAsyncAPI(AsyncAPI openAPI) {
        this.asyncAPI = asyncAPI;
    }
}
