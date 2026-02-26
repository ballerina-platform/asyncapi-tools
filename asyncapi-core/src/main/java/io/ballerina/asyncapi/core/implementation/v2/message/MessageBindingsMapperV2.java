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
package io.ballerina.asyncapi.core.implementation.v2.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageBindings;
import io.ballerina.asyncapi.core.implementation.utils.BindingUtils;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;
import io.ballerina.asyncapi.core.model.message.WsMessageBindings;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiMessageBindings} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings} for AsyncAPI 2.x.
 */
public final class MessageBindingsMapperV2 {

    private MessageBindingsMapperV2() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageBindings} to a model message bindings.
     *
     * @param bindings the Apicurio message bindings object
     * @return the mapped AsyncApiMessageBindings, or null if empty
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings
    map(AsyncApiMessageBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpMessageBindings http = mapHttpMessageBinding(bindings.getHttp());
        WsMessageBindings ws = bindings.getWs() != null ? new WsMessageBindings() : null;
        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings(
                http, ws, extensions);
    }

    /**
     * Maps an Apicurio HTTP message binding to {@link HttpMessageBindings}.
     *
     * @param binding the Apicurio HTTP binding object
     * @return the mapped HttpMessageBindings, or null if binding is null
     */
    private static HttpMessageBindings mapHttpMessageBinding(AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }
        Integer statusCode = BindingUtils.getItemAsInteger(binding, "statusCode");
        String bindingVersion = BindingUtils.getItemAsText(binding, "bindingVersion");
        return new HttpMessageBindings(null, statusCode, bindingVersion);
    }


}
