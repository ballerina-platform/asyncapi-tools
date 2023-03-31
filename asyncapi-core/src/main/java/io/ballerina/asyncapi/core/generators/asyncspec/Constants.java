/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.asyncapi.core.generators.asyncspec;

/**
 * Ballerina To AsyncApi Service Constants.
 */
public class Constants {
    public static final String ATTR_HOST = "host";
    public static final String SECURE_SOCKET="secureSocket";
    public static final String INT = "int";
    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String DECIMAL = "decimal";
    public static final String ARRAY = "array";
    public static final String FLOAT = "float";
    public static final String DOUBLE = "double";

    public static final String OBJECT="object";
    public static final String TYPE_REFERENCE = "type_reference";
    public static final String WEBSOCKET= "websocket";

    public static final String DISPATCHER_KEY ="dispatcherKey";
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

    public static final String SERVER_TYPE="development";
    public static final String WS="ws";
    public static final String WSS="wss";
    public static final String ASYNC_API_VERSION ="2.5.0";
    public static final String WS_PROTOCOL_VERSION="13";

    public static final String BINDING_VERSION_VALUE="0.1.0";

    public static final String DEFAULT_INFO_VERSION="1.0.0";
    public static final String PORT = "port";
    public static final String X_WWW_FORM_URLENCODED_POSTFIX = "+x-www-form-urlencoded";
    public static final String X_WWW_FORM_URLENCODED = "x-www-form-urlencoded";
    public static final String BINDING_VERSION="bindingVersion";

    public static final String QUERY="query";

    public static  final String HEADERS="headers";

    public static final String TEXT_PREFIX = "text/";
    public static final String MAP_JSON = "map<json>";
    public static final String MAP_STRING = "map<string>";
    public static final String MAP = "map";
    public static final String WEBSOCKET_CALLER = "websocket:Caller";
    public static final String WILD_CARD_CONTENT_KEY = "*/*";
    public static final String WILD_CARD_SUMMARY = "Any type of entity body";

    public static final String TUPLE = "tuple";

    public static final String SCHEMA_REFERENCE= "#/components/schemas/";

    public static final String $REF="$ref";

    public static final String MESSAGE_REFERENCE= "#/components/messages/";

    public static final String ON="on";
    public static final String X_RESPONSE= "x-response";
    public static final String X_RESPONSE_TYPE= "x-response-type";

    public static final String TYPE="type";

    public static final String PAYLOAD="payload";

    public static final String SIMPLE_RPC="simple-rpc";

    public static final String STREAMING="streaming";

    public static final String X_NULLABLE="x-nullable";

    public static final String SERVICE_CONFIG ="ServiceConfig";

    public static final String RETURN="return";

    public static final String DESCRIPTION="description";

    public static final String REMOTE_DESCRIPTION="remoteDescription";

    /**
     * Enum to select the Ballerina Type.
     * Ballerina service, mock and client generation is available
     */
    public enum BallerinaType {
        INT("int"),
        STRING("string"),
        DECIMAL("decimal"),
        BOOLEAN("boolean"),
        RECORD("record"),
        ARRAY("array");

        private String name;

        BallerinaType(String name) {
            this.name = name;
        }

        public String typeName() {
            return name;
        }

    }

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

        private String name;

        AsyncAPIType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final String SPECIAL_CHAR_REGEX = "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/\\_{}\\s|.$])";


    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";

    public static final String TRUE = "true";

    public static final String WS_LOCALHOST ="ws://localhost";
    public static final String WSS_LOCALHOST ="wss://localhost";

    public static final String WS_="ws://";

    public static final String WSS_="wss://";

    public static final String FALSE = "false";
    public static final String SLASH = "/";
    public static final String HYPHEN = "-";
    public static final String CONTRACT = "contract";
    public static final String VERSION = "'version";
    public static final String TITLE = "title";
    //TODO : Not yet done
    public static final String ASYNCAPI_ANNOTATION = "asyncapi:ServiceInfo";

    //File extensions
    public static final String YAML_EXTENSION = ".yaml";
    public static final String JSON_EXTENSION = ".json";
    public static final String YML_EXTENSION = ".yml";
    public static final String PLUS = "+";
    public static final String UNDERSCORE = "_";

    //CamelCase pattern
    public static final String CamelCasePattern = "^on[A-Z][a-z0-9A-Z]*$";

    //Invalid remote function names
    public static final String ON_IDLE_TIME_OUT="onIdleTimeOut";
    public static final String ON_MESSAGE="onMessage";
    public static final String ON_TEXT_MESSAGE="onTextMessage";
    public static final String ON_BINARY_MESSAGE="onBinaryMessage";
    public static final String ON_CLOSE="onClose";
    public static final String ON_OPEN="onOpen";
    public static final String ON_ERROR="onError";

    public static final String ON_PING="onPing";

    public static final String ON_PONG="onPong";

    //Exception Constants
    //dispatcherKey name
    public static final String DISPATCHER_KEY_TYPE_EXCEPTION = "dispatcherKey '%s' type must be a string in a record field";
    //dispatcherKey name
    //record name
    public static final String DISPATCHERKEY_OPTIONAL_EXCEPTION= "dispatcherKey '%s' cannot be optional in %s record";
    //dispatcherKey name
    //record name
    public static final String DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD="dispatcherKey '%s' is not present in %s record field, those should be equal";
    public static final String FUNCTION_SIGNATURE_ABSENT="Function signature must contain function method type ex:- onHeartbeat(Heartbeat message)";

    //type name
    //type
    public static final String FUNCTION_SIGNATURE_WRONG_TYPE= "%s type must be a record,%s given";
    public static final String FUNCTION_WRONG_NAME="Function name must start with 'on' and use camelCase convention ex-onHeartBeat,onRemoteFunctionTestName";
    public static final String FUNCTION_DEFAULT_NAME_CONTAINS_ERROR="OnIdleTimeOut, onMessage, OnTextMessage, OnBinaryMessage, OnClose, OnOpen, and OnError names are not permitted in a function's name";
    public static final String NO_ANNOTATION_PRESENT="No Annotation present, use @websocket:ServiceConfig{dispatcherKey: \"event\"} above the service";

//    public static final String NO_WEBSOCKET_SERVICE_CONFIG_ANNOTATION="No @websocket:ServiceConfig{dispatcherKey: \"event\"} annotation is present";

    public static final String NO_DISPATCHER_KEY="No dispatcherKey field is present in @websocket:ServiceConfig annotation";

    public static final String DISPATCHER_KEY_VALUE_CANNOT_BE_EMPTY="dispatcherKey cannot be empty";

    public static final String NO_TYPE_IN_STREAM="No type present in stream";

    public static final String NO_SERVICE_CLASS= "No service class present";

}
