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

import java.util.List;

/**
 * Represents a Ballerina service type to be generated from an AsyncAPI channel/operation group.
 * A service type aggregates the remote functions for all events that belong to a named service.
 *
 * @param serviceTypeName the Ballerina-safe name for the service type (from {@code x-ballerina-service-type})
 * @param remoteFunctions the list of remote functions this service type exposes, one per event
 */
public record HttpServiceType(
        String serviceTypeName,
        List<HttpRemoteFunction> remoteFunctions
) {
}
