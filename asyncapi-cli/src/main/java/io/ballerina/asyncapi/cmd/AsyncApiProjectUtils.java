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

import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.toml.syntax.tree.SyntaxTree;
import io.ballerina.toml.syntax.tree.TableNode;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for inspecting and navigating Ballerina project structures.
 */
public final class AsyncApiProjectUtils {

    private AsyncApiProjectUtils() {
    }

    /**
     * Reads the package name from the Ballerina.toml at the given project root.
     *
     * @param projectPath root of the Ballerina project
     * @return the package name declared in Ballerina.toml
     * @throws AsyncApiCmdToolException if the file cannot be read or the name is missing
     */
    public static String readPackageName(Path projectPath) {
        try {
            TextDocument configDocument = TextDocuments.from(
                    Files.readString(projectPath.resolve("Ballerina.toml")));
            SyntaxTree syntaxTree = SyntaxTree.from(configDocument);
            DocumentNode rootNode = syntaxTree.rootNode();
            for (DocumentMemberDeclarationNode member : rootNode.members()) {
                if (member instanceof TableNode tableNode) {
                    if (tableNode.identifier().toSourceCode().trim().equals("package")) {
                        for (KeyValueNode field : tableNode.fields()) {
                            if (field.identifier().toSourceCode().trim().equals("name")) {
                                return field.value().toSourceCode().trim().replaceAll("\"", "");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new AsyncApiCmdToolException(e, AsyncApiCmdToolDiagnostic.ASYNC_CLI_008);
        }
        throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_008);
    }

    /**
     * Resolves the output directory for generated files given a project root and optional module name.
     *
     * @param effectivePath root of the Ballerina project
     * @param module        optional module name (may be null or empty)
     * @param warningStream stream to receive non-fatal warnings
     * @return resolved output path
     * @throws AsyncApiCmdToolException if the path cannot be determined
     */
    public static Path resolveOutputPath(Path effectivePath, String module,
                                          PrintStream warningStream) {
        // Walk up to nearest existing ancestor so packageRoot() can function
        Path searchPath = effectivePath.toAbsolutePath().normalize();
        while (searchPath != null && !Files.exists(searchPath)) {
            searchPath = searchPath.getParent();
        }

        Path rootPath;
        try {
            if (searchPath == null) {
                throw new ProjectException("no existing ancestor found");
            }
            rootPath = ProjectPaths.packageRoot(searchPath);
        } catch (ProjectException e) {
            Path check = searchPath;
            while (check != null) {
                if (ProjectPaths.isWorkspaceProjectRoot(check)) {
                    throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_003);
                }
                check = check.getParent();
            }
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_004);
        }
        if (ProjectPaths.isWorkspaceProjectRoot(rootPath)) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_003);
        }
        if (!ProjectPaths.isBuildProjectRoot(rootPath)) {
            throw new AsyncApiCmdToolException(
                    AsyncApiCmdToolDiagnostic.ASYNC_CLI_005, rootPath.toString());
        }
        String packageName = readPackageName(rootPath);
        if (module == null || module.isBlank()) {
            Path rel = rootPath.normalize()
                    .relativize(effectivePath.toAbsolutePath().normalize());
            if (rel.toString().isEmpty()) {
                return rootPath;
            }
            if (rel.getNameCount() >= 2 && rel.getName(0).toString().equals("modules")) {
                return effectivePath;
            }
            throw new AsyncApiCmdToolException(
                    AsyncApiCmdToolDiagnostic.ASYNC_CLI_010, effectivePath.toString());
        }
        if (packageName.equals(module)) {
            return rootPath;
        }
        String fullModule = packageName + "." + module;
        if (!ProjectUtils.validateModuleName(fullModule)) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_006, module);
        }
        if (!ProjectUtils.validateNameLength(fullModule)) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_007, module);
        }
        return rootPath.resolve("modules").resolve(module);
    }

    /**
     * Validates that the given path points to a readable Ballerina source file.
     *
     * @param inputPath path to the .bal file
     * @throws AsyncApiCmdToolException if the file does not exist, is not a .bal file, or has compilation errors
     */
    public static void validateBalFile(Path inputPath) {
        Path absPath = inputPath.toAbsolutePath();
        // Walk up to check if this .bal file belongs to a package
        Path parent = absPath.getParent();
        Path packageRoot = null;
        while (parent != null) {
            if (Files.exists(parent.resolve("Ballerina.toml"))) {
                packageRoot = parent;
                break;
            }
            parent = parent.getParent();
        }

        BuildOptions buildOptions = BuildOptions.builder().setOffline(true).build();
        // For a package file load from the package root; for a standalone file ProjectLoader
        // detects no Ballerina.toml and loads it as a single-file project.
        Path loadPath = packageRoot != null ? packageRoot : absPath;
        DiagnosticResult result = ProjectLoader.load(loadPath, buildOptions)
                .project()
                .currentPackage()
                .getCompilation()
                .diagnosticResult();

        if (result.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic diagnostic : result.errors()) {
                sb.append(System.lineSeparator()).append(diagnostic.toString());
            }
            throw new AsyncApiCmdToolException(
                    AsyncApiCmdToolDiagnostic.ASYNC_CLI_009, sb.toString());
        }
    }
}
