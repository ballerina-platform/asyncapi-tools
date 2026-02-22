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
package io.ballerina.asyncapi.core.implementation.common;

import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Shared utility for iterating Apicurio {@link AsyncApiServers} and mapping each entry.
 */
public final class ServerMapper {

    private ServerMapper() {
    }

    /**
     * Iterates Apicurio {@link AsyncApiServers}, applies {@code entryMapper} to each item,
     * and collects non-null results into an insertion-ordered map.
     *
     * @param servers     the Apicurio servers object (maybe null)
     * @param entryMapper maps an Apicurio server to the common model server (may return null
     *                    to skip an entry)
     * @return the mapped servers map, or an empty map if servers is null or empty
     */
    public static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(
            AsyncApiServers servers,
            Function<AsyncApiServer,
                    io.ballerina.asyncapi.core.model.server.AsyncApiServer> entryMapper) {
        if (servers == null) {
            return Map.of();
        }
        List<String> serverNames = servers.getItemNames();
        if (serverNames == null || serverNames.isEmpty()) {
            return Map.of();
        }
        Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> result =
                new LinkedHashMap<>();
        for (String name : serverNames) {
            io.ballerina.asyncapi.core.model.server.AsyncApiServer mapped =
                    entryMapper.apply(servers.getItem(name));
            if (mapped != null) {
                result.put(name, mapped);
            }
        }
        return result;
    }
}
