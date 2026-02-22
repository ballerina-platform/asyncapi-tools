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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Contact;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.model.info.AsyncApiContact;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;

import java.util.Map;

/**
 * Maps Apicurio {@link Contact} to {@link AsyncApiContact} for AsyncAPI 2.x.
 */
public final class ContactMapper {

    private ContactMapper() {

    }

    /**
     * Maps an Apicurio {@link Contact} to an {@link AsyncApiContact}.
     *
     * @param contact the Apicurio contact object
     * @return the mapped AsyncApiContact, or null if contact is null
     */
    public static AsyncApiContact map(Contact contact) {
        if (contact == null) {
            return null;
        }

        Map<String, JsonNode> extensions = null;
        if (contact instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        return new AsyncApiContact(
                contact.getName(),
                URIUtils.toUri(contact.getUrl()),
                contact.getEmail(),
                extensions
        );
    }

}
