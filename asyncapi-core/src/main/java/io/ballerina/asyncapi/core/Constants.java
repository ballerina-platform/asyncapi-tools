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
 * Constants used across the AsyncAPI core module.
 */
public final class Constants {

    public static final String ERROR_PREFIX = "Failed to parse AsyncAPI specification: ";

    public static final String EXTERNAL_DOCS_REF_PREFIX = "#/components/externalDocs/";
    public static final String TAGS_REF_PREFIX = "#/components/tags/";
    public static final String SCHEMAS_REF_PREFIX = "#/components/schemas/";
    public static final String SERVERS_REF_PREFIX = "#/components/servers/";
    public static final String CHANNELS_REF_PREFIX = "#/components/channels/";
    public static final String V3_CHANNELS_REF_PREFIX = "#/channels/";
    public static final String MESSAGES_REF_PREFIX = "#/components/messages/";
    public static final String SECURITY_SCHEMES_REF_PREFIX = "#/components/securitySchemes/";
    public static final String SERVER_VARIABLES_REF_PREFIX = "#/components/serverVariables/";
    public static final String PARAMETERS_REF_PREFIX = "#/components/parameters/";
    public static final String CORRELATION_IDS_REF_PREFIX = "#/components/correlationIds/";
    public static final String OPERATIONS_REF_PREFIX = "#/components/operations/";
    public static final String REPLIES_REF_PREFIX = "#/components/replies/";
    public static final String REPLY_ADDRESSES_REF_PREFIX = "#/components/replyAddresses/";
    public static final String CHANNEL_BINDINGS_REF_PREFIX = "#/components/channelBindings/";

    private Constants() {
    }
}
