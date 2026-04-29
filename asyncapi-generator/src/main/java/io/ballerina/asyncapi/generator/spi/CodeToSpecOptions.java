/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.spi;

import java.io.PrintStream;

/**
 * Options controlling Ballerina-code-to-AsyncAPI-spec generation.
 */
public final class CodeToSpecOptions {

    private final String serviceName;
    private final boolean needJson;
    private final PrintStream outStream;

    private CodeToSpecOptions(Builder builder) {
        this.serviceName = builder.serviceName;
        this.needJson = builder.needJson;
        this.outStream = builder.outStream;
    }

    /**
     * Returns the name of the Ballerina service to export, or null for all services.
     *
     * @return service name, or null
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns whether the output spec should be JSON (true) or YAML (false).
     *
     * @return true for JSON output
     */
    public boolean isNeedJson() {
        return needJson;
    }

    /**
     * Returns the stream to use for informational output during generation.
     *
     * @return output print stream
     */
    public PrintStream getOutStream() {
        return outStream;
    }

    /**
     * Builder for {@link CodeToSpecOptions}.
     */
    public static class Builder {
        private String serviceName = null;
        private boolean needJson = false;
        private PrintStream outStream = System.out;

        /**
         * Sets the service name to export.
         *
         * @param serviceName Ballerina service name, or null for all services
         * @return this builder
         */
        public Builder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * Sets whether JSON output is required.
         *
         * @param needJson true for JSON, false for YAML
         * @return this builder
         */
        public Builder withNeedJson(boolean needJson) {
            this.needJson = needJson;
            return this;
        }

        /**
         * Sets the output stream for generation messages.
         *
         * @param outStream print stream
         * @return this builder
         */
        public Builder withOutStream(PrintStream outStream) {
            this.outStream = outStream;
            return this;
        }

        /**
         * Builds the {@link CodeToSpecOptions} instance.
         *
         * @return new options instance
         */
        public CodeToSpecOptions build() {
            return new CodeToSpecOptions(this);
        }
    }
}
