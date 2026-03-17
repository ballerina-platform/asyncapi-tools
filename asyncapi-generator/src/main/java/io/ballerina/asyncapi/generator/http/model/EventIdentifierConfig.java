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
 * @param type the location of the event identifier: {@code "header"} or {@code "body"}
 * @param path the header name (when type is {@code "header"}) or
 *             the dot-notation JSON path (when type is {@code "body"})
 */
public record EventIdentifierConfig(
        String type,
        String path
) {
}
