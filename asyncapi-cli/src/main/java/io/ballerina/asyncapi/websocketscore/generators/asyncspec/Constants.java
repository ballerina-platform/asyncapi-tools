/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.asyncspec;

/**
 * Ballerina To AsyncApi Service Constants.
 *
 */
public class Constants {
    public static final String ATTR_HOST = "host";
    public static final String SECURE_SOCKET = "secureSocket";
    public static final String INT = "int";
    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DECIMAL = "decimal";
    public static final String ARRAY = "array";
    public static final String FLOAT = "float";
    public static final String DOUBLE = "double";
    public static final String OBJECT = "object";
    public static final String TYPE_REFERENCE = "type_reference";
    public static final String WEBSOCKET = "websocket";
    public static final String DISPATCHER_KEY = "dispatcherKey";
    public static final String DISPATCHER_STREAM_ID = "dispatcherStreamId";
    public static final String BALLERINA = "ballerina";
    public static final String TYPEREFERENCE = "typeReference";
    public static final String HTTP_HEADER = "http:Header";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String OCTET_STREAM = "octet-stream";
    public static final String XML = "xml";
    public static final String JSON = "json";
    public static final String PLAIN = "plain";
    public static final String ASYNC_API_SUFFIX = "_asyncapi";
    public static final String SERVER = "server";
    public static final String SERVER_TYPE = "development";
    public static final String WS = "ws";
    public static final String ASYNC_API_VERSION = "2.5.0";
    public static final String WS_PROTOCOL_VERSION = "13";
    public static final String BINDING_VERSION_VALUE = "0.1.0";
    public static final String PORT = "port";
    public static final String BINDING_VERSION = "bindingVersion";
    public static final String QUERY = "query";
    public static final String HEADERS = "headers";
    public static final String MAP_JSON = "map<json>";
    public static final String MAP = "map";
    public static final String TUPLE = "tuple";
    public static final String SCHEMA_REFERENCE = "#/components/schemas/";
    public static final String REF = "$ref";
    public static final String ONEOF = "oneOf";
    public static final String MESSAGE_REFERENCE = "#/components/messages/";
    public static final String X_RESPONSE = "x-response";
    public static final String X_RESPONSE_TYPE = "x-response-type";
    public static final String X_REQUIRED = "x-required";
    public static final String PAYLOAD = "payload";
    public static final String SIMPLE_RPC = "simple-rpc";
    public static final String SERVER_STREAMING = "server-streaming";
    public static final String X_NULLABLE = "x-nullable";
    public static final String RETURN = "return";
    public static final String DESCRIPTION = "description";
    public static final String REMOTE_DESCRIPTION = "remoteDescription";
    public static final String SPECIAL_CHAR_REGEX = "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/\\_{}\\s|.$])";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String WS_LOCALHOST = "ws://localhost";
    public static final String WSS_LOCALHOST = "wss://localhost";
    public static final String WS_PREFIX = "ws://";
    public static final String WSS_PREFIX = "wss://";
    public static final String SLASH = "/";
    public static final String HYPHEN = "-";
    public static final String CONTRACT = "contract";
    public static final String VERSION = "'version";
    public static final String TITLE = "title";
    //TODO : Use this annotation after implementing this one
    public static final String ASYNCAPI_ANNOTATION = "asyncapi:ServiceInfo";
    public static final String YAML_EXTENSION = ".yaml";
    public static final String JSON_EXTENSION = ".json";
    public static final String YML_EXTENSION = ".yml";
    public static final String UNDERSCORE = "_";
    public static final String CAMEL_CASE_PATTERN = "^on[A-Z][a-z0-9A-Z]*$";
    public static final String ERROR = "Error";
    public static final String ON_MESSAGE = "onMessage";
    public static final String ON_TEXT_MESSAGE = "onTextMessage";
    public static final String ON_BINARY_MESSAGE = "onBinaryMessage";
    public static final String ON_CLOSE = "onClose";
    public static final String ON_OPEN = "onOpen";
    public static final String ON_PING = "onPing";
    public static final String ON_PONG = "onPong";
    public static final String ON_ERROR = "onError";
    //Exception Constants
    public static final String DISPATCHERKEY_NULLABLE_EXCEPTION = "ERROR: dispatcherKey '%s' cannot be " +
            "nullable in %s record";
    //dispatcherKey name
    public static final String DISPATCHER_KEY_TYPE_EXCEPTION = "ERROR: dispatcherKey '%s' type must be a string " +
            "in a record field";
    //dispatcherKey name
    //record name
    public static final String DISPATCHERKEY_OPTIONAL_EXCEPTION = "ERROR: dispatcherKey '%s' cannot be" +
            " optional in %s record";
    //dispatcherKey name
    //record name
    public static final String DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD = "ERROR: dispatcherKey '%s' is not present " +
            "in %s record field, those should be equal";
    //type name
    //type
    public static final String FUNCTION_SIGNATURE_WRONG_TYPE = "ERROR: %s type must be a record,%s given";
    public static final String FUNCTION_WRONG_NAME = "ERROR: Function name must start with 'on' and use camelCase " +
            "convention ex-onHeartBeat,onRemoteFunctionTestName";
    public static final String FUNCTION_PARAMETERS_EXCEEDED = "ERROR: Function name can only have two parameters," +
            " websocket:caller and type";
    public static final String FUNCTION_DEFAULT_NAME_CONTAINS_ERROR = "ERROR: onIdleTimeOut, onMessage, onTextMessage,"
            + " onBinaryMessage, onClose, onOpen, and onError names are not permitted in a function's name";
    public static final String NO_ANNOTATION_PRESENT = "ERROR: No Annotation present, " +
            "use @websocket:ServiceConfig{dispatcherKey: \"event\"} above the service";
    public static final String NO_DISPATCHER_KEY = "ERROR: No dispatcherKey field is present in " +
            "@websocket:ServiceConfig annotation";
    public static final String DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY = "ERROR: dispatcherKey cannot be empty";
    public static final String DISPATCHER_STREAM_ID_VALUE_CANNOT_BE_EMPTY = "ERROR: dispatcherStreamId cannot be empty";
    public static final String NO_TYPE_IN_STREAM = "ERROR: No type present in stream";
    public static final String NO_SERVICE_CLASS = "ERROR: No service class present";
    public static final String UNION_STREAMING_SIMPLE_RPC_ERROR = "ERROR: Response server streaming types cannot be " +
            "union with simple rpc types";
    public static final String PATH_PARAM_DASH_CONTAIN_ERROR = "ERROR: Path parameter contains an invalid" +
            " character '-'";

