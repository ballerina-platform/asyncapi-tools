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
 * Immutable value object holding the resolved, validated options for one asyncapi sub-command
 * invocation. Instances are created exclusively via {@link CmdOptionsBuilder} and are passed
 * directly to {@link SubCmdBase#generate(CmdOptions, CmdConstants.Mode)}.
 *
 */
public class CmdOptions {

    private final String input;
    private final String output;
    private final String licenseFilePath;
    private final String service;
    private final boolean json;
    private final boolean withTests;
    private final String module;

    private CmdOptions(CmdOptionsBuilder builder) {
        this.input = builder.input;
        this.output = builder.output;
        this.licenseFilePath = builder.licenseFilePath;
        this.service = builder.service;
        this.json = builder.json;
        this.withTests = builder.withTests;
        this.module = builder.module;
    }

    /**
     * Returns the path to the input AsyncAPI definition file.
     *
     * @return input file path, or {@code null} if not provided
     */
    public String getInput() {
        return input;
    }

    /**
     * Returns the path to the output directory for generated files.
     *
     * @return output directory path, or {@code null} if not provided
     */
    public String getOutput() {
        return output;
    }

    /**
     * Returns the path to the license header file to prepend to generated files.
     *
     * @return license file path, or {@code null} if not provided
     */
    public String getLicenseFilePath() {
        return licenseFilePath;
    }

    /**
     * Returns the optional service name filter; {@code null} means generate all services.
     *
     * @return service name, or {@code null}
     */
    public String getService() {
        return service;
    }

    /**
     * Returns {@code true} if JSON-format output was requested.
     *
     * @return whether to emit JSON output
     */
    public boolean isJson() {
        return json;
    }

    /**
     * Returns {@code true} if test scaffolding should be generated alongside service files.
     *
     * @return whether to generate tests
     */
    public boolean isWithTests() {
        return withTests;
    }

    /**
     * Returns the target module name inside the Ballerina project.
     *
     * @return module name, or {@code null} if not provided
     */
    public String getModule() {
        return module;
    }

    /**
     * Builder for {@link CmdOptions}.
     *
     */
    public static class CmdOptionsBuilder {

        private String input;
        private String output;
        private String licenseFilePath;
        private String service;
        private boolean json;
        private boolean withTests;
        private String module;

        /**
         * Sets the input AsyncAPI definition file path.
         *
         * @param input file path
         * @return this builder
         */
        public CmdOptionsBuilder withInput(String input) {
            this.input = input;
            return this;
        }

        /**
         * Sets the output directory path.
         *
         * @param output directory path
         * @return this builder
         */
        public CmdOptionsBuilder withOutput(String output) {
            this.output = output;
            return this;
        }

        /**
         * Sets the license header file path.
         *
         * @param path license file path
         * @return this builder
         */
        public CmdOptionsBuilder withLicenseFilePath(String path) {
            this.licenseFilePath = path;
            return this;
        }

        /**
         * Sets the service name filter.
         *
         * @param service service name
         * @return this builder
         */
        public CmdOptionsBuilder withService(String service) {
            this.service = service;
            return this;
        }

        /**
         * Sets whether JSON-format output is requested.
         *
         * @param json {@code true} to emit JSON output
         * @return this builder
         */
        public CmdOptionsBuilder withJson(boolean json) {
            this.json = json;
            return this;
        }

        /**
         * Sets whether test scaffolding should be generated.
         *
         * @param withTests {@code true} to generate tests
         * @return this builder
         */
        public CmdOptionsBuilder withTests(boolean withTests) {
            this.withTests = withTests;
            return this;
        }

        /**
         * Sets the target module name inside the Ballerina project.
         *
         * @param module module name, or {@code null} if not provided
         * @return this builder
         */
        public CmdOptionsBuilder withModule(String module) {
            this.module = module;
            return this;
        }

        /**
         * Constructs the {@link CmdOptions} from the values accumulated on this builder.
         *
         * @return a new immutable {@link CmdOptions}
         */
        public CmdOptions build() {
            return new CmdOptions(this);
        }
    }
}
