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
package io.ballerina.asyncapi.core.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import jakarta.ws.rs.core.MediaType;
import org.semver4j.Semver;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * This interface provides a version-agnostic abstraction over AsyncAPI 2.x and 3.0 specifications.
 */
public interface AsyncApiSpec {

    /**
     * Returns the AsyncAPI specification version used by this document.
     *
     * @return the semantic version of the AsyncAPI specification (e.g., "2.6.0", "3.0.0")
     */
    Semver getAsyncApiVersion();

    /**
     * Returns the unique identifier of the application described by this AsyncAPI document.
     *
     * @return an {@link Optional} containing the application identifier URI, or empty if not specified
     */
    Optional<URI> getAsyncApiId();

    /**
     * Returns the metadata information about the API.
     *
     * @return the API information object
     */
    AsyncApiInfo getAsyncApiInfo();

    /**
     * Returns the servers defined in this AsyncAPI document.
     *
     * @return a map of server names to their definitions
     */
    Map<String, AsyncApiServer> getAsyncApiServers();

    /**
     * Returns the default content type for message payloads.
     *
     * @return the default content type as a {@link MediaType}
     */
    MediaType getAsyncApiContentType();

    /**
     * Returns the channels defined in this AsyncAPI document.
     *
     * @return a map of channel names to their definitions
     */
    Map<String, AsyncApiChannel> getAsyncApiChannels();

    /**
     * Returns the operations defined in this AsyncAPI document.
     *
     * @return a map of operation names to their definitions
     */
    Map<String, AsyncApiOperation> getAsyncApiOperations();

    /**
     * Returns the reusable components defined in this AsyncAPI document.
     *
     * @return the components object containing reusable definitions
     */
    AsyncApiComponent getAsyncApiComponents();

    /**
     * Returns the specification extensions defined at the document level.
     *
     * @return a map of extension names (with "x-" prefix) to their JSON values
     */
    Map<String, JsonNode> getAsyncApiExtensions();
}
