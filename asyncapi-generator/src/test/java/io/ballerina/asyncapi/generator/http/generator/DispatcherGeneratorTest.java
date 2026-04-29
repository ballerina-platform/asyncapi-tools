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
package io.ballerina.asyncapi.generator.http.generator;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link DispatcherGenerator} verifying generated {@code dispatcher_service.bal}
 * content for body and header identifier types, and error handling for invalid inputs.
 */
public class DispatcherGeneratorTest {

    private static final List<HttpServiceType> SINGLE_SERVICE = List.of(
            new HttpServiceType("RepositoryService", List.of(
                    new HttpRemoteFunction("push", "PushEvent"),
                    new HttpRemoteFunction("create", "GenericEvent")
            ))
    );

    @Test
    void testGenerateWithBodyIdentifier() throws GeneratorException {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type");
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertFalse(source.isBlank(), "Generated dispatcher source should not be blank");
        Assert.assertTrue(source.contains("DispatcherService"),
                "Generated source should contain the DispatcherService class");
        Assert.assertTrue(source.contains("matchRemoteFunc"),
                "Generated source should contain the matchRemoteFunc method");
        Assert.assertTrue(source.contains("executeRemoteFunc"),
                "Generated source should contain the executeRemoteFunc method");
        Assert.assertTrue(source.contains("event.type"),
                "Generated source should reference the body identifier path 'event.type'");
    }

    @Test
    void testGenerateWithHeaderIdentifier() throws GeneratorException {
        EventIdentifierConfig config = new EventIdentifierConfig("header", "X-Event-Type", null);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertFalse(source.isBlank(), "Generated dispatcher source should not be blank");
        Assert.assertTrue(source.contains("DispatcherService"),
                "Generated source should contain the DispatcherService class");
        Assert.assertTrue(source.contains("matchRemoteFunc"),
                "Generated source should contain the matchRemoteFunc method");
    }

    @Test
    void testEmptyServiceTypesThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type");
        try {
            new DispatcherGenerator(List.of(), config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for empty service types list");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testNullServiceTypesThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type");
        try {
            new DispatcherGenerator(null, config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for null service types");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testInvalidIdentifierTypeThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("unknown", null, "event.type");
        try {
            new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for unsupported identifier type");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("unknown") || e.getMessage().contains("Unsupported"),
                    "Exception should mention the unsupported identifier type");
        }
    }
}
