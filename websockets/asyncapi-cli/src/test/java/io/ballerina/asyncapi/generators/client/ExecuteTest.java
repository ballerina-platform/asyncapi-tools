package io.ballerina.asyncapi.generators.client;

import io.ballerina.asyncapi.cli.AsyncAPIToBallerinaGenerator;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * This tests class is to generate ballerina client codes.
 */
public class ExecuteTest {

    @Test(description = "Generate Client for testings", enabled = false)
    public void generatePathWithPathParameterTests() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        AsyncAPIToBallerinaGenerator asyncAPIToBallerinaGenerator = new AsyncAPIToBallerinaGenerator();

        asyncAPIToBallerinaGenerator.generateClient(
                "src/test/resources/asyncapi-to-ballerina/client/StreamResponse/" +
                        "multiple_stream_with_dispatcherStreamId.yaml",
                "/Users/thushalya/Documents/out");

    }
}
