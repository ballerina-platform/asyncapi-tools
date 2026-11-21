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
 * The exit codes every Ballerina CLI tool uses, from section 8 of the Ballerina CLI Tool Design Guide. The list is
 * closed: a tool-specific failure exits with {@link #RUNTIME} and says what it is in the error's kind.
 */
public final class ExitCodes {

    /** The run succeeded, or a dry run passed every check. */
    public static final int SUCCESS = 0;
    /** Invalid input content, an unwritable destination, a write failure or any other failure. */
    public static final int RUNTIME = 1;
    /** An invalid invocation: a missing argument, an invalid value or conflicting flags. */
    public static final int USAGE = 2;
    /** The target is not a valid package or standalone file, or a .bal file is inside a package. */
    public static final int INVALID_TARGET = 3;
    /** The user declined to overwrite files, or the tool could not ask and --force was not given. */
    public static final int OVERWRITE_REFUSED = 4;
    /** The run was interrupted with SIGINT. */
    public static final int INTERRUPTED = 130;

    private ExitCodes() {
    }
}
