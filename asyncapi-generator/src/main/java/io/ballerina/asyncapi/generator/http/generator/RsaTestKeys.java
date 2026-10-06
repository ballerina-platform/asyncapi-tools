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
package io.ballerina.asyncapi.generator.http.generator;

/**
 * A fixed, throwaway RSA key pair for the generated dispatch tests of specs that verify a public-key
 * signature ({@code strategy: rsa}). The test client signs with the private key and the test
 * listener is configured with the matching certificate, so a generated suite can exercise real
 * signature verification without any provider credentials.
 *
 * <p>This key pair protects nothing: it exists only inside generated test code and is not used
 * anywhere else.
 */
final class RsaTestKeys {

    /** PKCS#8 PEM private key used by the generated test client to sign requests. */
    static final String PRIVATE_KEY_PEM = String.join("\n",
            "-----BEGIN PRIVATE KEY-----",
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCj6RUDHBueZ9o5",
            "p1XEgYFRPk6ev338Z3siunHhQc+Cf1aYbeoHigMUBb8bXnD2TSJMGfNky+Yu/WvR",
            "aJ6gz2tEm1LYUEV+8LZP+zxDuD+w7ocYHOlLxNXOadJGea/vWOWp8S0j/SSW+KcZ",
            "3zOO6vYjv9GGIZ1XKjd9gScmNyMRoI9gyRL8AD33HSzKRqQyn5DPjXgXs5Do9LpR",
            "fStUQh2ASpcAa+OTMvpTxmfFz89AcBmefFyMhpcAwOkwKNo7rDC1eshBK1Z794LZ",
            "A1Zvke+uZDeBtJcpdnBP13ka8TEV0rtziPriNk5pn4PeSgJvrgBUPdd+7IN/gocK",
            "sYTtKE0XAgMBAAECggEAMQOajm8BzUkV5yakTZpCXtaIcQ0nMeqJ1lU1h5wD3uyQ",
            "Kw23LWD2Ua70Ok1v7x8asfISp+IXJ5cNfjXQNZtA4uelitzaIz787YbduwxmM1To",
            "nevLUaZ/HvXi9MMfuq11I+/kRT5GCkU5xtFJXCPjzcXm9DqqD64moMeVuuZC/Npx",
            "YYFCz86uZTwH9avbKhWhV8jH0cTgVJkwXA1vLe44feyOVMl/Ofv73SX2aZGJ9noN",
            "5ipDT62A4CPUwqKUbZpTHCWDUnocAUxmBciZEnj5oeglkrtFGNsl4hfcNtZ5+6e6",
            "ZUZczOrpOQSrIhZOd8jdrEl14n6RPgZTjJtyZnqBkQKBgQDTwp0MUsEshIzXuYvq",
            "3FnGFauJOplIJpHHavmF+Xn6TDQI0DwE0KlOd/pJzVowsI9HlaTMQEV1/8HQarvL",
            "KGBCsdf7pjjFAlNmCG+E2eb81poEC3jTVuds1t0xDnJ/yL8tSY6/ShalFxRSmeRz",
            "C/GQcDabEWLKICKJOVAf5FzAJwKBgQDGJ1z2RdgpnxpJdNHVmW5/LzzqLGepM/Yp",
            "YEaxAivE6ePoQkgF9JKLAp2w1hJwYMKYM8cUH3AkPdqRVDz3j05MQb3pY7SZH+t7",
            "51gulBR7ecx3uug5NDhblcw5ZWywDvJPQnQ2jF1DoPYwSLwAyZBAnTgYdbWebdxe",
            "azjfnGgxkQKBgQCnJqA2wP/QdxSBl+mUyrhmPma3nLSvmhpKEevCUAlzhEEyj8RA",
            "fMCXuuaq6Nh/RG7kr3905mqt2OMQ858mnslU+/KjpjJ7d/mCubflYucMvwy1kqSe",
            "FaP5aqUQevnwWfJl+gEeh4nWaKBXDzifg1b7j0fbIV8ccz8vmDjh460+0wKBgHZk",
            "jHmkr4vmwPkEsF70Jn4dRkMQNvt0zW5ZVMNr7aTgrkhWWdwdDfW6oWdH8IpudbYk",
            "sZzHT+SHhHDyqN6tI/YSDZtF9GDNHpDQX/KsjTRdSJp89UVAey/VZ8kfXXov4/0R",
            "UohJA2xl3tJoktPRmrvQc/TBV7uKHnHXlIqeU+7RAoGBAMQINLReFs+kbVTP6Uo/",
            "UtMjlVJI5mcyYdAw4qaqirm6+dXh6RlyAjV8qN8KW5+Zyu3+lQ4ga4DWzSWbbMFG",
            "WJMDDiS1CeFWwQiBahR5ESt048mQ8OKxM+xP4xA9ao61LfT2koZ5Um5T4p0b3IW7",
            "dc+jvxmU0n66FG07r/uOfNWV",
            "-----END PRIVATE KEY-----");

