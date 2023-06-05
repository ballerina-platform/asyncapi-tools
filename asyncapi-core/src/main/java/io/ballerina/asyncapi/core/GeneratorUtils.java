/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.model.GenSrcFile;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

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

import static io.ballerina.asyncapi.core.GeneratorConstants.BALLERINA;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLOSE_CURLY_BRACE;
import static io.ballerina.asyncapi.core.GeneratorConstants.LINE_SEPARATOR;
import static io.ballerina.asyncapi.core.GeneratorConstants.OPEN_CURLY_BRACE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SLASH;
import static io.ballerina.asyncapi.core.GeneratorConstants.SPECIAL_CHARACTERS_REGEX;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.YAML_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.YML_EXTENSION;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * This class util for store all the common scenarios.
 *
 * @since 1.3.0
 */
public class GeneratorUtils {

    public static final MinutiaeList SINGLE_WS_MINUTIAE = getSingleWSMinutiae();
    public static final List<String> BAL_KEYWORDS = SyntaxInfo.keywords();
    public static final MinutiaeList SINGLE_END_OF_LINE_MINUTIAE = getEndOfLineMinutiae();
//    private static final Logger LOGGER = LoggerFactory.getLogger(UtilGenerator.class);

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

        return NodeFactory.createImportDeclarationNode(importKeyword, importOrgNameNode,
                moduleNodeList, null, semicolon);
    }

    public static QualifiedNameReferenceNode getQualifiedNameReferenceNode(String modulePrefix, String identifier) {
        Token modulePrefixToken = AbstractNodeFactory.createIdentifierToken(modulePrefix);
        Token colon = AbstractNodeFactory.createIdentifierToken(":");
        IdentifierToken identifierToken = AbstractNodeFactory.createIdentifierToken(identifier);
        return NodeFactory.createQualifiedNameReferenceNode(modulePrefixToken, colon, identifierToken);
    }

//    /**
//     * Generated resource function relative path node list.
//     *
//     * @param path      - resource path
//     * @param operation - resource operation
//     * @return - node lists
//     * @throws BallerinaAsyncApiException
//     */
//    public static List<Node> getRelativeResourcePath(String path, Operation operation, List<Node>
//    resourceFunctionDocs)
//            throws BallerinaAsyncApiException {
//
//        List<Node> functionRelativeResourcePath = new ArrayList<>();
//        String[] pathNodes = path.split(SLASH);
//        if (pathNodes.length >= 2) {
//            for (String pathNode : pathNodes) {
//                if (pathNode.contains(OPEN_CURLY_BRACE)) {
//                    String pathParam = pathNode;
//                    pathParam = pathParam.substring(pathParam.indexOf(OPEN_CURLY_BRACE) + 1);
//                    pathParam = pathParam.substring(0, pathParam.indexOf(CLOSE_CURLY_BRACE));
//                    pathParam = getValidName(pathParam, false);
//
//                    /**
//                     * TODO -> `onCall/[string id]\.json` type of url won't support from syntax
//                     * issue https://github.com/ballerina-platform/ballerina-spec/issues/1138
//                     * <pre>resource function get onCall/[string id]\.json() returns string {}</>
//                     */
//                    if (operation.getParameters() != null) {
//                        extractPathParameterDetails(operation, functionRelativeResourcePath, pathNode,
//                                pathParam, resourceFunctionDocs);
//                    }
//                } else if (!pathNode.isBlank()) {
//                    IdentifierToken idToken = createIdentifierToken(escapeIdentifier(pathNode.trim()));
//                    functionRelativeResourcePath.add(idToken);
//                    functionRelativeResourcePath.add(createToken(SLASH_TOKEN));
//                }
//            }
//            functionRelativeResourcePath.remove(functionRelativeResourcePath.size() - 1);
//        } else if (pathNodes.length == 0) {
//            IdentifierToken idToken = createIdentifierToken(".");
//            functionRelativeResourcePath.add(idToken);
//        } else {
//            IdentifierToken idToken = createIdentifierToken(pathNodes[1].trim());
//            functionRelativeResourcePath.add(idToken);
//        }
//        return functionRelativeResourcePath;
//    }

