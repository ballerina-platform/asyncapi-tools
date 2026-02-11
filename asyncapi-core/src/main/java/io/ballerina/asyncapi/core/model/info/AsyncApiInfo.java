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
package io.ballerina.asyncapi.core.model.info;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Represents the Info Object in an AsyncAPI document.
 * Provides metadata about the API.
 *
 * @param title          The title of the application.
 * @param version        The version of the application API.
 * @param description    A description of the application.
 * @param termsOfService A URI to the Terms of Service for the API.
 * @param contact        Contact information for the API.
 * @param license        License information for the API.
 * @param tags           A list of tags for API documentation control.
 * @param externalDocs   Additional external documentation.
 * @param extensions     Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiInfo(
        String title,
        String version,
        String description,
        URI termsOfService,
        AsyncApiContact contact,
        AsyncApiLicense license,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        Map<String, JsonNode> extensions
) {
}
