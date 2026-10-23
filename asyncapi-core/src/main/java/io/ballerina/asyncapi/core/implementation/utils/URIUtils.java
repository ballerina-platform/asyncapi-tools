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
package io.ballerina.asyncapi.core.implementation.utils;

import io.ballerina.asyncapi.core.Constants;

import java.net.URI;

public final class URIUtils {

    private URIUtils() {
    }

    /**
     * Converts a string to a {@link URI}.
     *
     * @param value the string to convert
     * @return the URI or null
     */
    public static URI toUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the host portion from a server URL string.
     *
     * @param url the server URL string
     * @return the host portion, or null if the URL is null
     */
    public static String parseHost(String url) {
        if (url == null) {
            return null;
        }
        int schemeEnd = url.indexOf(Constants.SCHEME_SEPARATOR);
        String afterScheme = schemeEnd >= 0 ? url.substring(schemeEnd + 3) : url;
        int pathStart = afterScheme.indexOf('/');
        return pathStart >= 0 ? afterScheme.substring(0, pathStart) : afterScheme;
    }

    /**
     * Extracts the pathname portion from a server URL string.
     *
     * @param url the server URL string
     * @return the pathname portion, or null if the URL is null or has no path
     */
    public static String parsePath(String url) {
        if (url == null) {
            return null;
        }
        int schemeEnd = url.indexOf(Constants.SCHEME_SEPARATOR);
        String afterScheme = schemeEnd >= 0 ? url.substring(schemeEnd + 3) : url;
        int pathStart = afterScheme.indexOf('/');
        return pathStart >= 0 ? afterScheme.substring(pathStart) : null;
    }
}
