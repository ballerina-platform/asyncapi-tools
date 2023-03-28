package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdditionalTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "test to chose correct service object has selected")
    public void testChoseCorrectServiceObject() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("additional/rs_scenario01.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "additional/rs_scenario01.yaml");
    }

}
