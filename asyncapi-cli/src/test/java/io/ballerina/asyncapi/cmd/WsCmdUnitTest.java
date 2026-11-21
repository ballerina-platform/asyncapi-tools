/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for WebSocket: input validation, mode resolution, the options ws supports, and the guide's rules on
 * targets, overwriting, licenses and output. Generation itself is covered by the generator tests.
 */
public class WsCmdUnitTest extends CmdTestBase {

    private static final String NL = System.lineSeparator();

    private static final String ECHO = String.join("\n",
            "asyncapi: 3.0.0",
            "info: {title: Echo, version: 1.0.0}",
            "x-dispatcherKey: type",
            "servers:",
            "  prod: {host: example.com, protocol: wss}",
            "channels:",
            "  ws:",
            "    address: /ws",
            "    messages:",
            "      ping: {$ref: '#/components/messages/Ping'}",
            "operations:",
            "  sendPing:",
            "    action: send",
            "    channel: {$ref: '#/channels/ws'}",
            "    messages: [{$ref: '#/channels/ws/messages/ping'}]",
            "components:",
            "  messages:",
            "    Ping:",
            "      payload:",
            "        type: object",
            "        required: [type]",
            "        properties: {type: {type: string, const: ping}}");

    private Path echoSpec() throws IOException {
        return Files.writeString(tmpDir.resolve("echo.yaml"), ECHO);
    }

    @Test(description = "A definition that does not exist fails with exit 1")
    public void testInvalidInputPath() {
        AsyncApiCmd cmd = run("/non/existent/file.yaml", "--protocol", "ws");
        Assert.assertTrue(getErr().contains("bal asyncapi: error: '/non/existent/file.yaml' does not exist"),
                "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
    }

    @Test(description = "A .bal input is accepted (code to spec) with --service")
    public void testBalFileAcceptedWithServiceFlag() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        run(balFile.toString(), "--protocol", "ws", "--service", "ChatService");
        Assert.assertFalse(getErr().contains("can't generate"), "Unexpected capability error. Err: " + getErr());
    }

    @Test(description = "The protocol is read from the definition's wss server, so --protocol is not needed")
    public void testProtocolDetectedFromServer() throws IOException {
        Path pkg = newPackage("detected");
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", pkg.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("client.bal")), "Err: " + getErr());
    }

    @Test(description = "Generated files carry the notice after the license, and the result lists them (GEN-3)")
    public void testLicenseAndNotice() throws IOException {
        Path pkg = newPackage("licensed");
        Path license = Files.writeString(tmpDir.resolve("license.txt"), "// Copyright (c) 2026 Example\r\n//\r\n");
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", pkg.toString(), "--license", license.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        String client = Files.readString(pkg.resolve("client.bal"));
        Assert.assertTrue(client.startsWith("// Copyright (c) 2026 Example\n//\n\n" + GeneratedHeader.NOTICE + "\n\n"),
                client);
        Assert.assertFalse(client.contains("\r"), "Generated sources use LF line endings (GEN-2)");
        Assert.assertTrue(getOut().contains("client.bal"), "Out: " + getOut());
        Assert.assertTrue(getOut().strip().endsWith("files."), "Out: " + getOut());
    }

