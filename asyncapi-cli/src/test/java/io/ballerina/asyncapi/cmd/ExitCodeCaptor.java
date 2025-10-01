/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import io.ballerina.asyncapi.cmd.AsyncApiCmd.ExitHandler;

/**
 * Test helper for capturing exit codes in tests.
 */
public class ExitCodeCaptor implements ExitHandler {
    private int exitCode = -1;
    private boolean exitCalled = false;

    @Override
    public void exit(int code) {
        this.exitCode = code;
        this.exitCalled = true;
    }

    public int getExitCode() {
        if (!exitCalled) {
            throw new IllegalStateException("exit() was not called");
        }
        return exitCode;
    }

    public boolean wasExitCalled() {
        return exitCalled;
    }
}
