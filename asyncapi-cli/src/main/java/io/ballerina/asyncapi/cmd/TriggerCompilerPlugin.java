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

import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.ast.TomlStringValueNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adds the trigger compiler plugin to a generated trigger package. Ballerina runs a plugin only for apps that
 * import its package directly, so the plugin has to travel inside each trigger: this writes the jar bundled in the
 * tool to {@code lib/} and the {@code CompilerPlugin.toml} that packs it, and {@code bal pack} puts both in the
 * trigger's bala. The plugin then adds the trigger's services to {@code endpoints.yaml} when an app is built with
 * {@code --export-endpoints}.
 * <p>
 * Both files go through the same {@link OutputStage} as the generated code, so {@code --force} and
 * {@code --dry-run} treat them like any other generated file.
 */
final class TriggerCompilerPlugin {

    static final String PLUGIN_ID = "asyncapi-compiler-plugin";
    static final String COMPILER_PLUGIN_TOML = "CompilerPlugin.toml";
    static final String BALLERINA_TOML = "Ballerina.toml";
    static final String JAR_PATH = "lib/asyncapi-trigger-compiler-plugin.jar";

    private static final String BUNDLED_JAR = "/trigger-plugin/asyncapi-trigger-compiler-plugin.jar";
    private static final String UNKNOWN_PLUGIN = "unknown";
    // The entry tool 0.13.3 added so the build copied the jar; the jar is now in lib/, so it is removed
    private static final String OLD_ENTRY_COMMENT =
            "# Copies the trigger's compiler plugin from the asyncapi tool during the build";
    private static final Pattern TOOL_HEADER = Pattern.compile("\\s*\\[\\[tool\\.asyncapi]]\\s*(#.*)?");
    private static final Pattern TRIGGER_PLUGIN_ID = Pattern.compile("\\s*id\\s*=\\s*\"trigger-plugin\".*");
    private static final String TOML_CONTENT = """
            # Written by bal asyncapi. Packs the trigger's compiler plugin, which lists the trigger's services in
            # target/artifact/endpoints.yaml when an app is built with bal build --export-endpoints.
            [plugin]
            id = "%s"
            class = "io.ballerina.asyncapi.trigger.plugin.AsyncAPICompilerPlugin"

            [[dependency]]
            path = "%s"
            """.formatted(PLUGIN_ID, JAR_PATH);

    private TriggerCompilerPlugin() {
    }

    /**
     * Stages the plugin files for a trigger generated at a package root. Code generated into a module of a bigger
     * package gets none, as the plugin belongs to the package being published.
     *
     * @param outputPath where the trigger code is generated
     * @param stagingDir the stage the generated files are written to
     * @param ctx        streams for reporting
     * @return true when the plugin is staged; {@link #isApplied} then says if the commit will keep it
     * @throws IOException if the bundled jar cannot be read or the files cannot be staged
     */
    static boolean stage(Path outputPath, Path stagingDir, CmdContext ctx) throws IOException {
        if (!Files.exists(outputPath.resolve(BALLERINA_TOML))) {
            return false;
        }
        Optional<String> otherPlugin = otherPluginId(outputPath.resolve(COMPILER_PLUGIN_TOML));
        if (otherPlugin.isPresent()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OTHER_COMPILER_PLUGIN, otherPlugin.get()));
            return false;
        }
        Path jar = stagingDir.resolve(JAR_PATH);
        Files.createDirectories(jar.getParent());
        try (InputStream bundled = TriggerCompilerPlugin.class.getResourceAsStream(BUNDLED_JAR)) {
            if (bundled == null) {
                throw new IOException("the trigger compiler plugin is missing from this build of the tool");
            }
            Files.copy(bundled, jar);
        }
        Files.writeString(stagingDir.resolve(COMPILER_PLUGIN_TOML), TOML_CONTENT);
        return true;
    }

    /**
     * Tells whether both plugin files will be written or are already up to date. If either is skipped, the old
     * {@code CompilerPlugin.toml} may still need the jar the old tool entry copies in, so that entry has to stay.
     *
     * @param stage the stage the plugin files were written to
     * @return true if neither plugin file will be skipped
     * @throws IOException if an existing file cannot be read
     */
    static boolean isApplied(OutputStage stage) throws IOException {
        return !stage.skips(Path.of(COMPILER_PLUGIN_TOML)) && !stage.skips(Path.of(JAR_PATH));
    }

    /**
     * Removes the {@code [[tool.asyncapi]]} entry an earlier version of this tool added to {@code Ballerina.toml},
     * which copied the jar during the build. The rest of the file is kept as it is.
     *
     * @param outputPath the package root
     * @param dryRun     true to only report what would change
     * @param ctx        streams for reporting
     * @throws IOException if {@code Ballerina.toml} cannot be read or written
     */
    static void removeOldToolEntry(Path outputPath, boolean dryRun, CmdContext ctx) throws IOException {
        Path ballerinaToml = outputPath.resolve(BALLERINA_TOML);
        String content = Files.readString(ballerinaToml);
        String updated = withoutOldToolEntry(content);
        if (updated.equals(content)) {
            return;
        }
        if (dryRun) {
            ctx.printInfo(ErrorMessages.TOOL_ENTRY_REMOVAL_PLANNED);
            return;
        }
        Files.writeString(ballerinaToml, updated);
        ctx.printInfo(ErrorMessages.TOOL_ENTRY_REMOVED);
    }

    /**
     * Returns the Ballerina.toml content without the trigger plugin's {@code [[tool.asyncapi]]} entry.
     *
     * @param content the current Ballerina.toml content
     * @return the updated content, the same string when there is no such entry
     */
    static String withoutOldToolEntry(String content) {
        List<String> lines = content.lines().toList();
        for (int header = 0; header < lines.size(); header++) {
            if (!TOOL_HEADER.matcher(lines.get(header)).matches()) {
                continue;
            }
            // The table runs to the next header, less the comments and blank lines just above it, which belong to it
            int next = header + 1;
            while (next < lines.size() && !lines.get(next).stripLeading().startsWith("[")) {
                next++;
            }
            int end = next;
            while (end > header + 1 && isCommentOrBlank(lines.get(end - 1))) {
                end--;
            }
            if (lines.subList(header + 1, end).stream().noneMatch(l -> TRIGGER_PLUGIN_ID.matcher(l).matches())) {
                continue;
            }
            int start = header > 0 && lines.get(header - 1).equals(OLD_ENTRY_COMMENT) ? header - 1 : header;
            // Also drops the blank line the entry was added after, so the file ends as it did before
            String nl = content.contains("\r\n") ? "\r\n" : "\n";
            String before = String.join(nl, lines.subList(0, start)).stripTrailing();
            String after = String.join(nl, lines.subList(end, lines.size())).strip();
            return after.isEmpty() ? before + nl : before + nl + nl + after + nl;
        }
        return content;
    }

    private static boolean isCommentOrBlank(String line) {
        String trimmed = line.strip();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    // The id of a plugin the package already has that isn't ours. Ours is replaced on regeneration.
    private static Optional<String> otherPluginId(Path toml) throws IOException {
        if (!Files.exists(toml)) {
            return Optional.empty();
        }
        String id = Toml.read(toml).getTable("plugin")
                .flatMap(plugin -> plugin.<TomlStringValueNode>get("id"))
                .map(TomlStringValueNode::getValue)
                .orElse(UNKNOWN_PLUGIN);
        return PLUGIN_ID.equals(id) ? Optional.empty() : Optional.of(id);
    }
}
