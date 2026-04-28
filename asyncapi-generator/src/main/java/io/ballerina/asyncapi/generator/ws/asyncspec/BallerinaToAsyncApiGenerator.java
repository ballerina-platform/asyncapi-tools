/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.ws.asyncspec;

import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.AsyncApiResult;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ServiceToAsyncApiConverterUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.directory.WorkspaceProject;
import io.ballerina.projects.util.ProjectPaths;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.CodegenUtils.resolveContractFileName;

/**
 * Generates AsyncAPI specification files from Ballerina WebSocket service definitions.
 * Corresponds to the legacy {@code BallerinaToAsyncApiGenerator}.
 */
public class BallerinaToAsyncApiGenerator {

    /**
     * Generates AsyncAPI definitions for all services found in the Ballerina source at
     * {@code servicePath} and writes them to {@code outPath}.
     *
     * @param servicePath path to the Ballerina source file or project
     * @param outPath     directory to write generated AsyncAPI files into
     * @param serviceName optional service name filter (null to export all services)
     * @param needJson    {@code true} to emit JSON, {@code false} for YAML
     * @param outStream   stream for progress and error messages
     * @return list of diagnostics produced during generation
     */
    public static List<AsyncApiConverterDiagnostic> generateAsyncAPIDefinitionsAllService(
            Path servicePath, Path outPath, String serviceName, Boolean needJson, PrintStream outStream) {
        final List<AsyncApiConverterDiagnostic> errors = new ArrayList<>();
        BuildOptions buildOptions = BuildOptions.builder().setOffline(true).build();
        Path absServicePath = servicePath.toAbsolutePath().normalize();
        Path loadPath = absServicePath.getParent();
        while (loadPath != null && !Files.exists(loadPath.resolve("Ballerina.toml"))) {
            loadPath = loadPath.getParent();
        }
        if (loadPath == null) {
            loadPath = absServicePath;
        }
        Project project = ProjectLoader.load(loadPath, buildOptions).project();
        Package currentPackage = project.currentPackage();
        if (project.kind().equals(ProjectKind.WORKSPACE_PROJECT)) {
            currentPackage = findWorkspaceMemberPackage(
                    (WorkspaceProject) project, absServicePath, currentPackage);
        }
        PackageCompilation compilation = currentPackage.getCompilation();
        if (compilation.diagnosticResult().hasErrors()) {
            printDiagnostics(outStream, compilation.diagnosticResult().diagnostics());
            return errors;
        }
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.SINGLE_FILE_PROJECT)) {
            Module currentModule = currentPackage.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        } else {
            Module defaultModule = currentPackage.getDefaultModule();
            String targetFile = absServicePath.getFileName().toString();
            docId = null;
            doc = null;
            for (DocumentId id : defaultModule.documentIds()) {
                Document candidate = defaultModule.document(id);
                if (candidate.syntaxTree().filePath().endsWith(targetFile)) {
                    docId = id;
                    doc = candidate;
                    break;
                }
            }
            if (docId == null) {
                docId = defaultModule.documentIds().iterator().next();
                doc = defaultModule.document(docId);
            }
        }
        // project.documentPath() iterates all workspace packages; use absServicePath directly for WORKSPACE.
        Path inputPath = project.kind().equals(ProjectKind.WORKSPACE_PROJECT)
                ? absServicePath
                : project.documentPath(docId).orElse(null);
        SyntaxTree syntaxTree = doc.syntaxTree();
        SemanticModel semanticModel = compilation.getSemanticModel(docId.moduleId());
        List<AsyncApiResult> asyncAPIDefinitions = ServiceToAsyncApiConverterUtils
                .generateAsyncAPISpecDefinition(syntaxTree, semanticModel, serviceName, needJson, inputPath);
        if (asyncAPIDefinitions.isEmpty()) {
            return errors;
        }
        List<String> fileNames = new ArrayList<>();
        for (AsyncApiResult definition : asyncAPIDefinitions) {
            writeToFile(outPath, definition, needJson, errors, fileNames);
        }
        printSuccessMessage(outStream, fileNames);
        return errors;
    }

    /**
     * Prints ERROR-severity diagnostics from a compilation result to the output stream.
     *
     * @param outStream   stream to print to
     * @param diagnostics compilation diagnostics
     */
    private static void printDiagnostics(PrintStream outStream, Collection<Diagnostic> diagnostics) {
        outStream.println("COMPILATION ERRORS:");
        for (Diagnostic e : diagnostics) {
            if (e.diagnosticInfo().severity().equals(DiagnosticSeverity.ERROR)) {
                outStream.println(e.message());
            }
        }
    }

    /**
     * Prints the success message listing all generated file names.
     *
     * @param outStream stream to print to
     * @param fileNames generated file names
     */
    private static void printSuccessMessage(PrintStream outStream, List<String> fileNames) {
        outStream.println("AsyncAPI definition(s) generated successfully and copied to :");
        for (String fileName : fileNames) {
            outStream.println("-- " + fileName);
        }
    }

    /**
     * Creates the output directory, adding a diagnostic on failure.
     *
     * @param outPath output directory path
     * @param errors  accumulator for diagnostics
     */
    private static void createOutputDirectory(Path outPath, List<AsyncApiConverterDiagnostic> errors) {
        try {
            Files.createDirectories(outPath);
        } catch (IOException e) {
            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
            errors.add(new ExceptionDiagnostic(message.getCode(), message.getDescription(), null,
                    e.getLocalizedMessage()));
        }
    }

    /**
     * Writes a single AsyncAPI definition to the output directory.
     *
     * @param outPath    output directory
     * @param definition conversion result to write
     * @param needJson   {@code true} to write JSON, {@code false} for YAML
     * @param errors     accumulator for diagnostics
     * @param fileNames  accumulator for successfully written file names
     */
    private static void writeToFile(Path outPath, AsyncApiResult definition, boolean needJson,
                                    List<AsyncApiConverterDiagnostic> errors, List<String> fileNames) {
        if (Files.notExists(outPath)) {
            createOutputDirectory(outPath, errors);
        }
        try {
            errors.addAll(definition.getDiagnostics());
            if (definition.getAsyncAPI().isEmpty()) {
                return;
            }
            Optional<String> content = needJson ? definition.getJson() : definition.getYaml();
            if (content.isEmpty()) {
                return;
            }
            String fileName = resolveContractFileName(outPath, definition.getServiceName(), needJson);
            CodegenUtils.writeFile(outPath.resolve(fileName), content.get());
            fileNames.add(fileName);
        } catch (IOException e) {
            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
            errors.add(new ExceptionDiagnostic(message.getCode(), message.getDescription(), null,
                    e.getLocalizedMessage()));
        }
    }

    /**
     * Returns the workspace member {@link Package} whose source root contains the given service path.
     * {@link WorkspaceProject#currentPackage()} always returns the first-listed workspace package;
     * this method finds the correct member by comparing source roots.
     *
     * @param wsProject      the workspace project
     * @param absServicePath absolute path to the target Ballerina source file
     * @param fallback       package to return if no matching member is found
     * @return matching member package, or {@code fallback}
     */
    private static Package findWorkspaceMemberPackage(
            WorkspaceProject wsProject, Path absServicePath, Package fallback) {
        Path targetRoot = ProjectPaths.packageRoot(absServicePath.getParent())
                .toAbsolutePath().normalize();
        for (BuildProject member : wsProject.projects()) {
            if (member.sourceRoot().toAbsolutePath().normalize().equals(targetRoot)) {
                return member.currentPackage();
            }
        }
        return fallback;
    }
}
