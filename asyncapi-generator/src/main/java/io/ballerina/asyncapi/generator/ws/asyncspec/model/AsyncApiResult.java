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
package io.ballerina.asyncapi.generator.ws.asyncspec.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.util.JsonUtil;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Holds the result of a single Ballerina-service-to-AsyncAPI 3.0 conversion,
 * including the generated document and any diagnostics.
 * Corresponds to the legacy {@code AsyncApiResult}.
 */
public class AsyncApiResult {

    private final String serviceName;
    private final AsyncApi30Document asyncApi;
    private final List<AsyncApiConverterDiagnostic> diagnostics;

    /**
     * Creates a new conversion result.
     *
     * @param serviceName name of the converted service (may be null for error-only results)
     * @param asyncApi    generated AsyncAPI 3.0 document, or null if generation failed
     * @param diagnostics list of diagnostics produced during conversion
     */
    public AsyncApiResult(String serviceName, AsyncApi30Document asyncApi,
                          List<AsyncApiConverterDiagnostic> diagnostics) {
        this.serviceName = serviceName;
        this.asyncApi = asyncApi;
        this.diagnostics = diagnostics != null
                ? Collections.unmodifiableList(diagnostics)
                : Collections.emptyList();
    }

    /**
     * Returns the service name.
     *
     * @return service name, or null for error-only results
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the generated AsyncAPI 3.0 document, if present.
     *
     * @return optional document
     */
    public Optional<AsyncApi30Document> getAsyncAPI() {
        return Optional.ofNullable(asyncApi);
    }

    /**
     * Returns the YAML serialisation of the spec, if the document is present.
     *
     * @return optional YAML content
     */
    public Optional<String> getYaml() {
        if (asyncApi == null) {
            return Optional.empty();
        }
        ObjectNode json = Library.writeDocument(asyncApi);
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        factory.enable(YAMLGenerator.Feature.SPLIT_LINES);
        factory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
        try {
            String yaml = new ObjectMapper(factory).writer(new DefaultPrettyPrinter())
                    .writeValueAsString(json);
            return Optional.ofNullable(yaml);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the JSON serialisation of the spec, if the document is present.
     *
     * @return optional JSON content
     */
    public Optional<String> getJson() {
        if (asyncApi == null) {
            return Optional.empty();
        }
        ObjectNode json = Library.writeDocument(asyncApi);
        return Optional.ofNullable(JsonUtil.stringify(json));
    }

    /**
     * Returns the diagnostics produced during conversion.
     *
     * @return unmodifiable list of diagnostics
     */
    public List<AsyncApiConverterDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
