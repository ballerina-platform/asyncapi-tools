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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.util.JsonUtil;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import java.util.List;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Document;
import java.util.Optional;


/**
 * This {@code AsyncAPIResult} is used to contain AsyncAPI definition in string format and error list.
 *
 * @since 2.0.0
 */
public class AsyncAPIResult {

    private AsyncApi25Document asyncAPI;
    private String serviceName; // added base path for key to definition
    private final List<AsyncAPIConverterDiagnostic> diagnostics;

    /**
     * This constructor is used to store the details that Map of {@code AsyncAPI} objects and diagnostic list.
     */
    public AsyncAPIResult(AsyncApi25Document asyncAPI, List<AsyncAPIConverterDiagnostic> diagnostics) {
        this.asyncAPI = asyncAPI;
        this.diagnostics = diagnostics;
    }

    public List<AsyncAPIConverterDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public Optional<AsyncApi25Document> getAsyncAPI() {
        return Optional.ofNullable(asyncAPI);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public Optional<String> getYaml() {

        ObjectNode json=  Library.writeDocument(this.asyncAPI);
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        factory.enable(YAMLGenerator.Feature.SPLIT_LINES);
        factory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);

        String finalYaml= null;
        try {
            finalYaml = new ObjectMapper(factory).writer(new DefaultPrettyPrinter()).writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Optional.ofNullable(finalYaml);
    }

    public Optional<String> getJson() {
        ObjectNode json= Library.writeDocument(this.asyncAPI);
        String finalJson= JsonUtil.stringify(json);
        return Optional.ofNullable(finalJson);
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setAsyncAPI(AsyncApi25DocumentImpl asyncAPI) {
        this.asyncAPI = asyncAPI;
    }
}
