package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OptionalTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "When the remote method has an optional return")
    public void testReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_return.yaml");
    }
}
