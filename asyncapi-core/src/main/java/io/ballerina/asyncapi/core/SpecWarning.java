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

/**
 * A problem in the spec that the tool worked around.
 *
 * @param location   JSON Pointer to the spec node involved, such as {@code /servers/prod/host}
 * @param message    what was wrong and how it was read: one line, lowercase, no trailing period
 * @param suggestion how to fix the spec, as one imperative sentence
 */
public record SpecWarning(String location, String message, String suggestion) {
}
