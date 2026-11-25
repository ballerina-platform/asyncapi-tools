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

import io.ballerina.asyncapi.cmd.AsyncApiCmdToolException.InputDiagnostic;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.environment.PackageLockingMode;
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.toml.syntax.tree.SyntaxTree;
import io.ballerina.toml.syntax.tree.TableNode;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for inspecting and navigating Ballerina project structures.
 */
public final class AsyncApiProjectUtils {

    private static final String DEPENDENCIES_TOML = "Dependencies.toml";
    // ERR-6: report every independent error, up to a cap
    private static final int MAX_DIAGNOSTICS = 50;

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
                    throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_003,
                            effectivePath.toString());
                }
                check = check.getParent();
            }
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_004,
                    effectivePath.toString());
        }
        if (ProjectPaths.isWorkspaceProjectRoot(rootPath)) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_003,
                    effectivePath.toString());
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
        /** A directory holding a Ballerina package, on its own or in a workspace. */
        PACKAGE,
        /** A single .bal file outside any package. */
        STANDALONE_FILE
    }

    /**
     * A Ballerina-code input, loaded with the Package API.
     *
     * @param kind        a package or a standalone file
     * @param packageRoot the package root, or the file for a standalone file
     * @param pkg         the package to compile: for a workspace, the member the input names
     * @param project     the loaded project
     */
    public record CodeInput(CodeInputKind kind, Path packageRoot, Package pkg, Project project) {
    }

    /**
     * The options a tool that compiles Ballerina code loads it with: the dependency versions locked in
     * Dependencies.toml, never newer ones, and without going online (CMP-1).
     *
     * @return the build options
     */
    public static BuildOptions compileOptions() {
        return BuildOptions.builder().setOffline(true).setLockingMode(PackageLockingMode.HARD).build();
    }

    /**
     * Loads a Ballerina-code input with the Package API and accepts only a package or a standalone file, the way
     * {@code bal run} does: a file inside a package must be given as the package, and a workspace as one of its
     * packages (TGT-1 to TGT-7).
     *
     * @param input path to a package directory or a .bal file
     * @return the loaded input
     * @throws AsyncApiCmdToolException if the input is not a package or a standalone file
     */
    public static CodeInput resolveCodeInput(Path input) {
        Path abs = input.toAbsolutePath().normalize();
        if (!Files.exists(abs)) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_017, input.toString());
        }
        Project project;
        try {
            project = ProjectLoader.load(abs, compileOptions()).project();
        } catch (ProjectException e) {
            throw notAPackage(input, abs, e);
        }
        Path root = project.sourceRoot().toAbsolutePath().normalize();
        switch (project.kind()) {
            case SINGLE_FILE_PROJECT:
                return new CodeInput(CodeInputKind.STANDALONE_FILE, abs, project.currentPackage(), project);
            case BUILD_PROJECT:
                if (!root.equals(abs)) {
                    throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_012, input.toString(),
                            root.toString());
                }
                return new CodeInput(CodeInputKind.PACKAGE, root, project.currentPackage(), project);
            case WORKSPACE_PROJECT:
                return workspaceMember((WorkspaceProject) project, input, abs);
            default:
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_015, input.toString());
        }
    }

    // A workspace root lists its packages; a package of a workspace is that member alone (TGT-7).
    private static CodeInput workspaceMember(WorkspaceProject workspace, Path input, Path abs) {
        List<String> members = new ArrayList<>();
        for (BuildProject member : workspace.projects()) {
            Path memberRoot = member.sourceRoot().toAbsolutePath().normalize();
            if (memberRoot.equals(abs)) {
                return new CodeInput(CodeInputKind.PACKAGE, memberRoot, member.currentPackage(), workspace);
            }
            if (abs.startsWith(memberRoot)) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_012, input.toString(),
                        memberRoot.toString());
            }
            members.add(member.currentPackage().packageName().value());
        }
        Collections.sort(members);
        throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_014, input.toString(),
                String.join(", ", members));
    }

    // The Package API can't load it: either a directory inside a package, or not Ballerina code at all.
    private static AsyncApiCmdToolException notAPackage(Path input, Path abs, ProjectException cause) {
        if (Files.isDirectory(abs)) {
            try {
                Path root = ProjectPaths.packageRoot(abs);
                return new AsyncApiCmdToolException(cause, AsyncApiCmdToolDiagnostic.ASYNC_CLI_012, input.toString(),
                        root.toString());
            } catch (ProjectException e) {
                // not inside a package either
            }
        }
        return new AsyncApiCmdToolException(cause, AsyncApiCmdToolDiagnostic.ASYNC_CLI_011, input.toString());
    }

    /**
     * Checks that a package locks its dependencies, as compiling without Dependencies.toml would pick the latest
     * compatible versions instead (CMP-2). A standalone file has no dependencies to lock.
     *
     * @param codeInput    the loaded input
     * @param inputCommand the input as the user gave it, for the {@code bal build} command to run
     * @throws AsyncApiCmdToolException if the package has no Dependencies.toml
     */
    public static void checkDependenciesLocked(CodeInput codeInput, String inputCommand) {
        if (codeInput.kind() != CodeInputKind.PACKAGE || codeInput.pkg().dependenciesToml().isPresent()
                || Files.exists(codeInput.packageRoot().resolve(DEPENDENCIES_TOML))
                || Files.exists(codeInput.project().sourceRoot().resolve(DEPENDENCIES_TOML))) {
            return;
        }
        throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_016,
                codeInput.pkg().packageName().value()).withCommand("bal build " + inputCommand);
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
        AsyncApiCmdToolException error =
                new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_013, module, available);
        throw "none".equals(available)
                ? error.withSuggestion("The package has no modules; leave out --module.") : error;
    }

    /**
     * Compiles the input and fails with every compilation error, each at its place in the source.
     *
     * @param codeInput the loaded input
     * @param input     the input as the user gave it
     * @throws AsyncApiCmdToolException if the code has compilation errors
     */
    public static void validateBalFile(CodeInput codeInput, String input) {
        DiagnosticResult result = codeInput.pkg().getCompilation().diagnosticResult();
        if (!result.hasErrors()) {
            return;
        }
        List<InputDiagnostic> errors = new ArrayList<>();
        for (Diagnostic diagnostic : result.errors()) {
            LineRange range = diagnostic.location().lineRange();
            errors.add(new InputDiagnostic(range.fileName(), range.startLine().line() + 1,
                    range.startLine().offset() + 1, "error", diagnostic.message()));
            if (errors.size() == MAX_DIAGNOSTICS) {
                break;
            }
        }
        throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_009, input).withDiagnostics(errors);
    }
}
