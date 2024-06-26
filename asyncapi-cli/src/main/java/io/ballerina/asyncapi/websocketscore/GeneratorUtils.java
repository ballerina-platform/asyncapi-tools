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
package io.ballerina.asyncapi.websocketscore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.model.GenSrcFile;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.BALLERINA;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLOSE_BRACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.LINE_SEPARATOR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.OPEN_BRACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SLASH;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SPECIAL_CHARACTERS_REGEX;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.YAML_EXTENSION;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.YML_EXTENSION;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * This class util for store all the common scenarios.
 *
 */
public class GeneratorUtils {

    public static final MinutiaeList SINGLE_WS_MINUTIAE = getSingleWSMinutiae();
    public static final List<String> BAL_KEYWORDS = SyntaxInfo.keywords();
    public static final MinutiaeList SINGLE_END_OF_LINE_MINUTIAE = getEndOfLineMinutiae();

    //Create imports;
    public static ImportDeclarationNode getImportDeclarationNode(String orgName, String moduleName) {
        Token importKeyword = AbstractNodeFactory.createIdentifierToken("import", SINGLE_WS_MINUTIAE,
                SINGLE_WS_MINUTIAE);
        Token orgNameToken = AbstractNodeFactory.createIdentifierToken(orgName);
        Token slashToken = AbstractNodeFactory.createIdentifierToken("/");
        ImportOrgNameNode importOrgNameNode = NodeFactory.createImportOrgNameNode(orgNameToken, slashToken);
        Token moduleNameToken = AbstractNodeFactory.createIdentifierToken(moduleName);
        SeparatedNodeList<IdentifierToken> moduleNodeList = AbstractNodeFactory.createSeparatedNodeList(
                moduleNameToken);
        Token semicolon = AbstractNodeFactory.createIdentifierToken(";");
        return NodeFactory.createImportDeclarationNode(importKeyword, importOrgNameNode, moduleNodeList, null,
                semicolon);
    }

    /**
     * Method for convert asyncAPI type to ballerina type.
     *
     * @param type AsyncAPI parameter types
     * @return ballerina type
     */
    public static String convertAsyncAPITypeToBallerina(String type) throws BallerinaAsyncApiExceptionWs {
        if (GeneratorConstants.TYPE_MAP.containsKey(type)) {
            return GeneratorConstants.TYPE_MAP.get(type);
        } else {
            throw new BallerinaAsyncApiExceptionWs("Unsupported AsyncAPI data type `" + type + "`");
        }
    }

    /**
     * This method will escape special characters used in method names and identifiers.
     *
     * @param identifier - identifier or method name
     * @return - escaped string
     */
    public static String escapeIdentifier(String identifier) {
        if (identifier.matches("\\b[0-9]*\\b")) {
            return "'" + identifier;
        } else if (!identifier.matches("\\b[_a-zA-Z][_a-zA-Z0-9]*\\b") || BAL_KEYWORDS.contains(identifier)) {
            identifier = identifier.replaceAll(GeneratorConstants.ESCAPE_PATTERN, "\\\\$1");
            return "'" + identifier;
        }
        return identifier;
    }

    /**
     * Generate operationId by removing special characters.
     *
     * @param identifier input function name, record name or operation Id
     * @return string with new generated name
     */
    public static String getValidName(String identifier, boolean isSchema) {
        //For the flatten enable we need to remove first Part of valid name check
        // this - > !identifier.matches("\\b[a-zA-Z][a-zA-Z0-9]*\\b") &&
        if (!identifier.matches("\\b[0-9]*\\b")) {
            String[] split = identifier.split(GeneratorConstants.ESCAPE_PATTERN);
            StringBuilder validName = new StringBuilder();
            for (String part : split) {
                if (!part.isBlank()) {
                    if (split.length > 1) {
                        part = part.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                                part.substring(1).toLowerCase(Locale.ENGLISH);
                    }
                    validName.append(part);
                }
            }
            identifier = validName.toString();
        }
        if (isSchema) {
            return identifier.substring(0, 1).toUpperCase(Locale.ENGLISH) + identifier.substring(1);
        } else {
            return escapeIdentifier(identifier.substring(0, 1).toLowerCase(Locale.ENGLISH) +
                    identifier.substring(1));
        }
    }

