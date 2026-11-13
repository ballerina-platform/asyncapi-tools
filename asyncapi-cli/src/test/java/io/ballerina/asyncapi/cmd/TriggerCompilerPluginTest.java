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

/**
 * Tests removing the [[tool.asyncapi]] entry an earlier version of the tool added to a trigger's Ballerina.toml.
 */
public class TriggerCompilerPluginTest {

    private static final String PACKAGE = "[package]\norg = \"demo\"\nname = \"app\"\nversion = \"0.1.0\"\n";
    private static final String OLD_ENTRY = "\n# Copies the trigger's compiler plugin from the asyncapi tool during "
            + "the build\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n";

    @Test(description = "The entry at the end of the file goes, with its comment and the blank line before it")
    public void testEntryAtTheEndIsRemoved() {
        Assert.assertEquals(TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE + OLD_ENTRY), PACKAGE);
    }

    @Test(description = "The entry between other tables goes, and the tables around it are kept as they are")
    public void testEntryInTheMiddleIsRemoved() {
        String after = "# Built with the trigger\n[[dependency]]\norg = \"ballerinax\"\nname = \"github.trigger\"\n";
        String updated = TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE + OLD_ENTRY + "\n" + after);
        Assert.assertEquals(updated, PACKAGE + "\n" + after);
    }

    @Test(description = "A comment inside the entry doesn't leave its later keys behind in the table above")
    public void testCommentInsideTheEntryIsRemoved() {
        String entry = "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\n# pinned\nversion = \"0.13.3\"\n";
        Assert.assertEquals(TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE + entry), PACKAGE);
    }

    @Test(description = "A comment just above the next table stays with that table")
    public void testCommentAboveTheNextTableIsKept() {
        String after = "# Built with the trigger\n[[dependency]]\norg = \"ballerinax\"\n";
        String updated = TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE + OLD_ENTRY + after);
        Assert.assertEquals(updated, PACKAGE + "\n" + after);
    }

    @Test(description = "An indented header after the entry ends it, so the next entry is kept")
    public void testIndentedNextHeaderIsKept() {
        String client = "  [[tool.asyncapi]]\nid = \"chat_client\"\nfilePath = \"chat.yaml\"\n";
        String updated = TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE + OLD_ENTRY + client);
        Assert.assertEquals(updated, PACKAGE + "\n" + client.stripLeading());
    }

    @Test(description = "CRLF line endings are kept")
    public void testCrlfIsKept() {
        String crlf = (PACKAGE + OLD_ENTRY).replace("\n", "\r\n");
        Assert.assertEquals(TriggerCompilerPlugin.withoutOldToolEntry(crlf), PACKAGE.replace("\n", "\r\n"));
    }

    @Test(description = "A [[tool.asyncapi]] entry for something else, such as a client, is left alone")
    public void testOtherToolEntryIsKept() {
        String content = PACKAGE + "\n[[tool.asyncapi]]\nid = \"chat_client\"\nfilePath = \"chat.yaml\"\n";
        Assert.assertEquals(TriggerCompilerPlugin.withoutOldToolEntry(content), content);
    }

    @Test(description = "A Ballerina.toml without the entry is unchanged")
    public void testNoEntryIsUnchanged() {
        Assert.assertEquals(TriggerCompilerPlugin.withoutOldToolEntry(PACKAGE), PACKAGE);
    }
}
