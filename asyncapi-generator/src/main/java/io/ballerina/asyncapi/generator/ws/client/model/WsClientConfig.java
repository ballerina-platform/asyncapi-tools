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
package io.ballerina.asyncapi.generator.ws.client.model;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;

/**
 * Configuration model for WebSocket client code generation.
 */
public class WsClientConfig {

    private final AsyncApiSpec asyncApi;
    private final String licenseHeader;

    private WsClientConfig(Builder builder) {
        this.asyncApi = builder.asyncApi;
        this.licenseHeader = builder.licenseHeader;
    }

    /**
     * Returns the parsed AsyncAPI specification.
     *
     * @return the AsyncAPI spec
     */
    public AsyncApiSpec getAsyncApi() {
        return asyncApi;
    }

    /**
     * Returns the license header to prepend to generated {@code .bal} files.
     *
     * @return the license header string
     */
    public String getLicenseHeader() {
        return licenseHeader;
    }

    /**
     * Builder for {@link WsClientConfig}.
     */
    public static class Builder {

        private AsyncApiSpec asyncApi;
        private String licenseHeader = "";

        /**
         * Sets the parsed AsyncAPI specification.
         *
         * @param asyncApi the spec to use during generation
         * @return this builder
         */
        public Builder withAsyncApi(AsyncApiSpec asyncApi) {
            this.asyncApi = asyncApi;
            return this;
        }

        /**
         * Sets the license header to prepend to generated {@code .bal} files.
         *
         * @param licenseHeader the license header string
         * @return this builder
         */
        public Builder withLicense(String licenseHeader) {
            this.licenseHeader = licenseHeader;
            return this;
        }

        /**
         * Builds a new {@link WsClientConfig} instance.
         *
         * @return the built config
         */
        public WsClientConfig build() {
            return new WsClientConfig(this);
        }
    }
}