    @Test(description = "A license with a line that is not a comment fails before anything is written (IO-3)")
    public void testInvalidLicense() throws IOException {
        Path pkg = newPackage("badlicense");
        Path license = Files.writeString(tmpDir.resolve("license.txt"), "// fine\nnot a comment\n");
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", pkg.toString(), "--license", license.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
        Assert.assertTrue(getErr().contains("has a line that is not a Ballerina comment"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains(":2:1: error: expected a comment starting with //"), "Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("client.bal")), "Nothing is written");
    }

    @Test(description = "A license file that does not exist fails with exit 1")
    public void testMissingLicense() throws IOException {
        Path pkg = newPackage("nolicense");
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", pkg.toString(), "--license", "/no/license.txt");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
        Assert.assertTrue(getErr().contains("cannot read license file '/no/license.txt'"), "Err: " + getErr());
    }

    @Test(description = "Replacing a changed file without --force and without a terminal fails with exit 4 (FF-6)")
    public void testOverwriteRefusedWithoutTerminal() throws IOException {
        Path pkg = newPackage("refused");
        Path spec = echoSpec();
        run(spec.toString(), "-o", pkg.toString());
        Files.writeString(pkg.resolve("client.bal"), "// hand edit");
        errContent.reset();
        AsyncApiCmd cmd = run(spec.toString(), "-o", pkg.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.OVERWRITE_REFUSED);
        Assert.assertTrue(getErr().contains("client.bal already exists"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("  Re-run with --force to overwrite." + NL), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("  Try: bal asyncapi "), "Err: " + getErr());
        Assert.assertTrue(getErr().strip().endsWith("--force"), "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("client.bal")), "// hand edit");
    }

    @Test(description = "--output-format json prints the file list as one JSON object on stdout (OUT-11)")
    public void testJsonResult() throws IOException {
        Path pkg = newPackage("jsonresult");
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", pkg.toString(), "--output-format", "json");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertEquals(getOut().lines().count(), 1, "Out: " + getOut());
        JsonNode result = new ObjectMapper().readTree(getOut());
        Assert.assertFalse(result.has("dryRun"), "dryRun is only on a dry run");
        Assert.assertTrue(result.get("files").size() > 0, "Out: " + getOut());
        for (JsonNode file : result.get("files")) {
            Assert.assertEquals(file.get("action").asText(), "created", "Out: " + getOut());
        }

        outContent.reset();
        run(echoSpec().toString(), "-o", pkg.toString(), "--output-format", "json", "--dry-run");
        Assert.assertTrue(new ObjectMapper().readTree(getOut()).get("dryRun").asBoolean(), "Out: " + getOut());
    }

    @Test(description = "A package directory is accepted as the input for code to spec")
    public void testPackageDirectoryAccepted() throws IOException {
        Path pkg = newPackage("chatpkg");
        lockDependencies(pkg);
        AsyncApiCmd cmd = run(pkg.toString(), "--protocol", "ws");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertTrue(getErr().contains("bal asyncapi: warning: no WebSocket service found in the module"),
                "What the generator says goes to stderr as a warning. Err: " + getErr());
        Assert.assertTrue(getOut().contains("No files were generated."), "Out: " + getOut());
    }

    @Test(description = "A package without Dependencies.toml is not compiled, and bal build is the fix (CMP-2)")
    public void testUnlockedPackage() throws IOException {
        Path pkg = newPackage("unlockedpkg");
        AsyncApiCmd cmd = run(pkg.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
        Assert.assertTrue(getErr().contains("error: dependencies are not locked for package 'unlockedpkg'"),
                "Err: " + getErr());
        Assert.assertTrue(getErr().contains("  Try: bal build "), "Err: " + getErr());
    }

    @Test(description = "A directory without a Ballerina.toml is not a package (exit 3)")
    public void testDirectoryWithoutBallerinaToml() throws IOException {
        Path dir = Files.createDirectory(tmpDir.resolve("plain"));
        AsyncApiCmd cmd = run(dir.toString(), "--protocol", "ws");
        Assert.assertTrue(getErr().contains("is not a Ballerina package"), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.INVALID_TARGET);
    }

    @Test(description = "A .bal file inside a package is rejected, naming the package (TGT-5)")
    public void testBalFileInsidePackageRejected() throws IOException {
        Path pkg = newPackage("insidepkg");
        AsyncApiCmd cmd = run(pkg.resolve("main.bal").toString(), "--protocol", "ws");
        Assert.assertTrue(getErr().contains("belongs to the Ballerina package"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Pass the package directory instead."), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.INVALID_TARGET);
    }

    @Test(description = "--module reads a module of the package")
    public void testModuleOfPackageAccepted() throws IOException {
        Path pkg = newPackage("modpkg");
        lockDependencies(pkg);
        Files.createDirectories(pkg.resolve("modules").resolve("chat"));
        Files.writeString(pkg.resolve("modules").resolve("chat").resolve("chat.bal"), "");
        AsyncApiCmd cmd = run(pkg.toString(), "--protocol", "ws", "--module", "chat");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertFalse(getErr().contains("error:"), "Err: " + getErr());
    }

    @Test(description = "A module the package does not have is rejected and the available ones are listed")
    public void testUnknownModuleRejected() throws IOException {
        Path pkg = newPackage("nomodpkg");
        Files.createDirectories(pkg.resolve("modules").resolve("chat"));
        Files.createDirectories(pkg.resolve("modules").resolve("admin"));
        AsyncApiCmd cmd = run(pkg.toString(), "--protocol", "ws", "--module", "missing");
        Assert.assertTrue(getErr().contains("module 'missing' not found in the package"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Use one of its modules: admin, chat."), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.INVALID_TARGET);
    }

    @Test(description = "A package without modules says it has none")
    public void testUnknownModuleWithoutModulesDirectory() throws IOException {
        Path pkg = newPackage("barepkg");
        run(pkg.toString(), "--protocol", "ws", "--module", "chat");
        Assert.assertTrue(getErr().contains("The package has no modules; leave out --module."), "Err: " + getErr());
    }

    @Test(description = "The package's own name selects its default module")
    public void testPackageNameSelectsDefaultModule() throws IOException {
        Path pkg = newPackage("selfpkg");
        lockDependencies(pkg);
        AsyncApiCmd cmd = run(pkg.toString(), "--protocol", "ws", "--module", "selfpkg");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
    }

    @Test(description = "--module with a standalone .bal file is a usage error (MOD-4)")
    public void testModuleWithStandaloneFile() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        AsyncApiCmd cmd = run(balFile.toString(), "--protocol", "ws", "--module", "chat");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("--module can't be used with a standalone .bal file"), "Err: " + getErr());
    }

    @Test(description = "--spec-format json with a .bal input produces no warning")
    public void testSpecFormatOnBalInput() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        run(balFile.toString(), "--protocol", "ws", "--service", "MyService", "--spec-format", "json");
        Assert.assertFalse(getErr().contains("warning: --spec-format"), "Unexpected warning. Err: " + getErr());
    }

    @Test(description = "--spec-format and --service warn when generating code from a spec")
    public void testSpecOutputOnlyOptionsWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        run(specFile.toString(), "--protocol", "ws", "--spec-format", "json", "--service", "MyService");
        Assert.assertTrue(getErr().contains("warning: --spec-format only applies"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: --service only applies"), "Err: " + getErr());
    }

    @Test(description = "--with-tests and --license are supported for ws and produce no warning")
    public void testSupportedOptionsDoNotWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        run(specFile.toString(), "--protocol", "ws", "--with-tests", "--license", licenseFile.toString(),
                "--output", tmpDir.resolve("out").toString());
        Assert.assertFalse(getErr().contains("warning:"), "Unexpected warning. Err: " + getErr());
    }

    @Test(description = "Generating into a directory that is not a package fails with exit 3")
    public void testOutputMustBeAPackage() throws IOException {
        AsyncApiCmd cmd = run(echoSpec().toString(), "-o", Files.createDirectory(tmpDir.resolve("plain")).toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.INVALID_TARGET);
        Assert.assertTrue(getErr().contains("is not a Ballerina package"), "Err: " + getErr());
    }

    @Test(description = "CmdUtils.resolveMode returns BALLERINA_TO_ASYNCAPI for a directory")
    public void testResolveModeDirectory() throws IOException {
        BaseCmd base = new BaseCmd();
        base.inputPath = Files.createDirectory(tmpDir.resolve("somedir")).toString();
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.BALLERINA_TO_ASYNCAPI);
    }

    @Test(description = "CmdUtils.resolveMode returns ASYNCAPI_TO_BALLERINA for a .yaml input")
    public void testResolveModeSpecToCode() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "definition.yaml";
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.ASYNCAPI_TO_BALLERINA);
    }

    @Test(description = "CmdUtils.resolveMode returns BALLERINA_TO_ASYNCAPI for a .bal input")
    public void testResolveModeCodeToSpec() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "service.bal";
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.BALLERINA_TO_ASYNCAPI);
    }

    @Test(description = "A spec warning names its line and column in the definition (ERR-5)")
    public void testHostWithSchemeWarns() throws IOException {
        Path specFile = Files.writeString(tmpDir.resolve("scheme.yaml"), ECHO.replace(
                "  prod: {host: example.com, protocol: wss}", "  prod: {host: 'wss://example.com', protocol: wss}"));
        Path pkg = newPackage("hostscheme");
        AsyncApiCmd cmd = run(specFile.toString(), "--protocol", "ws", "--output", pkg.toString());
        String where = specFile.toString().replace('\\', '/') + ":5:";
        Assert.assertTrue(getErr().contains(where), "Expected the line of the server. Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: server 'prod' host 'wss://example.com' holds more than the host "
                + "name; read it as host 'example.com'" + NL + "  Put only the host name in "
                + "'host', the scheme in 'protocol' and the path in 'pathname'."), "Err: " + getErr());
        Assert.assertFalse(getErr().contains("error:"), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS);
        Assert.assertTrue(Files.exists(pkg.resolve("client.bal")), "Err: " + getErr());
    }

