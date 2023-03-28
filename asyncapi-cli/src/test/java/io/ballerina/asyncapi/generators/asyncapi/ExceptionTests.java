package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExceptionTests {
    private static final Path RES_DIR =
            Paths.get("src/test/resources/ballerina-to-asyncapi/exceptions").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Use websocket alias as ws")
    public void notAcceptable() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("dispatcherKey_present.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "exceptions/check_dispatcherKey.yaml");
    }

}
