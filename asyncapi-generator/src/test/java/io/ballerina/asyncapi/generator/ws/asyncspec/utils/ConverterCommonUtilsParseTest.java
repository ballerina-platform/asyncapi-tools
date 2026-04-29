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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import io.ballerina.asyncapi.generator.ws.asyncspec.model.AsyncApiResult;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Tests for {@link ConverterCommonUtils#parseAsyncAPIFile(String)}.
 *
 * <p>The method returns {@link AsyncApiResult}:
 * <ul>
 *   <li>On success — {@code getDiagnostics().isEmpty()} and {@code getAsyncAPI().isPresent()}</li>
 *   <li>On missing file — diagnostic {@code AAS_CONVERTOR_103} (extension check runs
 *       independently, so a missing {@code .yaml} path only adds 103)</li>
 *   <li>On wrong extension — diagnostic {@code AAS_CONVERTOR_104} (file may exist)</li>
 * </ul>
 * Both error checks are independent if-blocks; a non-existent file with a wrong extension
 * produces both 103 and 104.  Tests here choose inputs that trigger exactly one code each.
 */
class ConverterCommonUtilsParseTest {

    private static final String VALID_YAML =
            "asyncapi: 3.0.0\n"
            + "info:\n"
            + "  title: Test\n"
            + "  version: 1.0.0\n";

    private static final String VALID_JSON =
            "{\"asyncapi\":\"3.0.0\",\"info\":{\"title\":\"Test\",\"version\":\"1.0.0\"}}";

    private Path tempDir;

    @BeforeMethod
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("asyncspec-parse-test-");
    }

    @AfterMethod
    void tearDown() throws IOException {
        deleteDir(tempDir);
    }

    private static void deleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // -------------------------------------------------------------------------
    // valid inputs
    // -------------------------------------------------------------------------

    @Test
    void testParseAsyncAPIFile_validYaml_documentIsPresentAndNoDiagnostics() throws IOException {
        Path yamlFile = tempDir.resolve("test_asyncapi.yaml");
        Files.writeString(yamlFile, VALID_YAML);
        AsyncApiResult result = ConverterCommonUtils.parseAsyncAPIFile(yamlFile.toString());
        Assert.assertTrue(result.getDiagnostics().isEmpty(),
                "valid YAML must produce no diagnostics");
        Assert.assertTrue(result.getAsyncAPI().isPresent(),
                "document must be present for a valid AsyncAPI 3.0 YAML file");
    }

    @Test
    void testParseAsyncAPIFile_validJson_documentIsPresentAndNoDiagnostics() throws IOException {
        Path jsonFile = tempDir.resolve("test_asyncapi.json");
        Files.writeString(jsonFile, VALID_JSON);
        AsyncApiResult result = ConverterCommonUtils.parseAsyncAPIFile(jsonFile.toString());
        Assert.assertTrue(result.getDiagnostics().isEmpty(),
                "valid JSON must produce no diagnostics");
        Assert.assertTrue(result.getAsyncAPI().isPresent(),
                "document must be present for a valid AsyncAPI 3.0 JSON file");
    }

    // -------------------------------------------------------------------------
    // error paths
    // -------------------------------------------------------------------------

    @Test
    void testParseAsyncAPIFile_nonExistentPath_returnsDiagnosticAasConvertor103() {
        // Non-existent path with .yaml extension: only AAS_CONVERTOR_103 (extension check passes).
        String nonExistentPath = tempDir.resolve("nonexistent.yaml").toString();
        AsyncApiResult result = ConverterCommonUtils.parseAsyncAPIFile(nonExistentPath);
        Assert.assertFalse(result.getDiagnostics().isEmpty(),
                "missing file must produce at least one diagnostic");
        Assert.assertTrue(
                result.getDiagnostics().stream()
                        .anyMatch(d -> d.getCode().equals("AAS_CONVERTOR_103")),
                "missing file must produce diagnostic AAS_CONVERTOR_103");
    }

    @Test
    void testParseAsyncAPIFile_wrongExtension_returnsDiagnosticAasConvertor104() throws IOException {
        // File exists but has .txt extension: only AAS_CONVERTOR_104 (existence check passes).
        Path txtFile = tempDir.resolve("test.txt");
        Files.writeString(txtFile, VALID_YAML);
        AsyncApiResult result = ConverterCommonUtils.parseAsyncAPIFile(txtFile.toString());
        Assert.assertFalse(result.getDiagnostics().isEmpty(),
                "wrong extension must produce at least one diagnostic");
        Assert.assertTrue(
                result.getDiagnostics().stream()
                        .anyMatch(d -> d.getCode().equals("AAS_CONVERTOR_104")),
                "wrong extension must produce diagnostic AAS_CONVERTOR_104");
    }
}
