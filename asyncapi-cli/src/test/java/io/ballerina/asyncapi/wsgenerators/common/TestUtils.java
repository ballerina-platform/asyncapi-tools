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
package io.ballerina.asyncapi.wsgenerators.common;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This util class for keeping all the common functions that use to tests.
 */
public class TestUtils {

    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/asyncapi-to-ballerina/").toAbsolutePath();
    private static final Path clientPath = RES_DIR.resolve("ballerina_project/client.bal");
    private static final Path schemaPath = RES_DIR.resolve("ballerina_project/types.bal");
    private static final Path utilPath = RES_DIR.resolve("ballerina_project/utils.bal");
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // Get diagnostics
    public static List<Diagnostic> getDiagnostics(SyntaxTree syntaxTree, AsyncApi25DocumentImpl asyncAPI,
                                                  IntermediateClientGenerator intermediateClientGenerator)
            throws FormatterException, IOException, BallerinaAsyncApiExceptionWs {
        List<TypeDefinitionNode> preGeneratedTypeDefinitionNodes = new LinkedList<>();
        preGeneratedTypeDefinitionNodes.addAll(intermediateClientGenerator.
                getBallerinaAuthConfigGenerator().getAuthRelatedTypeDefinitionNodes());
        preGeneratedTypeDefinitionNodes.addAll(intermediateClientGenerator.getTypeDefinitionNodeList());
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(
                asyncAPI,  preGeneratedTypeDefinitionNodes);
        SyntaxTree schemaSyntax = ballerinaSchemaGenerator.generateSyntaxTree();
        SyntaxTree utilSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        writeFile(clientPath, Formatter.format(syntaxTree).toString());
        writeFile(schemaPath, Formatter.format(schemaSyntax).toString());
        writeFile(utilPath, Formatter.format(utilSyntaxTree).toString());
        SemanticModel semanticModel = getSemanticModel(clientPath);
        return semanticModel.diagnostics();
    }

    public static List<Diagnostic> getDiagnostics(SyntaxTree syntaxTree) throws FormatterException, IOException {
        writeFile(schemaPath, Formatter.format(syntaxTree).toString());
        SemanticModel semanticModel = getSemanticModel(schemaPath);
        return semanticModel.diagnostics();
    }

    //Get string as a content of ballerina file
    public static String getStringFromGivenBalFile(Path expectedServiceFile) throws IOException {
        Stream<String> expectedServiceLines = Files.lines(expectedServiceFile);
        String expectedServiceContent = expectedServiceLines.collect(Collectors.joining(LINE_SEPARATOR));
        expectedServiceLines.close();
        return expectedServiceContent.replaceAll(LINE_SEPARATOR, "");
    }

    public static void compareGeneratedSyntaxTreeWithExpectedSyntaxTree(Path path, SyntaxTree syntaxTree)
            throws IOException {

        String expectedBallerinaContent = getStringFromGivenBalFile(path);
        String generatedSyntaxTree = syntaxTree.toString();
        generatedSyntaxTree = generatedSyntaxTree.replaceAll(LINE_SEPARATOR, "");
        generatedSyntaxTree = (generatedSyntaxTree.trim()).replaceAll("\\s+", "");
        expectedBallerinaContent = (expectedBallerinaContent.trim()).replaceAll("\\s+", "");
        Assert.assertEquals(generatedSyntaxTree, expectedBallerinaContent);
    }

    /*
     * Write the generated syntax tree to file.
     */
    public static void writeFile(Path filePath, String content) throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath.toString(), StandardCharsets.UTF_8)) {
            writer.print(content);
        }
    }

    public static SemanticModel getSemanticModel(Path servicePath) {
        // Load project instance for single ballerina file
        Project project = null;
        try {
            project = ProjectLoader.loadProject(servicePath);
        } catch (ProjectException ignored) {
        }

        Package packageName = project.currentPackage();
        DocumentId docId;

        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            docId = project.documentId(servicePath);
        } else {
            // Take module instance for traversing the syntax tree
            Module currentModule = packageName.getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();
            docId = documentIterator.next();
        }
        return project.currentPackage().getCompilation().getSemanticModel(docId.moduleId());
    }

//    public static AsyncApi25DocumentImpl getAsyncAPI(Path definitionPath) throws IOException,
//    BallerinaAsyncApiExceptionWs{
//        String asyncAPIFileContent = Files.readString(definitionPath);
//        SwaggerParseResult parseResult = new AsyncAPipar().readContents(asyncAPIFileContent);
//        return parseResult.getasyncAPI();
//    }

    public static String getStringFromGivenBalFile(Path expectedServiceFile, String s) throws IOException {
        Stream<String> expectedServiceLines = Files.lines(expectedServiceFile.resolve(s));
        String expectedServiceContent = expectedServiceLines.collect(Collectors.joining(LINE_SEPARATOR));
        expectedServiceLines.close();
        return expectedServiceContent;
    }

    public static void compareGeneratedSyntaxTreeWithExpectedSyntaxTree(String s, SyntaxTree syntaxTree)
            throws IOException {

        String expectedBallerinaContent = getStringFromGivenBalFile(RES_DIR.resolve(s));
        String generatedSyntaxTree = syntaxTree.toString();
        generatedSyntaxTree = (generatedSyntaxTree.trim()).replaceAll("\\s+", "");
        expectedBallerinaContent = (expectedBallerinaContent.trim()).replaceAll("\\s+", "");
        Assert.assertTrue(generatedSyntaxTree.contains(expectedBallerinaContent));
    }

    public static void compareGeneratedFiles(Path generatedFile, Path expectedFile) throws IOException {
        String generatedContent = getStringFromFile(generatedFile).replaceAll("\\s+", "");
        String expectedContent = getStringFromFile(expectedFile).replaceAll("\\s+", "");
        Assert.assertEquals(expectedContent, generatedContent);
    }

    public static String getStringFromFile(Path path) throws IOException {
        Stream<String> expectedLines = Files.lines(path);
        String expectedContent = expectedLines.collect(Collectors.joining(LINE_SEPARATOR));
        expectedLines.close();
        return expectedContent.replaceAll(LINE_SEPARATOR, "");
    }

    /**
     * Delete generated ballerina files.
     */
    public static void deleteGeneratedFiles() throws IOException {
        Path resourcesPath = RES_DIR.resolve("ballerina_project");
        if (Files.exists(resourcesPath)) {
            File[] listFiles = Objects.requireNonNull(new File(String.valueOf(resourcesPath)).listFiles());
            for (File existsFile : listFiles) {
                String fileName = existsFile.getName();
                if (fileName.endsWith(".bal")) {
                    existsFile.delete();
                }
            }
        }
    }
}
