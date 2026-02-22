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
package io.ballerina.asyncapi.core.implementation.v3.channel;

import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings;
import io.ballerina.asyncapi.core.model.channel.HttpChannelBindings;
import io.ballerina.asyncapi.core.model.channel.WsChannelBindings;

/**
 * Maps Apicurio channel bindings to
 * {@link io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings} for AsyncAPI 3.0.
 */
public final class ChannelBindingsMapperV3 {

    private ChannelBindingsMapperV3() {
    }

    /**
     * Maps Apicurio {@link AsyncApiChannelBindings} to the model
     * {@link io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings}.
     *
     * @param bindings the Apicurio channel bindings object
     * @return the mapped AsyncApiChannelBindings, or null if bindings is null or empty
     */
    public static io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings
            map(AsyncApiChannelBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpChannelBindings http = HttpChannelBindingMapperV3.map(bindings.getHttp());
        WsChannelBindings ws = WsChannelBindingMapperV3.map(bindings.getWs());
        if (http == null && ws == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings(http, ws, null);
    }
}