    @Test(description = "A sent message without a payload gets no function, with a warning, and the rest is generated")
    public void testMessageWithoutPayloadWarns() throws IOException {
        Path specFile = Files.writeString(tmpDir.resolve("nopayload.yaml"), String.join("\n",
                "asyncapi: 3.0.0",
                "info: {title: Echo, version: 1.0.0}",
                "x-dispatcherKey: type",
                "servers:",
                "  prod: {host: example.com, protocol: wss}",
                "channels:",
                "  ws:",
                "    address: /ws",
                "    messages:",
                "      ping: {$ref: '#/components/messages/Ping'}",
                "      echo: {contentType: text/plain}",
                "operations:",
                "  sendPing:",
                "    action: send",
                "    channel: {$ref: '#/channels/ws'}",
                "    messages: [{$ref: '#/channels/ws/messages/ping'}]",
                "  sendEcho:",
                "    action: send",
                "    channel: {$ref: '#/channels/ws'}",
                "    messages: [{$ref: '#/channels/ws/messages/echo'}]",
                "components:",
                "  messages:",
                "    Ping:",
                "      payload:",
                "        type: object",
                "        required: [type]",
                "        properties: {type: {type: string, const: ping}}"));
        Path pkg = newPackage("nopayload");
        run(specFile.toString(), "--protocol", "ws", "--output", pkg.toString());
        Assert.assertTrue(getErr().contains("warning: message 'echo' has no JSON Schema payload, so the client has no "
                + "function to send it" + NL
                + "  Give the message a JSON Schema 'payload' to get a function for it."), "Err: " + getErr());
        Assert.assertFalse(getErr().contains("error:"), "Err: " + getErr());
        String client = Files.readString(pkg.resolve("client.bal"));
        Assert.assertTrue(client.contains("remote isolated function doPing("), client);
        Assert.assertFalse(client.contains("doEcho"), client);
    }

