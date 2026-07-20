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
package io.ballerina.asyncapi.generator.http.model;

/**
 * Represents one generated dispatch-verification test case: one event, routed to one remote
 * function on one service type, identified by the raw header value a real delivery would carry.
 *
 * @param serviceTypeName  the Ballerina-safe service type name (e.g. {@code "MetaService"})
 * @param functionName     the Ballerina-safe remote function name (e.g. {@code "onMetaDeleted"})
 * @param eventIdentifier  the resolved {@code x-ballerina-event-type} value used for dispatch
 *                         matching (e.g. {@code "meta_deleted"}); also used as the test payload
 *                         fixture's file name ({@code <eventIdentifier>.json})
 * @param headerValue      the bare event-identifier header value a real delivery carries (e.g.
 *                         {@code "meta"}), independent of any composite action suffix
 * @param payloadTypeName  the Ballerina type name of the remote function's payload parameter
 */
public record DispatchTestCase(
        String serviceTypeName,
        String functionName,
        String eventIdentifier,
        String headerValue,
        String payloadTypeName
) {
}
