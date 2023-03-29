package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.generators.asyncapi.TestUtils.compareWithGeneratedFile;

public class ChannelTests {

    private static final Path RES_DIR = Paths.get("src/test/resources/ballerina-to-asyncapi/channels").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Generate AsyncAPI spec for resource has .")
    public void testChannelScenario01() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario01.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario01.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for resource has path param and query param")
    public void testChannelScenario02() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario02.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario02.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with multipath including .")
    public void testChannelScenario03() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario03.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario03.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with path parameter including .")
    public void testChannelScenario04() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario04.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario04.yaml");
    }
}
