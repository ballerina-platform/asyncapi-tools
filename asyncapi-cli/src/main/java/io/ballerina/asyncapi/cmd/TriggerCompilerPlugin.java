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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wires the trigger compiler plugin into a generated trigger package. Ballerina runs a plugin only for apps that
 * import its package directly, so the plugin has to travel inside each trigger. The jar stays in this tool: the
 * {@code [[tool.asyncapi]]} entry makes {@code bal build} and {@code bal pack} run {@link TriggerPluginBuildTool},
 * which copies the jar to {@code target/tool-cache/trigger-plugin/}, and {@code CompilerPlugin.toml} points there,
 * so {@code bal pack} puts it in the trigger's bala. The package holds only these two toml entries.
 */
final class TriggerCompilerPlugin {

    static final String PLUGIN_ID = "asyncapi-compiler-plugin";
    static final String COMPILER_PLUGIN_TOML = "CompilerPlugin.toml";
    static final String BALLERINA_TOML = "Ballerina.toml";
    static final String TOOL_ENTRY_ID = "trigger-plugin";
    static final String JAR_PATH = "target/tool-cache/" + TOOL_ENTRY_ID + "/" + TriggerPluginBuildTool.JAR_NAME;

    private static final String UNKNOWN_PLUGIN = "unknown";
    private static final String TOOL_PROPERTIES = "/asyncapi-tool.properties";
    // Where bal installs a tool: .../tool.asyncapi/<version>/java21/tool/libs/<jar>
    private static final Pattern INSTALLED_VERSION = Pattern.compile(".*[/\\\\]tool\\.asyncapi[/\\\\]([^/\\\\]+)"
            + "[/\\\\][^/\\\\]+[/\\\\]tool[/\\\\]libs[/\\\\][^/\\\\]+$");
    private static final Pattern RELEASE_VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    private static final Pattern TOOL_ENTRY = Pattern.compile(
            "(?m)^\\[\\[tool\\.asyncapi]]\\s*\\R(?:(?!\\[).*\\R?)*");
    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^(\\s*version\\s*=\\s*)\"[^\"]*\"");
    private static final String TOML_CONTENT = """
            # Written by bal asyncapi. Packs the trigger's compiler plugin, which lists the trigger's services in
            # target/artifact/endpoints.yaml when an app is built with bal build --export-endpoints. The jar is
            # copied there from the asyncapi tool during the build, by the [[tool.asyncapi]] entry in Ballerina.toml.
            [plugin]
            id = "%s"
            class = "io.ballerina.asyncapi.trigger.plugin.AsyncAPICompilerPlugin"

            [[dependency]]
            path = "%s"
            """.formatted(PLUGIN_ID, JAR_PATH);

    private TriggerCompilerPlugin() {
    }

    /**
     * Stages {@code CompilerPlugin.toml} for a trigger generated at a package root. Code generated into a module of
     * a bigger package gets none, as the plugin belongs to the package being published.
     *
     * @param outputPath where the trigger code is generated
     * @param stagingDir the stage the generated files are written to
     * @param ctx        streams for reporting
     * @return true when the plugin is wired in, so {@link #addToolEntry} should follow the commit
     * @throws IOException if an existing {@code CompilerPlugin.toml} cannot be read or the file cannot be staged
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
        Files.writeString(stagingDir.resolve(COMPILER_PLUGIN_TOML), TOML_CONTENT);
        return true;
    }

    /**
     * Adds the {@code [[tool.asyncapi]]} entry to the package's {@code Ballerina.toml}, or moves an existing entry
     * to this tool's version, so the build copies this version's plugin jar. The rest of the file is kept as it is.
     *
     * @param outputPath the package root
     * @param dryRun     true to only report what would change
     * @param ctx        streams for reporting
     * @throws IOException if {@code Ballerina.toml} cannot be read or written
     */
    static void addToolEntry(Path outputPath, boolean dryRun, CmdContext ctx) throws IOException {
        Path ballerinaToml = outputPath.resolve(BALLERINA_TOML);
        String content = Files.readString(ballerinaToml);
        String version = toolVersion();
        String updated = withToolEntry(content, version);
        if (updated.equals(content)) {
            return;
        }
        if (dryRun) {
            ctx.printInfo(String.format(ErrorMessages.TOOL_ENTRY_PLANNED, version));
            return;
        }
        Files.writeString(ballerinaToml, updated);
        ctx.printInfo(String.format(ErrorMessages.TOOL_ENTRY_WRITTEN, version));
    }

    /**
     * Returns the Ballerina.toml content with a {@code [[tool.asyncapi]]} entry for the given tool version.
     *
     * @param content the current Ballerina.toml content
     * @param version the tool version the entry should name
     * @return the updated content, the same string when nothing changes
     */
    static String withToolEntry(String content, String version) {
        Matcher entry = TOOL_ENTRY.matcher(content);
        if (entry.find()) {
            String block = entry.group();
            Matcher versionLine = VERSION_LINE.matcher(block);
            String newBlock = versionLine.find()
                    ? versionLine.replaceFirst("$1\"" + Matcher.quoteReplacement(version) + "\"")
                    : block.stripTrailing() + System.lineSeparator() + "version = \"" + version + "\""
                            + System.lineSeparator();
            return content.substring(0, entry.start()) + newBlock + content.substring(entry.end());
        }
        // One blank line before the entry, however the file ended
        String nl = System.lineSeparator();
        return content.stripTrailing() + nl + nl + String.join(nl, List.of(
                "# Copies the trigger's compiler plugin from the asyncapi tool during the build",
                "[[tool.asyncapi]]", "id = \"" + TOOL_ENTRY_ID + "\"", "version = \"" + version + "\"", ""));
    }

    /**
     * Returns this tool's version: the folder bal installed it in, or the version it was built as.
     *
     * @return the tool version, without a SNAPSHOT or timestamp suffix
     */
    static String toolVersion() {
        try {
            Path jar = Path.of(TriggerCompilerPlugin.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI());
            Matcher installed = INSTALLED_VERSION.matcher(jar.toString());
            if (installed.matches()) {
                return installed.group(1);
            }
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            // not installed as a bal tool, so the version it was built as is used
        }
        Properties properties = new Properties();
        try (InputStream in = TriggerCompilerPlugin.class.getResourceAsStream(TOOL_PROPERTIES)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // falls through to the default below
        }
        return releaseVersion(properties.getProperty("version", "0.0.0"));
    }

    /**
     * Returns the x.y.z part of a built version, dropping a -SNAPSHOT or CI timestamp suffix.
     *
     * @param builtVersion the version the tool was built as, such as 0.13.3-20261110-142100-1da1237
     * @return the release version, such as 0.13.3
     */
    static String releaseVersion(String builtVersion) {
        Matcher release = RELEASE_VERSION.matcher(builtVersion);
        return release.lookingAt() ? release.group() : builtVersion;
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
