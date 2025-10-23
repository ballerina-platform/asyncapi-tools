/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.cmd.websockets;

import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.model.AsyncApiResult;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.CodegenUtils;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ServiceToAsyncApiConverterUtils;
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
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.CodegenUtils.resolveContractFileName;

/**
 * AsyncApi related utility classes.
 *
 */
public class BallerinaToAsyncApiGenerator {

    public static List<AsyncApiConverterDiagnostic> generateAsyncAPIDefinitionsAllService(Path servicePath,
                                                                                          Path outPath,
                                                                                          String serviceName,
                                                                                          Boolean needJson,
                                                                                          PrintStream outStream) {
        SyntaxTree syntaxTree;
        SemanticModel semanticModel;
        final List<AsyncApiConverterDiagnostic> errors = new ArrayList<>();

        // Load project instance for single ballerina file
        // Set offline true because we don't need to download packages silently through the CLI
        BuildOptions buildOptions = BuildOptions.builder().setOffline(true).build();
        Project project = ProjectLoader.load(servicePath, buildOptions).project();
        Package currentPackage = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            docId = project.documentId(servicePath);
            doc = currentPackage.module(docId.moduleId()).document(docId);
        } else {
            // Take module instance for traversing the syntax tree
            Module currentModule = currentPackage.getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();
            docId = documentIterator.next();
            doc = currentModule.document(docId);
        }
        Path inputPath = project.documentPath(docId).orElse(null);
        syntaxTree = doc.syntaxTree();
        PackageCompilation compilation = currentPackage.getCompilation();
        boolean hasErrors = compilation.diagnosticResult().hasErrors();
        if (hasErrors) {
            printDiagnostics(outStream, compilation.diagnosticResult().diagnostics());
            return errors;
        }
        semanticModel = compilation.getSemanticModel(docId.moduleId());
        List<AsyncApiResult> asyncAPIDefinitions = ServiceToAsyncApiConverterUtils.
                generateAsyncAPISpecDefinition(syntaxTree, semanticModel, serviceName, needJson, inputPath);

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

    private static void printDiagnostics(PrintStream outStream, Collection<Diagnostic> diagnostics) {
        outStream.println("COMPILATION ERRORS:");
        for (Diagnostic e : diagnostics) {
            if (e.diagnosticInfo().severity().equals(DiagnosticSeverity.ERROR)) {
                outStream.println(e.message());
            }
        }
    }

    private static void printSuccessMessage(PrintStream outStream, List<String> fileNames) {
        outStream.println("AsyncAPI definition(s) generated successfully and copied to :");
        for (String fileName : fileNames) {
            outStream.println("-- " + fileName);
        }
    }

    private static void createOutputDirectory(Path outPath, List<AsyncApiConverterDiagnostic> errors) {
        try {
            Files.createDirectories(outPath);
        } catch (IOException e) {
            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
            ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(),
                    message.getDescription() + e.getLocalizedMessage(), null);
            errors.add(error);
        }
    }

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
            ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(),
                    message.getDescription() + e.getLocalizedMessage(), null);
            errors.add(error);
        }
    }
}
