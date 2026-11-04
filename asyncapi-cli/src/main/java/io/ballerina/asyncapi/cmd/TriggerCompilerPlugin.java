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
import java.util.Optional;

/**
 * Adds the trigger compiler plugin to a generated trigger package. Ballerina runs a plugin only for apps that
 * import its package directly, so the plugin has to travel inside each trigger: this writes the bundled jar and the
 * {@code CompilerPlugin.toml} that packs it, and {@code bal pack} puts both in the trigger's bala. The plugin then
 * adds the trigger's services to {@code endpoints.yaml} when an app is built with {@code --export-endpoints}.
 * <p>
 * Both files go through the same {@link OutputStage} as the generated code, so {@code --force} and
 * {@code --dry-run} treat them like any other generated file.
 */
final class TriggerCompilerPlugin {

    static final String PLUGIN_ID = "asyncapi-compiler-plugin";
    static final String COMPILER_PLUGIN_TOML = "CompilerPlugin.toml";
    static final String JAR_PATH = "compiler-plugin/asyncapi-trigger-compiler-plugin.jar";

    private static final String BUNDLED_JAR = "/trigger-plugin/asyncapi-trigger-compiler-plugin.jar";
    private static final String UNKNOWN_PLUGIN = "unknown";
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
     * @throws IOException if the bundled jar cannot be read or the files cannot be staged
     */
    static void stage(Path outputPath, Path stagingDir, CmdContext ctx) throws IOException {
        if (!Files.exists(outputPath.resolve("Ballerina.toml"))) {
            return;
        }
        Optional<String> otherPlugin = otherPluginId(outputPath.resolve(COMPILER_PLUGIN_TOML));
        if (otherPlugin.isPresent()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OTHER_COMPILER_PLUGIN, otherPlugin.get()));
            return;
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
