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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Tests the [[tool.asyncapi]] entry, the tool version and the build tool that copies the plugin jar.
 */
public class TriggerCompilerPluginTest {

    private static final String PACKAGE = "[package]\norg = \"demo\"\nname = \"app\"\nversion = \"0.1.0\"\n";

    @Test(description = "A Ballerina.toml without the entry gets it appended, with everything else kept")
    public void testEntryIsAppended() {
        String updated = TriggerCompilerPlugin.withToolEntry(PACKAGE, "1.2.3");
        Assert.assertTrue(updated.startsWith(PACKAGE.stripTrailing()), updated);
        Assert.assertFalse(updated.replace("\r\n", "\n").contains("\n\n\n"), "One blank line: " + updated);
        Assert.assertTrue(updated.contains("[[tool.asyncapi]]"), updated);
        Assert.assertTrue(updated.contains("id = \"trigger-plugin\""), updated);
        Assert.assertTrue(updated.contains("version = \"1.2.3\""), updated);
    }

    @Test(description = "An existing entry moves to the new version, and nothing after it changes")
    public void testEntryVersionIsUpdated() {
        String after = "\n[[dependency]]\norg = \"ballerinax\"\nname = \"github.trigger\"\nversion = \"0.2.0\"\n";
        String content = PACKAGE + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.0.1\"\n" + after;
        String updated = TriggerCompilerPlugin.withToolEntry(content, "1.2.3");
        Assert.assertTrue(updated.contains("version = \"1.2.3\""), updated);
        Assert.assertFalse(updated.contains("0.0.1"), updated);
        Assert.assertTrue(updated.endsWith(after), "The dependency's version must not change: " + updated);
    }

    @Test(description = "An entry without a version gets one")
    public void testEntryWithoutVersionGetsOne() {
        String updated = TriggerCompilerPlugin.withToolEntry(
                PACKAGE + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\n", "1.2.3");
        Assert.assertTrue(updated.contains("id = \"trigger-plugin\"\r\nversion = \"1.2.3\"")
                || updated.contains("id = \"trigger-plugin\"\nversion = \"1.2.3\""), updated);
    }

    @Test(description = "An entry that already names the version is left alone")
    public void testUpToDateEntryIsUnchanged() {
        String content = PACKAGE + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"1.2.3\"\n";
        Assert.assertEquals(TriggerCompilerPlugin.withToolEntry(content, "1.2.3"), content);
    }

    @Test(description = "Outside a bal tool install, the version is the one the tool was built as")
    public void testToolVersionIsTheBuiltVersion() {
        String version = TriggerCompilerPlugin.toolVersion();
        Assert.assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), version);
    }

    @Test(description = "A snapshot or CI timestamped build version becomes its release version")
    public void testReleaseVersionDropsTheBuildSuffix() {
        Assert.assertEquals(TriggerCompilerPlugin.releaseVersion("0.13.3-SNAPSHOT"), "0.13.3");
        Assert.assertEquals(TriggerCompilerPlugin.releaseVersion("0.13.3-20261110-142100-1da1237"), "0.13.3");
        Assert.assertEquals(TriggerCompilerPlugin.releaseVersion("0.13.3"), "0.13.3");
    }

    @Test(description = "The build tool copies the plugin jar bundled in the tool")
    public void testBuildToolCopiesThePlugin() throws Exception {
        Path dir = Files.createTempDirectory("tool-cache");
        Path jar = TriggerPluginBuildTool.copyPlugin(dir.resolve("trigger-plugin"));
        Assert.assertEquals(jar.getFileName().toString(), "asyncapi-trigger-compiler-plugin.jar");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Assert.assertNotNull(zip.getEntry("io/ballerina/asyncapi/trigger/plugin/AsyncAPICompilerPlugin.class"),
                    "The jar should hold the plugin class");
        }
    }
}