    /** X.509 certificate PEM (self-signed, holding the matching public key) used to configure the listener. */
    static final String CERTIFICATE_PEM = String.join("\n",
            "-----BEGIN CERTIFICATE-----",
            "MIIDHzCCAgegAwIBAgIUZpuLZsikcHGoQQ/U+zzZW1oQOa8wDQYJKoZIhvcNAQEL",
            "BQAwHjEcMBoGA1UEAwwTYXN5bmNhcGktdG9vbHMtdGVzdDAgFw0yNjEwMDYwNTE0",
            "NDhaGA8yMTI2MDkxMjA1MTQ0OFowHjEcMBoGA1UEAwwTYXN5bmNhcGktdG9vbHMt",
            "dGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKPpFQMcG55n2jmn",
            "VcSBgVE+Tp6/ffxneyK6ceFBz4J/Vpht6geKAxQFvxtecPZNIkwZ82TL5i79a9Fo",
            "nqDPa0SbUthQRX7wtk/7PEO4P7Duhxgc6UvE1c5p0kZ5r+9Y5anxLSP9JJb4pxnf",
            "M47q9iO/0YYhnVcqN32BJyY3IxGgj2DJEvwAPfcdLMpGpDKfkM+NeBezkOj0ulF9",
            "K1RCHYBKlwBr45My+lPGZ8XPz0BwGZ58XIyGlwDA6TAo2jusMLV6yEErVnv3gtkD",
            "Vm+R765kN4G0lyl2cE/XeRrxMRXSu3OI+uI2Tmmfg95KAm+uAFQ9137sg3+Chwqx",
            "hO0oTRcCAwEAAaNTMFEwHQYDVR0OBBYEFHkh+xquwERJZ7zklp+fsMQSrROgMB8G",
            "A1UdIwQYMBaAFHkh+xquwERJZ7zklp+fsMQSrROgMA8GA1UdEwEB/wQFMAMBAf8w",
            "DQYJKoZIhvcNAQELBQADggEBAEQZ1KFc+uJl4zGFp3VioqQFcuPOjh7Gf3+3CtyS",
            "GltL4NOwYZY/n0Az7oaoaUcB7KZD8GlNgHNZev2xCgGrWuHX41CnD7JDcAzPTQsT",
            "jJXKZQpTBtK+hnHonZfAF/XqI7+7FHJHpkLphFLZSfGngIxGi+JmHrbripKnS31s",
            "Lo/ekIEyi8AjuHRinBMiZiUDhRyVDwzU4mSKMjmi1hbuJY3wJGmAxHZrNkY/ADHu",
            "VZxV6Qg7sMbSakELVRwx4J8cXCO+AmPIC0HPvYXy/EOGTvv5pdI8XtmBPlV+L7Te",
            "+EdF/8qusGBkizuwbdoGUQV6t6MyrRXKWmEzIWf93yunQVw=",
            "-----END CERTIFICATE-----");

    private RsaTestKeys() {
    }
}
