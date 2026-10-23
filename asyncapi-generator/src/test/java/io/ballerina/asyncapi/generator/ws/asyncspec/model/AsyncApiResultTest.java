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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.ExceptionDiagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link AsyncApiResult} covering construction, getter behaviour,
 * YAML/JSON serialisation, and null-document safety.
 *
 * <p>{@code getYaml()} and {@code getJson()} both return {@code Optional<String>}: tests
 * assert {@code isPresent()} before calling {@code get()}.  When the wrapped document is
 * {@code null} both methods return {@code Optional.empty()} per the source implementation.
 */
class AsyncApiResultTest {

    private static AsyncApi30Document minimalDoc() {
        AsyncApi30Document doc = (AsyncApi30Document) Library.createDocument(ModelType.ASYNCAPI30);
        doc.setAsyncapi("3.0.0");
        Info info = doc.createInfo();
        info.setTitle("Test");
        info.setVersion("1.0.0");
        doc.setInfo(info);
        return doc;
    }

    // -------------------------------------------------------------------------
    // getServiceName / getDiagnostics
    // -------------------------------------------------------------------------

    @Test
    void testGetServiceName_returnsNamePassedToConstructor() {
        AsyncApiResult result = new AsyncApiResult("myService", minimalDoc(), List.of());
        Assert.assertEquals(result.getServiceName(), "myService",
                "getServiceName() must return the exact name passed to the constructor");
    }

    @Test
    void testGetDiagnostics_emptyList_returnsNonNullEmptyList() {
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), List.of());
        Assert.assertNotNull(result.getDiagnostics(),
                "getDiagnostics() must never return null");
        Assert.assertTrue(result.getDiagnostics().isEmpty(),
                "getDiagnostics() must be empty when an empty list was supplied");
    }

    @Test
    void testGetDiagnostics_oneDiagnostic_sizeIsOne() {
        List<AsyncApiConverterDiagnostic> diags = new ArrayList<>();
        diags.add(new ExceptionDiagnostic(
                DiagnosticMessages.AAS_CONVERTOR_100.getCode(),
                DiagnosticMessages.AAS_CONVERTOR_100.getDescription(),
                null));
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), diags);
        Assert.assertEquals(result.getDiagnostics().size(), 1,
                "getDiagnostics() size must equal the number of diagnostics passed in");
    }

    // -------------------------------------------------------------------------
    // getYaml
    // -------------------------------------------------------------------------

    @Test
    void testGetYaml_nonNullDoc_isPresentAndNonBlank() {
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), List.of());
        Optional<String> yaml = result.getYaml();
        Assert.assertTrue(yaml.isPresent(),
                "getYaml() must return a present Optional when the document is non-null");
        Assert.assertFalse(yaml.get().isBlank(),
                "YAML content must not be blank for a non-null document");
    }

    @Test
    void testGetYaml_containsAsyncapiKey() {
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), List.of());
        Optional<String> yaml = result.getYaml();
        Assert.assertTrue(yaml.isPresent(), "precondition: YAML must be present");
        Assert.assertTrue(yaml.get().contains("asyncapi"),
                "YAML output must contain the 'asyncapi' root key");
    }

    // -------------------------------------------------------------------------
    // getJson
    // -------------------------------------------------------------------------

    @Test
    void testGetJson_nonNullDoc_isPresentAndStartsWithBrace() {
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), List.of());
        Optional<String> json = result.getJson();
        Assert.assertTrue(json.isPresent(),
                "getJson() must return a present Optional when the document is non-null");
        Assert.assertTrue(json.get().trim().startsWith("{"),
                "JSON output must begin with '{'");
    }

    @Test
    void testGetJson_isParseable() throws Exception {
        AsyncApiResult result = new AsyncApiResult("svc", minimalDoc(), List.of());
        Optional<String> json = result.getJson();
        Assert.assertTrue(json.isPresent(), "precondition: JSON must be present");
        new ObjectMapper().readTree(json.get());
    }

    // -------------------------------------------------------------------------
    // null document
    // -------------------------------------------------------------------------

    @Test
    void testNullDocument_getYaml_returnsEmpty() {
        AsyncApiResult nullDocResult = new AsyncApiResult("svc", null, List.of());
        Assert.assertFalse(nullDocResult.getYaml().isPresent(),
                "getYaml() must return Optional.empty() when the wrapped document is null");
    }

    @Test
    void testNullDocument_getJson_returnsEmpty() {
        AsyncApiResult nullDocResult = new AsyncApiResult("svc", null, List.of());
        Assert.assertFalse(nullDocResult.getJson().isPresent(),
                "getJson() must return Optional.empty() when the wrapped document is null");
    }
}
