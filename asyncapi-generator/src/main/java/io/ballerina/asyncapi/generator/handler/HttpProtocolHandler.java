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
package io.ballerina.asyncapi.generator.handler;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.Protocol;
import io.ballerina.asyncapi.generator.http.HttpCodeGenerator;
import io.ballerina.asyncapi.generator.spi.ProtocolHandler;
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;

import java.nio.file.Path;

/**
 * {@link ProtocolHandler} implementation for HTTP AsyncAPI webhook generation.
 * Supports spec-to-code only; code-to-spec is not applicable for HTTP.
 */
public final class HttpProtocolHandler implements ProtocolHandler {

    @Override
    public Protocol getProtocol() {
        return Protocol.HTTP;
    }

    @Override
    public boolean supportsSpecToCode() {
        return true;
    }

    // HTTP does not support code-to-spec.
    // supportsCodeToSpec() stays false (inherited default).

    @Override
    public void specToCode(AsyncApiSpec spec,
                           Path outputPath,
                           SpecToCodeOptions options)
            throws GeneratorException {
        // --with-tests is not supported for HTTP yet.
        // options.isIncludeTestFiles() is intentionally ignored.
        new HttpCodeGenerator(spec).generate(outputPath);
    }
}
