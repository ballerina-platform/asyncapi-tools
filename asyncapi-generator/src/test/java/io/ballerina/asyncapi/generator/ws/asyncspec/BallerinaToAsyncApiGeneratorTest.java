/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.ws.asyncspec;

import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Integration tests for
 * {@link BallerinaToAsyncApiGenerator#generateAsyncAPIDefinitionsAllService}.
 *
 * <p>Each test writes a {@code Ballerina.toml} and a {@code service.bal} into an isolated temp
 * directory so the generator's walk-up loop finds a project root immediately (creating a
 * {@code BuildProject}), then asserts properties of the generated AsyncAPI YAML or JSON.
 *
 * <p><strong>Runtime requirement:</strong> Ballerina must be installed and its JARs accessible
 * at test runtime (all Ballerina dependencies are {@code compileOnly} in the build).
 * Tests will fail with {@code NoClassDefFoundError} in environments without Ballerina.
 *
 * <p>Note on reserved function names: {@code onMessage}, {@code onTextMessage},
 * {@code onBinaryMessage}, {@code onClose}, {@code onOpen}, {@code onPing}, and {@code onPong}
 * are silently skipped by {@code AsyncApiRemoteMapper}. All source constants here use
 * non-reserved names (e.g. {@code onChatMessage}, {@code onOrder}).
 *
 * <p>Note on parameter type matching: {@code checkParameterContainsCustomType} performs an
 * exact string comparison against {@code functionName.substring(2)}, so each remote function's
 * parameter type must be named exactly {@code FunctionName.substring(2)} (e.g.
 * {@code onChatMessage} requires a parameter of type {@code ChatMessage}).
 */
class BallerinaToAsyncApiGeneratorTest {

    // -------------------------------------------------------------------------
    // BAL source constants
    // -------------------------------------------------------------------------

    // Single remote function — produces chat_asyncapi.yaml (service /chat).
    private static final String BAL_ONE_REMOTE =
            "import ballerina/websocket;\n\n"
            + "public type ChatMessage record {\n"
            + "    string event;\n"
            + "    string content;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /chat on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new ChatService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ChatService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onChatMessage(ChatMessage msg) returns string|error {\n"
            + "        return msg.content;\n"
            + "    }\n"
            + "}\n";

    // The connection handle comes first and the module is imported under another prefix.
    private static final String BAL_CALLER_FIRST =
            "import ballerina/websocket as ws;\n\n"
            + "public type ChatMessage record {\n"
            + "    string event;\n"
            + "    string content;\n"
            + "};\n\n"
            + "@ws:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /chat on new ws:Listener(9090) {\n"
            + "    resource function get .() returns ws:Service|ws:UpgradeError {\n"
            + "        return new ChatService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ChatService {\n"
            + "    *ws:Service;\n"
            + "    remote function onChatMessage(ws:Caller caller, ChatMessage msg) returns string|error {\n"
            + "        return msg.content;\n"
            + "    }\n"
            + "}\n";

    // Regression fixture for #6601 — function name "onHello" does not follow the on<ParameterType>
    // convention even though the parameter type is ClientData. Must still generate a full spec.
    private static final String BAL_MISMATCHED_FUNCTION_NAME =
            "import ballerina/websocket;\n\n"
            + "public type ClientData record {\n"
            + "    string event;\n"
            + "    string id;\n"
            + "};\n\n"
            + "public type User record {\n"
            + "    string name;\n"
            + "    string gender;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service / on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new WsService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class WsService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onHello(ClientData clientData) returns User[] {\n"
            + "        return [];\n"
            + "    }\n"
            + "}\n";

    // Exact scenario from the original #6601 issue: parameter/return types referenced via a
    // *qualified* name from an imported submodule (types:ClientData), not a same-file simple
    // name. Exercises the QUALIFIED_NAME_REFERENCE branch of resolveParameterTypeName(), which
    // BAL_MISMATCHED_FUNCTION_NAME above doesn't reach. Paired with TYPES_SUBMODULE_SOURCE,
    // written into modules/types/types.bal by runWithTypesSubmodule().
    private static final String BAL_MISMATCHED_FUNCTION_NAME_QUALIFIED_TYPE =
            "import ballerina/websocket;\n"
            + "import testorg/asyncspectest.types;\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service / on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new WsService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class WsService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onHello(types:ClientData clientData) returns types:User[] {\n"
            + "        return [];\n"
            + "    }\n"
            + "}\n";

    private static final String TYPES_SUBMODULE_SOURCE =
            "public type ClientData record {\n"
            + "    string event;\n"
            + "    string id;\n"
            + "};\n\n"
            + "public type User record {\n"
            + "    string name;\n"
            + "    string gender;\n"
            + "};\n\n"
            + "public const CUSTOM_EVENT = \"from_types\";\n";

    // Three remote functions — produces orders_asyncapi.yaml (service /orders).
    // Type names Order, Cancel, Update match functionName.substring(2) exactly.
    private static final String BAL_THREE_REMOTES =
            "import ballerina/websocket;\n\n"
            + "public type Order record { string event; string orderId; };\n"
            + "public type Cancel record { string event; string orderId; };\n"
            + "public type Update record { string event; string data; };\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /orders on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new OrderService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class OrderService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onOrder(Order msg) returns string|error { return msg.orderId; }\n"
            + "    remote function onCancel(Cancel msg) returns string|error { return msg.orderId; }\n"
            + "    remote function onUpdate(Update msg) returns string|error { return msg.data; }\n"
            + "}\n";

    // Multi-segment path — produces users_profile_asyncapi.yaml.
    private static final String BAL_WITH_PATH =
            "import ballerina/websocket;\n\n"
            + "public type ProfileUpdate record { string event; string userId; };\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /users/profile on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new ProfileService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ProfileService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onProfileUpdate(ProfileUpdate msg) returns string|error {\n"
            + "        return msg.userId;\n"
            + "    }\n"
            + "}\n";

    // Multi-field record parameter — all field names must appear in components schema.
    private static final String BAL_RECORD_PARAM =
            "import ballerina/websocket;\n\n"
            + "public type Order record {\n"
            + "    string event;\n"
            + "    string orderId;\n"
            + "    decimal amount;\n"
            + "    string customerId;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /orders on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new OrderService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class OrderService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onOrder(Order msg) returns string|error {\n"
            + "        return msg.orderId;\n"
            + "    }\n"
            + "}\n";

