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
package io.ballerina.asyncapi.core.implementation.v2.component;

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.ballerina.asyncapi.core.Constants;

import java.util.Map;

/**
 * Utility for resolving AsyncAPI 2.x message $refs.
 */
public final class MessageRefResolverV2 {

    private MessageRefResolverV2() {
    }

    /**
     * Resolves a message $ref to an AsyncApiMessage from components.
     * Returns null if the $ref format is invalid, components is null, or the message is not found.
     *
     * @param $ref the message reference (e.g., "#/components/messages/UserMessage")
     * @param components the AsyncAPI 2.x components object
     * @return the resolved AsyncApiMessage, or null if resolution fails
     */
    public static AsyncApiMessage resolveMessageRef(String $ref, AsyncApiComponents components) {
        if ($ref == null || !$ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
            return null;
        }

        if (components == null) {
            return null;
        }

        String name = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
        Map<String, ? extends AsyncApiMessage> messagesMap = components.getMessages();
        return messagesMap != null ? messagesMap.get(name) : null;
    }
}
