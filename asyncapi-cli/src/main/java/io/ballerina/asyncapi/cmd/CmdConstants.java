/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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


/**
 * Constants for the refactored asyncapi CLI commands.
 *
 */
public final class CmdConstants {

    public static final String ASYNCAPI_CMD = "asyncapi";

    /**
     * Format string used to look up per-subcommand help text via
     * {@code BLauncherCmd.getCommandUsageInfo()}. The single {@code %s} placeholder is filled
     * with the subcommand's {@link CmdType#getName()} value (e.g. {@code "asyncapi-http"}).
     */
    public static final String COMMAND_IDENTIFIER = "asyncapi-%s";

    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final String BAL_EXTENSION = ".bal";
    public static final String YAML_EXTENSION = ".yaml";
    public static final String YML_EXTENSION = ".yml";
    public static final String JSON_EXTENSION = ".json";
    public static final String FILE_EXTENSION_SEPARATOR = ".";

    /**
     * Identifies the protocol-specific sub-command, providing the name string used by the
     * Ballerina launcher and in help text lookup.
     *
     */
    public enum CmdType {
        HTTP("http"),
        WS("ws");

        private final String name;

        CmdType(String name) {
            this.name = name;
        }

        /**
         * Returns the lowercase sub-command name as registered with the Ballerina launcher.
         *
         * @return sub-command name string
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Describes the direction of code generation for a given invocation.
     *
     */
    public enum Mode {
        /** Generate Ballerina listener/client code from an AsyncAPI definition. */
        ASYNCAPI_TO_BALLERINA,
        /** Export an AsyncAPI definition from a Ballerina service. */
        BALLERINA_TO_ASYNCAPI
    }

    private CmdConstants() {
    }
}
