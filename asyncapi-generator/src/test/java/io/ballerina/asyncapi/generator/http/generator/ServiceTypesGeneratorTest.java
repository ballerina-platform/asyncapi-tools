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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit tests for {@link ServiceTypesGenerator} verifying that the generated
 * {@code service_types.bal} content contains the expected service types,
 * remote functions, and the {@code GenericServiceType} union.
 */
public class ServiceTypesGeneratorTest {

    @Test
    void testGenerateWithSingleServiceType() throws GeneratorException {
        List<HttpRemoteFunction> fns = List.of(
                new HttpRemoteFunction("push", "PushEvent"),
                new HttpRemoteFunction("create", "GenericEvent")
        );
        HttpServiceType serviceType = new HttpServiceType("RepositoryService", fns);
        String source = new ServiceTypesGenerator(List.of(serviceType)).generate();

        Assert.assertFalse(source.isBlank(), "Generated source should not be blank");
        Assert.assertTrue(source.contains("RepositoryService"),
                "Generated source should contain the service type name");
        Assert.assertTrue(source.contains("onPush"),
                "Generated source should contain the onPush remote function");
        Assert.assertTrue(source.contains("onCreate"),
                "Generated source should contain the onCreate remote function");
        Assert.assertTrue(source.contains("GenericServiceType"),
                "Generated source should contain the GenericServiceType union");
        Assert.assertTrue(source.contains("service object"),
                "Generated source should declare a service object type");
    }

    @Test
    void testGenerateWithMultipleServiceTypes() throws GeneratorException {
        List<HttpServiceType> serviceTypes = List.of(
                new HttpServiceType("RepositoryService", List.of(
                        new HttpRemoteFunction("push", "PushEvent")
                )),
                new HttpServiceType("IssueService", List.of(
                        new HttpRemoteFunction("issues.opened", "IssueEvent")
                )),
                new HttpServiceType("DeploymentService", List.of(
                        new HttpRemoteFunction("deployment", "DeploymentEvent")
                ))
        );
        String source = new ServiceTypesGenerator(serviceTypes).generate();

        Assert.assertTrue(source.contains("RepositoryService"),
                "Generated source should contain RepositoryService");
        Assert.assertTrue(source.contains("IssueService"),
                "Generated source should contain IssueService");
        Assert.assertTrue(source.contains("DeploymentService"),
                "Generated source should contain DeploymentService");
        Assert.assertTrue(source.contains("GenericServiceType"),
                "Generated source should contain the GenericServiceType union with all three types");
    }

    @Test
    void testDisplayLabelOverridesGeneratedFunctionName() throws GeneratorException {
        List<HttpRemoteFunction> fns = List.of(
                new HttpRemoteFunction("qbo.account.merged.v1", "QuickBookEvent", false, "AccountMerged"),
                new HttpRemoteFunction("qbo.account.created.v1", "QuickBookEvent", false, null)
        );
        HttpServiceType serviceType = new HttpServiceType("AccountService", fns);
        String source = new ServiceTypesGenerator(List.of(serviceType)).generate();

        Assert.assertTrue(source.contains("onAccountMerged"),
                "A display label should be used to build the function name instead of the raw event type");
        Assert.assertFalse(source.contains("onQboAccountMergedV1"),
                "The raw event type should not leak into the function name when a display label is set");
        Assert.assertTrue(source.contains("onQboAccountCreatedV1"),
                "With no display label, the function name should still be derived from the raw event type");
    }

    @Test
    void testGenerateWithEmptyRemoteFunctionsThrows() {
        HttpServiceType empty = new HttpServiceType("EmptyService", List.of());
        try {
            new ServiceTypesGenerator(List.of(empty)).generate();
            Assert.fail("Expected GeneratorException for service type with no remote functions");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("EmptyService") || e.getMessage().contains("empty")
                            || e.getMessage().contains("Remote"),
                    "Exception should indicate the problem with the service type");
        }
    }
}