    // A ticker written from the server's side: the server sends Tick and receives Subscribe
    private static final String SERVER_SIDE_TICKER = String.join("\n",
            "asyncapi: 3.0.0",
            "info: {title: Ticker, version: 1.0.0}",
            "servers:",
            "  prod: {host: example.com, protocol: wss}",
            "channels:",
            "  ticks:",
            "    address: /ticks",
            "    messages:",
            "      Subscribe: {$ref: '#/components/messages/Subscribe'}",
            "      Tick: {$ref: '#/components/messages/Tick'}",
            "operations:",
            "  sendTick:",
            "    action: send",
            "    channel: {$ref: '#/channels/ticks'}",
            "    messages: [{$ref: '#/channels/ticks/messages/Tick'}]",
            "  onSubscribe:",
            "    action: receive",
            "    channel: {$ref: '#/channels/ticks'}",
            "    messages: [{$ref: '#/channels/ticks/messages/Subscribe'}]",
            "components:",
            "  messages:",
            "    Subscribe: {payload: {type: object, required: [symbol], properties: {symbol: {type: string}}}}",
            "    Tick: {payload: {type: object, required: [price], properties: {price: {type: number}}}}");

    @Test(description = "--spec-side service reads a spec written from the server's side: what it sends, the client "
            + "receives")
    public void testServiceSideSpecSwapsSendAndReceive() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Files.writeString(specFile, SERVER_SIDE_TICKER);
        Path pkg = newPackage("serverside");
        run(specFile.toString(), "--protocol", "ws", "--spec-side", "service", "--output", pkg.toString());
        Assert.assertFalse(getErr().contains("error:"), "Err: " + getErr());
        String client = Files.readString(pkg.resolve("client.bal"));
        Assert.assertTrue(client.contains("remote isolated function doSubscribe(Subscribe subscribe, decimal timeout)"),
                client);
        Assert.assertTrue(client.contains("remote isolated function doTick(decimal timeout) returns Tick|error"),
                client);
    }

    @Test(description = "Without --spec-side the spec is read as written, from the client's side")
    public void testClientSideIsTheDefault() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Files.writeString(specFile, SERVER_SIDE_TICKER);
        Path pkg = newPackage("clientside");
        run(specFile.toString(), "--protocol", "ws", "--spec-side", "CLIENT", "--output", pkg.toString());
        String client = Files.readString(pkg.resolve("client.bal"));
        Assert.assertTrue(client.contains("remote isolated function doTick(Tick tick, decimal timeout)"), client);
    }

    @Test(description = "An unknown --spec-side is a usage error naming the values it takes")
    public void testUnknownSpecSide() {
        AsyncApiCmd cmd = run("spec.yaml", "--protocol", "ws", "--spec-side", "server");
        Assert.assertTrue(getErr().contains("invalid value 'server' for --spec-side"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Use client or service."), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
    }

    @Test(description = "--spec-side warns when generating a definition from Ballerina code")
    public void testSpecSideWarnsForCodeToSpec() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        run(balFile.toString(), "--protocol", "ws", "--spec-side", "service");
        Assert.assertTrue(getErr().contains("warning: --spec-side only applies when generating code from an "
                + "AsyncAPI definition"), "Err: " + getErr());
    }
}
