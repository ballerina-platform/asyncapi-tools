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

/**
 * Options controlling AsyncAPI-spec-to-Ballerina-code generation.
 */
public final class SpecToCodeOptions {

    private final String licenseHeader;
    private final boolean includeTestFiles;

    private SpecToCodeOptions(Builder builder) {
        this.licenseHeader = builder.licenseHeader;
        this.includeTestFiles = builder.includeTestFiles;
    }

    /**
     * Returns the license header to prepend to generated files.
     *
     * @return license header string, or empty string if none
     */
    public String getLicenseHeader() {
        return licenseHeader;
    }

    /**
     * Returns whether test files should be included in the generated output.
     *
     * @return true if test files should be generated
     */
    public boolean isIncludeTestFiles() {
        return includeTestFiles;
    }

    /**
     * Builder for {@link SpecToCodeOptions}.
     */
    public static class Builder {
        private String licenseHeader = "";
        private boolean includeTestFiles = false;

        /**
         * Sets the license header.
         *
         * @param licenseHeader license header text
         * @return this builder
         */
        public Builder withLicenseHeader(String licenseHeader) {
            this.licenseHeader = licenseHeader;
            return this;
        }

        /**
         * Sets whether test files should be included.
         *
         * @param includeTestFiles true to generate test files
         * @return this builder
         */
        public Builder withIncludeTestFiles(boolean includeTestFiles) {
            this.includeTestFiles = includeTestFiles;
            return this;
        }

        /**
         * Builds the {@link SpecToCodeOptions} instance.
         *
         * @return new options instance
         */
        public SpecToCodeOptions build() {
            return new SpecToCodeOptions(this);
        }
    }
}
