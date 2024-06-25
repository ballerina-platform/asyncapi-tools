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

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;

/**
 * Because JsonNodes are used for bindings in the Apicurio data model, we must use Jackson ObjectMapper to
 * transform a Java object to a Jackson ObjectNode. But by default true values will not be excluded when try to convert
 * Java objects to Json nodes, therefore this approach has overridden the original Apicurio isEntity function
 * and changed it to false,thus it will not be included in the output AsyncApi specification.
 *
 */
public class BalAsyncApi25MessageImpl extends AsyncApi25MessageImpl {
    @Override
    public boolean isEntity() {
        return false;
    }

}
