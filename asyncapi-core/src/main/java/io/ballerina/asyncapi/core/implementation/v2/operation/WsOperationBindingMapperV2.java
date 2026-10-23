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
package io.ballerina.asyncapi.core.implementation.v2.operation;

import io.ballerina.asyncapi.core.model.operation.WsOperationBindings;

/**
 * Maps Apicurio WebSocket operation binding to {@link WsOperationBindings} for AsyncAPI 2.x.
 */
final class WsOperationBindingMapperV2 {

    private WsOperationBindingMapperV2() {
    }

    /**
     * Maps an Apicurio WebSocket operation binding to {@link WsOperationBindings}.
     *
     * <p>Note: WebSocket operation bindings in AsyncAPI 2.x don't define specific fields.
     * This mapper returns an empty binding object if the binding exists.</p>
     *
     * @param binding the Apicurio WebSocket binding object
     * @return the mapped WsOperationBindings, or null if binding is null
     */
    static WsOperationBindings map(io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }
        return new WsOperationBindings();
    }
}
