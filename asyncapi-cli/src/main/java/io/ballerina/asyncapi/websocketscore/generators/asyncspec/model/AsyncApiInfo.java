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
package io.ballerina.asyncapi.websocketscore.generators.asyncspec.model;

import java.util.Optional;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils.normalizeTitle;

/**
 * This {@code AsyncAPIInfo} contains details related to AsyncApi info section.
 *
 */
public class AsyncApiInfo {
    private final String title;
    private final String version;
    private final String contractPath;

    public AsyncApiInfo(AsyncAPIInfoBuilder asyncAPIInfoBuilder) {
        this.title = asyncAPIInfoBuilder.title;
        this.version = asyncAPIInfoBuilder.version;
        this.contractPath = asyncAPIInfoBuilder.contractPath;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(normalizeTitle(this.title));
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(this.version);
    }

    public Optional<String> getContractPath() {
        return Optional.ofNullable(this.contractPath);
    }

    /**
     * This is the builder class for the {@link AsyncApiInfo}.
     */
    public static class AsyncAPIInfoBuilder {
        private String title;
        private String version;
        private String contractPath;

        public AsyncAPIInfoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AsyncAPIInfoBuilder version(String version) {
            this.version = version;
            return this;
        }

        public AsyncAPIInfoBuilder contractPath(String contractPath) {
            this.contractPath = contractPath;
            return this;
        }

        public AsyncApiInfo build() {
            AsyncApiInfo asyncAPIInfo = new AsyncApiInfo(this);
            return asyncAPIInfo;
        }
    }
}
