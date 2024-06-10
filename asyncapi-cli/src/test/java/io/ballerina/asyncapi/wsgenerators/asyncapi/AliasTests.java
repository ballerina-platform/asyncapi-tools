package io.ballerina.asyncapi.wsgenerators.asyncapi;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This {@code AliasTests} contains all the alias related testings.
 */
public class AliasTests {
    private static final Path RES_DIR =
            Paths.get("src/test/resources/websockets/ballerina-to-asyncapi/alias").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Use websocket alias as ws")
    public void notAcceptable() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("websocket.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "alias/websocket.yaml");
    }
    @AfterMethod
    public void cleanUp() {
        TestUtils.deleteDirectory(this.tempDir);
    }

    @AfterTest
    public void clean() {
        System.setErr(null);
        System.setOut(null);
    }
}
