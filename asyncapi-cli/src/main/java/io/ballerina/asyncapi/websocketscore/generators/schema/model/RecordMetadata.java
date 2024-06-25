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
package io.ballerina.asyncapi.websocketscore.generators.schema.model;

import io.ballerina.compiler.syntax.tree.RecordRestDescriptorNode;

/**
 * RecordMetadata class for containing the details to generate record node. This contains the details with whether
 * record is opened record or not, and its restField details.
 *
 */
public class RecordMetadata {
    private final boolean isOpenRecord;
    private final RecordRestDescriptorNode restDescriptorNode;

    RecordMetadata(Builder builder) {
        this.isOpenRecord = builder.isOpenRecord;
        this.restDescriptorNode = builder.restDescriptorNode;
    }

    public boolean isOpenRecord() {
        return isOpenRecord;
    }

    public RecordRestDescriptorNode getRestDescriptorNode() {
        return restDescriptorNode;
    }

    public static class Builder {
        private boolean isOpenRecord = false;
        private RecordRestDescriptorNode restDescriptorNode = null;

        public Builder withIsOpenRecord(boolean isOpenRecord) {
            this.isOpenRecord = isOpenRecord;
            return this;
        }

        public Builder withRestDescriptorNode(RecordRestDescriptorNode restDescriptorNode) {
            this.restDescriptorNode = restDescriptorNode;
            return this;
        }

        public RecordMetadata build() {
            return new RecordMetadata(this);
        }
    }
}
