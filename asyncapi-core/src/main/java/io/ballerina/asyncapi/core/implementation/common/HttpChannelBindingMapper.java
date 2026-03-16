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
package io.ballerina.asyncapi.core.implementation.common;

import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.ballerina.asyncapi.core.model.channel.HttpChannelBindings;

/**
 * Maps Apicurio HTTP channel bindings to {@link HttpChannelBindings},
 * version-independently (AsyncAPI 2.x and 3.x).
 * HTTP channel bindings have no specific fields in AsyncAPI 2.x/3.x.
 */
public final class HttpChannelBindingMapper {

    private HttpChannelBindingMapper() {
    }

    /**
     * Maps an Apicurio HTTP channel binding to {@link HttpChannelBindings}.
     *
     * @param binding the Apicurio HTTP binding object
     * @return a new {@link HttpChannelBindings} if the binding is non-null, otherwise null
     */
    public static HttpChannelBindings map(AsyncApiBinding binding) {
        return binding != null ? new HttpChannelBindings() : null;
    }
}
