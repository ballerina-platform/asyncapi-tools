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
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
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
        Project project;
        final List<AsyncApiConverterDiagnostic> errors = new ArrayList<>();

        // Load project instance for single ballerina file
        project = ProjectLoader.loadProject(servicePath);
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            docId = project.documentId(servicePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            // Take module instance for traversing the syntax tree
            Module currentModule = packageName.getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();
            docId = documentIterator.next();
            doc = currentModule.document(docId);
        }
        Optional<Path> path = project.documentPath(docId);
        Path inputPath = path.orElse(null);
        syntaxTree = doc.syntaxTree();
        PackageCompilation compilation = project.currentPackage().getCompilation();
        boolean hasErrors = compilation.diagnosticResult().diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));

        if (hasErrors) {
            // if there are any compilation errors, do not proceed
            outStream.println("COMPILATION ERRORS:");
            for (Diagnostic e :compilation.diagnosticResult().diagnostics()) {
                if (e.diagnosticInfo().severity().equals(DiagnosticSeverity.ERROR)) {
                    outStream.println(e.message());
                }
            }
        } else {
            semanticModel = compilation.getSemanticModel(docId.moduleId());
            List<AsyncApiResult> asyncAPIDefinitions = ServiceToAsyncApiConverterUtils.
                    generateAsyncAPISpecDefinition(syntaxTree, semanticModel, serviceName, needJson, inputPath);

            if (!asyncAPIDefinitions.isEmpty()) {
                List<String> fileNames = new ArrayList<>();
                for (AsyncApiResult definition : asyncAPIDefinitions) {
                    if (Files.notExists(outPath)) {
                        try {
                            Files.createDirectories(outPath);
                        } catch (IOException e) {
                            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
                            ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(),
                                    message.getDescription() + e.getLocalizedMessage(), null);
                            errors.add(error);
                        }
                    }
                    try {
                        errors.addAll(definition.getDiagnostics());
                        if (definition.getAsyncAPI().isPresent()) {
                            Optional<String> content;
                            if (needJson) {
                                content = definition.getJson();
                            } else {
                                content = definition.getYaml();
                            }
                            String fileName = resolveContractFileName(outPath, definition.getServiceName(), needJson);
                            CodegenUtils.writeFile(outPath.resolve(fileName), content.get());
                            fileNames.add(fileName);
                        }
                    } catch (IOException e) {
                        DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
                        ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(),
                                message.getDescription() + e.getLocalizedMessage(), null);
                        errors.add(error);
                    }
                }
                outStream.println("AsyncAPI definition(s) generated successfully and copied to :");
                for (String fileName : fileNames) {
                    outStream.println("-- " + fileName);
                }
            }
        }
        return errors;
    }
}
