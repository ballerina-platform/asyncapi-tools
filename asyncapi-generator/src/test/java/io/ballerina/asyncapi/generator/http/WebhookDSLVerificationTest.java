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
}


