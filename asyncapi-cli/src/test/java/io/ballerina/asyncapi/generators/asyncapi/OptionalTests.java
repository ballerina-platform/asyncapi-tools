package io.ballerina.asyncapi.generators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This test class for optional returns.
 */
public class OptionalTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "When the remote method has an optional return")
    public void testIntReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_int_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_int_return.yaml");
    }

    @Test(description = "When the remote method has an optional return as error")
    public void testErrorReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_error_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_error_return.yaml");
    }

    @Test(description = "When the remote method has two union type returns with optional return")
    public void testTwoTypesReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_two_union_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_two_union_return.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "When the remote method has multiple return types including streaming " +
            "with optional return")
    public void testMultipleTypesStreamIncludeReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_multiple_type_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_multiple_type_return.yaml");
    }
}
