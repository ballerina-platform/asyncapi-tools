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
package io.ballerina.asyncapi.core;

import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.implementation.v2.AsyncApiSpecV2;
import io.ballerina.asyncapi.core.implementation.v3.AsyncApiSpecV3;

/**
 * Utility that creates the appropriate {@link AsyncApiSpec} implementation
 * based on the version of the parsed AsyncAPI document.
 */
public final class AsyncApiSpecCreator {

    private AsyncApiSpecCreator() {

    }

    /**
     * Creates an {@link AsyncApiSpec} instance for the given AsyncAPI document.
     *
     * @param asyncApiDocument the parsed AsyncAPI document
     * @return the version-specific {@link AsyncApiSpec} implementation
     * @throws AsyncApiParserException if the document version is unsupported
     */
    public static AsyncApiSpec create(AsyncApiDocument asyncApiDocument) throws AsyncApiParserException {
        String version = asyncApiDocument.getAsyncapi();
        if (version != null && version.startsWith("2.")) {
            return new AsyncApiSpecV2(asyncApiDocument);
        }
        if (version != null && version.startsWith("3.")) {
            return new AsyncApiSpecV3(asyncApiDocument);
        }
        throw new AsyncApiParserException(
                "Unsupported AsyncAPI version: " + version);
    }
}
