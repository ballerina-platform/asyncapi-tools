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
package io.ballerina.asyncapi.trigger.plugin;

/**
 * An HTTP endpoint a trigger listener serves: services on the same port and path share one.
 *
 * @param port     the port, or {@code 0} when it cannot be read from the source
 * @param basePath the path the services are attached to, starting with {@code /}, or empty for the root
 */
record TriggerEndpoint(int port, String basePath) {

    // Same defaults the ballerina/http plugin uses for an HTTP service, minus its OpenAPI file.
    static final String TYPE = "REST";
    static final String SCHEMA_PATH = "";
}
