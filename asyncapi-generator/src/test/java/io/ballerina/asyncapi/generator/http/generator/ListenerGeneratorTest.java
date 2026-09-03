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
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
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
        String source = new ListenerGenerator(serviceTypes, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty(), "Test Connector").generate();

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
        Assert.assertTrue(source.contains("ListenerConfig listenerConfig = {}"),
                "With no connection auth, listenerConfig should stay defaultable");
    }

    @Test
    void testGenerateWithConnectionAuthMakesListenerConfigRequired() throws GeneratorException {
        // Regression check for the listener.bal compile bug (#9028): once connection auth
        // contributes required fields (e.g. username/password) with no safe default, the
        // listenerConfig parameter itself must become a required parameter - a partial default
        // like {webhookSecret: DEFAULT_SECRET} would leave those required fields unset, which
        // Ballerina rejects outright.
        List<HttpServiceType> serviceTypes = List.of(
                new HttpServiceType("RepositoryService", List.of(
                        new HttpRemoteFunction("push", "PushEvent")
                ))
        );
        ConnectionAuthConfig connectionAuthConfig = new ConnectionAuthConfig(
                ConnectionAuthConfig.TYPE_USER_PASSWORD, null, null, null);
        String source = new ListenerGenerator(serviceTypes, Optional.<WebhookAuthConfig>empty(),
                Optional.of(connectionAuthConfig), "Test Connector").generate();

        Assert.assertTrue(source.contains("ListenerConfig listenerConfig,"),
                "With connection auth present, listenerConfig should be a required parameter: " + source);
        Assert.assertFalse(source.contains("listenerConfig = {webhookSecret"),
                "listenerConfig should not carry a partial default value: " + source);
    }

    @Test
    void testGenerateUsesSpecTitleAsDisplayLabelAndIncludesIconPath() throws GeneratorException {
        // The @display annotation must carry a real label (from the spec's info.title) and an
        // iconPath - previously both were hardcoded blank/absent, leaving the class unlabeled and
        // iconless in any visual/low-code tooling that reads @display.
        List<HttpServiceType> serviceTypes = List.of(
                new HttpServiceType("RepositoryService", List.of(
                        new HttpRemoteFunction("push", "PushEvent")
                ))
        );
        String source = new ListenerGenerator(serviceTypes, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty(), "GitHub Webhooks API").generate();

        Assert.assertTrue(source.contains("@display {label: \"GitHub Webhooks API\", iconPath: \"icon.png\"}"),
                "The Listener class should carry the spec's title as its display label and a real "
                        + "iconPath: " + source);
    }

    @Test
    void testGetServiceTypeStrReturnsErrorInsteadOfPanicking() throws GeneratorException {
        // An unrecognized service type must fail as an ordinary error the caller can check/handle,
        // not a panic that crashes the whole running listener over one bad attach() call.
        List<HttpServiceType> serviceTypes = List.of(
                new HttpServiceType("RepositoryService", List.of(
                        new HttpRemoteFunction("push", "PushEvent")
                ))
        );
        String source = new ListenerGenerator(serviceTypes, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty(), "Test Connector").generate();

        Assert.assertFalse(source.contains("panic"),
                "getServiceTypeStr must not panic on an unrecognized service type: " + source);
        Assert.assertTrue(source.contains("returns string|error"),
                "getServiceTypeStr's return type must be string|error, not plain string: " + source);
        Assert.assertTrue(source.contains("return error(\"Unrecognized service type attached to the listener\")"),
                "The terminal else branch should return an error: " + source);
        Assert.assertTrue(source.contains("check self.getServiceTypeStr(serviceRef)"),
                "attach/detach must propagate getServiceTypeStr's error via check: " + source);
    }

    @Test
    void testEmptyServiceTypesThrows() {
        try {
            new ListenerGenerator(List.of(), Optional.<WebhookAuthConfig>empty(),
                    Optional.<ConnectionAuthConfig>empty(), "Test Connector").generate();
            Assert.fail("Expected GeneratorException for empty service types list");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testNullServiceTypesThrows() {
        try {
            new ListenerGenerator(null, Optional.<WebhookAuthConfig>empty(),
                    Optional.<ConnectionAuthConfig>empty(), "Test Connector").generate();
            Assert.fail("Expected GeneratorException for null service types");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }
}
