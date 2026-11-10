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

import io.ballerina.projects.buildtools.CodeGeneratorTool;
import io.ballerina.projects.buildtools.ToolConfig;
import io.ballerina.projects.buildtools.ToolContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Runs as a build tool when a trigger's {@code Ballerina.toml} lists {@code [[tool.asyncapi]]}: before the package
 * is compiled and packed, it copies the compiler plugin jar bundled in this tool to
 * {@code target/tool-cache/<id>/}, where the trigger's {@code CompilerPlugin.toml} points.
 */
@ToolConfig(name = "asyncapi")
public class TriggerPluginBuildTool implements CodeGeneratorTool {

    static final String JAR_NAME = "asyncapi-trigger-compiler-plugin.jar";
    private static final String BUNDLED_JAR = "/trigger-plugin/" + JAR_NAME;

    @Override
    public void execute(ToolContext context) {
        try {
            copyPlugin(context.cachePath());
        } catch (IOException e) {
            report(context, "failed to copy the trigger compiler plugin: " + e.getMessage());
        }
    }

    /**
     * Copies the plugin jar bundled in this tool into the given directory.
     *
     * @param directory where the trigger's {@code CompilerPlugin.toml} expects the jar
     * @return the copied jar
     * @throws IOException if the jar is missing from this tool or cannot be written
     */
    static Path copyPlugin(Path directory) throws IOException {
        Path target = directory.resolve(JAR_NAME);
        try (InputStream bundled = TriggerPluginBuildTool.class.getResourceAsStream(BUNDLED_JAR)) {
            if (bundled == null) {
                throw new IOException("the trigger compiler plugin is missing from this build of the asyncapi tool");
            }
            Files.createDirectories(directory);
            Files.copy(bundled, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static void report(ToolContext context, String message) {
        DiagnosticInfo info = new DiagnosticInfo("ASYNCAPI_TOOL_001", message, DiagnosticSeverity.ERROR);
        context.reportDiagnostic(DiagnosticFactory.createDiagnostic(info, NO_LOCATION));
    }

    // The failure isn't tied to a line of source
    private static final Location NO_LOCATION = new Location() {
        @Override
        public LineRange lineRange() {
            LinePosition start = LinePosition.from(0, 0);
            return LineRange.from("Ballerina.toml", start, start);
        }

        @Override
        public TextRange textRange() {
            return TextRange.from(0, 0);
        }
    };
}
