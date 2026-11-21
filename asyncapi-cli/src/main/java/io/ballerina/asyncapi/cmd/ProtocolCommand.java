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

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.Protocol;

import java.io.IOException;

/**
 * CLI-side behaviour of one protocol: where output goes and what is reported to the user. What the protocol
 * can do (directions, tests, license) is declared by its generator {@code ProtocolHandler}, not here.
 */
interface ProtocolCommand {

    /**
     * What the command worked out before generating: the checked license, and for Ballerina code the loaded
     * input.
     *
     * @param license   the license header, or an empty string
     * @param codeInput the Ballerina code to read, or {@code null} when generating from a definition
     */
    record Resolved(String license, AsyncApiProjectUtils.CodeInput codeInput) {
    }

    /**
     * Returns the protocol this command serves.
     *
     * @return the protocol
     */
    Protocol protocol();

    /**
     * Runs one generation. The arguments, the input and the license have already been checked, and unsupported
     * options warned about.
     *
     * @param opts     the resolved options
     * @param mode     the direction of generation
     * @param resolved what was worked out before generating
     * @param ctx      where warnings and the result are printed
     * @throws AsyncApiCmdToolException if the output location is not valid or files can't be replaced
     * @throws GeneratorException       if the input can't be generated from
     * @throws IOException              if the generated files can't be written
     */
    void generate(CmdOptions opts, CmdConstants.Mode mode, Resolved resolved, CmdContext ctx)
            throws GeneratorException, IOException;
}
