/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.client.model;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;

/**
 * This class stores metadata that related to client code generations.
 *
 */
public class AasClientConfig {
    private final AsyncApi25DocumentImpl asyncAPI;
    private final String license;

    private AasClientConfig(Builder clientConfigBuilder) {
        this.asyncAPI = clientConfigBuilder.asyncAPI;
        this.license = clientConfigBuilder.license;
    }

    public AsyncApi25DocumentImpl getAsyncAPI() {
        return asyncAPI;
    }

    public String getLicense() {
        return license;
    }

    /**
     * Client IDL plugin meta data builder class.
     */
    public static class Builder {
        private AsyncApi25DocumentImpl asyncAPI;
        private String license = "// AUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
                "// This file is auto-generated by the Ballerina AsyncAPI tool.\n";

        public Builder withAsyncApi(AsyncApi25DocumentImpl asyncAPI) {
            this.asyncAPI = asyncAPI;
            return this;
        }

        public Builder withLicense(String license) {
            this.license = license;
            return this;
        }

        public AasClientConfig build() {
            return new AasClientConfig(this);
        }
    }
}
