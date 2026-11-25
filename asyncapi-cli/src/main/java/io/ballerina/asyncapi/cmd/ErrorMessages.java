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
 * Warning and error messages shared by the asyncapi command. Each is one line that starts in lowercase and has
 * no trailing period; the command adds the {@code bal asyncapi: warning:} or {@code error:} prefix.
 */
public final class ErrorMessages {

    public static final String WARN_OPTION_ONLY_FOR_CODE_OUTPUT =
            "%s only applies when generating code from an AsyncAPI definition, so it is ignored";

    public static final String WARN_OPTION_ONLY_FOR_SPEC_OUTPUT =
            "%s only applies when generating a definition from Ballerina code, so it is ignored";

    public static final String WARN_OPTION_UNSUPPORTED = "%s is not supported for the %s protocol, so it is ignored";

    public static final String WARN_FORMAT_RENAMED = "--format is renamed to --spec-format";

    public static final String WARN_HTTP_TESTS_NEED_AUTH =
            "--with-tests generated no tests: they send signed events, so the definition needs an "
                    + "x-ballerina-auth block";

    public static final String WARN_OTHER_COMPILER_PLUGIN =
            "the package already has a compiler plugin (%s), so the trigger's plugin was not added and "
                    + "bal build --export-endpoints will not list this trigger's services";

    public static final String WARN_WRITTEN_BEFORE_FAILURE = "these files were written before the failure: %s";

    public static final String TARGET_IS_DIRECTORY =
            "%s is a directory, so the generated file cannot be written there";

    public static final String UNEXPECTED_FAILURE = "unexpected failure: ";

    private ErrorMessages() {
    }
}
