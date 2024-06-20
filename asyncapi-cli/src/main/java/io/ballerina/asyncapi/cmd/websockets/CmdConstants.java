/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.asyncapi.cmd.websockets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants for asyncapi commands.
 *
 * @since 1.2.0
 */
public class CmdConstants {
    public static final String BAL_EXTENSION = ".bal";
    public static final Map<String, String> TYPE_MAP;

    /**
     * Enum to select the code generation mode.
     * Ballerina service, mock and client generation is available
     */
    public enum GenType {
        GEN_SERVICE("gen_service"),
        GEN_CLIENT("gen_client"),
        GEN_BOTH("gen_both");

        private String name;

        GenType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
    public static final String DEFAULT_CLIENT_PKG = "client";
    public static final String CLIENT_FILE_NAME = "client.bal";
    public static final String TYPE_FILE_NAME = "types.bal";
    public static final String TEST_DIR = "tests";
    public static final String TEST_FILE_NAME = "test.bal";
    public static final String CONFIG_FILE_NAME = "Config.toml";
    public static final String DEFAULT_MOCK_PKG = "mock";

    static {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("integer", "int");
        typeMap.put("string", "string");
        typeMap.put("boolean", "boolean");
        typeMap.put("array", "[]");
        typeMap.put("object", "record {}");
        typeMap.put("decimal", "decimal");
        typeMap.put("number", "decimal");
        typeMap.put("double", "decimal");
        typeMap.put("float", "float");
        typeMap.put("binary", "byte[]");
        typeMap.put("byte", "byte[]");
        TYPE_MAP = Collections.unmodifiableMap(typeMap);
    }

}
