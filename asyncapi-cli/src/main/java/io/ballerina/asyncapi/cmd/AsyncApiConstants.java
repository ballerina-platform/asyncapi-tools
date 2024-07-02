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

package io.ballerina.asyncapi.cmd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds constants used in the asyncapi commands.
 *
 */
public class AsyncApiConstants {
    public static final Set<String> VALID_HTTP_NAMES = new HashSet<>(Arrays.asList("http", "https"));
    public static final Set<String> VALID_WS_NAMES = new HashSet<>(Arrays.asList("ws", "wss", "websocket"));
    public static final String LICENSE_FLAG = "--license";
    public static final String SERVICE_FLAG = "--service";
    public static final String TEST_FLAG = "--with-tests";
    public static final String JSON_FLAG = "--json";
    public static final String INPUT_FLAG = "--input";
    public static final String OUTPUT_FLAG = "--output";
    public static final String INPUT_FLAG_ALT = "-i";
    public static final String OUTPUT_FLAG_ALT = "-o";
    public static final String PROTOCOL_FLAG = "--protocol";
    public static final String SPEC = "spec";
    public static final String CLIENT = "client";
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String EXPERIMENTAL_WARNING = "WARNING The support for websockets protocol is currently " +
            "an experimental feature, and its behavior may be subject to change in future releases." + LINE_SEPARATOR;
}
