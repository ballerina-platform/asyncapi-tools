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

import io.ballerina.asyncapi.generator.Protocol;

/**
 * CLI-side behaviour of one protocol: where output goes and what is reported to the user. What the protocol
 * can do (directions, tests, license) is declared by its generator {@code ProtocolHandler}, not here.
 */
interface ProtocolCommand {

    /**
     * Returns the protocol this command serves.
     *
     * @return the protocol
     */
    Protocol protocol();

    /**
     * Runs one generation. Input has already been validated and unsupported options already warned about.
     *
     * @param opts the resolved options
     * @param mode the direction of generation
     * @param ctx  streams and helpers
     */
    void generate(CmdOptions opts, CmdConstants.Mode mode, CmdContext ctx);
}
