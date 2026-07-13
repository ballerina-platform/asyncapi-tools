package io.ballerina.asyncapi.generator.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebhookDSLVerificationTest {

    private static final Path RES_DIR = Paths.get("src/test/resources/specs").toAbsolutePath();

    @Test
    public void testGithubWebhookVerification() throws Exception {
        Path asyncapiPath = RES_DIR.resolve("github_verification.yaml");

        // 1. Prepare YAML content for parsing
        String yamlContent = Files.readString(asyncapiPath);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectNode specNode = (ObjectNode) yamlMapper.readTree(yamlContent);

        // 2. Inject required metadata that the generator pipeline expects
        ObjectNode eventIdentifier = specNode.putObject("x-ballerina-event-identifier");
        eventIdentifier.put("type", "header");
        eventIdentifier.put("name", "X-GitHub-Event");

        // Navigate to the subscribe node
        ObjectNode subscribeNode = (ObjectNode) specNode.path("channels")
                .path("events")
                .path("subscribe");

        // Set message event type
        ObjectNode messageNode = (ObjectNode) subscribeNode.path("message");
        messageNode.put("x-ballerina-event-type", "github_event");

        // Promote webhook auth to top-level for the Extractor to find it
        if (subscribeNode.has("x-ballerina-auth")) {
            specNode.set("x-ballerina-auth", subscribeNode.get("x-ballerina-auth"));
        }

        // 3. Parse and Generate
        String jsonContent = new ObjectMapper().writeValueAsString(specNode);
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(jsonContent);

        Path tempOutputDir = Files.createTempDirectory("asyncapi-gen-test");
        HttpCodeGenerator generator = new HttpCodeGenerator(spec);
        generator.generate(tempOutputDir);

        // 4. Validate output
        Path dispatcherFile = tempOutputDir.resolve("dispatcher_service.bal");
        Assert.assertTrue(Files.exists(dispatcherFile), "dispatcher_service.bal was not generated!");

        String content = Files.readString(dispatcherFile);

        // 5. Final Assertions
        Assert.assertTrue(content.contains("crypto:hmacSha256"), "Should contain HMAC-SHA256 algorithm");
        Assert.assertTrue(
            content.contains("string `${check request.getTextPayload()}`"),
            "Should extract text payload");
        Assert.assertTrue(
            content.contains("string `sha256=${computedSignature}`"),
            "Should format expected header with sha256=");
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

        // callbackUrl must be threaded through as a real configurable field, not just referenced.
        String dataTypesContent = Files.readString(tempOutputDir.resolve("types.bal"));
        Assert.assertTrue(
                dataTypesContent.contains("string callbackUrl"),
                "ListenerConfiguration should declare a callbackUrl field");
        String listenerContent = Files.readString(tempOutputDir.resolve("listener.bal"));
        Assert.assertTrue(
                listenerContent.contains("configuration.callbackUrl"),
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


