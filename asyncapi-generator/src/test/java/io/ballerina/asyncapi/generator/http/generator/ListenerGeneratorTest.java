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
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link ListenerGenerator} verifying generated {@code listener.bal} content
 * and error handling for empty or null service type inputs.
 */
public class ListenerGeneratorTest {

    @Test
    void testGenerateProducesValidSource() throws GeneratorException {
        List<HttpServiceType> serviceTypes = List.of(
                new HttpServiceType("RepositoryService", List.of(
                        new HttpRemoteFunction("push", "PushEvent")
                )),
                new HttpServiceType("IssueService", List.of(
                        new HttpRemoteFunction("issues.opened", "IssueEvent")
                ))
        );
        String source = new ListenerGenerator(serviceTypes, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertFalse(source.isBlank(), "Generated listener source should not be blank");
        Assert.assertTrue(source.contains("Listener"),
                "Generated source should contain the Listener class");
        Assert.assertTrue(source.contains("getServiceTypeStr"),
                "Generated source should contain the getServiceTypeStr method");
        Assert.assertTrue(source.contains("RepositoryService"),
                "Generated source should reference RepositoryService in getServiceTypeStr");
        Assert.assertTrue(source.contains("IssueService"),
                "Generated source should reference IssueService in getServiceTypeStr");
        Assert.assertTrue(source.contains("http"),
                "Generated source should import the ballerina/http module");
    }

    @Test
    void testEmptyServiceTypesThrows() {
        try {
            new ListenerGenerator(List.of(), Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for empty service types list");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testNullServiceTypesThrows() {
        try {
            new ListenerGenerator(null, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for null service types");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }
}
