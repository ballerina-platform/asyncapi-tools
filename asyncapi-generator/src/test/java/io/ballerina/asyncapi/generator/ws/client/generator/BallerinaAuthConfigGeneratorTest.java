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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link BallerinaAuthConfigGenerator} verifying auth-type flag setting,
 * API-key name list population, parameter string generation, and type definition output.
 */
public class BallerinaAuthConfigGeneratorTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{}}}";

    private static final String SPEC_WITH_API_KEY_SCHEME =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{},"
                    + "\"components\":{\"securitySchemes\":{"
                    + "\"myKey\":{\"type\":\"httpApiKey\",\"name\":\"X-Api-Key\",\"in\":\"header\"}}}}";

    @Test
    void testSetAuthTypes_setsHttpApiKeyFlag_whenApiKeyScheme() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        Assert.assertTrue(gen.isHttpApiKey(),
                "setAuthTypes with 'httpApiKey' type should set the httpApiKey flag to true");
    }

    @Test
    void testSetAuthTypes_setsHeaderParamFlag_whenHeaderParamScheme() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        Assert.assertTrue(gen.getHeaderApiKeyNameList().containsKey("myKey"),
                "setAuthTypes with in='header' should populate headerApiKeyNameList with the scheme key");
    }

    @Test
    void testSetAuthTypes_throwsGeneratorException_whenUnknownSchemeType() {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "apiKey", null, null, null, null, null, null, null, null);
        try {
            gen.setAuthTypes(Map.of("badKey", scheme));
            Assert.fail("setAuthTypes should throw GeneratorException for unsupported 'apiKey' scheme type");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("apiKey"),
                    "GeneratorException message should mention the unsupported 'apiKey' scheme type");
        }
    }

    @Test
    void testGenerateApiKeysConfig_containsApiKeysConfigRecord() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        TypeDefinitionNode node = gen.generateApiKeysConfig();
        Assert.assertTrue(node.toString().contains("ApiKeysConfig"),
                "generateApiKeysConfig() should produce a type definition named 'ApiKeysConfig'");
    }

    @Test
    void testGetApiKeyMapClassVariable_nonNull_whenApiKeyAuth() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        ObjectFieldNode field = gen.getApiKeyMapClassVariable();
        Assert.assertNotNull(field,
                "getApiKeyMapClassVariable() should return a non-null node when API-key auth is active");
    }

    @Test
    void testGetApiKeyMapClassVariable_null_whenNoApiKeyAuth() {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        ObjectFieldNode field = gen.getApiKeyMapClassVariable();
        Assert.assertNull(field,
                "getApiKeyMapClassVariable() should return null when no API-key auth has been configured");
    }

    @Test
    void testGetAuthRelatedTypeDefinitionNodes_countMatchesSchemeCount()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_API_KEY_SCHEME);
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        gen.addAuthRelatedRecords(spec);
        Assert.assertEquals(gen.getAuthRelatedTypeDefinitionNodes().size(), 1,
                "addAuthRelatedRecords() should produce one ApiKeysConfig type definition for one httpApiKey scheme");
    }

    @Test
    void testBuildInitParamString_containsServiceUrl() {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        String params = gen.buildInitParamString("wss://example.com");
        Assert.assertTrue(params.contains("wss://example.com"),
                "buildInitParamString should embed the provided serviceUrl as the default parameter value");
    }

    @Test
    void testBuildInitParamString_containsAuthParamNames() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        String params = gen.buildInitParamString("wss://example.com");
        Assert.assertTrue(params.contains("ApiKeysConfig"),
                "buildInitParamString should include 'ApiKeysConfig' as param type when API-key auth is active");
        Assert.assertTrue(params.contains("apiKeyConfig"),
                "buildInitParamString should include 'apiKeyConfig' as param name when API-key auth is active");
    }

    @Test
    void testGetHeaderApiKeyNameList_keyedBySchemeNameWithHeaderValue() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "X-Api-Key", "header", null, null, null, null, null);
        gen.setAuthTypes(Map.of("myKey", scheme));
        Map<String, String> headerMap = gen.getHeaderApiKeyNameList();
        Assert.assertTrue(headerMap.containsKey("myKey"),
                "getHeaderApiKeyNameList should contain an entry keyed by the scheme name 'myKey'");
        Assert.assertEquals(headerMap.get("myKey"), "X-Api-Key",
                "getHeaderApiKeyNameList should map scheme key to the API-key header name 'X-Api-Key'");
    }

    @Test
    void testGetQueryApiKeyNameList_keyedBySchemeNameWithQueryValue() throws GeneratorException {
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        AsyncApiSecurityScheme scheme = new AsyncApiSecurityScheme(
                "httpApiKey", null, "token", "query", null, null, null, null, null);
        gen.setAuthTypes(Map.of("queryKey", scheme));
        Map<String, String> queryMap = gen.getQueryApiKeyNameList();
        Assert.assertTrue(queryMap.containsKey("queryKey"),
                "getQueryApiKeyNameList should contain an entry keyed by the scheme name 'queryKey'");
        Assert.assertEquals(queryMap.get("queryKey"), "token",
                "getQueryApiKeyNameList should map scheme key to the API-key query param name 'token'");
    }

    @Test
    void testSetFunctionParameters_appendsCorrectNodeCount()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        AsyncApiChannel channel = spec.getAsyncApiChannels().orElse(null).values().iterator().next();
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        List<Node> parameterList = new ArrayList<>();
        Token comma = AbstractNodeFactory.createToken(SyntaxKind.COMMA_TOKEN);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode headerSchema = mapper.createObjectNode();
        ObjectNode props = headerSchema.putObject("properties");
        props.putObject("Authorization").put("type", "string");
        gen.setFunctionParameters(channel, parameterList, comma, null, headerSchema);
        Assert.assertEquals(parameterList.size(), 2,
                "setFunctionParameters with one header property should append 2 nodes (param + comma)");
    }

    @Test
    void testAddAuthRelatedRecords_doesNotThrow_whenValidSchemes()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_API_KEY_SCHEME);
        BallerinaAuthConfigGenerator gen = new BallerinaAuthConfigGenerator();
        gen.addAuthRelatedRecords(spec);
        Assert.assertTrue(gen.isHttpApiKey(),
                "addAuthRelatedRecords should set the httpApiKey flag for an httpApiKey security scheme in the spec");
    }
}