    /**
     * This util function is for updating the list of nodes {@link TypeDefinitionNode}.
     * It updates the list while checking the duplicates.
     *
     * @param typeName               - Given node name
     * @param typeDefNode            - Generated node
     * @param typeDefinitionNodeList - Current node list
     */
    public static void updateTypeDefNodeList(String typeName, TypeDefinitionNode typeDefNode,
                                             List<TypeDefinitionNode> typeDefinitionNodeList) {
        boolean anyMatch = typeDefinitionNodeList.stream().anyMatch(node ->
                (node.typeName().text().trim().equals(typeName)));
        if (!anyMatch) {
            typeDefinitionNodeList.add(typeDefNode);
        }
    }

    /**
     * Check the given recordName is valid name.
     *
     * @param recordName - String record name
     * @return - boolean value
     */
    public static boolean isValidSchemaName(String recordName) {
        return !recordName.matches("\\b[0-9]*\\b");
    }

    /**
     * This method will extract reference type by splitting the reference string.
     *
     * @param referenceVariable - Reference String
     * @return Reference variable name
     * @throws BallerinaAsyncApiExceptionWs - Throws an exception if the reference string is incompatible.
     *                                    Note : Current implementation will not support external links a references.
     */
    public static String extractReferenceType(String referenceVariable) throws BallerinaAsyncApiExceptionWs {
        if (referenceVariable.startsWith("#") && referenceVariable.contains("/")) {
            String[] refArray = referenceVariable.split("/");
            return refArray[refArray.length - 1];
        } else {
            throw new BallerinaAsyncApiExceptionWs("Invalid reference value : " + referenceVariable
                    + "\nBallerina only supports local reference values.");
        }
    }

    public static boolean hasTags(List<String> tags, List<String> filterTags) {
        return !Collections.disjoint(filterTags, tags);
    }

