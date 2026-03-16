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

    public static final String SCHEME_SEPARATOR = "://";

    public static final String EXTERNAL_DOCS_REF_PREFIX = "#/components/externalDocs/";
    public static final String CHANNELS_REF_PREFIX = "#/components/channels/";
    public static final String SERVERS_REF_PREFIX = "#/components/servers/";
    public static final String SERVER_BINDINGS_REF_PREFIX = "#/components/serverBindings/";
    public static final String SERVER_VARIABLES_REF_PREFIX = "#/components/serverVariables/";
    public static final String SECURITY_SCHEMES_REF_PREFIX = "#/components/securitySchemes/";
    public static final String V3_CHANNELS_REF_PREFIX = "#/channels/";
    public static final String V3_SERVERS_REF_PREFIX = "#/servers/";
    public static final String MESSAGES_REF_PREFIX = "#/components/messages/";
    public static final String SCHEMAS_REF_PREFIX = "#/components/schemas/";
    public static final String CHANNEL_BINDINGS_REF_PREFIX = "#/components/channelBindings/";
    public static final String PARAMETERS_REF_PREFIX = "#/components/parameters/";
    public static final String MESSAGE_TRAITS_REF_PREFIX = "#/components/messageTraits/";

    public static final String X_BALLERINA_EVENT_TYPE = "x-ballerina-event-type";

    public static final String SCHEMA_REF = "$ref";

    // JSON Schema keywords
    public static final String SCHEMA_TITLE = "title";
    public static final String SCHEMA_TYPE = "type";
    public static final String SCHEMA_FORMAT = "format";
    public static final String SCHEMA_DESCRIPTION = "description";
    public static final String SCHEMA_DEFAULT = "default";
    public static final String SCHEMA_PATTERN = "pattern";
    public static final String SCHEMA_MULTIPLE_OF = "multipleOf";
    public static final String SCHEMA_MAXIMUM = "maximum";
    public static final String SCHEMA_EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    public static final String SCHEMA_MINIMUM = "minimum";
    public static final String SCHEMA_EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    public static final String SCHEMA_MAX_LENGTH = "maxLength";
    public static final String SCHEMA_MIN_LENGTH = "minLength";
    public static final String SCHEMA_MAX_ITEMS = "maxItems";
    public static final String SCHEMA_MIN_ITEMS = "minItems";
    public static final String SCHEMA_UNIQUE_ITEMS = "uniqueItems";
    public static final String SCHEMA_MAX_PROPERTIES = "maxProperties";
    public static final String SCHEMA_MIN_PROPERTIES = "minProperties";
    public static final String SCHEMA_READ_ONLY = "readOnly";
    public static final String SCHEMA_WRITE_ONLY = "writeOnly";
    public static final String SCHEMA_DEPRECATED = "deprecated";
    public static final String SCHEMA_CONST = "const";
    public static final String SCHEMA_REQUIRED = "required";
    public static final String SCHEMA_ENUM = "enum";
    public static final String SCHEMA_EXAMPLES = "examples";
    public static final String SCHEMA_PROPERTIES = "properties";
    public static final String SCHEMA_ITEMS = "items";
    public static final String SCHEMA_ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String SCHEMA_ALL_OF = "allOf";
    public static final String SCHEMA_ONE_OF = "oneOf";
    public static final String SCHEMA_ANY_OF = "anyOf";
    public static final String SCHEMA_IF = "if";
    public static final String SCHEMA_THEN = "then";
    public static final String SCHEMA_ELSE = "else";
    public static final String SCHEMA_NOT = "not";
    public static final String EXTENSION_PREFIX = "x-";

    private Constants() {
    }
}
