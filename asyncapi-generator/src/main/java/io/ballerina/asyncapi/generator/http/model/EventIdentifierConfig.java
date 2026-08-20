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
 * Holds the resolved event identifier configuration from the {@code x-ballerina-event-identifier}
 * extension on an AsyncAPI document.
 *
 * @param type    the source of the event identifier: {@code "header"}, {@code "body"}, or {@code "composite"}
 * @param name    the request header name; non-null for {@code "header"} and {@code "composite"} types,
 *                {@code null} for {@code "body"}
 * @param path    the dot-notation JSON body path; non-null for {@code "body"} and {@code "composite"} types,
 *                {@code null} for {@code "header"}
 * @param batched whether a single POST body carries an array of events rather than one event; when
 *                {@code true}, the generated dispatcher iterates the array, extracting the identifier
 *                and dispatching each element independently, isolating one malformed event from the rest
 */
public record EventIdentifierConfig(
        String type,
        String name,
        String path,
        boolean batched
) {
}