    /**
     * Util for take AsyncAPI spec from given yaml file.
     */
    public static AsyncApi25DocumentImpl getAsyncAPIFromAsyncAPIParser(Path definitionPath) throws
            IOException, BallerinaAsyncApiExceptionWs {

        Path contractPath = java.nio.file.Paths.get(definitionPath.toString());
        if (!Files.exists(contractPath)) {
            throw new BallerinaAsyncApiExceptionWs(ErrorMessages.invalidFilePath(definitionPath.toString()));
        }
        if (!(definitionPath.toString().endsWith(YAML_EXTENSION) ||
                definitionPath.toString().endsWith(JSON_EXTENSION) ||
                definitionPath.toString().endsWith(YML_EXTENSION))) {
            throw new BallerinaAsyncApiExceptionWs(ErrorMessages.invalidFileType());
        }
        //    add a parser
        String asyncAPIFileContent = Files.readString(definitionPath);
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(asyncAPIFileContent, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        AsyncApi25DocumentImpl document;
        try {
            document = (AsyncApi25DocumentImpl) Library.readDocumentFromJSONString
                    (jsonWriter.writeValueAsString(obj));
        } catch (ClassCastException e) {
            throw new BallerinaAsyncApiExceptionWs("AsyncAPI definition has errors. " +
                    "Ballerina client code can only be generate for 2.5.0 version");
        }

        List<ValidationProblem> validationProblems = Library.validate(document, null);
        if (!validationProblems.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("AsyncAPI definition has errors: \n");
            for (ValidationProblem validationProblem : validationProblems) {
                errorMessage.append(validationProblem.message).append(LINE_SEPARATOR);
            }
            throw new BallerinaAsyncApiExceptionWs(errorMessage.toString());
        }
        return document;
    }

    public static String getStreamGeneratorName(String returnType) {
        if (returnType.contains("|")) {
            returnType = returnType.replaceAll("\\|", "");
        }
        return returnType.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + returnType.substring(1);
    }

    /*
     * Generate expressionStatementNode.
     */
    public static ExpressionStatementNode getSimpleExpressionStatementNode(String expression) {
        SimpleNameReferenceNode expressionNode = createSimpleNameReferenceNode(
                createIdentifierToken(expression));
        return createExpressionStatementNode(null, expressionNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * If there are template values in the {@code absUrl} derive resolved url using {@code variables}.
     *
     * @param absUrl    abstract url with template values
     * @param variables variable values to populate the url template
     * @return resolved url
     */
    public static String buildUrl(String absUrl, Map<String, ServerVariable> variables) {
        String url = absUrl;
        if (variables != null) {
            for (Map.Entry<String, ServerVariable> entry : variables.entrySet()) {
                // According to the oas spec, default value must be specified
                String replaceKey = "\\{" + entry.getKey() + '}';
                url = url.replaceAll(replaceKey, entry.getValue().getDefault());
            }
        }
        return url;
    }

    private static MinutiaeList getSingleWSMinutiae() {
        Minutiae whitespace = AbstractNodeFactory.createWhitespaceMinutiae(" ");
        MinutiaeList leading = AbstractNodeFactory.createMinutiaeList(whitespace);
        return leading;
    }

    private static MinutiaeList getEndOfLineMinutiae() {
        Minutiae endOfLineMinutiae = AbstractNodeFactory.createEndOfLineMinutiae(LINE_SEPARATOR);
        MinutiaeList leading = AbstractNodeFactory.createMinutiaeList(endOfLineMinutiae);
        return leading;
    }

    /**
     * This method for setting the file name for generated file.
     *
     * @param listFiles      generated files
     * @param gFile          GenSrcFile object
     * @param duplicateCount add the tag with duplicate number if file already exist
     */
    public static void setGeneratedFileName(List<File> listFiles, GenSrcFile gFile, int duplicateCount) {
        for (File listFile : listFiles) {
            String listFileName = listFile.getName();
            if (listFileName.contains(".") && ((listFileName.split("\\.")).length >= 2) &&
                    (listFileName.split("\\.")[0].equals(gFile.getFileName().split("\\.")[0]))) {
                duplicateCount = 1 + duplicateCount;
            }
        }
        gFile.setFileName(gFile.getFileName().split("\\.")[0] + "." + (duplicateCount) + "." +
                gFile.getFileName().split("\\.")[1]);
    }

    public static boolean checkImportDuplicate(List<ImportDeclarationNode> imports, String module) {
        for (ImportDeclarationNode importModule : imports) {
            StringBuilder moduleBuilder = new StringBuilder();
            for (IdentifierToken identifierToken : importModule.moduleName()) {
                moduleBuilder.append(identifierToken.toString().trim());
            }
            if (BALLERINA.equals((importModule.orgName().get()).orgName().toString().trim()) &&
                    module.equals(moduleBuilder.toString())) {
                return true;
            }
        }
        return false;
    }

    public static void addImport(List<ImportDeclarationNode> imports, String module) {
        if (!checkImportDuplicate(imports, module)) {
            ImportDeclarationNode importModule = GeneratorUtils.getImportDeclarationNode(BALLERINA, module);
            imports.add(importModule);
        }
    }

    /**
     * Check the given URL include complex scenarios.
     * ex: /admin/api/2021-10/customers/{customer_id}.json parameterised path parameters
     * TODO: address the other /{id}.json.{name}, /report.{format}
     */
    public static boolean isComplexURL(String path) {
        String[] subPathSegment = path.split(SLASH);
        Pattern pattern = Pattern.compile(SPECIAL_CHARACTERS_REGEX);
        for (String subPath : subPathSegment) {
            if (subPath.contains(OPEN_BRACE) &&
                    pattern.matcher(subPath.split(CLOSE_BRACE, 2)[1]).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add function statements for handle complex URL ex: /admin/api/2021-10/customers/{customer_id}.json.
     *
     * <pre>
     *     if !customerIdDotJson.endsWith(".json") { return error("bad URL"); }
     *     string customerId = customerIdDotJson.substring(0, customerIdDotJson.length() - 4);
     * </pre>
     */
    public static List<StatementNode> generateBodyStatementForComplexUrl(String path) {
        String[] subPathSegment = path.split(SLASH);
        Pattern pattern = Pattern.compile(SPECIAL_CHARACTERS_REGEX);
        List<StatementNode> bodyStatements = new ArrayList<>();
        for (String subPath : subPathSegment) {
            if (subPath.contains(OPEN_BRACE) &&
                    pattern.matcher(subPath.split(CLOSE_BRACE, 2)[1]).find()) {
                String pathParam = subPath;
                pathParam = pathParam.substring(pathParam.indexOf(OPEN_BRACE) + 1);
                pathParam = pathParam.substring(0, pathParam.indexOf(CLOSE_BRACE));
                pathParam = getValidName(pathParam, false);
                String[] subPathSplit = subPath.split(CLOSE_BRACE, 2);
                String pathParameter = getValidName(subPath, false);
                String restSubPath = subPathSplit[1];
                String resSubPathLength = String.valueOf(restSubPath.length() - 1);
                String ifBlock = "if !" + pathParameter + ".endsWith(\"" + restSubPath + "\") { return error(\"bad " +
                        "URL\"); }";
                StatementNode ifBlockStatement = NodeParser.parseStatement(ifBlock);

                String pathParameterState = "string " + pathParam + " = " + pathParameter + ".substring(0, " +
                        pathParameter + ".length() - " + resSubPathLength + ");";
                StatementNode pathParamStatement = NodeParser.parseStatement(pathParameterState);
                bodyStatements.add(ifBlockStatement);
                bodyStatements.add(pathParamStatement);
            }
        }
        return bodyStatements;
    }

    /**
     * This util is to check if the given schema contains any constraints.
     */
    public static boolean hasConstraints(Schema schema) {
        AsyncApi25SchemaImpl value = (AsyncApi25SchemaImpl) schema;
        if (value.getProperties() != null) {
            boolean constraintExists = value.getProperties().values().stream()
                    .anyMatch(GeneratorUtils::hasConstraints);
            if (constraintExists) {
                return true;
            }
        } else if ((value.getProperties() != null &&
                (value.getOneOf() != null || value.getAllOf() != null || value.getAnyOf() != null))) {
            List<Schema> allOf = value.getAllOf();
            List<AsyncApiSchema> oneOf = value.getOneOf();
            List<AsyncApiSchema> anyOf = value.getAnyOf();
            boolean constraintExists = false;
            if (allOf != null) {
                constraintExists = allOf.stream().anyMatch(GeneratorUtils::hasConstraints);
            } else if (oneOf != null) {
                constraintExists = oneOf.stream().anyMatch(GeneratorUtils::hasConstraints);
            } else if (anyOf != null) {
                constraintExists = anyOf.stream().anyMatch(GeneratorUtils::hasConstraints);
            }
            if (constraintExists) {
                return true;
            }
        } else if (value.getType() != null && value.getType().equals("array")) {
            if (!isConstraintExists(value)) {
                return isConstraintExists((AsyncApi25SchemaImpl) value.getItems());
            }
        }
        return isConstraintExists(value);
    }

    private static boolean isConstraintExists(AsyncApi25SchemaImpl propertyValue) {
        return propertyValue.getMaximum() != null || propertyValue.getMinimum() != null ||
                propertyValue.getMaxLength() != null || propertyValue.getMinLength() != null ||
                propertyValue.getMaxItems() != null || propertyValue.getMinItems() != null ||
                propertyValue.getExclusiveMinimum() != null || propertyValue.getExclusiveMaximum() != null;
    }

    /**
     * Normalized AsyncAPI specification with adding proper naming to schema.
     *
     * @param asyncAPIPath - asyncAPI file path
     * @return - asyncAPI specification
     * @throws IOException
     * @throws BallerinaAsyncApiExceptionWs
     */
    public static AsyncApi25DocumentImpl normalizeAsyncAPI(Path asyncAPIPath) throws IOException,
            BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = getAsyncAPIFromAsyncAPIParser(asyncAPIPath);
        //TODO: have to add a asyncapi validator to here
        return asyncAPI;
    }

    public static String removeNonAlphanumeric(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "");
    }
}
