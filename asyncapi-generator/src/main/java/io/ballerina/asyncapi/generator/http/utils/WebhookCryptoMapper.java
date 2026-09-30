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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.generator.GeneratorException;

/**
 * Maps {@code x-ballerina-auth} {@code algorithm}/{@code encoding} values to the {@code ballerina/crypto}
 * function names generated code should call. Shared by server-side verification codegen and
 * client-side test-signing codegen so both accept exactly the same set of algorithms/encodings and
 * report identical errors for unsupported ones.
 */
public final class WebhookCryptoMapper {

    private WebhookCryptoMapper() {
    }

    /** Resolves a keyed-HMAC {@code crypto:hmacXxx} function name for the given algorithm. */
    public static String hmacFunctionFor(String algorithm) throws GeneratorException {
        return switch (algorithm.toLowerCase()) {
            case "sha1" -> "hmacSha1";
            case "sha256" -> "hmacSha256";
            case "sha384" -> "hmacSha384";
            case "sha512" -> "hmacSha512";
            default -> throw unsupportedAlgorithm(algorithm);
        };
    }

    /** Resolves a plain, unkeyed {@code crypto:hashXxx} function name for the given algorithm. */
    public static String hashFunctionFor(String algorithm) throws GeneratorException {
        return switch (algorithm.toLowerCase()) {
            case "sha1" -> "hashSha1";
            case "sha256" -> "hashSha256";
            case "sha384" -> "hashSha384";
            case "sha512" -> "hashSha512";
            default -> throw unsupportedAlgorithm(algorithm);
        };
    }

    /** Resolves the {@code byte[]} encoding method call (e.g. {@code "toBase16()"}) for the given encoding. */
    public static String encodeFunctionFor(String encoding) throws GeneratorException {
        return switch (encoding.toLowerCase()) {
            case "hex" -> "toBase16()";
            case "base64" -> "toBase64()";
            default -> throw new GeneratorException(String.format(
                    "Unsupported x-ballerina-auth signature encoding: '%s'. Supported values: hex, base64.",
                    encoding));
        };
    }

    private static GeneratorException unsupportedAlgorithm(String algorithm) {
        return new GeneratorException(String.format(
                "Unsupported x-ballerina-auth signature algorithm: '%s'. Supported values: "
                        + "sha1, sha256, sha384, sha512.",
                algorithm));
    }
}
