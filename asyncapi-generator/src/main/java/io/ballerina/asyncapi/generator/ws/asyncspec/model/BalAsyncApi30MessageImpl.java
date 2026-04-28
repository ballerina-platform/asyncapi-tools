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
package io.ballerina.asyncapi.generator.ws.asyncspec.model;

import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageImpl;

/**
 * Ballerina-specific extension of the Apicurio {@code AsyncApi30MessageImpl} used during AsyncAPI 3.0
 * spec generation.
 *
 * <p>Overrides {@code isEntity()} to return {@code false} so that Jackson does not attempt to serialise
 * this node as a nested entity when it is used transiently inside the mapper graph.
 */
public class BalAsyncApi30MessageImpl extends AsyncApi30MessageImpl {

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code false} to prevent Jackson from serialising this node as a nested entity.
     */
    @Override
    public boolean isEntity() {
        return false;
    }
}