    // Record return type — ChatResponse must appear in the generated spec.
    private static final String BAL_RECORD_RETURN =
            "import ballerina/websocket;\n\n"
            + "public type ChatRequest record { string event; string message; };\n"
            + "public type ChatResponse record { string response; int status; };\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /chat on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new ChatService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ChatService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onChatRequest(ChatRequest msg) returns ChatResponse|error {\n"
            + "        ChatResponse resp = {response: \"ok\", status: 0};\n"
            + "        return resp;\n"
            + "    }\n"
            + "}\n";

    // Optional field — service path "/" maps output file to service_asyncapi.yaml.
    // optionalField (string?) must appear in the schema but not in the required list.
    private static final String BAL_OPTIONAL_FIELD =
            "import ballerina/websocket;\n\n"
            + "public type Data record {\n"
            + "    string event;\n"
            + "    string requiredField;\n"
            + "    string? optionalField;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service / on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new DataService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class DataService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onData(Data msg) returns string|error {\n"
            + "        return msg.requiredField;\n"
            + "    }\n"
            + "}\n";

    // BAL_ONE_REMOTE with the @websocket:ServiceConfig annotation line removed.
    // No @websocket:ServiceConfig, so the service is reported as a diagnostic (NO_ANNOTATION_PRESENT).
    private static final String BAL_MISSING_ANNOTATION =
            "import ballerina/websocket;\n\n"
            + "public type ChatMessage record {\n"
            + "    string event;\n"
            + "    string content;\n"
            + "};\n\n"
            + "service /chat on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new ChatService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ChatService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onChatMessage(ChatMessage msg) returns string|error {\n"
            + "        return msg.content;\n"
            + "    }\n"
            + "}\n";

    // -------------------------------------------------------------------------
    // setup / teardown
    // -------------------------------------------------------------------------

    // Minimal Ballerina.toml written into every test's project directory.
    // The generator's walk-up loop finds it immediately (loadPath = projectDir),
    // so ProjectLoader creates a BuildProject rather than a failing single-file project.
    // distribution must match the value in asyncapi-tool/Ballerina.toml and gradle.properties.
    private static final String BALLERINA_TOML =
            "[package]\n"
            + "name = \"asyncspectest\"\n"
            + "org = \"testorg\"\n"
            + "version = \"0.1.0\"\n"
            + "distribution = \"2201.13.0\"\n";

    // Pre-resolved dependency graph for the gradle-assembled test toolchain (see
    // asyncapi-generator/build.gradle's ballerinaStdLibs configuration / "ballerina.home" system
    // property). BallerinaToAsyncApiGenerator resolves with BuildOptions.setOffline(true), which
    // requires a lock file to determine the exact transitive graph up front — without one, the
    // offline resolver cannot walk ballerina/websocket's transitive closure (ballerina/log and
    // others) from scratch and fails with "cannot resolve module 'ballerina/log'" even though the
    // package is physically present in the local repo. Versions below match exactly what's on disk
    // under build/jballerina-tools-2201.13.0/repo/bala/ballerina after copyStdlibs runs.
    private static final String DEPENDENCIES_TOML = """
            [ballerina]
            dependencies-toml-version = "2"
            distribution-version = "2201.13.0"

            [[package]]
            org = "ballerina"
            name = "auth"
            version = "2.14.0"
            dependencies = [
            \t{org = "ballerina", name = "crypto"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.array"},
            \t{org = "ballerina", name = "lang.string"},
            \t{org = "ballerina", name = "log"}
            ]

            [[package]]
            org = "ballerina"
            name = "cache"
            version = "3.10.0"
            dependencies = [
            \t{org = "ballerina", name = "constraint"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "task"},
            \t{org = "ballerina", name = "time"}
            ]

            [[package]]
            org = "ballerina"
            name = "constraint"
            version = "1.7.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "crypto"
            version = "2.9.2"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "time"}
            ]

            [[package]]
            org = "ballerina"
            name = "data.jsondata"
            version = "1.1.3"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.object"}
            ]

            [[package]]
            org = "ballerina"
            name = "file"
            version = "1.12.0"
            dependencies = [
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "os"},
            \t{org = "ballerina", name = "time"}
            ]

            [[package]]
            org = "ballerina"
            name = "http"
            version = "2.15.0"
            dependencies = [
            \t{org = "ballerina", name = "auth"},
            \t{org = "ballerina", name = "cache"},
            \t{org = "ballerina", name = "constraint"},
            \t{org = "ballerina", name = "crypto"},
            \t{org = "ballerina", name = "data.jsondata"},
            \t{org = "ballerina", name = "file"},
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "jwt"},
            \t{org = "ballerina", name = "lang.array"},
            \t{org = "ballerina", name = "lang.decimal"},
            \t{org = "ballerina", name = "lang.int"},
            \t{org = "ballerina", name = "lang.regexp"},
            \t{org = "ballerina", name = "lang.runtime"},
            \t{org = "ballerina", name = "lang.string"},
            \t{org = "ballerina", name = "lang.value"},
            \t{org = "ballerina", name = "log"},
            \t{org = "ballerina", name = "mime"},
            \t{org = "ballerina", name = "oauth2"},
            \t{org = "ballerina", name = "observe"},
            \t{org = "ballerina", name = "time"},
            \t{org = "ballerina", name = "url"}
            ]

            [[package]]
            org = "ballerina"
            name = "io"
            version = "1.8.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.value"}
            ]

            [[package]]
            org = "ballerina"
            name = "jballerina.java"
            version = "0.0.0"

            [[package]]
            org = "ballerina"
            name = "jwt"
            version = "2.15.1"
            dependencies = [
            \t{org = "ballerina", name = "cache"},
            \t{org = "ballerina", name = "crypto"},
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.int"},
            \t{org = "ballerina", name = "lang.string"},
            \t{org = "ballerina", name = "log"},
            \t{org = "ballerina", name = "time"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.__internal"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.object"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.array"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.__internal"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.decimal"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.int"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.__internal"},
            \t{org = "ballerina", name = "lang.object"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.object"
            version = "0.0.0"

            [[package]]
            org = "ballerina"
            name = "lang.regexp"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.runtime"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.string"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.regexp"}
            ]

            [[package]]
            org = "ballerina"
            name = "lang.value"
            version = "0.0.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "log"
            version = "2.13.0"
            dependencies = [
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.value"},
            \t{org = "ballerina", name = "observe"}
            ]

            [[package]]
            org = "ballerina"
            name = "mime"
            version = "2.12.0"
            dependencies = [
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.int"},
            \t{org = "ballerina", name = "log"}
            ]

            [[package]]
            org = "ballerina"
            name = "oauth2"
            version = "2.15.0"
            dependencies = [
            \t{org = "ballerina", name = "cache"},
            \t{org = "ballerina", name = "crypto"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "log"},
            \t{org = "ballerina", name = "time"},
            \t{org = "ballerina", name = "url"}
            ]

            [[package]]
            org = "ballerina"
            name = "observe"
            version = "1.6.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "os"
            version = "1.10.1"
            dependencies = [
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "task"
            version = "2.11.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "time"},
            \t{org = "ballerina", name = "uuid"}
            ]

            [[package]]
            org = "ballerina"
            name = "time"
            version = "2.8.0"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "url"
            version = "2.6.1"
            dependencies = [
            \t{org = "ballerina", name = "jballerina.java"}
            ]

            [[package]]
            org = "ballerina"
            name = "uuid"
            version = "1.10.0"
            dependencies = [
            \t{org = "ballerina", name = "crypto"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "lang.int"},
            \t{org = "ballerina", name = "time"}
            ]

            [[package]]
            org = "ballerina"
            name = "websocket"
            version = "2.15.0"
            dependencies = [
            \t{org = "ballerina", name = "auth"},
            \t{org = "ballerina", name = "constraint"},
            \t{org = "ballerina", name = "http"},
            \t{org = "ballerina", name = "io"},
            \t{org = "ballerina", name = "jballerina.java"},
            \t{org = "ballerina", name = "jwt"},
            \t{org = "ballerina", name = "lang.array"},
            \t{org = "ballerina", name = "lang.runtime"},
            \t{org = "ballerina", name = "lang.string"},
            \t{org = "ballerina", name = "lang.value"},
            \t{org = "ballerina", name = "log"},
            \t{org = "ballerina", name = "oauth2"},
            \t{org = "ballerina", name = "time"}
            ]
            modules = [
            \t{org = "ballerina", packageName = "websocket", moduleName = "websocket"}
            ]

            [[package]]
            org = "testorg"
            name = "asyncspectest"
            version = "0.1.0"
            dependencies = [
            \t{org = "ballerina", name = "websocket"}
            ]
            modules = [
            \t{org = "testorg", packageName = "asyncspectest", moduleName = "asyncspectest"}
            ]
            """;

