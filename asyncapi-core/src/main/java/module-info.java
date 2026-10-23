/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

module io.ballerina.asyncapi.core {
    requires apicurio.data.models;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.semver4j;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;

    exports io.ballerina.asyncapi.core;
    exports io.ballerina.asyncapi.core.api;
    exports io.ballerina.asyncapi.core.model.channel;
    exports io.ballerina.asyncapi.core.model.component;
    exports io.ballerina.asyncapi.core.model.doc;
    exports io.ballerina.asyncapi.core.model.info;
    exports io.ballerina.asyncapi.core.model.message;
    exports io.ballerina.asyncapi.core.model.operation;
    exports io.ballerina.asyncapi.core.model.security;
    exports io.ballerina.asyncapi.core.model.server;
    exports io.ballerina.asyncapi.core.model.tag;
}
