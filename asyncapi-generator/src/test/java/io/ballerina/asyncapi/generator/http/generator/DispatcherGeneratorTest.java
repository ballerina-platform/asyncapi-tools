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
 * content for body, header, and composite identifier types, and error handling for invalid inputs.
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
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", false);
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
    void testDiagnosticTraceLogsRemovedButDispatchFailedKept() throws GeneratorException {
        // MATCH_LEVEL_1_*/MATCH_LEVEL_2_*/DISPATCHER_ENTERED/SIGNATURE_VERIFIED/HANDLER_EXECUTED_*
        // were pure diagnostic trace noise with no value to an end user - removed. DISPATCH_FAILED
        // is a real error signal (includes the causing error) and must stay.
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", false);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty())
                .generate();

        Assert.assertTrue(source.contains("DISPATCH_FAILED"),
                "DISPATCH_FAILED is a real error signal and must be kept: " + source);
        Assert.assertFalse(source.contains("MATCH_LEVEL"), "MATCH_LEVEL_1/2 trace logs should be removed: " + source);
        Assert.assertFalse(source.contains("DISPATCHER_ENTERED"),
                "DISPATCHER_ENTERED trace log should be removed: " + source);
        Assert.assertFalse(source.contains("HANDLER_EXECUTED"),
                "HANDLER_EXECUTED trace log should be removed: " + source);
        Assert.assertTrue(source.contains("invokeRemoteFunction"),
                "The real remote-function invocation must survive removing its log wrapper: " + source);
    }

    @Test
    void testGenerateWithHeaderIdentifier() throws GeneratorException {
        EventIdentifierConfig config = new EventIdentifierConfig("header", "X-Event-Type", null, false);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertFalse(source.isBlank(), "Generated dispatcher source should not be blank");
        Assert.assertTrue(source.contains("DispatcherService"),
                "Generated source should contain the DispatcherService class");
        Assert.assertTrue(source.contains("matchRemoteFunc"),
                "Generated source should contain the matchRemoteFunc method");
    }

    @Test
    void testGenerateWithCompositeIdentifier() throws GeneratorException {
        EventIdentifierConfig config = new EventIdentifierConfig("composite", "X-GitHub-Event", "action", false);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertFalse(source.isBlank(), "Generated dispatcher source should not be blank");
        Assert.assertTrue(source.contains("DispatcherService"),
                "Generated source should contain the DispatcherService class");
        Assert.assertTrue(source.contains("matchRemoteFunc"),
                "Generated source should contain the matchRemoteFunc method");
        Assert.assertTrue(source.contains("X-GitHub-Event"),
                "Generated source should reference the composite header name");
        Assert.assertTrue(source.contains("eventIdentifier"),
                "Generated source should contain eventIdentifier for composite type");
    }

    @Test
    void testMissingRequiredHeaderRespondsBadRequestNotPlainCheck() throws GeneratorException {
        // A missing required header (X-GitHub-Event, X-Event-Type, etc.) is a malformed request -
        // a client error, not a server fault - so it must produce an explicit 400 response instead
        // of propagating via a bare `check`, which the framework would report as a 500. Covers both
        // identifier types that read a header ("header" and "composite").
        EventIdentifierConfig headerConfig = new EventIdentifierConfig("header", "X-Event-Type", null, false);
        String headerSource = new DispatcherGenerator(SINGLE_SERVICE, headerConfig, Optional.<WebhookAuthConfig>empty())
                .generate();
        assertRespondsBadRequestOnMissingHeader(headerSource);

        EventIdentifierConfig compositeConfig =
                new EventIdentifierConfig("composite", "X-GitHub-Event", "action", false);
        String compositeSource = new DispatcherGenerator(SINGLE_SERVICE, compositeConfig,
                Optional.<WebhookAuthConfig>empty()).generate();
        assertRespondsBadRequestOnMissingHeader(compositeSource);
    }

    private void assertRespondsBadRequestOnMissingHeader(String source) {
        Assert.assertFalse(source.contains("check request.getHeader"),
                "The header read must not be a bare check - a missing header must be handled "
                        + "explicitly, not propagated as a 500: " + source);
        Assert.assertTrue(source.contains("string|error eventTypeResult = request.getHeader"),
                "The header read should be captured as string|error: " + source);
        Assert.assertTrue(source.contains("http:STATUS_BAD_REQUEST"),
                "A missing header should respond 400, not fall through to a 500: " + source);
    }

    @Test
    void testAckSentBeforeDispatchAndDispatchErrorsAreNotPropagated() throws GeneratorException {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", false);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        int ackIndex = source.indexOf("ackResponse.statusCode = http:STATUS_OK;");
        int dispatchIndex = source.indexOf("error? dispatchResult =");
        Assert.assertTrue(ackIndex >= 0, "Generated source should ack with STATUS_OK");
        Assert.assertTrue(source.contains("check caller->respond(ackResponse);"),
                "Ack must be sent via a real http:Response with statusCode set - passing the status "
                        + "constant directly to respond() sends it as the response body instead, and the "
                        + "framework falls back to its default status for the method (201 for POST)");
        Assert.assertTrue(dispatchIndex >= 0,
                "Generated source should capture the dispatch result instead of propagating it via check");
        Assert.assertTrue(ackIndex < dispatchIndex,
                "The ack must be sent before dispatching to the user's handler, so a handler error can "
                        + "never prevent the caller from receiving an acknowledgement");
        Assert.assertFalse(source.contains("check self.matchRemoteFunc(genericDataType, eventType);"),
                "The post resource function's dispatch call must not use check - a handler error should "
                        + "be caught and logged, not propagated and left unacknowledged");
    }

    @Test
    void testGenerateWithBatchedBodyIdentifier() throws GeneratorException {
        // Some providers (e.g. HubSpot) batch multiple events into a single POST body as a JSON
        // array. batched=true must produce a foreach-based dispatcher that acks once for the whole
        // batch, then identifies/converts/dispatches each element independently - one malformed
        // element must not abort the rest of the batch.
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", true);
        String source = new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();

        Assert.assertTrue(source.contains("json[] eventsArray = check payload.ensureType();"),
                "Batched dispatch should parse the body as a JSON array: " + source);
        Assert.assertTrue(source.contains("foreach json event in eventsArray"),
                "Batched dispatch should iterate the array: " + source);
        Assert.assertTrue(source.contains("event.event.type"),
                "Each loop iteration should read the identifier path off its own element, not the "
                        + "whole payload: " + source);

        int ackIndex = source.indexOf("ackResponse.statusCode = http:STATUS_OK;");
        int loopIndex = source.indexOf("foreach json event in eventsArray");
        Assert.assertTrue(ackIndex >= 0 && loopIndex >= 0 && ackIndex < loopIndex,
                "The whole batch must be acked once, before the per-element loop runs: " + source);

        Assert.assertFalse(source.contains("check event.cloneWithType"),
                "A single element's conversion failure must not propagate via check and abort the "
                        + "rest of the batch: " + source);
        Assert.assertTrue(source.contains("continue"),
                "A malformed element should be logged and skipped via continue, not crash the batch: "
                        + source);
    }

    @Test
    void testEmptyServiceTypesThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", false);
        try {
            new DispatcherGenerator(List.of(), config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for empty service types list");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testNullServiceTypesThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("body", null, "event.type", false);
        try {
            new DispatcherGenerator(null, config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for null service types");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testInvalidIdentifierTypeThrows() {
        EventIdentifierConfig config = new EventIdentifierConfig("unknown", null, "event.type", false);
        try {
            new DispatcherGenerator(SINGLE_SERVICE, config, Optional.<WebhookAuthConfig>empty()).generate();
            Assert.fail("Expected GeneratorException for unsupported identifier type");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("unknown") || e.getMessage().contains("Unsupported"),
                    "Exception should mention the unsupported identifier type");
        }
    }
}
