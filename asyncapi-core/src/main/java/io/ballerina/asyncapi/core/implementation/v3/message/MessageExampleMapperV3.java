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
package io.ballerina.asyncapi.core.implementation.v3.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageExample;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageTrait;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio message examples to model
 * {@link AsyncApiMessageExample} for AsyncAPI 3.0.
 */
final class MessageExampleMapperV3 {

    private MessageExampleMapperV3() {
    }

    /**
     * Maps message examples from an AsyncAPI 3.0 message.
     *
     * @param message the Apicurio message object
     * @return a list of mapped examples, or null if not available
     */
    static List<AsyncApiMessageExample> map(AsyncApi30Message message) {
        if (message == null) {
            return null;
        }
        List<AsyncApi30MessageExample> examples = message.getExamples();
        if (examples == null) {
            return null;
        }
        return examples.stream().map(MessageExampleMapperV3::mapMessageExampleItem).toList();
    }

    /**
     * Maps message examples from an AsyncAPI 3.0 message trait.
     *
     * @param trait the Apicurio message trait object
     * @return a list of mapped examples, or null if not available
     */
    static List<AsyncApiMessageExample> map(AsyncApi30MessageTrait trait) {
        if (trait == null) {
            return null;
        }
        List<AsyncApi30MessageExample> examples = trait.getExamples();
        if (examples == null) {
            return null;
        }
        return examples.stream().map(MessageExampleMapperV3::mapMessageExampleItem).toList();
    }

    /**
     * Maps an Apicurio {@link AsyncApi30MessageExample} to a model
     * {@link AsyncApiMessageExample}.
     *
     * @param example the Apicurio message example object
     * @return the mapped AsyncApiMessageExample, or null if input is null
     */
    private static AsyncApiMessageExample mapMessageExampleItem(AsyncApi30MessageExample example) {
        if (example == null) {
            return null;
        }
        Map<String, Object> headers = null;
        Map<String, JsonNode> apicurioHeaders = example.getHeaders();
        if (apicurioHeaders != null) {
            headers = new LinkedHashMap<>(apicurioHeaders);
        }
        return new AsyncApiMessageExample(
                headers,
                example.getPayload(),
                example.getName(),
                example.getSummary(),
                example.getExtensions()
        );
    }
}