    // Constants related to websocket close frame
    public static final String FRAME_TYPE = "frametype";
    public static final String FRAME_TYPE_DESCRIPTION = "WS frame type";
    public static final String FRAME_TYPE_CLOSE = "close";
    public static final String CLOSE_FRAME = "CloseFrame";
    public static final String CLOSE_FRAME_DESCRIPTION = "Representation of a websocket close-frame";
    public static final String CLOSE_FRAME_TYPE = "type";
    public static final String CLOSE_FRAME_STATUS = "status";
    public static final String CLOSE_FRAME_STATUS_DESCRIPTION = "status code";
    public static final String CLOSE_FRAME_REASON = "reason";
    public static final String CLOSE_FRAME_REASON_DESCRIPTION = "Message to be sent";
    public static final String PREDEFINED_CLOSE_FRAME_TYPE = "PredefinedCloseFrameType";
    public static final String CUSTOM_CLOSE_FRAME_TYPE = "CustomCloseFrameType";
    public static final String X_BALLERINA_WS_CLOSE_FRAME = "x-ballerina-ws-closeframe";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_TYPE = "type";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_TYPE_BODY = "body";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_PATH = "path";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_PATH_FRAME_TYPE = "event.frametype";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_VALUE = "value";
    public static final String X_BALLERINA_WS_CLOSE_FRAME_VALUE_CLOSE = "close";

    /**
     * Enum to select the Ballerina Type.
     * Ballerina service, mock and client generation is available
     */
    public enum AsyncAPIType {
        INTEGER("integer"),
        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array");

        private final String name;

        AsyncAPIType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
