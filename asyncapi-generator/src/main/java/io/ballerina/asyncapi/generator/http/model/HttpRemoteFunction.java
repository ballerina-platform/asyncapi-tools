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
 * @param functionName the Ballerina-safe function name derived from the event name
 * @param eventType    the schema type reference for the event payload
 */
public record HttpRemoteFunction(String functionName, String eventType) {
}
