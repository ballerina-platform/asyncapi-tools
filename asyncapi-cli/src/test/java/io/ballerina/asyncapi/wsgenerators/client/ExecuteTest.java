package io.ballerina.asyncapi.wsgenerators.client;

import io.ballerina.asyncapi.cmd.websockets.AsyncAPIToBallerinaGenerator;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * This tests class is to generate ballerina client codes.
 */
public class ExecuteTest {

    @Test(description = "Generate Client for testings", enabled = false)
    public void generatePathWithPathParameterTests() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        AsyncAPIToBallerinaGenerator asyncAPIToBallerinaGenerator = new AsyncAPIToBallerinaGenerator();

        asyncAPIToBallerinaGenerator.generateClient(
                "src/test/resources/websockets/asyncapi-to-ballerina/client/StreamResponse/" +
                        "multiple_stream_with_dispatcherStreamId.yaml",
                "/Users/thushalya/Documents/out");

    }
}
