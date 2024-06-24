/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.cmd;

import io.ballerina.cli.launcher.BLauncherException;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHERKEY_NULLABLE_EXCEPTION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHERKEY_OPTIONAL_EXCEPTION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHER_KEY_TYPE_EXCEPTION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FUNCTION_SIGNATURE_WRONG_TYPE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.FUNCTION_WRONG_NAME;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.NO_ANNOTATION_PRESENT;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.NO_DISPATCHER_KEY;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.NO_SERVICE_CLASS;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.PATH_PARAM_DASH_CONTAIN_ERROR;


/**
 * This {@code BallerinaToAsyncAPIExceptionTests} represents the tests for all the exceptions in the
 * ballerina to asyncapi command.
 *
 * @since 2.5.0
 */
public class BallerinaToAsyncAPIWsExceptionTests extends AsyncAPIWsCommandTest {

    @BeforeTest(description = "This will create a new ballerina project for testing below scenarios.")
    public void setupBallerinaProject() throws IOException {
        super.setup();
    }

    @Test(description = "Test websocket:serviceConfig annotation not present exception")
    public void testWebsocketServiceConfigAnnotationNotPresent() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/check_websocket_serviceConfig_annotation.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(NO_ANNOTATION_PRESENT));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test dispatcherKey is not present in a record field")
    public void testDispatcherKeyFieldNotPresentInEachRecord() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/dispatcherKey_field_not_present_in_record.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD,
                    "event", "Subscribe")));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test dispatcherKey is not string type in a record field")
    public void testDispatcherKeyFieldIsNotStringType() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/dispatcherKey_field_string_type_check.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(DISPATCHER_KEY_TYPE_EXCEPTION, "event")));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test dispatcherKey field is not present in annotation ")
    public void testDispatcherKeyFieldIsNotPresentInAnnotation() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/dispatcherKey_field_not_present_in_annotation.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(NO_DISPATCHER_KEY)));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test dispatcherKey is empty")
    public void testDispatcherKeyIsEmpty() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/empty_dispatcherKey_check.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY)));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test service class is not called")
    public void testNoServiceClassPresent() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/no_service_class_present.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(NO_SERVICE_CLASS)));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }
//
//    @Test(description = "Test onClose remote function is present as a remote function")
//    public void testOnCloseRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onClose_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onError remote function is present as a remote function")
//    public void testOnErrorRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onError_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onIdleTimeOut remote function is present as a remote function")
//    public void testOnIdleTimeOutRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onIdleTimeOut_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onTextMessage remote function is present as a remote function")
//    public void testOnTextMessageRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onTextMessage_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onBinaryMessage remote function is present as a remote function")
//    public void testOnBinaryMessageRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onBinaryMessage_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onOpen remote function is present as a remote function")
//    public void testOnOpenRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onOpen_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//
//    @Test(description = "Test onMessage remote function is present as a remote function")
//    public void testOnMessageRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onMessage_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onPing remote function is present as a remote function")
//    public void testOnPingRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onPing_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }
//
//    @Test(description = "Test onPong remote function is present as a remote function")
//    public void testOnPongRemoteFunctionNotPermitted() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/onPong_check.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_DEFAULT_NAME_CONTAINS_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }

    @Test(description = "Test dispatcherKey is optional not allowed in a field")
    public void testDispatcherKeyIsOptionalNotAllowedInAField() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/optional_dispatcherKey_present.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(DISPATCHERKEY_OPTIONAL_EXCEPTION,
                    "event", "Subscribe")));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    //
    @Test(description = "Test dispatcherKey is nullable not allowed in a field")
    public void testDispatcherKeyIsNullableNotAllowedInAField() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/nullable_dispatcherKey_present.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(DISPATCHERKEY_NULLABLE_EXCEPTION,
                    "event", "Subscribe")));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test remote function name is not in camel case form")
    public void testRemoteFunctionNameCamelCaseNotPresent() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/remotefunction_camelCase_not_present.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(FUNCTION_WRONG_NAME));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test remote function name is not start with onPrefix")
    public void testRemoteFunctionNameNotStartWithOnPrefix() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/remoteFunction_name_start.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(FUNCTION_WRONG_NAME));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test remote function request type is not a record a type")
    public void testRequestTypeRecordNotPresent() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/request_type_record_check.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(String.format(FUNCTION_SIGNATURE_WRONG_TYPE, "Subscribe", "int")));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    @Test(description = "Test path param contains dash character")
    public void testPathParamDashContain() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
                "/path_param_dash_contain.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Assert.assertTrue(output.trim().contains(PATH_PARAM_DASH_CONTAIN_ERROR));
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

//    @Test(description = "Test remote function returns multiple return options with stream type")
//    public void testMultipleReturnTypesIncludingStreamTypeError() {
//        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/exceptions" +
//                "/optional_multiple_type_stream_include_return.bal"));
//        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
//        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(cmd).parseArgs(args);
//
//        String output = "";
//        try {
//            cmd.execute();
//            output = readOutput(true);
//            Assert.assertTrue(output.trim().contains(String.format(UNION_STREAMING_SIMPLE_RPC_ERROR)));
//        } catch (BLauncherException | IOException e) {
//            output = e.toString();
//            Assert.fail(output);
//        }
//    }


}
