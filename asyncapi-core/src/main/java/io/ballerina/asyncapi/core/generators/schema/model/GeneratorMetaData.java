/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.schema.model;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;

/**
 * Stores metadata related to Ballerina types generation.
 *
 * @since 1.3.0
 */
public class GeneratorMetaData {

    private static GeneratorMetaData generatorMetaData = null;
    private final AsyncApi25DocumentImpl asyncAPI;

    private GeneratorMetaData(AsyncApi25DocumentImpl asyncAPI) {
        this.asyncAPI = asyncAPI;
    }

    public static void createInstance(AsyncApi25DocumentImpl asyncAPI) {
        generatorMetaData = new GeneratorMetaData(asyncAPI);
    }

    public static GeneratorMetaData getInstance() {
        return generatorMetaData;
    }

    public AsyncApi25DocumentImpl getAsyncAPI() {
        return asyncAPI;
    }


}
