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
package io.ballerina.asyncapi.generator.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public class WebhookDSLVerificationTest {

    private static final Path RES_DIR = Paths.get("src/test/resources/specs").toAbsolutePath();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Test
    public void testGithubWebhookVerification() throws Exception {
        String content = generateDispatcherService("github_verification.yaml");
        Assert.assertTrue(content.contains("crypto:hmacSha256"), "Should contain HMAC-SHA256 algorithm");
        Assert.assertTrue(
        content.contains("string `${check request.getTextPayload()}`"),
        "Should extract text payload");
        Assert.assertTrue(
        content.contains("string `sha256=${computedSignature}`"),
        "Should format expected header with sha256=");
    }

    @Test
    public void testGithubDispatchTestSignatureFormatAndStatusCode() throws Exception {
        // github_verification.yaml configures headerFormat: "sha256=$signature", matching the real
        // prefix GitHub always sends. The generated tests/dispatch_test.bal must build its outgoing
        // signature using that same prefix, and assert the status code the dispatcher actually
        // returns (STATUS_OK) - not a bare/wrong-status signature that would never pass real
        // verification.
        Path tempOutputDir = generateOutputDir("github_verification.yaml");
        Path dispatchTestFile = tempOutputDir.resolve("tests").resolve("dispatch_test.bal");
        Assert.assertTrue(Files.exists(dispatchTestFile), "tests/dispatch_test.bal was not generated!");
        String dispatchTestContent = Files.readString(dispatchTestFile);

        Assert.assertTrue(
                dispatchTestContent.contains("string `sha256=${computedSignature}`"),
                "Should splice the computed signature into the configured sha256= headerFormat prefix");
        Assert.assertTrue(
                dispatchTestContent.contains("http:STATUS_OK"),
                "Should assert the status code the dispatcher actually returns");
        Assert.assertFalse(
                dispatchTestContent.contains("http:STATUS_CREATED"),
                "Should not assert a status code the dispatcher never returns");
    }

    @Test
    public void testStripeWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("stripe_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"Stripe-Signature\")"),
        "Should validate Stripe signature header");
    Assert.assertTrue(content.contains("string timestamp = extractedHeaderValues[\"timestamp\"]"),
        "Should extract and declare timestamp from Stripe signature header");
    Assert.assertTrue(content.contains("`${timestamp}.${check request.getTextPayload()}`"),
        "Should compose Stripe payload using dot-based DSL tokens");
    }

    @Test
    public void testSlackWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("slack_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Slack-Signature\")"),
        "Should validate Slack signature header");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Slack-Request-Timestamp\")"),
        "Should include Slack timestamp header in payload composition");
    Assert.assertTrue(content.contains("`v0:${check request.getHeader(\"X-Slack-Request-Timestamp\")}:"
        + "${check request.getTextPayload()}`"),
        "Should build Slack payload from dot-based DSL expression");
    }

    @Test
    public void testShopifyWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("shopify_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Shopify-Hmac-Sha256\")"),
        "Should validate Shopify signature header");
    Assert.assertTrue(content.contains("computedDigest.toBase64()"),
        "Should apply base64 encoding for Shopify signatures");
    }

    @Test
    public void testHubspotWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("hubspot_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-HubSpot-Signature-v3\")"),
        "Should validate HubSpot signature header");
    Assert.assertTrue(content.contains("${request.method}${request.rawPath}${check request.getTextPayload()}"
        + "${check request.getHeader(\"X-HubSpot-Request-Timestamp\")}"),
        "Should compose HubSpot payload using method, uri, body, and timestamp");
    }

    @Test
    public void testGitlabStaticTokenVerificationDsl() throws Exception {
    String content = generateDispatcherService("gitlab_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Gitlab-Token\")"),
        "Should validate GitLab token header");
    Assert.assertTrue(content.contains("string signature = extractedHeaderValues[\"signature\"]"),
        "Should extract signature token from header template");
    Assert.assertTrue(content.contains("crypto:equalConstantTime("
    + "signature.toBytes(), webhookSecret.toBytes())"),
        "Should compare extracted signature directly with webhook secret for static token mode");
    Assert.assertFalse(content.contains("crypto:hmacSha"),
        "Should not generate HMAC code when algorithm is not configured");
    }

    @Test
    public void testInvalidAdjacentPlaceholdersDsl() throws Exception {
    assertGeneratorException(
        "invalid_adjacent_placeholders.yaml",
        "adjacent placeholders are not allowed");
    }

    @Test
    public void testInvalidMissingSignatureDsl() throws Exception {
    assertGeneratorException(
        "invalid_missing_signature.yaml",
        "headerFormat must include $signature/{signature}");
    }

    @Test
    public void testInvalidBadPayloadTokenDsl() throws Exception {
    assertGeneratorException(
        "invalid_bad_payload_token.yaml",
        "Invalid input token in webhook DSL");
    }

    private Path generateOutputDir(String specFile) throws Exception {
    Path asyncapiPath = RES_DIR.resolve(specFile);
    String yamlContent = Files.readString(asyncapiPath);
    ObjectNode specNode = (ObjectNode) YAML_MAPPER.readTree(yamlContent);
        normalizeForAsyncApi3(specNode);
    String jsonContent = JSON_MAPPER.writeValueAsString(specNode);
    AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(jsonContent);

    String specName = specFile.replace(".yaml", "").replace(".yml", "");
    Path tempOutputDir = Paths.get("build", "generated-test-output", "webhook-dsl", specName).toAbsolutePath();
    Files.createDirectories(tempOutputDir);
    HttpCodeGenerator generator = new HttpCodeGenerator(spec);
    generator.generate(tempOutputDir);
    return tempOutputDir;
    }

    private String generateDispatcherService(String specFile) throws Exception {
    Path tempOutputDir = generateOutputDir(specFile);
    Path dispatcherFile = tempOutputDir.resolve("dispatcher_service.bal");
    Assert.assertTrue(Files.exists(dispatcherFile), "dispatcher_service.bal was not generated!");
    return Files.readString(dispatcherFile);
    }

    private void normalizeForAsyncApi3(ObjectNode specNode) {
        String version = specNode.path("asyncapi").asText("");
        if (!version.startsWith("3")) {
            return;
        }

        // Keep verification-focused fixtures parser-friendly for AsyncAPI 3.
        specNode.remove("servers");

        JsonNode channelsNode = specNode.get("channels");
        if (!(channelsNode instanceof ObjectNode channelsObj)) {
            return;
        }

        ObjectNode operationsNode = specNode.with("operations");
        Iterator<Map.Entry<String, JsonNode>> channels = channelsObj.fields();
        while (channels.hasNext()) {
            Map.Entry<String, JsonNode> channelEntry = channels.next();
            String channelName = channelEntry.getKey();
            if (!(channelEntry.getValue() instanceof ObjectNode channelObj)) {
                continue;
            }

            if (!channelObj.has("address")) {
                channelObj.put("address", channelName);
            }

            JsonNode subscribeNode = channelObj.get("subscribe");
            if (!(subscribeNode instanceof ObjectNode subscribeObj)) {
                continue;
            }

            ObjectNode messagesObj = JSON_MAPPER.createObjectNode();
            JsonNode messageNode = subscribeObj.get("message");
            if (messageNode != null) {
                if (messageNode.has("oneOf") && messageNode.get("oneOf") instanceof ArrayNode oneOfArray) {
                    for (int i = 0; i < oneOfArray.size(); i++) {
                        JsonNode oneOfMessage = oneOfArray.get(i);
                        String eventType = oneOfMessage.path("x-ballerina-event-type").asText("event_" + i);
                        messagesObj.set(eventTypeToMessageKey(eventType, i), oneOfMessage);
                    }
                } else {
                    String eventType = messageNode.path("x-ballerina-event-type").asText(channelName + "_event");
                    messagesObj.set(eventTypeToMessageKey(eventType, 0), messageNode);
                }
            }

            channelObj.remove("subscribe");
            channelObj.set("messages", messagesObj);

            String operationName = sanitizeIdentifier(channelName);
            ObjectNode operation = JSON_MAPPER.createObjectNode();
            operation.put("action", "receive");

            ObjectNode channelRef = JSON_MAPPER.createObjectNode();
            channelRef.put("$ref", "#/channels/" + channelName);
            operation.set("channel", channelRef);

            ArrayNode opMessages = JSON_MAPPER.createArrayNode();
            Iterator<String> msgNames = messagesObj.fieldNames();
            while (msgNames.hasNext()) {
                String msgName = msgNames.next();
                ObjectNode msgRef = JSON_MAPPER.createObjectNode();
                msgRef.put("$ref", "#/channels/" + channelName + "/messages/" + msgName);
                opMessages.add(msgRef);
            }
            operation.set("messages", opMessages);
            operationsNode.set(operationName, operation);
        }
    }

    private String eventTypeToMessageKey(String eventType, int index) {
        String key = sanitizeIdentifier(eventType);
        return key.isBlank() ? "message_" + index : key;
    }

    private String sanitizeIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private void assertGeneratorException(String specFile, String expectedMessagePart) throws Exception {
        try {
            generateDispatcherService(specFile);
            Assert.fail("Expected GeneratorException for invalid DSL in " + specFile);
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessagePart),
                "Validation message mismatch for " + specFile + ": " + e.getMessage());
        }
    }

    @Test
    public void testHubspotV3WebhookVerification() throws Exception {
        Path tempOutputDir = generateFromFixture("hubspot_v3_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        // HubSpot v3: $config('callbackUrl') resolves to a self field, folded into the HMAC input
        // alongside method/body/timestamp; encoding is base64, not hex.
        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcherContent.contains("crypto:hmacSha256"), "Should use HMAC-SHA256");
        Assert.assertTrue(
                dispatcherContent.contains("${self.callbackUrl}"),
                "Should reference the config-derived callbackUrl field");
        Assert.assertTrue(
                dispatcherContent.contains("computedDigest.toBase64()"),
                "Should base64-encode the computed digest");

        // Freshness check: a staleness guard independent of signature verification.
        Assert.assertTrue(
                dispatcherContent.contains("decimal:fromString(freshnessHeaderValue)"),
                "Should parse the freshness header as a decimal timestamp");
        Assert.assertTrue(
                dispatcherContent.contains("time:utcNow()"),
                "Should compare against the current time");
        Assert.assertTrue(
                dispatcherContent.contains("Request Timestamp Expired"),
                "Should reject stale requests");
        Assert.assertTrue(
                dispatcherContent.contains("freshnessSkewMillis.abs()"),
                "The freshness window must be symmetric - a timestamp far in the future must be "
                        + "rejected too, not just one that's too old: " + dispatcherContent);

        // callbackUrl must be threaded through as a real configurable field, not just referenced.
        String dataTypesContent = Files.readString(tempOutputDir.resolve("data_types.bal"));
        Assert.assertTrue(
                dataTypesContent.contains("string callbackUrl"),
                "ListenerConfig should declare a callbackUrl field");
        String listenerContent = Files.readString(tempOutputDir.resolve("listener.bal"));
        Assert.assertTrue(
                listenerContent.contains("listenerConfig.callbackUrl"),
                "Listener init should pass callbackUrl through to DispatcherService");

        // ballerina/time must be imported since freshness is configured.
        Assert.assertTrue(dispatcherContent.contains("import ballerina/time;"), "Should import ballerina/time");
    }

    @Test
    public void testHubspotV1WebhookVerification() throws Exception {
        Path tempOutputDir = generateFromFixture("hubspot_v1_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        // HubSpot v1/v2: plain (unkeyed) digest of secret+body, not a keyed HMAC.
        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcherContent.contains("crypto:hashSha256"), "Should use plain hashSha256");
        Assert.assertFalse(dispatcherContent.contains("crypto:hmacSha256"), "Should not use HMAC");
        Assert.assertTrue(
                dispatcherContent.contains("${webhookSecret}"),
                "Should fold the secret directly into the hashed payload via $secret");
        Assert.assertTrue(
                dispatcherContent.contains("computedDigest.toBase16()"),
                "Should hex-encode the computed digest");

        // No freshness block in this fixture: no staleness check, no time import.
        Assert.assertFalse(
                dispatcherContent.contains("Request Timestamp Expired"),
                "Should not emit a freshness check when none is configured");
        Assert.assertFalse(dispatcherContent.contains("import ballerina/time;"),
                "Should not import ballerina/time when no freshness check is configured");
    }

    @Test
    public void testHubspotV3DispatchTestUsesGeneralSigningNotGithubDefaults() throws Exception {
        // DispatchTestGenerator previously hardcoded GitHub's scheme (single composite header,
        // plain HMAC-SHA256 hex of the raw body) regardless of what the spec actually declared.
        // HubSpot's v3 scheme is structurally different: input is method+config+body+timestamp,
        // base64-encoded, with a freshness header the test itself must synthesize. This proves the
        // generated tests/dispatch_test.bal now follows the spec's real DSL, not GitHub's shape.
        Path tempOutputDir = generateFromFixture("hubspot_v3_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        Path dispatchTestFile = tempOutputDir.resolve("tests").resolve("dispatch_test.bal");
        Assert.assertTrue(Files.exists(dispatchTestFile), "tests/dispatch_test.bal was not generated!");
        String dispatchTestContent = Files.readString(dispatchTestFile);

        Assert.assertTrue(
                dispatchTestContent.contains("import ballerina/time;"),
                "Should import ballerina/time to synthesize the freshness timestamp: " + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("const TRIGGER_TEST_CALLBACK_URL"),
                "Should synthesize a test value for the $config('callbackUrl') reference: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("callbackUrl: TRIGGER_TEST_CALLBACK_URL"),
                "Should thread the config value into the test listener's configuration: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("crypto:hmacSha256(payloadToHash.toBytes(), "
                        + "TRIGGER_TEST_SECRET.toBytes())"),
                "Should compute a keyed HMAC-SHA256, not GitHub's plain-body hex default: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("computedDigest.toBase64()"),
                "Should base64-encode the digest per the spec's encoding, not GitHub's hex default: "
                        + dispatchTestContent);
        Assert.assertFalse(
                dispatchTestContent.contains("digest.toBase16()"),
                "Should not fall back to GitHub's hardcoded hex encoding: " + dispatchTestContent);
    }

    @Test
    public void testCustomVariableSharingReservedTokenPrefixIsNotCorrupted() throws Exception {
        // "bodyHash" is a custom variable extracted from the signature header, not the builtin
        // "$body" token -- but it starts with the substring "body". A naive String.replace("$body", ...)
        // on the input DSL would corrupt "$bodyHash" into "${check request.getTextPayload()}Hash"
        // before the custom-variable pass ever runs. The fix must treat "$body" as matched only
        // when it isn't immediately followed by another identifier character.
        Path tempOutputDir = generateFromFixture("token_corruption_verification.yaml", "X-Test-Event",
                "test_event");

        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(
                dispatcherContent.contains("${bodyHash}"),
                "Should interpolate the custom bodyHash variable extracted from the signature header");
        Assert.assertTrue(
                dispatcherContent.contains("${check request.getTextPayload()}"),
                "Should still correctly interpolate the builtin $body token");
        Assert.assertFalse(
                dispatcherContent.contains("${check request.getTextPayload()}Hash"),
                "$body substitution must not corrupt the unrelated $bodyHash custom variable");
    }

    /**
     * Loads a webhook-verification test fixture, injects the metadata the generator pipeline
     * expects, and runs it through {@link HttpCodeGenerator}.
     *
     * @param fixtureFileName the fixture file name under {@code src/test/resources/specs}
     * @param eventHeaderName the header used for event-type dispatch
     * @param eventType       the {@code x-ballerina-event-type} to assign to the fixture's message
     * @return the temp directory the generator wrote output files into
     */
    private Path generateFromFixture(String fixtureFileName, String eventHeaderName, String eventType)
            throws Exception {
        Path asyncapiPath = RES_DIR.resolve(fixtureFileName);

        String yamlContent = Files.readString(asyncapiPath);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectNode specNode = (ObjectNode) yamlMapper.readTree(yamlContent);

        ObjectNode eventIdentifier = specNode.putObject("x-ballerina-event-identifier");
        eventIdentifier.put("type", "header");
        eventIdentifier.put("name", eventHeaderName);

        ObjectNode subscribeNode = (ObjectNode) specNode.path("channels")
                .path("events")
                .path("subscribe");
        ObjectNode messageNode = (ObjectNode) subscribeNode.path("message");
        messageNode.put("x-ballerina-event-type", eventType);

        if (subscribeNode.has("x-ballerina-auth")) {
            specNode.set("x-ballerina-auth", subscribeNode.get("x-ballerina-auth"));
        }

        String jsonContent = new ObjectMapper().writeValueAsString(specNode);
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(jsonContent);

        Path tempOutputDir = Files.createTempDirectory("asyncapi-gen-test");
        HttpCodeGenerator generator = new HttpCodeGenerator(spec);
        generator.generate(tempOutputDir);
        return tempOutputDir;
    }
}


