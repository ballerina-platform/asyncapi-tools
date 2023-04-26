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

    private final AsyncApi25DocumentImpl asyncAPI;
    private final boolean nullable;
    private final boolean generateServiceType;
    private static GeneratorMetaData generatorMetaData = null;

    private GeneratorMetaData(AsyncApi25DocumentImpl asyncAPI, boolean nullable, boolean generateServiceType) {
        this.asyncAPI = asyncAPI;
        this.nullable = nullable;
        this.generateServiceType = generateServiceType;
    }

    public static void createInstance(AsyncApi25DocumentImpl openAPI, boolean nullable, boolean generateServiceType) {
        generatorMetaData = new GeneratorMetaData(openAPI, nullable, generateServiceType);
    }

    public static GeneratorMetaData getInstance() {
        return generatorMetaData;
    }

    public AsyncApi25DocumentImpl getAsyncAPI() {
        return asyncAPI;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isServiceTypeRequired() {
        return generateServiceType;
    }
}
