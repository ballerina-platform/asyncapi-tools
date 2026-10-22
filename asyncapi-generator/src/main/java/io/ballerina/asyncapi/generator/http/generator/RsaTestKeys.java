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
 * listener is configured with the matching public key, so a generated suite can exercise real
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

    /**
     * The matching PEM public key, used to configure the listener the way a provider's downloaded key is (a
     * certificate works too; the generated listener reads either).
     */
    static final String PUBLIC_KEY_PEM = String.join("\n",
            "-----BEGIN PUBLIC KEY-----",
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo+kVAxwbnmfaOadVxIGB",
            "UT5Onr99/Gd7Irpx4UHPgn9WmG3qB4oDFAW/G15w9k0iTBnzZMvmLv1r0WieoM9r",
            "RJtS2FBFfvC2T/s8Q7g/sO6HGBzpS8TVzmnSRnmv71jlqfEtI/0klvinGd8zjur2",
            "I7/RhiGdVyo3fYEnJjcjEaCPYMkS/AA99x0sykakMp+Qz414F7OQ6PS6UX0rVEId",
            "gEqXAGvjkzL6U8Znxc/PQHAZnnxcjIaXAMDpMCjaO6wwtXrIQStWe/eC2QNWb5Hv",
            "rmQ3gbSXKXZwT9d5GvExFdK7c4j64jZOaZ+D3koCb64AVD3XfuyDf4KHCrGE7ShN",
            "FwIDAQAB",
            "-----END PUBLIC KEY-----");

    private RsaTestKeys() {
    }
}
