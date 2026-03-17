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
package io.ballerina.asyncapi.generator.http;

import io.ballerina.compiler.syntax.tree.SyntaxInfo;

import java.util.List;

/**
 * Constants used across the HTTP code generator.
 */
public final class Constants {

    public static final String X_BALLERINA_EVENT_TYPE = "x-ballerina-event-type";
    public static final String X_BALLERINA_SERVICE_TYPE = "x-ballerina-service-type";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER = "x-ballerina-event-identifier";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE = "type";
    public static final String X_BALLERINA_EVENT_TYPE_HEADER = "header";
    public static final String X_BALLERINA_EVENT_TYPE_BODY = "body";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH = "path";
    public static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME = "name";

    // AsyncAPI schema primitive type identifiers
    public static final String SCHEMA_TYPE_INTEGER = "integer";
    public static final String SCHEMA_TYPE_STRING = "string";
    public static final String SCHEMA_TYPE_BOOLEAN = "boolean";
    public static final String SCHEMA_TYPE_NUMBER = "number";
    public static final String SCHEMA_TYPE_DECIMAL = "decimal";
    public static final String SCHEMA_TYPE_FLOAT = "float";
    public static final String SCHEMA_TYPE_DOUBLE = "double";
    public static final String SCHEMA_TYPE_ARRAY = "array";
    public static final String SCHEMA_TYPE_OBJECT = "object";

    public static final String GENERIC_SERVICE_TYPE = "GenericServiceType";
    public static final String GENERIC_DATA_TYPE = "GenericDataType";
    public static final String CLONE_WITH_TYPE_VAR_NAME = "genericDataType";
    public static final String EXECUTE_REMOTE_FUNC_NAME = "executeRemoteFunc";
    public static final String LISTENER_GET_SERVICE_TYPE_FUNC = "getServiceTypeStr";
    public static final String DISPATCHER_MATCH_REMOTE_FUNC = "matchRemoteFunc";
    public static final String LISTENER_CONFIG_TYPE = "ListenerConfiguration";
    public static final String HTTP_MODULE = "http";
    public static final String BALLERINA_ORG = "ballerina";
    public static final String ESCAPE_PATTERN =
            "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/\\_{}\\s|.$])";
    public static final String REMOTE_FUNCTION_NAME_PREFIX = "on";
    public static final String SERVICE_TYPE_NAME_SUFFIX = "Service";
    public static final String DISPATCHER_SERVICE_CLASS_NAME = "DispatcherService";
    public static final String NATIVE_HANDLER_ORG = "ballerinax";
    public static final String NATIVE_HANDLER_MODULE_ALIAS = "handler";
    public static final String NATIVE_HANDLER_TYPE = "NativeHandler";
    public static final String DISPATCHER_SERVICES_FIELD = "services";
    public static final String DISPATCHER_NATIVE_HANDLER_FIELD = "nativeHandler";
    public static final String ADD_SERVICE_REF_FUNC = "addServiceRef";
    public static final String REMOVE_SERVICE_REF_FUNC = "removeServiceRef";
    public static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT = "genericEvent";
    public static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT_NAME = "eventName";
    public static final String EXECUTE_REMOTE_FUNC_PARAM_SERVICE_TYPE = "serviceTypeStr";
    public static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT_FUNC = "eventFunction";
    public static final String EXECUTE_REMOTE_FUNC_LOCAL_SERVICE = "genericService";
    // Listener class identifiers
    public static final String LISTENER_CLASS_NAME = "Listener";
    public static final String LISTENER_HTTP_LISTENER_FIELD = "httpListener";
    public static final String LISTENER_DISPATCHER_SERVICE_FIELD = "dispatcherService";

    // Generated Ballerina output file names
    public static final String DATA_TYPES_BAL = "types.bal";
    public static final String SERVICE_TYPES_BAL = "service_types.bal";
    public static final String LISTENER_BAL = "listener.bal";
    public static final String DISPATCHER_SERVICE_BAL = "dispatcher_service.bal";

    public static final List<String> BAL_KEYWORDS = SyntaxInfo.keywords();

    private Constants() {
    }
}
