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
package io.ballerina.asyncapi.core;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Integration tests for {@link AsyncApiParser#parseFromJsonString(String)}.
 * Covers all 8 supported AsyncAPI versions (2.0.0 – 2.6.0 and 3.0.0),
 * verifying that the version-agnostic {@link AsyncApiSpec} model is correctly
 * populated for HTTP-webhook and WebSocket channels.
 */
public class AsyncApiParserIntegrationTest {

    private static final String SPEC_DIR = "specs/";

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String loadSpec(String resourcePath) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Test resource not found: " + resourcePath);
        }
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Provides spec file names and expected version strings for all 8 supported versions.
     */
    @DataProvider(name = "allVersionArgs")
    Object[][] allVersionArgs() {
        return new Object[][] {
            {SPEC_DIR + "asyncapi-2.0.0.json", "2.0.0"},
            {SPEC_DIR + "asyncapi-2.1.0.json", "2.1.0"},
            {SPEC_DIR + "asyncapi-2.2.0.json", "2.2.0"},
            {SPEC_DIR + "asyncapi-2.3.0.json", "2.3.0"},
            {SPEC_DIR + "asyncapi-2.4.0.json", "2.4.0"},
            {SPEC_DIR + "asyncapi-2.5.0.json", "2.5.0"},
            {SPEC_DIR + "asyncapi-2.6.0.json", "2.6.0"},
            {SPEC_DIR + "asyncapi-3.0.0.json", "3.0.0"},
        };
    }

    @Test(dataProvider = "allVersionArgs")
    void version_shouldMatchSpecHeader(String specFile, String expectedVersion)
            throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(specFile));
        Assert.assertEquals(spec.getAsyncApiVersion().toString(), expectedVersion);
    }

    @Test(dataProvider = "allVersionArgs")
    void info_titleContactAndLicense_areMapped(String specFile, String ignored)
            throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(specFile));
        AsyncApiInfo info = spec.getAsyncApiInfo();
        Assert.assertNotNull(info);
        Assert.assertEquals(info.title(), "User Events API");
        Assert.assertEquals(info.version(), "1.0.0");
        Assert.assertNotNull(info.contact());
        Assert.assertEquals(info.contact().name(), "API Support");
        Assert.assertEquals(info.contact().email(), "support@example.com");
        Assert.assertNotNull(info.license());
        Assert.assertEquals(info.license().name(), "Apache 2.0");
    }

    @Test(dataProvider = "allVersionArgs")
    void servers_httpAndWs_areMapped(String specFile, String ignored)
            throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(specFile));
        Map<String, AsyncApiServer> servers = spec.getAsyncApiServers().orElseThrow();
        Assert.assertTrue(servers.containsKey("webhookServer"), "Expected key 'webhookServer' in servers map");
        AsyncApiServer webhookServer = servers.get("webhookServer");
        Assert.assertEquals(webhookServer.protocol(), "http");
        Assert.assertEquals(webhookServer.host(), "api.example.com");
        Assert.assertTrue(servers.containsKey("wsServer"), "Expected key 'wsServer' in servers map");
        AsyncApiServer wsServer = servers.get("wsServer");
        Assert.assertEquals(wsServer.protocol(), "ws");
        Assert.assertEquals(wsServer.host(), "api.example.com");
    }

    @Test
    void v200_publishOp_hasSendAction() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, AsyncApiOperation> ops = spec.getAsyncApiOperations().orElseThrow();
        Assert.assertTrue(ops.containsKey("SendUserCommand"), "Expected operation key 'SendUserCommand'");
        Assert.assertEquals(ops.get("SendUserCommand").action(), AsyncApiOperation.Action.SEND);
    }

    @Test
    void v200_subscribeOp_hasReceiveAction() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, AsyncApiOperation> ops = spec.getAsyncApiOperations().orElseThrow();
        Assert.assertTrue(ops.containsKey("ReceiveUserSignedUp"), "Expected operation key 'ReceiveUserSignedUp'");
        Assert.assertEquals(ops.get("ReceiveUserSignedUp").action(), AsyncApiOperation.Action.RECEIVE);
    }

    @Test
    void v200_operationChannelId_isPascalCaseOfChannelKey() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiOperation op = spec.getAsyncApiOperations().orElseThrow().get("ReceiveUserSignedUp");
        Assert.assertNotNull(op);
        Assert.assertEquals(op.channelId(), "UserSignedup");
    }

    @Test
    void v200_channelsMapKey_isPascalCase() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, AsyncApiChannel> channels = spec.getAsyncApiChannels().orElseThrow();
        Assert.assertTrue(channels.containsKey("UserSignedup"), "Expected channel key 'UserSignedup'");
        Assert.assertTrue(channels.containsKey("UserStream"), "Expected channel key 'UserStream'");
    }

    @Test
    void v200_httpChannel_address_equalsChannelKey() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiChannels().orElseThrow().get("UserSignedup").address(), "user/signedup");
    }

    @Test
    void v200_wsChannel_address_equalsChannelKey() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiChannels().orElseThrow().get("UserStream").address(), "user/stream");
    }

    @Test
    void v200_subscribeOp_messages_containsOneOfMembers() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, AsyncApiMessage> messages = spec.getAsyncApiOperations().orElseThrow().get("ReceiveUserSignedUp")
                                                .messages();
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.containsKey("UserSignedUp"), "Expected message key 'UserSignedUp'");
        Assert.assertTrue(messages.containsKey("UserUpdated"), "Expected message key 'UserUpdated'");
    }

    @Test
    void v200_httpChannelMessages_aggregatesAllOpsMessages() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, AsyncApiMessage> messages = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup").messages();
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.containsKey("UserSignedUp"), "Expected 'UserSignedUp' in channel messages");
        Assert.assertTrue(messages.containsKey("UserUpdated"), "Expected 'UserUpdated' in channel messages");
        Assert.assertTrue(messages.containsKey("UserCommand"), "Expected 'UserCommand' in channel messages");
    }

    @Test
    void v200_channelParameters_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        Map<String, ?> params = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup").parameters();
        Assert.assertNotNull(params);
        Assert.assertTrue(params.containsKey("userId"), "Expected parameter key 'userId'");
    }

    @Test
    void v200_httpChannelBindings_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup");
        Assert.assertNotNull(channel.bindings(), "Expected non-null channel bindings");
        Assert.assertNotNull(channel.bindings().httpChannelBindings(), "Expected non-null HTTP channel bindings");
    }

    @Test
    void v200_wsChannelBindings_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElseThrow().get("UserStream");
        Assert.assertNotNull(channel.bindings(), "Expected non-null channel bindings");
        Assert.assertNotNull(channel.bindings().wsChannelBindings(), "Expected non-null WS channel bindings");
        Assert.assertEquals(channel.bindings().wsChannelBindings().method(), "GET");
    }

    @Test
    void v200_messageHttpBindings_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiMessage msg = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup").messages().
                                get("UserSignedUp");
        Assert.assertNotNull(msg, "Expected 'UserSignedUp' message in channel");
        Assert.assertNotNull(msg.bindings(), "Expected non-null message bindings");
        Assert.assertNotNull(msg.bindings().httpMessageBindings(), "Expected non-null HTTP message bindings");
    }

    @Test
    void v200_messagePayloadSchemaRef_isResolved() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiMessage msg = spec.getAsyncApiOperations().orElseThrow().get("ReceiveUserSignedUp").messages().
                                get("UserSignedUp");
        Assert.assertNotNull(msg, "Expected 'UserSignedUp' in operation messages");
        Assert.assertNotNull(msg.payload(), "Expected non-null payload");
        Assert.assertTrue(msg.payload() instanceof AsyncApiSchema, "Expected payload to be an AsyncApiSchema");
        Assert.assertEquals(((AsyncApiSchema) msg.payload()).type(), "object");
    }

    @Test
    void v200_componentSchemas_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.schemas());
        Assert.assertTrue(components.schemas().containsKey("UserPayload"), "Expected schema key 'UserPayload'");
        Assert.assertEquals(components.schemas().get("UserPayload").type(), "object");
    }

    @Test
    void v200_missingOptionalFields_returnNull() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup");
        Assert.assertNull(channel.title(), "v2.x channels have no title field");
        Assert.assertNull(channel.summary(), "v2.x channels have no summary field");
        Assert.assertNull(channel.tags(), "v2.x channels have no tags field");
    }

    @Test
    void v210_spec_parsesSuccessfully() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.1.0.json"));
        Assert.assertNotNull(spec);
        Assert.assertEquals(spec.getAsyncApiVersion().toString(), "2.1.0");
    }

    @Test
    void v220_channelServers_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.2.0.json"));
        AsyncApiChannel httpChannel = spec.getAsyncApiChannels().orElseThrow().get("UserSignedup");
        Assert.assertNotNull(httpChannel.servers(), "Expected non-null servers list on HTTP channel");
        Assert.assertEquals(httpChannel.servers().size(), 1);
        Assert.assertEquals(httpChannel.servers().get(0).protocol(), "http");
        AsyncApiChannel wsChannel = spec.getAsyncApiChannels().orElseThrow().get("UserStream");
        Assert.assertNotNull(wsChannel.servers(), "Expected non-null servers list on WS channel");
        Assert.assertEquals(wsChannel.servers().size(), 1);
        Assert.assertEquals(wsChannel.servers().get(0).protocol(), "ws");
    }

    @Test
    void v230_componentsServers_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.3.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.servers(), "Expected non-null component servers map");
        Assert.assertTrue(components.servers().containsKey("sharedWebhookServer"),
                "Expected 'sharedWebhookServer' in component servers");
        Assert.assertEquals(components.servers().get("sharedWebhookServer").protocol(), "http");
    }

    @Test
    void v240_spec_parsesSuccessfully() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.4.0.json"));
        Assert.assertNotNull(spec);
        Assert.assertEquals(spec.getAsyncApiVersion().toString(), "2.4.0");
        Assert.assertFalse(spec.getAsyncApiOperations().orElseThrow().isEmpty(), "Expected at least one operation");
    }

    @Test
    void v250_serverTags_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.5.0.json"));
        AsyncApiServer webhookServer = spec.getAsyncApiServers().orElseThrow().get("webhookServer");
        Assert.assertNotNull(webhookServer.tags(), "Expected non-null tags on webhookServer");
        Assert.assertFalse(webhookServer.tags().isEmpty(), "Expected at least one server tag");
        Assert.assertEquals(webhookServer.tags().get(0).name(), "prod");
    }

    @Test
    void v300_operations_areTopLevelNotInChannels() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Map<String, AsyncApiOperation> ops = spec.getAsyncApiOperations().orElseThrow();
        Assert.assertNotNull(ops);
        Assert.assertFalse(ops.isEmpty(), "Expected top-level operations in v3.0");
        Assert.assertEquals(ops.size(), 3, "Expected 3 operations: receive, send, stream");
    }

    @Test
    void v300_channelsMapKey_isPreservedAsIs() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Map<String, AsyncApiChannel> channels = spec.getAsyncApiChannels().orElseThrow();
        Assert.assertTrue(channels.containsKey("userSignedUp"), "v3 channel key must not be PascalCased");
        Assert.assertTrue(channels.containsKey("userStream"), "v3 channel key must not be PascalCased");
    }

    @Test
    void v300_httpChannel_address_isExplicitField() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiChannels().orElseThrow().get("userSignedUp").address(), "/user/signed-up");
    }

    @Test
    void v300_wsChannel_address_isExplicitField() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiChannels().orElseThrow().get("userStream").address(), "/user/stream");
    }

    @Test
    void v300_operationChannelId_isPascalCaseOfChannelKey() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiOperation op = spec.getAsyncApiOperations().orElseThrow().get("receiveUserSignedUp");
        Assert.assertNotNull(op);
        Assert.assertEquals(op.channelId(), "UserSignedUp");
    }

    @Test
    void v300_receiveOp_hasReceiveAction() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiOperations().orElseThrow().get("receiveUserSignedUp").action(),
                AsyncApiOperation.Action.RECEIVE);
    }

    @Test
    void v300_sendOp_hasSendAction() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Assert.assertEquals(spec.getAsyncApiOperations().orElseThrow().get("sendUserCommand").action(),
                AsyncApiOperation.Action.SEND);
    }

    @Test
    void v300_httpChannelBindings_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElseThrow().get("userSignedUp");
        Assert.assertNotNull(channel.bindings(), "Expected non-null channel bindings");
        Assert.assertNotNull(channel.bindings().httpChannelBindings(), "Expected non-null HTTP channel bindings");
    }

    @Test
    void v300_wsChannelBindings_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElseThrow().get("userStream");
        Assert.assertNotNull(channel.bindings(), "Expected non-null channel bindings");
        Assert.assertNotNull(channel.bindings().wsChannelBindings(), "Expected non-null WS channel bindings");
    }

    @Test
    void v300_httpChannelMessages_areFromChannelMessagesMap() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Map<String, AsyncApiMessage> messages = spec.getAsyncApiChannels().orElseThrow().get("userSignedUp").messages();
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.containsKey("UserSignedUp"), "Expected 'UserSignedUp' in channel messages");
        Assert.assertTrue(messages.containsKey("UserCommand"), "Expected 'UserCommand' in channel messages");
    }

    @Test
    void v300_operationMessages_resolvedFromChannelRef() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        Map<String, AsyncApiMessage> messages = spec.getAsyncApiOperations().orElseThrow().get("receiveUserSignedUp")
                                                .messages();
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.containsKey("UserSignedUp"), "Expected 'UserSignedUp' in operation messages");
    }

    @Test
    void v300_componentSchemas_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.schemas());
        Assert.assertTrue(components.schemas().containsKey("UserPayload"), "Expected schema key 'UserPayload'");
    }

    @Test
    void v200_directSchemaRef_resolvedInMessagePayload() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.0.0.json"));
        AsyncApiMessage msg = spec.getAsyncApiOperations().orElseThrow().get("ReceiveUserSignedUp").messages().
                                get("UserSignedUp");
        Assert.assertNotNull(msg);
        Assert.assertTrue(msg.payload() instanceof AsyncApiSchema);
        AsyncApiSchema payload = (AsyncApiSchema) msg.payload();
        Assert.assertNotNull(payload.properties(), "Expected resolved schema to have properties");
        Assert.assertTrue(payload.properties().containsKey("userId"), "Expected property 'userId'");
        Assert.assertTrue(payload.properties().containsKey("email"), "Expected property 'email'");
    }

    @Test
    void v260_serverVariable_fieldsAreMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.6.0.json"));
        AsyncApiServer webhookServer = spec.getAsyncApiServers().orElseThrow().get("webhookServer");
        Assert.assertNotNull(webhookServer.variables(), "Expected non-null variables map on webhookServer");
        Assert.assertTrue(webhookServer.variables().containsKey("version"), "Expected variable key 'version'");
        AsyncApiServerVariable v = webhookServer.variables().get("version");
        Assert.assertEquals(v.defaultValue(), "v1");
        Assert.assertEquals(v.description(), "API version");
        Assert.assertNotNull(v.enumValues(), "Expected non-null enumValues");
        Assert.assertEquals(v.enumValues().size(), 2);
        Assert.assertTrue(v.enumValues().contains("v1"));
        Assert.assertTrue(v.enumValues().contains("v2"));
    }

    @Test
    void v260_serverVariable_examplesAreMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.6.0.json"));
        AsyncApiServerVariable v = spec.getAsyncApiServers().orElseThrow()
                .get("webhookServer").variables().get("version");
        Assert.assertNotNull(v.examples(), "Expected non-null examples on server variable");
        Assert.assertEquals(v.examples().get(0), "v1");
    }

    @Test
    void v260_componentServerVariables_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.6.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.serverVariables(), "Expected non-null component serverVariables");
        Assert.assertTrue(components.serverVariables().containsKey("sharedVersion"),
                "Expected 'sharedVersion' in component serverVariables");
        AsyncApiServerVariable sv = components.serverVariables().get("sharedVersion");
        Assert.assertEquals(sv.defaultValue(), "v1");
        Assert.assertEquals(sv.description(), "Shared API version variable");
        Assert.assertNotNull(sv.enumValues());
        Assert.assertEquals(sv.enumValues().size(), 3);
    }

    @Test
    void v300_serverVariable_fieldsAreMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiServer webhookServer = spec.getAsyncApiServers().orElseThrow().get("webhookServer");
        Assert.assertNotNull(webhookServer.variables(), "Expected non-null variables map on webhookServer");
        Assert.assertTrue(webhookServer.variables().containsKey("version"), "Expected variable key 'version'");
        AsyncApiServerVariable v = webhookServer.variables().get("version");
        Assert.assertEquals(v.defaultValue(), "v1");
        Assert.assertEquals(v.description(), "API version");
        Assert.assertNotNull(v.enumValues(), "Expected non-null enumValues");
        Assert.assertEquals(v.enumValues().size(), 2);
    }

    @Test
    void v300_serverVariable_examplesAreMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiServerVariable v = spec.getAsyncApiServers().orElseThrow()
                .get("webhookServer").variables().get("version");
        Assert.assertNotNull(v.examples(), "Expected non-null examples on server variable");
        Assert.assertEquals(v.examples().get(0), "v1");
    }

    @Test
    void v300_componentServerVariables_areMapped() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-3.0.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.serverVariables(), "Expected non-null component serverVariables");
        Assert.assertTrue(components.serverVariables().containsKey("sharedVersion"),
                "Expected 'sharedVersion' in component serverVariables");
        AsyncApiServerVariable sv = components.serverVariables().get("sharedVersion");
        Assert.assertEquals(sv.defaultValue(), "v1");
        Assert.assertEquals(sv.description(), "Shared API version variable");
        Assert.assertNotNull(sv.enumValues());
        Assert.assertEquals(sv.enumValues().size(), 3);
        Assert.assertNotNull(sv.examples());
        Assert.assertEquals(sv.examples().get(0), "v2");
    }

    @Test
    void v260_chainedSchemaRef_resolvesTransitively() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.6.0.json"));
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        Assert.assertNotNull(components.schemas());
        // UserRef chains to User; verify the terminal User schema is mapped with its properties
        Assert.assertTrue(components.schemas().containsKey("User"), "Expected terminal 'User' schema");
        AsyncApiSchema user = components.schemas().get("User");
        Assert.assertEquals(user.type(), "object");
        Assert.assertNotNull(user.properties(), "Expected 'User' schema to have properties");
        Assert.assertTrue(user.properties().containsKey("id"), "Expected property 'id' on User schema");
        Assert.assertTrue(user.properties().containsKey("name"), "Expected property 'name' on User schema");
        // Verify the wrapper schema referencing the chain is also present
        Assert.assertTrue(components.schemas().containsKey("UserEventWrapper"),
                "Expected 'UserEventWrapper' schema referencing the chain");
    }

    @Test
    void v260_cyclicSchemaRef_doesNotCauseStackOverflow() throws IOException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(loadSpec(SPEC_DIR + "asyncapi-2.6.0.json"));
        Assert.assertNotNull(spec, "Expected spec to parse without stack overflow from cyclic schema ref");
        Assert.assertNotNull(spec.getAsyncApiComponents().orElseThrow().schemas());
        Assert.assertTrue(spec.getAsyncApiComponents().orElseThrow().schemas().containsKey("TreeNode"),
                "Expected 'TreeNode' schema to be present");
    }

    @Test
    void v300_standardNullableKeyword_isCapturedOnSchema() throws AsyncApiParserException {
        // AsyncAPI 3.0's JSON Schema dialect has no typed 'nullable' keyword (Apicurio's schema
        // model doesn't expose one), so a real-world spec still using the OpenAPI-style
        // 'nullable: true' convention must be read from the schema's generic extra-properties
        // bucket rather than a typed accessor or the x-prefixed extensions map - see
        // SchemaMapper#mapNullable.
        String json = "{\"asyncapi\":\"3.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                + "\"components\":{\"schemas\":{\"Foo\":{\"type\":\"object\",\"properties\":{"
                + "\"bar\":{\"type\":\"string\",\"nullable\":true},"
                + "\"baz\":{\"type\":\"string\"}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        AsyncApiSchema foo = components.schemas().get("Foo");
        Assert.assertEquals(foo.properties().get("bar").nullable(), Boolean.TRUE,
                "A field marked 'nullable: true' should have nullable() == true");
        Assert.assertNull(foo.properties().get("baz").nullable(),
                "A field with no 'nullable' keyword should have nullable() == null, not false");
    }

    @Test
    void v300_refWithSiblingNullable_isCapturedOnStub() throws AsyncApiParserException {
        // A field that's a $ref PLUS a sibling 'nullable: true' - real-world shape, e.g. GitHub's
        // spec: assignee: {$ref: '#/components/schemas/User', nullable: true}. The property maps
        // to a minimal ref stub (only 'name' set) rather than going through the normal schema
        // mapping path, so the sibling 'nullable' has to be attached onto that stub explicitly,
        // mirroring how a sibling 'description' is already attached (see SchemaMapper#mapProperties).
        String json = "{\"asyncapi\":\"3.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                + "\"components\":{\"schemas\":{"
                + "\"User\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}},"
                + "\"PullRequestPayload\":{\"type\":\"object\",\"required\":[\"assignee\"],"
                + "\"properties\":{\"assignee\":{\"$ref\":\"#/components/schemas/User\",\"nullable\":true}}}"
                + "}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        AsyncApiComponent components = spec.getAsyncApiComponents().orElseThrow();
        AsyncApiSchema payload = components.schemas().get("PullRequestPayload");
        AsyncApiSchema assignee = payload.properties().get("assignee");
        Assert.assertEquals(assignee.name(), "User",
                "The $ref stub should still resolve to the referenced schema's name");
        Assert.assertEquals(assignee.nullable(), Boolean.TRUE,
                "The sibling 'nullable: true' next to the $ref must be attached onto the stub");
    }

    @Test
    void nullInput_throwsAsyncApiParserException() {
        Assert.assertThrows(AsyncApiParserException.class, () -> AsyncApiParser.parseFromJsonString(null));
    }

    @Test
    void blankInput_throwsAsyncApiParserException() {
        Assert.assertThrows(AsyncApiParserException.class, () -> AsyncApiParser.parseFromJsonString("   "));
    }

    @Test
    void invalidJson_throwsAsyncApiParserException() {
        Assert.assertThrows(AsyncApiParserException.class,
                () -> AsyncApiParser.parseFromJsonString("{ not valid json }"));
    }

    @Test
    void openApiDocument_throwsAsyncApiParserException() {
        String openApiJson = "{\"openapi\":\"3.0.0\","
                + "\"info\":{\"title\":\"Test\",\"version\":\"1.0.0\"},"
                + "\"paths\":{}}";
        Assert.assertThrows(AsyncApiParserException.class,
                () -> AsyncApiParser.parseFromJsonString(openApiJson));
    }
}
