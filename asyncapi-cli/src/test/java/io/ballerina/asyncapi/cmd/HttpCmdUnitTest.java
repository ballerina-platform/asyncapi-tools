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
package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for {@code --protocol http}: input validation, the capability checks and warn-and-continue handling
 * of options the protocol does not support. Generation itself is covered by the generator tests.
 */
public class HttpCmdUnitTest extends CmdTestBase {

    @Test(description = "A non-existent spec prints the invalid path error")
    public void testInvalidInputPath() {
        parse("/non/existent/file.yaml", "--protocol", "http").execute();
        Assert.assertTrue(getErr().contains("error: invalid input path '/non/existent/file.yaml'"),
                "Expected invalid path error. Err: " + getErr());
    }

    @Test(description = "A .bal input is rejected because http cannot generate a definition from code")
    public void testBalInputRejected() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "http").execute();
        Assert.assertTrue(getErr().contains("The http protocol does not support Ballerina-to-AsyncAPI"),
                "Expected the capability error. Err: " + getErr());
    }

    @Test(description = "--service with a .bal input is rejected the same way")
    public void testServiceWithBalInputRejected() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "http", "--service", "MyService").execute();
        Assert.assertTrue(getErr().contains("does not support Ballerina-to-AsyncAPI"),
                "Expected the capability error. Err: " + getErr());
    }

    @Test(description = "--with-tests warns that http does not support it, and generation is still attempted")
    public void testWithTestsWarnsAndContinues() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--with-tests").execute();
        Assert.assertTrue(getErr().contains("warning: --with-tests is not supported for the http protocol"),
                "Expected the unsupported option warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("ERROR:"),
                "Generation should still be attempted after the warning. Err: " + getErr());
    }

    @Test(description = "--license warns that http does not support it, and generation is still attempted")
    public void testLicenseWarnsAndContinues() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        parse(specFile.toString(), "--protocol", "http", "--license", licenseFile.toString()).execute();
        Assert.assertTrue(getErr().contains("warning: --license is not supported for the http protocol"),
                "Expected the unsupported option warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("ERROR:"),
                "Generation should still be attempted after the warning. Err: " + getErr());
    }

    @Test(description = "--json and --service warn when generating code from a spec")
    public void testSpecOutputOnlyOptionsWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--json", "--service", "MyService").execute();
        Assert.assertTrue(getErr().contains("warning: --json only applies"),
                "Expected the --json warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: --service only applies"),
                "Expected the --service warning. Err: " + getErr());
    }

    @Test(description = "No warning is printed when only supported options are given")
    public void testNoWarningForSupportedOptions() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--output", tmpDir.resolve("out").toString()).execute();
        Assert.assertFalse(getErr().contains("warning:"), "Unexpected warning. Err: " + getErr());
    }
}