//    private static void extractPathParameterDetails(Operation operation, List<Node> functionRelativeResourcePath,
//                                                 String pathNode, String pathParam, List<Node> resourceFunctionDocs)
//            throws BallerinaAsyncApiException {
//        // check whether path parameter segment has special character
//        String[] split = pathNode.split(CLOSE_CURLY_BRACE, 2);
//        Pattern pattern = Pattern.compile(SPECIAL_CHARACTERS_REGEX);
//        Matcher matcher = pattern.matcher(split[1]);
//        boolean hasSpecialCharacter = matcher.find();
//
//        for (Parameter parameter : operation.getParameters()) {
//            if (parameter.getIn() == null) {
//                break;
//            }
//            if (pathParam.trim().equals(getValidName(parameter.getName().trim(), false))
//                    && parameter.getIn().equals("path")) {
//
//                // TypeDescriptor
//                BuiltinSimpleNameReferenceNode builtSNRNode = createBuiltinSimpleNameReferenceNode(
//                        null,
//                        parameter.getSchema() == null ?
//                                createIdentifierToken(STRING) :
//                                createIdentifierToken(
//                                        convertAsyncAPITypeToBallerina(parameter.getSchema().getType())));
//                IdentifierToken paramName = createIdentifierToken(
//                        hasSpecialCharacter ?
//                                getValidName(pathNode, false) :
//                                pathParam);
//                ResourcePathParameterNode resourcePathParameterNode =
//                        createResourcePathParameterNode(
//                                SyntaxKind.RESOURCE_PATH_SEGMENT_PARAM,
//                                createToken(OPEN_BRACKET_TOKEN),
//                                NodeFactory.createEmptyNodeList(),
//                                builtSNRNode,
//                                null,
//                                paramName,
//                                createToken(CLOSE_BRACKET_TOKEN));
//                functionRelativeResourcePath.add(resourcePathParameterNode);
//                functionRelativeResourcePath.add(createToken(SLASH_TOKEN));
//
//                // Add documentation
//                if (resourceFunctionDocs != null) {
//                    String parameterName = paramName.text();
//                    String paramComment = parameter.getDescription() != null && !parameter.getDescription()
//                    .isBlank() ?
//                            parameter.getDescription() : DEFAULT_PARAM_COMMENT;
//                    MarkdownParameterDocumentationLineNode paramAPIDoc =
//                            DocCommentsGenerator.createAPIParamDoc(parameterName, paramComment);
//                    resourceFunctionDocs.add(paramAPIDoc);
//                }
//                break;
//            }
//        }
//    }

    /**
     * Method for convert openApi type to ballerina type.
     *
     * @param type OpenApi parameter types
     * @return ballerina type
     */
    public static String convertAsyncAPITypeToBallerina(String type) throws BallerinaAsyncApiException {

        if (GeneratorConstants.TYPE_MAP.containsKey(type)) {
            return GeneratorConstants.TYPE_MAP.get(type);
        } else {
            throw new BallerinaAsyncApiException("Unsupported AsyncAPI data type `" + type + "`");
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
     * @throws BallerinaAsyncApiException - Throws an exception if the reference string is incompatible.
     *                                    Note : Current implementation will not support external links a references.
     */
    public static String extractReferenceType(String referenceVariable) throws BallerinaAsyncApiException {


        if (referenceVariable.startsWith("#") && referenceVariable.contains("/")) {
            String[] refArray = referenceVariable.split("/");
            return refArray[refArray.length - 1];
        } else {
            throw new BallerinaAsyncApiException("Invalid reference value : " + referenceVariable
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
            IOException, BallerinaAsyncApiException {

        Path contractPath = java.nio.file.Paths.get(definitionPath.toString());
        if (!Files.exists(contractPath)) {
            throw new BallerinaAsyncApiException(ErrorMessages.invalidFilePath(definitionPath.toString()));
        }
        if (!(definitionPath.toString().endsWith(YAML_EXTENSION) ||
                definitionPath.toString().endsWith(JSON_EXTENSION) ||
                definitionPath.toString().endsWith(YML_EXTENSION))) {
            throw new BallerinaAsyncApiException(ErrorMessages.invalidFileType());
        }
        //    add a parser


        String asyncAPIFileContent = Files.readString(definitionPath);
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(asyncAPIFileContent, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();

        AsyncApi25DocumentImpl document = (AsyncApi25DocumentImpl) Library.readDocumentFromJSONString
                (jsonWriter.writeValueAsString(obj));
        List<ValidationProblem> validationProblems = Library.validate(document, null);
        if (!validationProblems.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("AsyncAPI definition has errors: \n");
            for (ValidationProblem validationProblem : validationProblems) {
                errorMessage.append(validationProblem.message).append(LINE_SEPARATOR);
            }
            throw new BallerinaAsyncApiException(errorMessage.toString());
        }
        return document;

    }


    /*
     * Generate variableDeclarationNode.
     */
    public static VariableDeclarationNode getSimpleStatement(String responseType, String variable,
                                                             String initializer) {

        SimpleNameReferenceNode resTypeBind = createSimpleNameReferenceNode(createIdentifierToken(responseType));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(createIdentifierToken(variable));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(resTypeBind, bindingPattern);
        SimpleNameReferenceNode init = createSimpleNameReferenceNode(createIdentifierToken(initializer));

        return createVariableDeclarationNode(createEmptyNodeList(), null, typedBindingPatternNode,
                createToken(EQUAL_TOKEN), init, createToken(SEMICOLON_TOKEN));
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

//    /**
//     * Create each item of the encoding map.
//     *
//     * @param filedOfMap Includes all the items in the encoding map
//     * @param style      Defines how multiple values are delimited and explode
//     * @param explode    Specifies whether arrays and objects should generate separate parameters
//     * @param key        Key of the item in the map
//     */
//    public static void createEncodingMap(List<Node> filedOfMap, String style, Boolean explode, String key) {
//
//        IdentifierToken fieldName = createIdentifierToken('"' + key + '"');
//        Token colon = createToken(COLON_TOKEN);
//        SpecificFieldNode styleField = createSpecificFieldNode(null,
//                createIdentifierToken(STYLE), createToken(COLON_TOKEN),
//                createRequiredExpressionNode(createIdentifierToken(style.toUpperCase(Locale.ROOT))));
//        SpecificFieldNode explodeField = createSpecificFieldNode(null,
//                createIdentifierToken(EXPLODE), createToken(COLON_TOKEN),
//                createRequiredExpressionNode(createIdentifierToken(explode.toString())));
//        ExpressionNode expressionNode = createMappingConstructorExpressionNode(
//                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(styleField, createToken(COMMA_TOKEN),
//                        explodeField),
//                createToken(CLOSE_BRACE_TOKEN));
//        SpecificFieldNode specificFieldNode = createSpecificFieldNode(null,
//                fieldName, colon, expressionNode);
//        filedOfMap.add(specificFieldNode);
//        filedOfMap.add(createToken(COMMA_TOKEN));
//    }

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
            if (subPath.contains(OPEN_CURLY_BRACE) &&
                    pattern.matcher(subPath.split(CLOSE_CURLY_BRACE, 2)[1]).find()) {
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
            if (subPath.contains(OPEN_CURLY_BRACE) &&
                    pattern.matcher(subPath.split(CLOSE_CURLY_BRACE, 2)[1]).find()) {
                String pathParam = subPath;
                pathParam = pathParam.substring(pathParam.indexOf(OPEN_CURLY_BRACE) + 1);
                pathParam = pathParam.substring(0, pathParam.indexOf(CLOSE_CURLY_BRACE));
                pathParam = getValidName(pathParam, false);

                String[] subPathSplit = subPath.split(CLOSE_CURLY_BRACE, 2);
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

        return propertyValue.getMaximum() != null ||
                propertyValue.getMinimum() != null ||
                propertyValue.getMaxLength() != null ||
                propertyValue.getMinLength() != null ||
                propertyValue.getMaxItems() != null ||
                propertyValue.getMinItems() != null ||
                propertyValue.getExclusiveMinimum() != null ||
                propertyValue.getExclusiveMaximum() != null;
    }

    /**
     * Normalized OpenAPI specification with adding proper naming to schema.
     *
     * @param asyncAPIPath - openAPI file path
     * @return - openAPI specification
     * @throws IOException
     * @throws BallerinaAsyncApiException
     */
    public static AsyncApi25DocumentImpl normalizeAsyncAPI(Path asyncAPIPath) throws IOException,
            BallerinaAsyncApiException {
        AsyncApi25DocumentImpl asyncAPI = getAsyncAPIFromAsyncAPIParser(asyncAPIPath);
        //TODO: have to add a asyncapi validator to here

        //TODO: throw this exception when creating remote functions
//        if (asyncAPI.getComponents() != null) {
//            AsyncApi25ComponentsImpl components = (AsyncApi25ComponentsImpl) asyncAPI.getComponents();
//
//            if (components.getSchemas() != null) {
//                Map<String, Schema> componentsSchemas = components.getSchemas();
//                //Remove unnecessary characters from the schema name
//                for (Map.Entry<String, Schema> schemaEntry : componentsSchemas.entrySet()) {
//                    // Remove default name
//
//                    components.removeSchema(schemaEntry.getKey());
//                    // Refactor schema name with valid name
//                    String name = getValidName(schemaEntry.getKey(), true);
//                    components.addSchema(name, schemaEntry.getValue());
//                }
//                asyncAPI.setComponents(components);
//            } else {
//                throw new BallerinaAsyncApiException("Schemas section missing");
//            }
//        } else {
//            throw new BallerinaAsyncApiException("Components section missing");
//        }
        return asyncAPI;
    }

//    /**
//     * Check whether an operationId has been defined in each path. If given rename the operationId to accepted format.
//     * -- ex: GetPetName -> getPetName
//     *
//     * @param paths List of paths given in the OpenAPI definition
//     * @throws BallerinaAsyncApiException When operationId is missing in any path
//     */
//    public static void validateOperationIds(Set<Map.Entry<String, PathItem>> paths)
//    throws BallerinaAsyncApiException {
//        List<String> errorList = new ArrayList<>();
//        for (Map.Entry<String, PathItem> entry : paths) {
//            for (Map.Entry<PathItem.HttpMethod, Operation> operation :
//                    entry.getValue().readOperationsMap().entrySet()) {
//                if (operation.getValue().getOperationId() != null) {
//                    String operationId = getValidName(operation.getValue().getOperationId(), false);
//                    operation.getValue().setOperationId(operationId);
//                } else {
//                    errorList.add(String.format("OperationId is missing in the resource path: %s(%s)", entry.getKey(),
//                            operation.getKey()));
//                }
//            }
//        }
//        if (!errorList.isEmpty()) {
//            throw new BallerinaAsyncApiException(
//                    "OpenAPI definition has errors: " + LINE_SEPARATOR + String.join(LINE_SEPARATOR, errorList));
//        }
//    }

//    /**
//     * Validate if requestBody found in GET/DELETE/HEAD operation.
//     *
//     * @param paths - List of paths given in the OpenAPI definition
//     * @throws BallerinaOpenApiException - If requestBody found in GET/DELETE/HEAD operation
//     */
//    public static void validateRequestBody(Set<Map.Entry<String, PathItem>> paths) throws BallerinaOpenApiException {
//        List<String> errorList = new ArrayList<>();
//        for (Map.Entry<String, PathItem> entry : paths) {
//            if (!entry.getValue().readOperationsMap().isEmpty()) {
//                for (Map.Entry<PathItem.HttpMethod, Operation> operation : entry.getValue().readOperationsMap()
//                        .entrySet()) {
//                    String method = operation.getKey().name().trim().toLowerCase(Locale.ENGLISH);
//                    boolean isRequestBodyInvalid = method.equals(GET) || method.equals(HEAD);
//                    if (isRequestBodyInvalid && operation.getValue().getRequestBody() != null) {
//                        errorList.add(method.toUpperCase(Locale.ENGLISH) + " operation cannot have a requestBody. "
//                                + "Error at operationId: " + operation.getValue().getOperationId());
//                    }
//                }
//            }
//        }
//
//        if (!errorList.isEmpty()) {
//            StringBuilder errorMessage = new StringBuilder("OpenAPI definition has errors: " + LINE_SEPARATOR);
//            for (String message : errorList) {
//                errorMessage.append(message).append(LINE_SEPARATOR);
//            }
//            throw new BallerinaOpenApiException(errorMessage.toString());
//        }
//    }
//
//    public static String removeUnusedEntities(SyntaxTree schemaSyntaxTree, String clientContent, String schemaContent,
//                                              String serviceContent) throws IOException, FormatterException {
//        Map<String, String> tempSourceFiles = new HashMap<>();
//        tempSourceFiles.put(CLIENT_FILE_NAME, clientContent);
//        tempSourceFiles.put(TYPE_FILE_NAME, schemaContent);
//        if (serviceContent != null) {
//            tempSourceFiles.put(SERVICE_FILE_NAME, schemaContent);
//        }
//        List<String> unusedTypeDefinitionNameList = getUnusedTypeDefinitionNameList(tempSourceFiles);
//        while (unusedTypeDefinitionNameList.size() > 0) {
//            ModulePartNode modulePartNode = schemaSyntaxTree.rootNode();
//            NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
//            List<ModuleMemberDeclarationNode> unusedTypeDefinitionNodeList = new ArrayList<>();
//            for (ModuleMemberDeclarationNode node : members) {
//                if (node.kind().equals(SyntaxKind.TYPE_DEFINITION)) {
//                    for (ChildNodeEntry childNodeEntry : node.childEntries()) {
//                        if (childNodeEntry.name().equals(TYPE_NAME)) {
//                            if (unusedTypeDefinitionNameList.contains(childNodeEntry.node().get().toString())) {
//                                unusedTypeDefinitionNodeList.add(node);
//                            }
//                        }
//                    }
//                } else if (node.kind().equals(SyntaxKind.ENUM_DECLARATION)) {
//                    for (ChildNodeEntry childNodeEntry : node.childEntries()) {
//                        if (childNodeEntry.name().equals(IDENTIFIER)) {
//                            if (unusedTypeDefinitionNameList.contains(childNodeEntry.node().get().toString())) {
//                                unusedTypeDefinitionNodeList.add(node);
//                            }
//                        }
//                    }
//                }
//            }
//            NodeList<ModuleMemberDeclarationNode> modifiedMembers = members.removeAll
//                    (unusedTypeDefinitionNodeList);
//            ModulePartNode modiedModulePartNode = modulePartNode.modify(modulePartNode.imports(),
//                    modifiedMembers, modulePartNode.eofToken());
//            schemaSyntaxTree = schemaSyntaxTree.modifyWith(modiedModulePartNode);
//            schemaContent = Formatter.format(schemaSyntaxTree).toString();
//            tempSourceFiles.put(TYPE_FILE_NAME, schemaContent);
//            unusedTypeDefinitionNameList = getUnusedTypeDefinitionNameList(tempSourceFiles);
//        }
//        ModulePartNode rootNode = schemaSyntaxTree.rootNode();
//        NodeList<ImportDeclarationNode> imports = rootNode.imports();
//        imports = removeUnusedImports(rootNode, imports);
//
//        ModulePartNode modiedModulePartNode = rootNode.modify(imports, rootNode.members(), rootNode.eofToken());
//        schemaSyntaxTree = schemaSyntaxTree.modifyWith(modiedModulePartNode);
//        schemaContent = Formatter.format(schemaSyntaxTree).toString();
//        return schemaContent;
//    }

//    private static NodeList<ImportDeclarationNode> removeUnusedImports(ModulePartNode rootNode,
//                                                                       NodeList<ImportDeclarationNode> imports) {
//        //TODO: This function can be extended to check all the unused imports, for this time only handle constraint
//        // imports
//        boolean hasConstraint = false;
//        NodeList<ModuleMemberDeclarationNode> members = rootNode.members();
//        for (ModuleMemberDeclarationNode member:members) {
//            if (member.kind().equals(SyntaxKind.TYPE_DEFINITION)) {
//                TypeDefinitionNode typeDefNode = (TypeDefinitionNode) member;
//                if (typeDefNode.typeDescriptor().kind().equals(SyntaxKind.RECORD_TYPE_DESC)) {
//                    RecordTypeDescriptorNode record = (RecordTypeDescriptorNode) typeDefNode.typeDescriptor();
//                    NodeList<Node> fields = record.fields();
//                    //Traverse record fields to check for constraints
//                    for (Node node: fields) {
//                        if (node instanceof RecordFieldNode) {
//                            RecordFieldNode recField = (RecordFieldNode) node;
//                            if (recField.metadata().isPresent()) {
//                                hasConstraint = traverseAnnotationNode(recField.metadata(), hasConstraint);
//                            }
//                        }
//                        if (hasConstraint) {
//                            break;
//                        }
//                    }
//                }
//
//                if (typeDefNode.metadata().isPresent()) {
//                    hasConstraint = traverseAnnotationNode(typeDefNode.metadata(), hasConstraint);
//                }
//            }
//            if (hasConstraint) {
//                break;
//            }
//        }
//        if (!hasConstraint) {
//            for (ImportDeclarationNode importNode: imports) {
//                if (importNode.orgName().isPresent()) {
//                    if (importNode.orgName().get().toString().equals("ballerina/") &&
//                            importNode.moduleName().get(0).text().equals(CONSTRAINT)) {
//                        imports = imports.remove(importNode);
//                    }
//                }
//            }
//        }
//        return imports;
//    }

//    private static boolean traverseAnnotationNode(Optional<MetadataNode> recField, boolean hasConstraint) {
//        MetadataNode metadata = recField.get();
//        for (AnnotationNode annotation : metadata.annotations()) {
//            String annotationRef = annotation.annotReference().toString();
//            if (annotationRef.startsWith(CONSTRAINT)) {
//                hasConstraint = true;
//                break;
//            }
//        }
//        return hasConstraint;
//    }

//    private static List<String> getUnusedTypeDefinitionNameList(Map<String, String> srcFiles) throws IOException {
//        List<String> unusedTypeDefinitionNameList = new ArrayList<>();
//        Path tmpDir = Files.createTempDirectory(".openapi-tmp" + System.nanoTime());
//        writeFilesTemp(srcFiles, tmpDir);
//        if (Files.exists(tmpDir.resolve(CLIENT_FILE_NAME)) && Files.exists(tmpDir.resolve(TYPE_FILE_NAME)) &&
//                Files.exists(tmpDir.resolve(BALLERINA_TOML))) {
//            SemanticModel semanticModel = getSemanticModel(tmpDir.resolve(CLIENT_FILE_NAME));
//            List<Symbol> symbols = semanticModel.moduleSymbols();
//            for (Symbol symbol : symbols) {
//                if (symbol.kind().equals(SymbolKind.TYPE_DEFINITION) || symbol.kind().equals(SymbolKind.ENUM)) {
//                    List<Location> references = semanticModel.references(symbol);
//                    if (references.size() == 1) {
//                        unusedTypeDefinitionNameList.add(symbol.getName().get());
//                    }
//                }
//            }
//        }
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                FileUtils.deleteDirectory(tmpDir.toFile());
//            } catch (IOException ex) {
////                LOGGER.error("Unable to delete the temporary directory : " + tmpDir, ex);
//            }
//        }));
//        return unusedTypeDefinitionNameList;
//    }

//    private static void writeFilesTemp(Map<String, String> srcFiles, Path tmpDir) throws IOException {
//        srcFiles.put(BALLERINA_TOML, BALLERINA_TOML_CONTENT);
//        PrintWriter writer = null;
//        for (Map.Entry<String, String> entry : srcFiles.entrySet()) {
//            String key = entry.getKey();
//            Path filePath = tmpDir.resolve(key);
//            try {
//                writer = new PrintWriter(filePath.toString(), StandardCharsets.UTF_8);
//                writer.print(entry.getValue());
//            } finally {
//                if (writer != null) {
//                    writer.close();
//                }
//            }
//        }
//    }


    public static String removeNonAlphanumeric(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "");
    }
}
