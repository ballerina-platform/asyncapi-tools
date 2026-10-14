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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Tests the per-channel routing rules: when they apply, how each channel's identifier is chosen, and which
 * addresses can be resource paths.
 */
public class ChannelRoutesTest {

    private static final EventIdentifierConfig BODY = new EventIdentifierConfig("body", null, "MessageStatus");
    private static final EventIdentifierConfig HEADER = new EventIdentifierConfig("header", "X-Event-Type", null);

    private static HttpServiceType channel(String address, EventIdentifierConfig identifier) {
        return new HttpServiceType(address, List.of(), address, identifier);
    }

    private static void assertRejected(List<HttpServiceType> serviceTypes, EventIdentifierConfig document,
                                       String expectedMessagePart) {
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> ChannelRoutes.resolve(serviceTypes, document));
        Assert.assertTrue(e.getMessage().contains(expectedMessagePart), e.getMessage());
    }

    @Test
    void testPerChannelOnlyWhenAChannelDeclaresAnIdentifier() {
        Assert.assertFalse(ChannelRoutes.isPerChannel(List.of(channel("a", null), channel("b", null))));
        Assert.assertTrue(ChannelRoutes.isPerChannel(List.of(channel("a", null), channel("b", BODY))));
    }

    @Test
    void testChannelsWithoutTheirOwnIdentifierUseTheDocuments() throws GeneratorException {
        List<HttpServiceType> resolved =
                ChannelRoutes.resolve(List.of(channel("smsStatus", BODY), channel("alerts", null)), HEADER);
        Assert.assertEquals(resolved.get(0).identifier(), BODY);
        Assert.assertEquals(resolved.get(1).identifier(), HEADER);
    }

    @Test
    void testInvalidRoutesAreRejected() {
        assertRejected(List.of(channel("smsStatus", BODY), channel("alerts", null)), null,
                "Channel 'alerts' has no x-ballerina-event-identifier");
        assertRejected(List.of(channel("a", new EventIdentifierConfig("composite", "X-E", "action"))), null,
                "each channel's must be 'body' or 'header'");
        assertRejected(List.of(channel("smsStatus", BODY), channel("/smsStatus", HEADER)), null,
                "Two channels share the address");
        assertRejected(List.of(channel("sms status", BODY)), null, "cannot be a resource path");
        assertRejected(List.of(channel("/", BODY)), null, "cannot be a resource path");
    }

    @Test
    void testAddressesBecomePathsAndResourceSegments() throws GeneratorException {
        Assert.assertEquals(ChannelRoutes.urlPath("callStatus"), "/callStatus");
        Assert.assertEquals(ChannelRoutes.urlPath("/voice/status/"), "/voice/status");
        Assert.assertEquals(ChannelRoutes.segments("voice/call-status"), List.of("voice", "call-status"));
        Assert.assertEquals(ChannelRoutes.resourceSegment("call-status"), "call\\-status");
        Assert.assertEquals(ChannelRoutes.resourceSegment("check"), "'check", "Keywords are quoted");
    }
}
