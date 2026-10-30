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
package io.ballerina.asyncapi.generator.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public class WebhookDSLVerificationTest {

    private static final Path RES_DIR = Paths.get("src/test/resources/specs").toAbsolutePath();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Test
    public void testGithubWebhookVerification() throws Exception {
        String content = generateDispatcherService("github_verification.yaml");
        Assert.assertTrue(content.contains("crypto:hmacSha256"), "Should contain HMAC-SHA256 algorithm");
        Assert.assertTrue(
        content.contains("string `${check request.getTextPayload()}`"),
        "Should extract text payload");
        Assert.assertTrue(
        content.contains("string `sha256=${computedSignature}`"),
        "Should format expected header with sha256=");
    }

    @Test
    public void testGithubDispatchTestSignatureFormatAndStatusCode() throws Exception {
        // github_verification.yaml configures headerFormat: "sha256=$signature", matching the real
        // prefix GitHub always sends. The generated tests/dispatch_test.bal must build its outgoing
        // signature using that same prefix, and assert the status code the dispatcher actually
        // returns (STATUS_OK) - not a bare/wrong-status signature that would never pass real
        // verification.
        Path tempOutputDir = generateOutputDir("github_verification.yaml");
        Path dispatchTestFile = tempOutputDir.resolve("tests").resolve("dispatch_test.bal");
        Assert.assertTrue(Files.exists(dispatchTestFile), "tests/dispatch_test.bal was not generated!");
        String dispatchTestContent = Files.readString(dispatchTestFile);

        Assert.assertTrue(
                dispatchTestContent.contains("string `sha256=${computedSignature}`"),
                "Should splice the computed signature into the configured sha256= headerFormat prefix");
        Assert.assertTrue(
                dispatchTestContent.contains("http:STATUS_OK"),
                "Should assert the status code the dispatcher actually returns");
        Assert.assertFalse(
                dispatchTestContent.contains("http:STATUS_CREATED"),
                "Should not assert a status code the dispatcher never returns");
    }

    @Test
    public void testDispatchTestsCompareTheBoundPayloadWithTheFixture() throws Exception {
        // #9425: a test passed when the handler fired, even if a field was lost in binding. Each handler now keeps
        // the payload serialized back with its original keys, and the test compares it with the fixture it sent.
        Path tempOutputDir = generateOutputDir("github_verification.yaml");
        String dispatchTest = Files.readString(tempOutputDir.resolve("tests").resolve("dispatch_test.bal"));

        Assert.assertTrue(dispatchTest.contains("import ballerina/data.jsondata;"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("readonly & json bound = jsondata:toJson(payload).cloneReadOnly();"),
                dispatchTest);
        Assert.assertTrue(dispatchTest.contains("function assertBindsFixture(json bound, json fixture, string at)"),
                dispatchTest);
        Assert.assertTrue(dispatchTest.contains("json fixture = check io:fileReadJson("), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("assertBindsFixture(bound, deliveredEvent(bound, fixture), "),
                dispatchTest);
    }

    @Test
    public void testStripeWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("stripe_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"Stripe-Signature\")"),
        "Should validate Stripe signature header");
    Assert.assertTrue(content.contains("string timestamp = extractedHeaderValues[\"timestamp\"]"),
        "Should extract and declare timestamp from Stripe signature header");
    Assert.assertTrue(content.contains("`${timestamp}.${check request.getTextPayload()}`"),
        "Should compose Stripe payload using dot-based DSL tokens");
    }

    @Test
    public void testSlackWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("slack_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Slack-Signature\")"),
        "Should validate Slack signature header");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Slack-Request-Timestamp\")"),
        "Should include Slack timestamp header in payload composition");
    Assert.assertTrue(content.contains("`v0:${check request.getHeader(\"X-Slack-Request-Timestamp\")}:"
        + "${check request.getTextPayload()}`"),
        "Should build Slack payload from dot-based DSL expression");
    }

    @Test
    public void testShopifyWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("shopify_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Shopify-Hmac-Sha256\")"),
        "Should validate Shopify signature header");
    Assert.assertTrue(content.contains("computedDigest.toBase64()"),
        "Should apply base64 encoding for Shopify signatures");
    }

    @Test
    public void testHubspotWebhookVerificationDsl() throws Exception {
    String content = generateDispatcherService("hubspot_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-HubSpot-Signature-v3\")"),
        "Should validate HubSpot signature header");
    Assert.assertTrue(content.contains("${request.method}${request.rawPath}${check request.getTextPayload()}"
        + "${check request.getHeader(\"X-HubSpot-Request-Timestamp\")}"),
        "Should compose HubSpot payload using method, uri, body, and timestamp");
    }

    @Test
    public void testGitlabStaticTokenVerificationDsl() throws Exception {
    String content = generateDispatcherService("gitlab_verification.yaml");
    Assert.assertTrue(content.contains("request.getHeader(\"X-Gitlab-Token\")"),
        "Should validate GitLab token header");
    Assert.assertTrue(content.contains("string signature = extractedHeaderValues[\"signature\"]"),
        "Should extract signature token from header template");
    Assert.assertTrue(content.contains("crypto:equalConstantTime("
    + "signature.toBytes(), webhookSecret.toBytes())"),
        "Should compare extracted signature directly with webhook secret for static token mode");
    Assert.assertFalse(content.contains("crypto:hmacSha"),
        "Should not generate HMAC code when algorithm is not configured");
    }

    @Test
    public void testInvalidAdjacentPlaceholdersDsl() throws Exception {
    assertGeneratorException(
        "invalid_adjacent_placeholders.yaml",
        "adjacent placeholders are not allowed");
    }

    @Test
    public void testInvalidMissingSignatureDsl() throws Exception {
    assertGeneratorException(
        "invalid_missing_signature.yaml",
        "headerFormat must include $signature/{signature}");
    }

    @Test
    public void testInvalidBadPayloadTokenDsl() throws Exception {
    assertGeneratorException(
        "invalid_bad_payload_token.yaml",
        "Invalid input token in webhook DSL");
    }

    @Test
    public void testConnectionAuthFieldCollisionWithWebhookDslConfigFieldThrows() throws Exception {
        // The webhook DSL declares $config('clientId'), and the spec also declares an oauth2
        // clientCredentials securityScheme, which generates its own "clientId" field. Both would
        // land in the same ListenerConfig record under the same name, producing invalid Ballerina
        // source (duplicate field) if not caught explicitly.
        assertGeneratorException(
                "connection_auth_field_collision.yaml",
                "collide with existing webhook DSL config field name(s)");
    }

    @Test
    public void testHttpApiKeyFieldCollisionWithWebhookDslConfigFieldThrows() throws Exception {
        // Same collision check, exercised through the real DSL-parsing -> extractor ->
        // collision-check path end to end, for the newly-added httpApiKey mechanism.
        assertGeneratorException(
                "connection_auth_field_collision_httpapikey.yaml",
                "collide with existing webhook DSL config field name(s)");
    }

    @Test
    public void testX509FieldCollisionWithWebhookDslConfigFieldThrows() throws Exception {
        // Same collision check, exercised end to end, for the newly-added X509 mechanism.
        assertGeneratorException(
                "connection_auth_field_collision_x509.yaml",
                "collide with existing webhook DSL config field name(s)");
    }

    @Test
    public void testFormBodiesAreReadAndSignedWithSortedParams() throws Exception {
        // Twilio: form-encoded status callbacks, signed over the URL and the parameters sorted by name.
        Path outputDir = generateOutputDir("form_bodies.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcher.contains("map<string> formParams = check request.getFormParams();"), dispatcher);
        Assert.assertFalse(dispatcher.contains("getJsonPayload"), "A form body is not JSON: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("int|error converted = int:fromString(formParams.get(\"NumMedia\"));"),
                "An integer field is converted from its string: " + dispatcher);
        Assert.assertFalse(dispatcher.contains("fromString(formParams.get(\"MessageSid\"))"),
                "String fields stay strings: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("string payloadToHash = string `${self.webhookUrl}"
                + "${self.sortedFormParams(check request.getFormParams())}`;"), dispatcher);
        Assert.assertTrue(dispatcher.contains("private isolated function sortedFormParams(map<string> params)"),
                dispatcher);
        Assert.assertTrue(dispatcher.contains("crypto:hmacSha1"), dispatcher);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains("import ballerina/url;"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("formParamsText += name + params.get(name);"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("\"application/x-www-form-urlencoded\");"),
                "The test client posts form bodies: " + dispatchTest);
    }

    @Test
    public void testMixedFormAndJsonMessagesAreRejected() throws Exception {
        assertGeneratorException("form_mixed_content.yaml", "all messages must use the same content type");
    }

    @Test
    public void testFormFieldTypedInOneMessageAndStringInAnotherIsRejected() throws Exception {
        assertGeneratorException("form_field_type_conflict.yaml",
                "a form field must have one type");
    }

    @Test
    public void testFormParamsWithoutFormBodiesIsRejected() throws Exception {
        assertGeneratorException("form_params_without_form.yaml",
                "uses $formParams, but the messages are not form-encoded");
    }

    @Test
    public void testChannelIdentifiersGiveEachChannelItsOwnResource() throws Exception {
        // Twilio-style: calls and messages name their status in different fields and share values like `failed`.
        Path outputDir = generateOutputDir("channel_identifiers.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcher.contains("resource function post callStatus(http:Caller caller"), dispatcher);
        Assert.assertTrue(dispatcher.contains("resource function post smsStatus(http:Caller caller"), dispatcher);
        Assert.assertTrue(dispatcher.contains("resource function post alerts(http:Caller caller"), dispatcher);
        Assert.assertFalse(dispatcher.contains("resource function post .("), "No catch-all resource: " + dispatcher);
        Assert.assertFalse(dispatcher.contains("function matchRemoteFunc("),
                "Each resource dispatches to its own service only: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("string eventType = (check payload.CallStatus).toString();"), dispatcher);
        Assert.assertTrue(dispatcher.contains("string eventType = (check payload.MessageStatus).toString();"),
                dispatcher);
        Assert.assertTrue(dispatcher.contains("request.getHeader(\"X-Event-Type\")"),
                "A channel without its own identifier uses the document's: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("self.matchRemoteFuncForCallStatus(payload, eventType)"), dispatcher);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains(
                "sendSignedTriggerWebhook(\"/callStatus\", \"callStatus/failed\", {})"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains(
                "sendSignedTriggerWebhook(\"/smsStatus\", \"smsStatus/failed\", {})"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains(
                "sendSignedTriggerWebhook(\"/alerts\", \"alerts/warning\", {\"X-Event-Type\": "), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("function testCallStatusServiceFailedDispatch()"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("function testSmsStatusServiceFailedDispatch()"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("string payloadToHash = string `${path}${bodyText}`;"),
                "$uri signs the channel's path: " + dispatchTest);
    }

    @Test
    public void testChannelWithoutAnyIdentifierIsRejected() throws Exception {
        assertGeneratorException("channel_identifier_missing.yaml",
                "Channel 'alerts' has no x-ballerina-event-identifier");
    }

    @Test
    public void testBodyKeyIdentifierDispatchesOnTheKeyAndNamesFixturesByMessage() throws Exception {
        // WSO2 Identity Server: the event URI is the single key under `events`.
        Path outputDir = generateOutputDir("body_key_identifier.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcher.contains("string eventType = keyedEvents.keys()[0];"), dispatcher);
        Assert.assertTrue(dispatcher.contains(
                "\"https://schemas.identity.wso2.org/events/login/event-type/loginSuccess\" =>"), dispatcher);

        String serviceTypes = Files.readString(outputDir.resolve("service_types.bal"));
        Assert.assertTrue(serviceTypes.contains("remote function onLoginSuccess("), serviceTypes);
        Assert.assertTrue(serviceTypes.contains("remote function onLoginFailed("), serviceTypes);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains("sendSignedTriggerWebhook(\"loginSuccess\")"),
                "A URI cannot name a file, so the message key names the fixture: " + dispatchTest);
        Assert.assertFalse(dispatchTest.contains("sendSignedTriggerWebhook(\"https://"), dispatchTest);
        Assert.assertTrue(
                dispatchTest.contains("test:assertEquals(triggerFired.keys(), [\"LoginService.onLoginSuccess\"]"),
                "Each event must reach only its own function: " + dispatchTest);
        Assert.assertFalse(dispatchTest.contains("x-hub-signature\": headerValue"),
                "A body-key identifier sends no identifier header: " + dispatchTest);
    }

    @Test
    public void testFreshnessInSecondsScalesTheHeaderTimestampToMillis() throws Exception {
        // Slack sends X-Slack-Request-Timestamp in epoch seconds. Compared against the millisecond
        // clock unscaled, every request would look decades old and be rejected as expired.
        Path outputDir = generateOutputDir("slack_freshness_seconds.yaml");

        String dispatcherContent = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(
                dispatcherContent.contains("freshnessTimestamp = freshnessTimestamp * 1000;"),
                "A seconds timestamp must be scaled to milliseconds before it is compared: "
                        + dispatcherContent);
        Assert.assertTrue(
                dispatcherContent.contains("freshnessSkewMillis.abs()"),
                "The window must stay symmetric: " + dispatcherContent);

        String dispatchTestContent = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(
                dispatchTestContent.contains("= time:utcNow()[0].toString();"),
                "The generated test client must send epoch seconds, or it would fail its own freshness "
                        + "check: " + dispatchTestContent);
        Assert.assertFalse(
                dispatchTestContent.contains("(time:utcNow()[0] * 1000).toString()"),
                "The test client must not send milliseconds for a seconds header: " + dispatchTestContent);
    }

    @Test
    public void testRejectedSignatureIsLoggedAndEmptySecretCountsAsMissing() throws Exception {
        // #9397: a rejection says why in the log, and "" is reported like a missing secret.
        Path outputDir = generateOutputDir("github_verification.yaml");
        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));

        int log = dispatcher.indexOf(
                "log:printWarn(\"WEBHOOK_SIGNATURE_REJECTED\", reason = reason, path = request.rawPath);");
        int unauthorized = dispatcher.indexOf("r.statusCode = http:STATUS_UNAUTHORIZED;");
        Assert.assertTrue(log >= 0 && log < unauthorized,
                "A rejected request should be logged before the 401 is sent: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("verifyResult.message().startsWith(\"Unauthorized:\")")
                        && dispatcher.contains("\"Unauthorized: Malformed Request\""),
                "Only the fixed reasons are logged, never text taken from the request: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("if webhookSecret is () || webhookSecret.trim() == \"\" {"),
                "An empty secret should be treated as not configured: " + dispatcher);
    }

    @Test
    public void testUnparsableFreshnessHeaderGetsAGenericError() throws Exception {
        // #9397: the conversion error would carry the received header value into the rejection log.
        Path outputDir = generateOutputDir("slack_freshness_seconds.yaml");
        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));

        int parse = dispatcher.indexOf("freshnessTimestamp = check decimal:fromString(freshnessHeaderValue);");
        int onFail = dispatcher.indexOf("on fail {", parse);
        int generic = dispatcher.indexOf("return error(\"Unauthorized: Invalid Freshness Header\");", onFail);
        Assert.assertTrue(parse > 0 && onFail > parse && generic > onFail,
                "A conversion failure should be caught and replaced with a generic error: " + dispatcher);
        Assert.assertFalse(dispatcher.contains("parsedTimestamp"),
                "No extra local that a headerFormat placeholder could collide with: " + dispatcher);
    }

    @Test
    public void testPostHandshakeEchoesTheChallengeBeforeDispatching() throws Exception {
        // Slack's url_verification: answered by the listener, after the signature check, before any dispatch.
        Path outputDir = generateOutputDir("handshake_post.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        int verify = dispatcher.indexOf("verifyWebhookSignature(request");
        int handshake = dispatcher.indexOf("handshakeMarker == \"url_verification\"");
        int eventType = dispatcher.indexOf("string eventType =");
        Assert.assertTrue(verify >= 0 && handshake > verify && eventType > handshake,
                "The handshake is answered after the signature check and before the event type is read: "
                        + dispatcher);
        Assert.assertTrue(dispatcher.contains("json|error handshakeMarker = payload.'type;"),
                "A keyword path segment must be escaped: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("handshakeResponse.setTextPayload(challenge.toString());"),
                "The challenge is echoed as plain text: " + dispatcher);
        Assert.assertFalse(dispatcher.contains("resource function get"),
                "A POST handshake needs no GET resource: " + dispatcher);

        String serviceTypes = Files.readString(outputDir.resolve("service_types.bal"));
        Assert.assertFalse(serviceTypes.toLowerCase(java.util.Locale.ROOT).contains("verification"),
                "The handshake must not become a remote function: " + serviceTypes);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains("function testVerificationHandshake()"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("isolated function sendSignedTriggerBody(byte[] body)"),
                "The handshake test signs its own body: " + dispatchTest);
        Assert.assertTrue(dispatchTest.contains("\"type\": \"url_verification\""), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("\"challenge\": \"trigger-test-challenge\""), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("test:assertEquals(response.statusCode, 200);"), dispatchTest);
    }

    @Test
    public void testWebSubVerificationGetsAGetResourceThatEchoesTheChallenge() throws Exception {
        // WebSub hubs (Identity Server, Asgardeo) confirm a subscription with GET ?hub.mode=subscribe&hub.challenge=.
        Path outputDir = generateOutputDir("handshake_websub.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcher.contains("resource function get .(http:Caller caller, http:Request request)"),
                dispatcher);
        Assert.assertTrue(dispatcher.contains("request.getQueryParamValue(\"hub.mode\") != \"subscribe\""),
                "Another hub.mode is not confirmed: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("request.getQueryParamValue(\"hub.topic\") is ()"),
                "A request without the required hub.topic is not confirmed: " + dispatcher);
        Assert.assertTrue(dispatcher.contains("handshakeResponse.statusCode = http:STATUS_NOT_FOUND;"), dispatcher);
        Assert.assertTrue(dispatcher.contains("string? challenge = request.getQueryParamValue(\"hub.challenge\");"),
                dispatcher);
        Assert.assertFalse(dispatcher.contains("handshakeMarker"),
                "A WebSub verification adds nothing to the POST resource: " + dispatcher);

        String serviceTypes = Files.readString(outputDir.resolve("service_types.bal"));
        Assert.assertFalse(serviceTypes.contains("Verif"), "The handshake must not become a service: " + serviceTypes);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains(
                "triggerClient->get(\"/?hub.mode=subscribe&hub.topic=trigger-test-value"
                        + "&hub.challenge=trigger-test-challenge\")"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains("function testVerificationHandshakeIsNotConfirmedForOtherRequests()"),
                dispatchTest);
        Assert.assertTrue(dispatchTest.contains("function testVerificationHandshakeNeedsEveryRequiredParameter()"),
                dispatchTest);
        Assert.assertFalse(dispatchTest.contains("sendSignedTriggerBody"),
                "A GET is not signed, so the send helper stays as it was: " + dispatchTest);
    }

    @Test
    public void testFreshnessDefaultsToMillisecondsWithoutAUnit() throws Exception {
        Path tempOutputDir = generateFromFixture("hubspot_v3_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertFalse(
                dispatcherContent.contains("freshnessTimestamp * 1000"),
                "A freshness block without a unit must keep treating the header as milliseconds");
        String dispatchTestContent = Files.readString(
                tempOutputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(
                dispatchTestContent.contains("(time:utcNow()[0] * 1000).toString()"),
                "The test client must keep sending milliseconds by default");
    }

    @Test
    public void testInvalidFreshnessUnitIsRejected() throws Exception {
        assertGeneratorException(
                "invalid_freshness_unit.yaml",
                "expected 'seconds' or 'milliseconds', but found 'minutes'");
    }

    @Test
    public void testRsaStrategyVerifiesTheSignatureWithTheProvidersPublicKey() throws Exception {
        // Aayu MFT Gateway signs with a private key and the consumer verifies with the downloaded
        // public key, so there is no shared secret to key an HMAC with.
        Path outputDir = generateOutputDir("rsa_signature_verification.yaml");

        String dispatcherContent = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcherContent.contains("import ballerina/lang.array;"),
                "Should import lang.array to decode the signature header: " + dispatcherContent);
        Assert.assertTrue(
                dispatcherContent.contains("crypto:PublicKey|error publicKey = self.rsaPublicKey(webhookSecret);"),
                "The configured value must be read as the provider's public key: " + dispatcherContent);
        Assert.assertTrue(dispatcherContent.contains("crypto:decodeRsaPublicKeyFromContent(pem.toBytes())"),
                "A certificate is still accepted: " + dispatcherContent);
        Assert.assertTrue(dispatcherContent.contains("crypto:buildRsaPublicKey("),
                "A PEM public key is built from its modulus and exponent: " + dispatcherContent);
        Assert.assertTrue(dispatcherContent.contains("array:fromBase64(signature)"),
                "The signature header must be decoded per the declared encoding: " + dispatcherContent);
        Assert.assertTrue(
                dispatcherContent.contains(
                        "crypto:verifyRsaSha256Signature(payloadToHash.toBytes(), receivedSignature, publicKey)"),
                "Should verify against the raw body with the RSA verifier: " + dispatcherContent);
        Assert.assertFalse(dispatcherContent.contains("crypto:hmac"),
                "A public-key scheme must not derive an HMAC from the configured value: " + dispatcherContent);
        Assert.assertFalse(dispatcherContent.contains("equalConstantTime"),
                "A public-key signature cannot be recomputed and compared: " + dispatcherContent);

        String dispatchTestContent = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTestContent.contains("const TRIGGER_TEST_PRIVATE_KEY = "),
                "The generated test must carry a throwaway private key to sign with: " + dispatchTestContent);
        Assert.assertTrue(dispatchTestContent.contains("webhookSecret: TRIGGER_TEST_PUBLIC_KEY"),
                "The test listener must be configured with the matching public key: " + dispatchTestContent);
        Assert.assertTrue(dispatchTestContent.contains("-----BEGIN PUBLIC KEY-----"), dispatchTestContent);
        Assert.assertTrue(dispatchTestContent.contains("crypto:signRsaSha256(payloadToHash.toBytes(), privateKey)"),
                "The test client must sign with the private key: " + dispatchTestContent);
        Assert.assertFalse(dispatchTestContent.contains("TRIGGER_TEST_SECRET"),
                "No shared secret exists in a public-key scheme: " + dispatchTestContent);
    }

    @Test
    public void testSortedJsonBodySignsTheBodyWithItsTopLevelKeysSorted() throws Exception {
        // Aayu MFT Gateway signs JSON.stringify of the payload with its top-level keys sorted, not the sent body.
        Path outputDir = generateOutputDir("rsa_sorted_json_body.yaml");

        String dispatcher = Files.readString(outputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcher.contains(
                "string payloadToHash = string `${check self.sortedJsonBody(check request.getJsonPayload())}`;"),
                dispatcher);
        Assert.assertTrue(dispatcher.contains("private isolated function sortedJsonBody(json payload)"), dispatcher);
        Assert.assertTrue(dispatcher.contains("from string name in fields.keys().sort()"), dispatcher);
        Assert.assertTrue(dispatcher.contains("private isolated function compactJson(json value)"), dispatcher);

        String dispatchTest = Files.readString(outputDir.resolve("tests").resolve("dispatch_test.bal"));
        Assert.assertTrue(dispatchTest.contains("check sortedJsonBody(check bodyText.fromJsonString())"),
                "The test client signs the same sorted body: " + dispatchTest);
        Assert.assertTrue(dispatchTest.contains("isolated function sortedJsonBody(json payload)"), dispatchTest);
        // A fixed expected string, so a wrong sort on both sides cannot pass.
        Assert.assertTrue(dispatchTest.contains("function testSortedJsonBody() returns error?"), dispatchTest);
        Assert.assertTrue(dispatchTest.contains(
                "\"{\\\"a\\\":{\\\"y\\\":true,\\\"b\\\":[1,\\\"x\\\",null]},\\\"m\\\":\\\"s\\\",\\\"z\\\":1}\""),
                dispatchTest);
    }

    @Test
    public void testSortedJsonBodyIsOnlyGeneratedWhenUsed() throws Exception {
        String dispatcher = Files.readString(
                generateOutputDir("rsa_signature_verification.yaml").resolve("dispatcher_service.bal"));
        Assert.assertFalse(dispatcher.contains("sortedJsonBody"), dispatcher);
    }

    @Test
    public void testRsaStrategyWithoutAnAlgorithmIsRejected() throws Exception {
        assertGeneratorException(
                "invalid_rsa_missing_algorithm.yaml",
                "strategy 'rsa' requires an algorithm");
    }

    @Test
    public void testUnknownSignatureStrategyIsRejected() throws Exception {
        assertGeneratorException(
                "invalid_signature_strategy.yaml",
                "Unsupported x-ballerina-auth signature strategy: 'ecdsa'");
    }

    private Path generateOutputDir(String specFile) throws Exception {
    Path asyncapiPath = RES_DIR.resolve(specFile);
    String yamlContent = Files.readString(asyncapiPath);
    ObjectNode specNode = (ObjectNode) YAML_MAPPER.readTree(yamlContent);
        normalizeForAsyncApi3(specNode);
    String jsonContent = JSON_MAPPER.writeValueAsString(specNode);
    AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(jsonContent);

    String specName = specFile.replace(".yaml", "").replace(".yml", "");
    Path tempOutputDir = Paths.get("build", "generated-test-output", "webhook-dsl", specName).toAbsolutePath();
    Files.createDirectories(tempOutputDir);
    HttpCodeGenerator generator = new HttpCodeGenerator(spec);
    generator.generate(tempOutputDir);
    return tempOutputDir;
    }

    private String generateDispatcherService(String specFile) throws Exception {
    Path tempOutputDir = generateOutputDir(specFile);
    Path dispatcherFile = tempOutputDir.resolve("dispatcher_service.bal");
    Assert.assertTrue(Files.exists(dispatcherFile), "dispatcher_service.bal was not generated!");
    return Files.readString(dispatcherFile);
    }

    private void normalizeForAsyncApi3(ObjectNode specNode) {
        String version = specNode.path("asyncapi").asText("");
        if (!version.startsWith("3")) {
            return;
        }

        // Keep verification-focused fixtures parser-friendly for AsyncAPI 3.
        specNode.remove("servers");

        JsonNode channelsNode = specNode.get("channels");
        if (!(channelsNode instanceof ObjectNode channelsObj)) {
            return;
        }

        ObjectNode operationsNode = specNode.with("operations");
        Iterator<Map.Entry<String, JsonNode>> channels = channelsObj.fields();
        while (channels.hasNext()) {
            Map.Entry<String, JsonNode> channelEntry = channels.next();
            String channelName = channelEntry.getKey();
            if (!(channelEntry.getValue() instanceof ObjectNode channelObj)) {
                continue;
            }

            if (!channelObj.has("address")) {
                channelObj.put("address", channelName);
            }

            JsonNode subscribeNode = channelObj.get("subscribe");
            if (!(subscribeNode instanceof ObjectNode subscribeObj)) {
                continue;
            }

            ObjectNode messagesObj = JSON_MAPPER.createObjectNode();
            JsonNode messageNode = subscribeObj.get("message");
            if (messageNode != null) {
                if (messageNode.has("oneOf") && messageNode.get("oneOf") instanceof ArrayNode oneOfArray) {
                    for (int i = 0; i < oneOfArray.size(); i++) {
                        JsonNode oneOfMessage = oneOfArray.get(i);
                        String eventType = oneOfMessage.path("x-ballerina-event-type").asText("event_" + i);
                        messagesObj.set(eventTypeToMessageKey(eventType, i), oneOfMessage);
                    }
                } else {
                    String eventType = messageNode.path("x-ballerina-event-type").asText(channelName + "_event");
                    messagesObj.set(eventTypeToMessageKey(eventType, 0), messageNode);
                }
            }

            channelObj.remove("subscribe");
            channelObj.set("messages", messagesObj);

            String operationName = sanitizeIdentifier(channelName);
            ObjectNode operation = JSON_MAPPER.createObjectNode();
            operation.put("action", "receive");

            ObjectNode channelRef = JSON_MAPPER.createObjectNode();
            channelRef.put("$ref", "#/channels/" + channelName);
            operation.set("channel", channelRef);

            ArrayNode opMessages = JSON_MAPPER.createArrayNode();
            Iterator<String> msgNames = messagesObj.fieldNames();
            while (msgNames.hasNext()) {
                String msgName = msgNames.next();
                ObjectNode msgRef = JSON_MAPPER.createObjectNode();
                msgRef.put("$ref", "#/channels/" + channelName + "/messages/" + msgName);
                opMessages.add(msgRef);
            }
            operation.set("messages", opMessages);
            operationsNode.set(operationName, operation);
        }
    }

    private String eventTypeToMessageKey(String eventType, int index) {
        String key = sanitizeIdentifier(eventType);
        return key.isBlank() ? "message_" + index : key;
    }

    private String sanitizeIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private void assertGeneratorException(String specFile, String expectedMessagePart) throws Exception {
        try {
            generateDispatcherService(specFile);
            Assert.fail("Expected GeneratorException for invalid DSL in " + specFile);
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessagePart),
                "Validation message mismatch for " + specFile + ": " + e.getMessage());
        }
    }

    @Test
    public void testHubspotV3WebhookVerification() throws Exception {
        Path tempOutputDir = generateFromFixture("hubspot_v3_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        // HubSpot v3: $config('callbackUrl') resolves to a self field, folded into the HMAC input
        // alongside method/body/timestamp; encoding is base64, not hex.
        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcherContent.contains("crypto:hmacSha256"), "Should use HMAC-SHA256");
        Assert.assertTrue(
                dispatcherContent.contains("${self.callbackUrl}"),
                "Should reference the config-derived callbackUrl field");
        Assert.assertTrue(
                dispatcherContent.contains("computedDigest.toBase64()"),
                "Should base64-encode the computed digest");

        // Freshness check: a staleness guard independent of signature verification.
        Assert.assertTrue(
                dispatcherContent.contains("decimal:fromString(freshnessHeaderValue)"),
                "Should parse the freshness header as a decimal timestamp");
        Assert.assertTrue(
                dispatcherContent.contains("time:utcNow()"),
                "Should compare against the current time");
        Assert.assertTrue(
                dispatcherContent.contains("Request Timestamp Expired"),
                "Should reject stale requests");
        Assert.assertTrue(
                dispatcherContent.contains("freshnessSkewMillis.abs()"),
                "The freshness window must be symmetric - a timestamp far in the future must be "
                        + "rejected too, not just one that's too old: " + dispatcherContent);

        // callbackUrl must be threaded through as a real configurable field, not just referenced.
        String dataTypesContent = Files.readString(tempOutputDir.resolve("types.bal"));
        Assert.assertTrue(
                dataTypesContent.contains("string callbackUrl"),
                "ListenerConfig should declare a callbackUrl field");
        String listenerContent = Files.readString(tempOutputDir.resolve("listener.bal"));
        Assert.assertTrue(
                listenerContent.contains("listenerConfig.callbackUrl"),
                "Listener init should pass callbackUrl through to DispatcherService");

        // ballerina/time must be imported since freshness is configured.
        Assert.assertTrue(dispatcherContent.contains("import ballerina/time;"), "Should import ballerina/time");
    }

    @Test
    public void testHubspotV1WebhookVerification() throws Exception {
        Path tempOutputDir = generateFromFixture("hubspot_v1_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        // HubSpot v1/v2: plain (unkeyed) digest of secret+body, not a keyed HMAC.
        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(dispatcherContent.contains("crypto:hashSha256"), "Should use plain hashSha256");
        Assert.assertFalse(dispatcherContent.contains("crypto:hmacSha256"), "Should not use HMAC");
        Assert.assertTrue(
                dispatcherContent.contains("${webhookSecret}"),
                "Should fold the secret directly into the hashed payload via $secret");
        Assert.assertTrue(
                dispatcherContent.contains("computedDigest.toBase16()"),
                "Should hex-encode the computed digest");

        // No freshness block in this fixture: no staleness check, no time import.
        Assert.assertFalse(
                dispatcherContent.contains("Request Timestamp Expired"),
                "Should not emit a freshness check when none is configured");
        Assert.assertFalse(dispatcherContent.contains("import ballerina/time;"),
                "Should not import ballerina/time when no freshness check is configured");
    }

    @Test
    public void testHubspotV3DispatchTestUsesGeneralSigningNotGithubDefaults() throws Exception {
        // DispatchTestGenerator previously hardcoded GitHub's scheme (single composite header,
        // plain HMAC-SHA256 hex of the raw body) regardless of what the spec actually declared.
        // HubSpot's v3 scheme is structurally different: input is method+config+body+timestamp,
        // base64-encoded, with a freshness header the test itself must synthesize. This proves the
        // generated tests/dispatch_test.bal now follows the spec's real DSL, not GitHub's shape.
        Path tempOutputDir = generateFromFixture("hubspot_v3_verification.yaml", "X-HubSpot-Event",
                "hubspot_event");

        Path dispatchTestFile = tempOutputDir.resolve("tests").resolve("dispatch_test.bal");
        Assert.assertTrue(Files.exists(dispatchTestFile), "tests/dispatch_test.bal was not generated!");
        String dispatchTestContent = Files.readString(dispatchTestFile);

        Assert.assertTrue(
                dispatchTestContent.contains("import ballerina/time;"),
                "Should import ballerina/time to synthesize the freshness timestamp: " + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("const TRIGGER_TEST_CALLBACK_URL"),
                "Should synthesize a test value for the $config('callbackUrl') reference: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("callbackUrl: TRIGGER_TEST_CALLBACK_URL"),
                "Should thread the config value into the test listener's configuration: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("crypto:hmacSha256(payloadToHash.toBytes(), "
                        + "TRIGGER_TEST_SECRET.toBytes())"),
                "Should compute a keyed HMAC-SHA256, not GitHub's plain-body hex default: "
                        + dispatchTestContent);
        Assert.assertTrue(
                dispatchTestContent.contains("computedDigest.toBase64()"),
                "Should base64-encode the digest per the spec's encoding, not GitHub's hex default: "
                        + dispatchTestContent);
        Assert.assertFalse(
                dispatchTestContent.contains("digest.toBase16()"),
                "Should not fall back to GitHub's hardcoded hex encoding: " + dispatchTestContent);
    }

    @Test
    public void testCustomVariableSharingReservedTokenPrefixIsNotCorrupted() throws Exception {
        // "bodyHash" is a custom variable extracted from the signature header, not the builtin
        // "$body" token -- but it starts with the substring "body". A naive String.replace("$body", ...)
        // on the input DSL would corrupt "$bodyHash" into "${check request.getTextPayload()}Hash"
        // before the custom-variable pass ever runs. The fix must treat "$body" as matched only
        // when it isn't immediately followed by another identifier character.
        Path tempOutputDir = generateFromFixture("token_corruption_verification.yaml", "X-Test-Event",
                "test_event");

        String dispatcherContent = Files.readString(tempOutputDir.resolve("dispatcher_service.bal"));
        Assert.assertTrue(
                dispatcherContent.contains("${bodyHash}"),
                "Should interpolate the custom bodyHash variable extracted from the signature header");
        Assert.assertTrue(
                dispatcherContent.contains("${check request.getTextPayload()}"),
                "Should still correctly interpolate the builtin $body token");
        Assert.assertFalse(
                dispatcherContent.contains("${check request.getTextPayload()}Hash"),
                "$body substitution must not corrupt the unrelated $bodyHash custom variable");
    }

    /**
     * Loads a webhook-verification test fixture, injects the metadata the generator pipeline
     * expects, and runs it through {@link HttpCodeGenerator}.
     *
     * @param fixtureFileName the fixture file name under {@code src/test/resources/specs}
     * @param eventHeaderName the header used for event-type dispatch
     * @param eventType       the {@code x-ballerina-event-type} to assign to the fixture's message
     * @return the temp directory the generator wrote output files into
     */
    private Path generateFromFixture(String fixtureFileName, String eventHeaderName, String eventType)
            throws Exception {
        Path asyncapiPath = RES_DIR.resolve(fixtureFileName);

        String yamlContent = Files.readString(asyncapiPath);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectNode specNode = (ObjectNode) yamlMapper.readTree(yamlContent);

        ObjectNode eventIdentifier = specNode.putObject("x-ballerina-event-identifier");
        eventIdentifier.put("type", "header");
        eventIdentifier.put("name", eventHeaderName);

        ObjectNode subscribeNode = (ObjectNode) specNode.path("channels")
                .path("events")
                .path("subscribe");
        ObjectNode messageNode = (ObjectNode) subscribeNode.path("message");
        messageNode.put("x-ballerina-event-type", eventType);

        if (subscribeNode.has("x-ballerina-auth")) {
            specNode.set("x-ballerina-auth", subscribeNode.get("x-ballerina-auth"));
        }

        String jsonContent = new ObjectMapper().writeValueAsString(specNode);
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(jsonContent);

        Path tempOutputDir = Files.createTempDirectory("asyncapi-gen-test");
        HttpCodeGenerator generator = new HttpCodeGenerator(spec);
        generator.generate(tempOutputDir);
        return tempOutputDir;
    }
}