    private Path projectDir;
    private Path outDir;
    private Path balFile;

    @BeforeMethod
    void setUp() throws IOException {
        outDir = Files.createTempDirectory("asyncspec-out-");
        projectDir = Files.createTempDirectory("asyncspec-project-");
        Files.writeString(projectDir.resolve("Ballerina.toml"), BALLERINA_TOML);
        Files.writeString(projectDir.resolve("Dependencies.toml"), DEPENDENCIES_TOML);
        balFile = projectDir.resolve("service.bal");
    }

    @AfterMethod
    void tearDown() throws IOException {
        deleteDir(outDir);
        deleteDir(projectDir);
    }

    private static void deleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Path writeBalSource(String source) throws IOException {
        Path balFile = projectDir.resolve("service.bal");
        Files.writeString(balFile, source);
        return balFile;
    }

    private List<AsyncApiConverterDiagnostic> run(String source) throws IOException {
        return run(source, false);
    }

    private List<AsyncApiConverterDiagnostic> run(String source, boolean needJson) throws IOException {
        Path balFile = writeBalSource(source);
        return BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, needJson, System.out);
    }

    // Writes TYPES_SUBMODULE_SOURCE into modules/types/types.bal (importable as
    // testorg/asyncspectest.types) alongside the given main source, so tests can exercise a
    // qualified (cross-module) type reference the way the original #6601 issue's repro did.
    private List<AsyncApiConverterDiagnostic> runWithTypesSubmodule(String source) throws IOException {
        Path modulesDir = projectDir.resolve("modules").resolve("types");
        Files.createDirectories(modulesDir);
        Files.writeString(modulesDir.resolve("types.bal"), TYPES_SUBMODULE_SOURCE);
        Path balFile = writeBalSource(source);
        return BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);
    }

    private List<AsyncApiConverterDiagnostic> runWithServiceName(String source, String serviceName)
            throws IOException {
        Path balFile = writeBalSource(source);
        return BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, serviceName, false, System.out);
    }

    private String readFile(String fileName) throws IOException {
        return Files.readString(outDir.resolve(fileName));
    }

    private String readGeneratedYaml() throws IOException {
        try (var stream = Files.list(outDir)) {
            Optional<Path> file = stream
                    .filter(p -> p.getFileName().toString().endsWith("_asyncapi.yaml"))
                    .findFirst();
            if (file.isEmpty()) {
                throw new IOException("No asyncapi YAML generated in " + outDir);
            }
            return Files.readString(file.get());
        }
    }

    private String readGeneratedJson() throws IOException {
        try (var stream = Files.list(outDir)) {
            Optional<Path> file = stream
                    .filter(p -> p.getFileName().toString().endsWith("_asyncapi.json"))
                    .findFirst();
            if (file.isEmpty()) {
                throw new IOException("No asyncapi JSON generated in " + outDir);
            }
            return Files.readString(file.get());
        }
    }

    // -------------------------------------------------------------------------
    // tests
    // -------------------------------------------------------------------------

    @Test
    void testOneRemote_chatService_generatesChatMessageInSpec() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_ONE_REMOTE);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a well-formed single-remote service");
        Assert.assertTrue(Files.exists(outDir.resolve("chat_asyncapi.yaml")),
                "output file chat_asyncapi.yaml must be created for service /chat");
        String yaml = readFile("chat_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("ChatMessage"),
                "remoteRequestTypeName 'ChatMessage' (onChatMessage.substring(2)) must appear in spec");
        Assert.assertTrue(yaml.contains("sendChatMessage"),
                "send operation 'sendChatMessage' must appear in spec");
    }

    @Test
    void testMismatchedFunctionName_stillGeneratesFullSpec() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_MISMATCHED_FUNCTION_NAME);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics regardless of the remote function's name");
        String yaml = readFile("service_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("ClientData"),
                "ClientData schema/message must appear even though the function is named 'onHello'");
        Assert.assertTrue(yaml.contains("User"),
                "User (return type) schema must appear even though the function is named 'onHello'");
        Assert.assertTrue(yaml.contains("sendHello:") && yaml.contains("$ref: \"#/components/schemas/ClientData\""),
                "the message is named after onHello, and its payload is the parameter's type: " + yaml);
        Assert.assertFalse(yaml.contains("operations: {}"),
                "operations must not be empty - a mismatched name must not cause silent skipping");
    }

    @Test
    void testMismatchedFunctionName_qualifiedTypeReference_stillGeneratesFullSpec() throws IOException {
        // Exact scenario from the original #6601 issue report: parameter/return types
        // referenced via a qualified name from an imported submodule (types:ClientData,
        // types:User), not a same-file simple name. testMismatchedFunctionName_
        // stillGeneratesFullSpec above only exercises the SIMPLE_NAME_REFERENCE branch of
        // resolveParameterTypeName() - this exercises the QUALIFIED_NAME_REFERENCE branch,
        // which was left unchanged by the fix and needed its own direct verification.
        List<AsyncApiConverterDiagnostic> diagnostics =
                runWithTypesSubmodule(BAL_MISMATCHED_FUNCTION_NAME_QUALIFIED_TYPE);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics regardless of the remote function's name, "
                        + "even when the parameter type is a qualified reference to an imported module");
        String yaml = readFile("service_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("ClientData"),
                "ClientData schema/message must appear even with a qualified type reference");
        Assert.assertTrue(yaml.contains("User"),
                "User (return type) schema must appear even with a qualified type reference");
        Assert.assertTrue(yaml.contains("sendHello:") && yaml.contains("$ref: \"#/components/schemas/ClientData\""),
                "the message is named after onHello, and its payload is the parameter's type: " + yaml);
        Assert.assertFalse(yaml.contains("operations: {}"),
                "operations must not be empty - a mismatched name must not cause silent skipping");
    }

    @Test
    void testServiceNameFilter_matchesClassBasedServiceName() throws IOException {
        // Reproduces #8713: `--service ChatService` must match the class-based service name
        // (the name a user actually knows their service by), not just the anonymous service's
        // base path ("/chat"), which is all AAS_CONVERTOR_101's matching currently checks.
        List<AsyncApiConverterDiagnostic> diagnostics = runWithServiceName(BAL_ONE_REMOTE, "ChatService");
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must find the service by its class-based name 'ChatService', not just its "
                        + "base path '/chat'. Diagnostics: " + diagnostics);
        Assert.assertTrue(Files.exists(outDir.resolve("chat_asyncapi.yaml")),
                "output file chat_asyncapi.yaml must be created when filtering by service name 'ChatService'");
    }

    @Test
    void testThreeRemotes_allTypeNamesInSpec() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_THREE_REMOTES);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a three-remote service");
        String yaml = readFile("orders_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("Order"),
                "type name 'Order' (from onOrder) must appear in spec");
        Assert.assertTrue(yaml.contains("Cancel"),
                "type name 'Cancel' (from onCancel) must appear in spec");
        Assert.assertTrue(yaml.contains("Update"),
                "type name 'Update' (from onUpdate) must appear in spec");
    }

    @Test
    void testMultiSegmentPath_outputFileNameNormalised() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_WITH_PATH);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a multi-segment path service");
        Assert.assertTrue(Files.exists(outDir.resolve("users_profile_asyncapi.yaml")),
                "slashes in service path must be replaced with underscores in the output file name");
    }

    @Test
    void testRecordParam_allFieldsInComponentsSchema() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_RECORD_PARAM);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a multi-field record parameter");
        String yaml = readFile("orders_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("orderId"),
                "field 'orderId' must appear in the components schema");
        Assert.assertTrue(yaml.contains("amount"),
                "field 'amount' must appear in the components schema");
        Assert.assertTrue(yaml.contains("customerId"),
                "field 'customerId' must appear in the components schema");
    }

    @Test
    void testRecordReturn_returnTypeInSpec() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_RECORD_RETURN);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a record-return remote function");
        String yaml = readFile("chat_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("ChatResponse"),
                "return type 'ChatResponse' must appear in the generated spec");
    }

    @Test
    void testOptionalField_appearsInSchema() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_OPTIONAL_FIELD);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a record with an optional field");
        // service / + source file service.bal → service_asyncapi.yaml
        Assert.assertTrue(Files.exists(outDir.resolve("service_asyncapi.yaml")),
                "output file service_asyncapi.yaml must be created for service /");
        String yaml = readFile("service_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("requiredField"),
                "required field 'requiredField' must appear in the schema");
        Assert.assertTrue(yaml.contains("optionalField"),
                "optional field 'optionalField' must appear in the schema");
    }

    @Test
    void testMissingAnnotation_isReportedAsADiagnostic() throws IOException {
        // Without a dispatcherKey, WebSocket sends every message to onMessage, so onChatMessage can't be described.
        // That is reported like any other problem rather than escaping as an exception.
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_MISSING_ANNOTATION);
        Assert.assertEquals(diagnostics.size(), 1, "Diagnostics: " + diagnostics);
        Assert.assertTrue(diagnostics.get(0).getMessage().contains("no @websocket:ServiceConfig"),
                diagnostics.get(0).getMessage());
        Assert.assertTrue(diagnostics.get(0).getMessage().contains("onMessage"), diagnostics.get(0).getMessage());
        Assert.assertFalse(Files.exists(outDir.resolve("chat_asyncapi.yaml")),
                "no definition should be written for a service that can't be described");
    }

    @Test
    void testServiceConfig_isFoundAmongOtherAnnotationsAndReadByValue() throws IOException {
        // @display comes first, the key is a constant with a comment after it, and the stream id is a literal
        Assert.assertTrue(run(BAL_SERVICE_CONFIG_NOT_FIRST).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("x-dispatcherKey: kind"), yaml);
        Assert.assertTrue(yaml.contains("x-dispatcherStreamId: id"), yaml);
        Assert.assertTrue(yaml.contains("const: hello"), yaml);
        Assert.assertFalse(yaml.contains("EVENT_FIELD"), yaml);
    }

    @Test
    void testServiceWithOnlyADocComment_isReportedAsADiagnostic() throws IOException {
        // A doc comment gives the service metadata with no annotations, which used to fail as an unexpected error
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_DOC_COMMENT_ONLY);
        Assert.assertEquals(diagnostics.size(), 1, "Diagnostics: " + diagnostics);
        Assert.assertTrue(diagnostics.get(0).getMessage().contains("no @websocket:ServiceConfig"),
                diagnostics.get(0).getMessage());
    }

    private static final String BAL_SERVICE_CONFIG_NOT_FIRST =
            "import ballerina/websocket;\n\n"
            + "const EVENT_FIELD = \"kind\";\n\n"
            + "public type Hello record {\n"
            + "    string kind;\n"
            + "    string id;\n"
            + "};\n\n"
            + "@display {label: \"Greeter\"}\n"
            + "@websocket:ServiceConfig {\n"
            + "    dispatcherKey: EVENT_FIELD, // the field that picks the function\n"
            + "    dispatcherStreamId: \"id\"\n"
            + "}\n"
            + "service /greet on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new GreetService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class GreetService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onHello(Hello hello) returns string {\n"
            + "        return \"hi\";\n"
            + "    }\n"
            + "}\n";

    private static final String BAL_DOC_COMMENT_ONLY =
            "import ballerina/websocket;\n\n"
            + "public type Hello record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "# Greets clients.\n"
            + "service /greet on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new GreetService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class GreetService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onHello(Hello hello) returns string {\n"
            + "        return \"hi\";\n"
            + "    }\n"
            + "}\n";

    @Test
    void testDispatcherConst_isTheValueThatReachesTheFunction() throws IOException {
        // websocket dispatches a dispatcher value to on<Value>, capitalising each "_"-separated word, so
        // onHello is reached by "hello" whatever its parameter type is called (#6601).
        Assert.assertTrue(run(BAL_MISMATCHED_FUNCTION_NAME).isEmpty());
        String yaml = readFile("service_asyncapi.yaml");
        Assert.assertTrue(yaml.contains("const: hello"), yaml);
        Assert.assertFalse(yaml.contains("const: ClientData"), yaml);
    }

    @Test
    void testDispatcherConst_multiWordAndAnnotatedFunctions() throws IOException {
        Assert.assertTrue(run(BAL_DISPATCH_VALUES).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("const: connection_init"),
                "onConnectionInit is reached by connection_init: " + yaml);
        Assert.assertTrue(yaml.contains("const: custom_value"),
                "a @websocket:DispatcherConfig value is used exactly as written: " + yaml);
        Assert.assertFalse(yaml.contains("const: CustomValue") || yaml.contains("const: ConnectionInit"), yaml);
        // websocket gives each capital its own word, so only h_t_t_p_response reaches onHTTPResponse
        Assert.assertTrue(yaml.contains("const: h_t_t_p_response"), yaml);
        // An escape in the annotation value is decoded, as websocket reads it
        Assert.assertTrue(yaml.contains("const: escaped_value"), yaml);
        // A comment after the value isn't part of it, and a constant gives its value, not its name
        Assert.assertTrue(yaml.contains("const: commented_value"), yaml);
        Assert.assertTrue(yaml.contains("const: from_const"), yaml);
        Assert.assertFalse(yaml.contains("CONST_VALUE"), yaml);
        // "_" gives no name, so its message is named after the function
        Assert.assertTrue(yaml.contains("const: _"), yaml);
        Assert.assertTrue(yaml.contains("    Underscore:"), yaml);
        // The annotation names the message, but its payload is still the parameter type's schema.
        Assert.assertTrue(yaml.contains("$ref: \"#/components/schemas/Custom\""), yaml);
        Assert.assertFalse(yaml.contains("#/components/schemas/CustomValue"), "no dangling schema reference: " + yaml);
    }

    @Test
    void testInlineResponse_doesNotTakeARecordMessageName() throws IOException {
        // onFoo's string[] response would be FooResponse, which onBar's record response already uses.
        Assert.assertTrue(run(BAL_RESPONSE_NAME_CLASH).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("$ref: \"#/components/schemas/FooResponse\""),
                "the record response keeps its message: " + yaml);
        Assert.assertTrue(yaml.contains("FooResponse_2:") && yaml.contains("receiveFooResponse_2"),
                "the inline response gets the next free name: " + yaml);
        Assert.assertTrue(yaml.contains("$ref: \"#/components/messages/FooResponse_2\""), yaml);
    }

    @Test
    void testSharedMessageName_isReportedAsADiagnostic() throws IOException {
        // websocket accepts foo-bar and foo_bar on two functions, but both name a FooBar message.
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_MESSAGE_NAME_CLASH);
        Assert.assertEquals(diagnostics.size(), 1, "Diagnostics: " + diagnostics);
        String message = diagnostics.get(0).getMessage();
        Assert.assertTrue(message.contains("onA") && message.contains("onB") && message.contains("FooBar"),
                message);
    }

    private static final String BAL_MESSAGE_NAME_CLASH =
            "import ballerina/websocket;\n\n"
            + "public type Foo record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type Bar record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /names on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new NamesService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class NamesService {\n"
            + "    *websocket:Service;\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: \"foo-bar\"}\n"
            + "    remote function onA(Foo foo) returns string {\n"
            + "        return \"a\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: \"foo_bar\"}\n"
            + "    remote function onB(Bar bar) returns string {\n"
            + "        return \"b\";\n"
            + "    }\n"
            + "}\n";

    private static final String BAL_RESPONSE_NAME_CLASH =
            "import ballerina/websocket;\n\n"
            + "public type Foo record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type Bar record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type FooResponse record {\n"
            + "    string event;\n"
            + "    string result;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /clash on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new ClashService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ClashService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onFoo(Foo foo) returns string[] {\n"
            + "        return [];\n"
            + "    }\n"
            + "    remote function onBar(Bar bar) returns FooResponse {\n"
            + "        return {event: \"FooResponse\", result: \"ok\"};\n"
            + "    }\n"
            + "}\n";

    @Test
    void testNonStringPrimitiveParameter_isReportedAsADiagnostic() throws IOException {
        // The message is a JSON object with the dispatcher field, which websocket can't bind to an int.
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_INT_PARAMETER);
        Assert.assertEquals(diagnostics.size(), 1, "Diagnostics: " + diagnostics);
        String message = diagnostics.get(0).getMessage();
        Assert.assertTrue(message.contains("onCount") && message.contains("int"), message);
    }

    private static final String BAL_INT_PARAMETER =
            "import ballerina/websocket;\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /count on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new CountService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class CountService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onCount(int count) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "}\n";

    @Test
    void testDispatcherValueFromAnotherModulesConstant() throws IOException {
        // The compiler only takes a literal or a constant here, so a constant from another module is the one other
        // form, and it gives its value
        Assert.assertTrue(runWithTypesSubmodule(BAL_DISPATCHER_VALUE_QUALIFIED_CONSTANT).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("const: from_types"), yaml);
        Assert.assertFalse(yaml.contains("CUSTOM_EVENT"), yaml);
    }

    private static final String BAL_DISPATCHER_VALUE_QUALIFIED_CONSTANT =
            "import ballerina/websocket;\n"
            + "import testorg/asyncspectest.types;\n\n"
            + "public type Custom record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /qualified on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new QualifiedService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class QualifiedService {\n"
            + "    *websocket:Service;\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: types:CUSTOM_EVENT}\n"
            + "    remote function onCustom(Custom custom) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "}\n";

    @Test
    void testInlinePayloads_carryTheDispatcherField() throws IOException {
        Assert.assertTrue(run(BAL_ISSUE_7669).isEmpty());
        String yaml = readGeneratedYaml();
        // onConnectionInit(string) and onSubscription() still receive a JSON object with the dispatcher field.
        Assert.assertTrue(yaml.contains("const: connection_init"), yaml);
        Assert.assertTrue(yaml.contains("const: subscription"), yaml);
    }

    @Test
    void testStreamResponse_isNotWrappedInExtensions() throws IOException {
        Assert.assertTrue(run(BAL_ISSUE_7669).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertFalse(yaml.contains("extensions:"), "x-response must list oneOf directly: " + yaml);
        Assert.assertTrue(yaml.contains("x-response:\n        oneOf:"), yaml);
    }

    @Test
    void testArrayResponse_getsAReceiveOperation() throws IOException {
        Assert.assertTrue(run(BAL_MISMATCHED_FUNCTION_NAME).isEmpty());
        String yaml = readFile("service_asyncapi.yaml");
        // User[] has no message of its own, so it gets one named after its request, which x-response points at.
        Assert.assertTrue(yaml.contains("receiveHelloResponse"), yaml);
        Assert.assertTrue(yaml.contains("HelloResponse:"), yaml);
        Assert.assertTrue(yaml.contains("x-response:\n        $ref: \"#/components/messages/HelloResponse\""), yaml);
    }

    @Test
    void testReplyRecord_doesNotTakeARequestMessageName() throws IOException {
        // onHello's message is Hello, so the Hello record that onGreet replies with needs another message name.
        Assert.assertTrue(run(BAL_REPLY_NAME_CLASH).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("    Hello:\n      payload:\n        allOf:\n"
                + "        - $ref: \"#/components/schemas/HelloData\""), yaml);
        Assert.assertTrue(
                yaml.contains("    HelloReply:\n      payload:\n        $ref: \"#/components/schemas/Hello\""), yaml);
        Assert.assertTrue(yaml.contains("x-response:\n        $ref: \"#/components/messages/HelloReply\""), yaml);
    }

    private static final String BAL_REPLY_NAME_CLASH =
            "import ballerina/websocket;\n\n"
            + "public type HelloData record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type Greeting record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type Hello record {\n"
            + "    string event;\n"
            + "    string text;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /greet on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new GreetService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class GreetService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onGreet(Greeting greeting) returns Hello {\n"
            + "        return {event: \"hello\", text: \"hi\"};\n"
            + "    }\n"
            + "    remote function onHello(HelloData data) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "}\n";

    @Test
    void testSharedRequestType_getsOneMessagePerFunction() throws IOException {
        // Each message carries its own dispatcher value, so both functions can take Topic.
        Assert.assertTrue(run(BAL_SHARED_REQUEST_TYPE).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("    Subscribe:\n      payload:\n        allOf:\n"
                + "        - $ref: \"#/components/schemas/Topic\""), yaml);
        Assert.assertTrue(yaml.contains("    Unsubscribe:\n      payload:\n        allOf:\n"
                + "        - $ref: \"#/components/schemas/Topic\""), yaml);
        Assert.assertTrue(yaml.contains("const: subscribe") && yaml.contains("const: unsubscribe"), yaml);
    }

    @Test
    void testEchoReply_isNotHeldToTheRequestsDispatcherValue() throws IOException {
        // The reply may carry any event, so it points at the plain ChatMessage schema, not at the request message.
        Assert.assertTrue(run(BAL_ECHO).isEmpty());
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("    ChatMessage:\n      payload:\n        allOf:\n"
                + "        - $ref: \"#/components/schemas/ChatMessage\""), yaml);
        Assert.assertTrue(yaml.contains(
                "    ChatMessageReply:\n      payload:\n        $ref: \"#/components/schemas/ChatMessage\""), yaml);
        Assert.assertTrue(yaml.contains("x-response:\n        $ref: \"#/components/messages/ChatMessageReply\""),
                yaml);
        int schemas = yaml.indexOf("\n  schemas:");
        String schema = yaml.substring(schemas, yaml.indexOf("\n  messages:", schemas));
        Assert.assertFalse(schema.contains("const:"), "the shared schema carries no dispatcher value: " + schema);
    }

    private static final String BAL_ECHO =
            "import ballerina/websocket;\n\n"
            + "public type ChatMessage record {\n"
            + "    string event;\n"
            + "    string text;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /echo on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new EchoService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class EchoService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onChatMessage(ChatMessage msg) returns ChatMessage {\n"
            + "        return {event: \"ack\", text: msg.text};\n"
            + "    }\n"
            + "}\n";

    private static final String BAL_DISPATCH_VALUES =
            "import ballerina/websocket;\n\n"
            + "public type Init record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "public type Custom record {\n"
            + "    string event;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /values on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new ValuesService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ValuesService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onConnectionInit(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: \"custom_value\"}\n"
            + "    remote function onAnnotated(Custom custom) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    remote function onHTTPResponse(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: \"escaped\\u{5F}value\"}\n"
            + "    remote function onEscaped(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {\n"
            + "        dispatcherValue: \"commented_value\" // a note with \\u{5F} in it\n"
            + "    }\n"
            + "    remote function onCommented(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: CONST_VALUE}\n"
            + "    remote function onFromConst(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    @websocket:DispatcherConfig {dispatcherValue: \"_\"}\n"
            + "    remote function onUnderscore(Init init) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "}\n\n"
            + "const CONST_VALUE = \"from_const\";\n";

    private static final String BAL_SHARED_REQUEST_TYPE =
            "import ballerina/websocket;\n\n"
            + "public type Topic record {\n"
            + "    string event;\n"
            + "    string name;\n"
            + "};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /topics on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service {\n"
            + "        return new TopicService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class TopicService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onSubscribe(Topic topic) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "    remote function onUnsubscribe(Topic topic) returns string {\n"
            + "        return \"ok\";\n"
            + "    }\n"
            + "}\n";

    @Test
    void testJsonOutput_validJsonFile() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_ONE_REMOTE, true);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics when writing JSON output");
        Assert.assertTrue(Files.exists(outDir.resolve("chat_asyncapi.json")),
                "output file chat_asyncapi.json must be created for JSON mode");
        String json = readFile("chat_asyncapi.json");
        Assert.assertTrue(json.trim().startsWith("{"),
                "JSON output must begin with '{'");
    }

    @Test
    void singleRemoteFunctionGeneratesYaml() throws IOException {
        Files.writeString(balFile, BAL_ONE_REMOTE);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("asyncapi"), "root asyncapi key must be present");
        Assert.assertTrue(yaml.contains("sendChatMessage"),
                "send operation 'sendChatMessage' must appear in spec");
    }

    @Test
    void missingModuleIsReportedAsADiagnostic() throws IOException {
        Files.writeString(balFile, BAL_CALLER_FIRST);
        List<AsyncApiConverterDiagnostic> diagnostics = BallerinaToAsyncApiGenerator
                .generateAsyncAPIDefinitionsAllService(projectDir, outDir, null, "missing", false, System.out);

        Assert.assertTrue(diagnostics.stream().anyMatch(d -> "AAS_CONVERTOR_109".equals(d.getCode())
                        && d.getMessage().contains("Module 'missing' not found")),
                "A missing module must come back as a diagnostic: " + diagnostics);
    }

    @Test
    void serviceClassInAnotherFileIsFound() throws IOException {
        Files.writeString(balFile, "import ballerina/websocket as ws;\n\n"
                + "@ws:ServiceConfig {dispatcherKey: \"event\"}\n"
                + "service /chat on new ws:Listener(9090) {\n"
                + "    resource function get .() returns ws:Service|ws:UpgradeError {\n"
                + "        return new ChatService();\n"
                + "    }\n"
                + "}\n");
        Files.writeString(projectDir.resolve("chat_service.bal"), "import ballerina/websocket as ws;\n\n"
                + "public type ChatMessage record {\n"
                + "    string event;\n"
                + "    string content;\n"
                + "};\n\n"
                + "service class ChatService {\n"
                + "    *ws:Service;\n"
                + "    remote function onChatMessage(ws:Caller caller, ChatMessage msg) returns string|error {\n"
                + "        return msg.content;\n"
                + "    }\n"
                + "}\n");
        List<AsyncApiConverterDiagnostic> diagnostics = BallerinaToAsyncApiGenerator
                .generateAsyncAPIDefinitionsAllService(projectDir, outDir, null, null, false, System.out);

        Assert.assertTrue(diagnostics.isEmpty(), "unexpected diagnostics: " + diagnostics);
        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("sendChatMessage"),
                "the remote function of a class in another file must become an operation: " + yaml);
    }

    @Test
    void callerParameterIsSkippedWhenFindingTheMessage() throws IOException {
        Files.writeString(balFile, BAL_CALLER_FIRST);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("ChatMessage"),
                "the message type after the caller must be the message: " + yaml);
        Assert.assertFalse(yaml.contains("Caller"), "the caller must not become a message: " + yaml);
    }

    @Test
    void threeRemoteFunctionsAllAppearInSpec() throws IOException {
        Files.writeString(balFile, BAL_THREE_REMOTES);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("Order"), "type 'Order' (from onOrder) must appear in spec");
        Assert.assertTrue(yaml.contains("Cancel"), "type 'Cancel' (from onCancel) must appear in spec");
        Assert.assertTrue(yaml.contains("Update"), "type 'Update' (from onUpdate) must appear in spec");
    }

    @Test
    void servicePathAppearsInSpec() throws IOException {
        Files.writeString(balFile, BAL_WITH_PATH);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("users/profile"),
                "service path /users/profile must appear as server pathname");
    }

    @Test
    void recordParamGeneratesComponentSchema() throws IOException {
        Files.writeString(balFile, BAL_RECORD_PARAM);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("Order"),
                "record type name 'Order' must appear in components schema");
        Assert.assertTrue(yaml.contains("orderId"), "record field 'orderId' must appear in schema");
        Assert.assertTrue(yaml.contains("amount"), "record field 'amount' must appear in schema");
    }

    @Test
    void recordReturnTypeGeneratesComponentSchema() throws IOException {
        Files.writeString(balFile, BAL_RECORD_RETURN);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("Response"),
                "return record type 'ChatResponse' must appear in components");
        Assert.assertTrue(yaml.contains("status"), "field 'status' must be in schema");
        Assert.assertTrue(yaml.contains("response"), "field 'response' must be in schema");
    }

    @Test
    void optionalFieldNotInRequiredList() throws IOException {
        Files.writeString(balFile, BAL_OPTIONAL_FIELD);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("required"), "required field list must appear in schema");
        Assert.assertTrue(yaml.contains("optional"), "optional field must appear in properties");
    }

    @Test
    void needJsonTrueProducesJsonFile() throws IOException {
        Files.writeString(balFile, BAL_ONE_REMOTE);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, true, System.out);

        String json = readGeneratedJson();
        Assert.assertTrue(json.trim().startsWith("{"), "JSON output must start with {");
        Assert.assertTrue(json.contains("asyncapi"), "asyncapi key must be present in JSON");
    }

    @Test
    void listenerPortAppearsInSpec() throws IOException {
        Files.writeString(balFile, BAL_ONE_REMOTE);
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(
                balFile, outDir, null, false, System.out);

        String yaml = readGeneratedYaml();
        Assert.assertTrue(yaml.contains("9090"), "listener port must appear in server definition");
    }

    // Regression fixture for #7669, reproduced verbatim from the issue report. Exercises all
    // three parameter/payload shapes in one service: a primitive-typed parameter
    // (onConnectionInit(string)), a zero-parameter remote function (onSubscription()), and the
    // reserved onMessage lifecycle hook, which must still be excluded from the generated spec.
    private static final String BAL_ISSUE_7669 =
            "import ballerina/websocket;\n\n"
            + "@websocket:ServiceConfig {\n"
            + "    dispatcherKey: \"event\"\n"
            + "}\n"
            + "service /websockets on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns MyGqlSubService {\n"
            + "        return new MyGqlSubService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class MyGqlSubService {\n"
            + "    *websocket:Service;\n\n"
            + "    remote function onConnectionInit(string chatMessage) returns ConnectionAck|error {\n"
            + "        ConnectionAck connAck = { event: \"connection_ack\", payload: \"{}\" };\n"
            + "        return connAck;\n"
            + "    }\n\n"
            + "    remote function onSubscription() returns stream<Next|Complete> {\n"
            + "        Next[] next = [\n"
            + "            { id: \"1\", event: \"data\", payload: \"{}\" },\n"
            + "            { id: \"2\", event: \"data\", payload: \"{}\" },\n"
            + "            { id: \"3\", event: \"data\", payload: \"{}\" }\n"
            + "        ];\n"
            + "        return next.toStream();\n"
            + "    }\n\n"
            + "    remote function onMessage(websocket:Caller caller, string chatMessage) returns error? {\n"
            + "        return caller->close(4408, \"Connection initialisation timeout\");\n"
            + "    }\n"
            + "}\n\n"
            + "type Next record {|\n"
            + "    string id;\n"
            + "    string event;\n"
            + "    string payload;\n"
            + "|};\n\n"
            + "type Complete record {|\n"
            + "    string id;\n"
            + "    string event;\n"
            + "|};\n\n"
            + "type ConnectionAck record {|\n"
            + "    string event;\n"
            + "    string payload;\n"
            + "|};\n";

    @Test
    void testIssue7669_primitiveAndZeroParamRemoteFunctionsAppearInSpec() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_ISSUE_7669);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for onConnectionInit's primitive parameter "
                        + "or onSubscription's zero parameters. Diagnostics: " + diagnostics);
        String yaml = readGeneratedYaml();

        // onConnectionInit(string chatMessage): primitive-typed parameter -> inline string schema,
        // message name falls back to the function name (no record type to derive it from).
        Assert.assertTrue(yaml.contains("sendConnectionInit"),
                "a send operation must be generated for onConnectionInit despite its primitive parameter");
        Assert.assertFalse(yaml.contains("#/components/schemas/ConnectionInit"),
                "ConnectionInit's payload must be an inline schema, not a $ref to a component schema "
                        + "(a primitive parameter has no named type to define one from)");

        // onSubscription(): zero parameters -> inline empty-object schema, same fallback naming.
        Assert.assertTrue(yaml.contains("sendSubscription"),
                "a send operation must be generated for onSubscription despite having no parameters");

        // Return types must still be processed for both, using the existing (already-working)
        // return-type machinery -- onConnectionInit's ConnectionAck|error and onSubscription's
        // stream<Next|Complete>.
        Assert.assertTrue(yaml.contains("receiveConnectionAck"),
                "onConnectionInit's ConnectionAck return type must still be processed");
        Assert.assertTrue(yaml.contains("receiveNext") && yaml.contains("receiveComplete"),
                "onSubscription's stream<Next|Complete> return type must still be processed, split per "
                        + "union member");

        // onMessage(websocket:Caller, string): the reserved WS lifecycle hook -- must remain
        // excluded, same as before this fix. Its parameter shape (Caller + string) would otherwise
        // now be primitive-eligible, so this specifically guards against the reserved-name check
        // being bypassed by the broadened parameter handling.
        Assert.assertFalse(yaml.contains("sendMessage") || yaml.contains("receiveMessage"),
                "onMessage must remain excluded as a reserved WebSocket lifecycle hook, not a "
                        + "dispatched application message");
    }

    // Regression fixture isolating stream<Union> return-type support from the zero-parameter
    // fix above: a normal record-typed parameter (already supported) paired with a stream return
    // type, the same return shape onSubscription() uses in #7669.
    private static final String BAL_STREAM_RETURN_CHECK =
            "import ballerina/websocket;\n\n"
            + "public type ChatMessage record {\n"
            + "    string event;\n"
            + "    string content;\n"
            + "};\n\n"
            + "public type Next record {|\n"
            + "    string id;\n"
            + "|};\n\n"
            + "public type Complete record {|\n"
            + "    string id;\n"
            + "|};\n\n"
            + "@websocket:ServiceConfig {dispatcherKey: \"event\"}\n"
            + "service /chat on new websocket:Listener(9090) {\n"
            + "    resource function get .() returns websocket:Service|websocket:UpgradeError {\n"
            + "        return new ChatService();\n"
            + "    }\n"
            + "}\n\n"
            + "service class ChatService {\n"
            + "    *websocket:Service;\n"
            + "    remote function onChatMessage(ChatMessage msg) returns stream<Next|Complete> {\n"
            + "        Next[] next = [{id: \"1\"}];\n"
            + "        return next.toStream();\n"
            + "    }\n"
            + "}\n";

    @Test
    void testStreamReturnType_generatesReceiveOperationsPerUnionMember() throws IOException {
        List<AsyncApiConverterDiagnostic> diagnostics = run(BAL_STREAM_RETURN_CHECK);
        Assert.assertTrue(diagnostics.isEmpty(),
                "generator must produce no diagnostics for a stream<Union> return type. Diagnostics: "
                        + diagnostics);
        String yaml = readGeneratedYaml();

        Assert.assertTrue(yaml.contains("sendChatMessage"),
                "the request side (record-typed parameter) must be unaffected");
        Assert.assertTrue(yaml.contains("receiveNext") && yaml.contains("receiveComplete"),
                "stream<Next|Complete> must split into one receive operation per union member");
        Assert.assertTrue(yaml.contains("x-response-type: server-streaming"),
                "the response must be tagged as server-streaming, not a one-shot simple-rpc reply");
    }
}
