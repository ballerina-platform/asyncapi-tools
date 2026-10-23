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
 * Represents a remote function to be generated in a Ballerina service type.
 * Each remote function corresponds to a single event type handled by an HTTP listener.
 *
 * @param functionName     the Ballerina-safe function name derived from the event name
 * @param eventType        the schema type reference for the event payload
 * @param matchOnEventType {@code true} if this event's payload has no enumerable action field
 *                         (i.e. its {@code action} property, if any, declares no fixed {@code enum}
 *                         of possible values), so the generated match clause must compare against
 *                         the bare event type instead of the composite {@code eventType_action}
 *                         identifier -- there is no fixed action value to safely bake into the
 *                         composite literal at generation time. Always {@code false} for
 *                         {@code "header"} and {@code "body"} identifier types, where no composite
 *                         identifier is ever built.
 * @param displayLabel     an optional, human-friendlier string (from a message's
 *                         {@code x-ballerina-event-label} extension) to derive the generated
 *                         function name and doc comment from instead of {@code functionName}.
 *                         Exists for cases where the real wire-matching value makes an awkward
 *                         function name (e.g. a versioned, prefixed CloudEvents type string like
 *                         {@code "qbo.account.merged.v1"}). {@code null} preserves the pre-existing
 *                         behavior of deriving the name from {@code functionName} itself; the match
 *                         clause always compares against {@code functionName}, never this label.
 */
public record HttpRemoteFunction(String functionName, String eventType, boolean matchOnEventType,
        String displayLabel) {

    /**
     * Creates a remote function that matches on the composite identifier (the pre-existing
     * behavior), for callers that don't need to distinguish enumerable from free-form events.
     *
     * @param functionName the Ballerina-safe function name derived from the event name
     * @param eventType    the schema type reference for the event payload
     */
    public HttpRemoteFunction(String functionName, String eventType) {
        this(functionName, eventType, false, null);
    }

    /**
     * Creates a remote function with no display label override (the pre-existing behavior).
     *
     * @param functionName     the Ballerina-safe function name derived from the event name
     * @param eventType        the schema type reference for the event payload
     * @param matchOnEventType see the class-level parameter doc
     */
    public HttpRemoteFunction(String functionName, String eventType, boolean matchOnEventType) {
        this(functionName, eventType, matchOnEventType, null);
    }
}
