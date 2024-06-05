package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This {@code SecurityTests} test class if for the covering the security tests for ballerina to asyncapi generation.
 */
public class SecurityTests {
    private static final Path RES_DIR =
            Paths.get("src/test/resources/ballerina-to-asyncapi/security").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Check for wss protocol if there is secure socket present as an annotation")
    public void notAcceptable() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("secure_socket_present.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "security/secure_socket_present.yaml");
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
