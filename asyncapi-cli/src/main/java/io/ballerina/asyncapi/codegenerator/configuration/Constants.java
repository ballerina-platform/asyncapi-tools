/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.codegenerator.configuration;

import java.util.List;

/**
 * This file contains the constants which will be used in the code.
 */
public final class Constants {
    public static final String EMPTY_BALLERINA_FILE_CONTENT = "";
    public static final String DATA_TYPES_BAL_FILE_NAME = "data_types.bal";
    public static final String LISTENER_BAL_FILE_NAME = "listener.bal";
    public static final String SERVICE_TYPES_BAL_FILE_NAME = "service_types.bal";
    public static final String DISPATCHER_SERVICE_BAL_FILE_NAME = "dispatcher_service.bal";
    public static final String DISPATCHER_SERVICE_BAL_FILE_NAME_FOR_EVENT_IDENTIFIER_IN_BODY =
            "dispatcher_service.bal";
    public static final String DISPATCHER_SERVICE_BAL_FILE_NAME_FOR_EVENT_IDENTIFIER_IN_HEADER =
            "dispatcher_service_for_event_identifier_in_header.bal";
    public static final List<String> BAL_KEYWORDS;
    public static final List<String> BAL_TYPES;
    public static final String ESCAPE_PATTERN = "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/\\_{}\\s|.$])";
    //TODO Update keywords if Ballerina Grammer changes
    private static final String[] KEYWORDS = new String[]{"abort", "aborted", "abstract", "all", "annotation",
            "any", "anydata", "boolean", "break", "byte", "catch", "channel", "check", "checkpanic", "client",
            "committed", "const", "continue", "decimal", "else", "error", "external", "fail", "final", "finally",
            "float", "flush", "fork", "function", "future", "handle", "if", "import", "in", "int", "is", "join",
            "json", "listener", "lock", "match", "new", "object", "OBJECT_INIT", "onretry", "parameter", "panic",
            "private", "public", "record", "remote", "resource", "retries", "retry", "return", "returns", "service",
            "source", "start", "stream", "string", "table", "transaction", "try", "type", "typedesc", "typeof",
            "trap", "throw", "wait", "while", "with", "worker", "var", "version", "xml", "xmlns", "BOOLEAN_LITERAL",
            "NULL_LITERAL", "ascending", "descending", "foreach", "map", "group", "from", "default", "field",
            "limit", "as", "on", "isolated", "readonly", "distinct", "where", "select", "do", "transactional"
            , "commit", "enum", "base16", "base64", "rollback", "configurable", "class", "module", "never",
            "outer", "order", "null", "key", "let", "by", "equals"};
    private static final String[] TYPES = new String[]{"int", "any", "anydata", "boolean", "byte", "float", "int",
            "json", "string", "table", "var", "xml"};

    static {
        BAL_KEYWORDS = List.of(KEYWORDS);
        BAL_TYPES = List.of(TYPES);
    }

    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DECIMAL = "decimal";
    public static final String ARRAY = "array";
    public static final String RECORD = "record";
    public static final String OBJECT = "object";
    public static final String FLOAT = "float";
    public static final String DOUBLE = "double";

    public static final String X_BALLERINA_EVENT_TYPE = "x-ballerina-event-type";
    public static final String X_BALLERINA_SERVICE_TYPE = "x-ballerina-service-type";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER = "x-ballerina-event-identifier";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE = "type";
    public static final String X_BALLERINA_EVENT_TYPE_HEADER = "header";
    public static final String X_BALLERINA_EVENT_TYPE_BODY = "body";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH = "path";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME = "name";
    public static final String CLONE_WITH_TYPE_VAR_NAME = "genericDataType";
    public static final String INTEROP_INVOKE_FUNCTION_NAME = "executeRemoteFunc";
    public static final String LISTENER_SERVICE_TYPE_FILTER_FUNCTION_NAME = "getServiceTypeStr";
    public static final String DISPATCHER_SERVICE_RESOURCE_FILTER_FUNCTION_NAME = "matchRemoteFunc";
    public static final String DISPATCHER_SERVICE_POST_FUNCTION_NAME = "post";
    public static final String REMOTE_FUNCTION_NAME_PREFIX = "on";
    public static final String SERVICE_TYPE_NAME_SUFFIX = "Service";

    public static final String SELF_KEYWORD = "self";
    public static final String GENERIC_SERVICE_TYPE = "GenericServiceType";
    public static final String GENERIC_DATA_TYPE = "GenericDataType";

    private Constants() {
    }
}
