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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

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

    /** What a Ballerina-code input to the CLI turned out to be. */
    public enum CodeInputKind {
        /** A directory holding a Ballerina package. */
        PACKAGE,
        /** A single .bal file outside any package. */
        STANDALONE_FILE
    }

    /**
     * Classifies a Ballerina-code input as a package or a standalone file, rejecting everything else the way
     * {@code bal run} does: a directory must be a package, and a file inside a package must be given as the
     * package instead.
     *
     * @param input path to a package directory or a .bal file
     * @return the kind of input
     * @throws AsyncApiCmdToolException if the directory is not a package or is a workspace, or the file
     *                                  belongs to a package
     */
    public static CodeInputKind resolveCodeInput(Path input) {
        Path abs = input.toAbsolutePath().normalize();
        if (Files.isDirectory(abs)) {
            if (!Files.exists(abs.resolve("Ballerina.toml"))) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_011, input.toString());
            }
            if (ProjectPaths.isWorkspaceProjectRoot(abs)) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_014, input.toString());
            }
            return CodeInputKind.PACKAGE;
        }
        Path parent = abs.getParent();
        while (parent != null) {
            if (Files.exists(parent.resolve("Ballerina.toml"))) {
                throw new AsyncApiCmdToolException(
                        AsyncApiCmdToolDiagnostic.ASYNC_CLI_012, input.toString(), parent.toString());
            }
            parent = parent.getParent();
        }
        return CodeInputKind.STANDALONE_FILE;
    }

    /**
     * Checks that a module exists in a package and normalises it: the package's own name means its default
     * module, which is returned as {@code null}.
     *
     * @param packageRoot root of the package
     * @param module      module requested by the user, may be null or blank
     * @return the module to read, or {@code null} for the default module
     * @throws AsyncApiCmdToolException if the package has no such module
     */
    public static String resolveModule(Path packageRoot, String module) {
        if (module == null || module.isBlank() || module.equals(readPackageName(packageRoot))) {
            return null;
        }
        Path modules = packageRoot.resolve("modules");
        if (Files.isDirectory(modules.resolve(module))) {
            return module;
        }
        String available = "none";
        if (Files.isDirectory(modules)) {
            try (var children = Files.list(modules)) {
                available = children.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted()
                        .collect(Collectors.joining(", "));
            } catch (IOException e) {
                available = "unknown";
            }
            if (available.isEmpty()) {
                available = "none";
            }
        }
        throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_013, module, available);
    }

    /**
     * Validates that the given path points to a Ballerina source file or package that compiles.
     *
     * @param inputPath path to the .bal file or the package directory
     * @throws AsyncApiCmdToolException if the file does not exist, is not a .bal file, or has compilation errors
     */
    public static void validateBalFile(Path inputPath) {
        Path absPath = inputPath.toAbsolutePath();
        // A package directory loads as itself; a file loads from the package that contains it, if any
        Path packageRoot = null;
        Path parent = Files.isDirectory(absPath) ? absPath : absPath.getParent();
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
