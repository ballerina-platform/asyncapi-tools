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

import java.util.List;

/**
 * The generated helpers that turn the listener's {@code webhookSecret} into the RSA public key for
 * {@code strategy: rsa}. It may be an X.509 certificate or a PEM public key ({@code -----BEGIN PUBLIC KEY-----}), as
 * Aayu MFT Gateway provides; {@code ballerina/crypto} only decodes the first, so the second is read here: its modulus
 * and exponent are taken from the DER {@code SubjectPublicKeyInfo} and passed to {@code crypto:buildRsaPublicKey}.
 */
public final class RsaPublicKeys {

    public static final String RSA_PUBLIC_KEY_FUNC = "rsaPublicKey";
    private static final String DER_ELEMENT_FUNC = "derElement";
    private static final String BASE64_URL_FUNC = "base64Url";

    private RsaPublicKeys() {
    }

    /**
     * The Ballerina source of the dispatcher methods that read the key.
     *
     * @return the method definitions
     */
    public static List<String> dispatcherMethods() {
        String publicKey = String.format(
                "private isolated function %1$s(string pem) returns crypto:PublicKey|error {"
                        + " if pem.includes(\"BEGIN CERTIFICATE\") {"
                        + " return crypto:decodeRsaPublicKeyFromContent(pem.toBytes()); }"
                        + " string[] lines = from string line in re `\\n`.split(pem)"
                        + " where !line.trim().startsWith(\"-----\") select line.trim();"
                        + " byte[] der = check array:fromBase64(string:'join(\"\", ...lines));"
                        // SEQUENCE { SEQUENCE { algorithm }, BIT STRING { SEQUENCE { INTEGER n, INTEGER e } } }
                        + " [int, int] keyInfo = check self.%2$s(der, 0, 0x30);"
                        + " [int, int] algorithm = check self.%2$s(der, keyInfo[0], 0x30);"
                        + " [int, int] bitString = check self.%2$s(der, algorithm[0] + algorithm[1], 0x03);"
                        + " [int, int] rsaKey = check self.%2$s(der, bitString[0] + 1, 0x30);"
                        + " [int, int] modulus = check self.%2$s(der, rsaKey[0], 0x02);"
                        + " [int, int] exponent = check self.%2$s(der, modulus[0] + modulus[1], 0x02);"
                        + " return crypto:buildRsaPublicKey("
                        + "self.%3$s(der.slice(modulus[0], modulus[0] + modulus[1])),"
                        + " self.%3$s(der.slice(exponent[0], exponent[0] + exponent[1]))); }",
                RSA_PUBLIC_KEY_FUNC, DER_ELEMENT_FUNC, BASE64_URL_FUNC);
        // Where a DER element's content starts and how long it is, after checking its tag.
        String derElement = String.format(
                "private isolated function %s(byte[] der, int offset, int tag) returns [int, int]|error {"
                        + " if offset + 2 > der.length() || der[offset] != tag {"
                        + " return error(\"Invalid RSA public key\"); }"
                        + " int length = der[offset + 1];"
                        + " int contentOffset = offset + 2;"
                        + " if length >= 128 {"
                        + " int count = length - 128;"
                        + " if count > 4 || contentOffset + count > der.length() {"
                        + " return error(\"Invalid RSA public key\"); }"
                        + " length = 0;"
                        + " foreach int i in 0 ..< count { length = length * 256 + der[contentOffset + i]; }"
                        + " contentOffset += count; }"
                        // A truncated key must fail here, not panic on a later slice past the end.
                        + " if contentOffset + length > der.length() { return error(\"Invalid RSA public key\"); }"
                        + " return [contentOffset, length]; }",
                DER_ELEMENT_FUNC);
        // An unsigned big-endian integer as base64url without padding, the form crypto:buildRsaPublicKey takes.
        String base64Url = String.format(
                "private isolated function %s(byte[] bytes) returns string {"
                        + " byte[] unsigned = bytes.length() > 1 && bytes[0] == 0 ? bytes.slice(1) : bytes;"
                        + " string encoded = re `[+]`.replaceAll(unsigned.toBase64(), \"-\");"
                        + " return re `=+$`.replace(re `[/]`.replaceAll(encoded, \"_\"), \"\"); }",
                BASE64_URL_FUNC);
        return List.of(publicKey, derElement, base64Url);
    }
}
