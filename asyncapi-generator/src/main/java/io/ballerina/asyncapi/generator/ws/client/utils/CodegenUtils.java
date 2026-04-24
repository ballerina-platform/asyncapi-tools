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
package io.ballerina.asyncapi.generator.ws.client.utils;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.Minutiae;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxInfo;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility methods for WebSocket client code generation.
 */
public final class CodegenUtils {

    /** Pattern used to detect and escape special characters in Ballerina identifiers. */
    public static final String ESCAPE_PATTERN = "([\\[\\]\\\\?!<>@#&~`*\\-=^+();:\\/{}\\s|.$])";

    /** List of reserved Ballerina keywords. */
    public static final List<String> BAL_KEYWORDS = SyntaxInfo.keywords();

    /** Extension key for the close-frame marker on schemas. */
    private static final String X_CLOSE_FRAME = "x-close-frame";

    /** Maps AsyncAPI / JSON Schema primitive types to Ballerina type names. */
    public static final Map<String, String> TYPE_MAP;

    static {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("integer", "int");
        typeMap.put("string", "string");
        typeMap.put("boolean", "boolean");
        typeMap.put("decimal", "decimal");
        typeMap.put("number", "decimal");
        typeMap.put("double", "decimal");
        typeMap.put("float", "float");
        typeMap.put("binary", "byte[]");
        typeMap.put("byte", "byte[]");
        typeMap.put("{}", "json");
        TYPE_MAP = Collections.unmodifiableMap(typeMap);
    }

    private CodegenUtils() {
    }

    /**
     * Escapes special characters used in Ballerina method names and identifiers.
     *
     * @param identifier the identifier or method name to escape
     * @return the escaped string
     */
    public static String escapeIdentifier(String identifier) {
        if (identifier.matches("\\b[0-9]*\\b")) {
            return String.format("'%s", identifier);
        } else if (!identifier.matches("\\b[_a-zA-Z][_a-zA-Z0-9]*\\b")
                || BAL_KEYWORDS.contains(identifier)) {
            identifier = identifier.replaceAll(ESCAPE_PATTERN, "\\\\$1");
            return String.format("'%s", identifier);
        }
        return identifier;
    }

    /**
     * Generates a valid Ballerina name by removing special characters from an operation ID,
     * function name, or record name.
     *
     * @param identifier          the input identifier to sanitise
     * @param capitalizeFirstChar whether to capitalise the first character
     * @return the sanitised name
     */
    public static String getValidName(String identifier, boolean capitalizeFirstChar) {
        if (!identifier.matches("\\b[0-9]*\\b")) {
            String[] split = identifier.split("_|" + ESCAPE_PATTERN);
            StringBuilder validName = new StringBuilder();
            for (String part : split) {
                if (!part.isBlank()) {
                    if (split.length > 1) {
                        part = part.substring(0, 1).toUpperCase(Locale.ENGLISH)
                                + part.substring(1).toLowerCase(Locale.ENGLISH);
                    }
                    validName.append(part);
                }
            }
            identifier = validName.toString();
        }
        if (capitalizeFirstChar) {
            return String.format("%s%s",
                    identifier.substring(0, 1).toUpperCase(Locale.ENGLISH),
                    identifier.substring(1));
        } else {
            return escapeIdentifier(String.format("%s%s",
                    identifier.substring(0, 1).toLowerCase(Locale.ENGLISH),
                    identifier.substring(1)));
        }
    }

    /**
     * Extracts the type name from a local {@code $ref} string
     * (e.g. {@code "#/components/schemas/Foo"} returns {@code "Foo"}).
     * Only local references starting with {@code #/} are supported.
     *
     * @param referenceVariable the value of the {@code $ref} field
     * @return the last path segment of the reference
     * @throws GeneratorException if the reference is not a local reference
     */
    public static String extractReferenceType(String referenceVariable) throws GeneratorException {
        if (referenceVariable.startsWith("#/") && referenceVariable.contains("/")) {
            String[] refArray = referenceVariable.split("/");
            return refArray[refArray.length - 1];
        } else {
            throw new GeneratorException(String.format(
                    "Invalid reference value: %s%nBallerina only supports local reference values.",
                    referenceVariable));
        }
    }

    /**
     * Builds a server URL by composing {@code host} and {@code pathname} and substituting
     * any server variable templates ({@code {varName}}) with their default values.
     *
     * @param host      the server host (may contain variable templates)
     * @param pathname  the server pathname (may be {@code null})
     * @param variables map of variable names to their definitions (may be {@code null})
     * @return the resolved URL string
     */
    public static String buildUrl(String host, String pathname,
                                  Map<String, AsyncApiServerVariable> variables) {
        String url = String.format("%s%s", host, pathname != null ? pathname : "");
        if (variables != null) {
            for (Map.Entry<String, AsyncApiServerVariable> entry : variables.entrySet()) {
                String replaceKey = String.format("\\{%s}", entry.getKey());
                String defaultValue = entry.getValue().defaultValue();
                if (defaultValue != null) {
                    url = url.replaceAll(replaceKey, defaultValue);
                }
            }
        }
        return url;
    }

    /**
     * Creates a Ballerina {@code import} declaration node for the given org and module.
     *
     * @param orgName    the organisation name (e.g. {@code "ballerina"})
     * @param moduleName the module name (e.g. {@code "websocket"})
     * @return the import declaration node
     */
    public static ImportDeclarationNode getImportDeclarationNode(String orgName, String moduleName) {
        MinutiaeList singleWs = getSingleWSMinutiae();
        Token importKeyword = AbstractNodeFactory.createIdentifierToken("import", singleWs, singleWs);
        Token orgNameToken = AbstractNodeFactory.createIdentifierToken(orgName);
        Token slashToken = AbstractNodeFactory.createIdentifierToken("/");
        ImportOrgNameNode importOrgNameNode = NodeFactory.createImportOrgNameNode(orgNameToken, slashToken);
        Token moduleNameToken = AbstractNodeFactory.createIdentifierToken(moduleName);
        SeparatedNodeList<IdentifierToken> moduleNodeList =
                AbstractNodeFactory.createSeparatedNodeList(moduleNameToken);
        Token semicolon = AbstractNodeFactory.createIdentifierToken(";");
        return NodeFactory.createImportDeclarationNode(importKeyword, importOrgNameNode,
                moduleNodeList, null, semicolon);
    }

    /**
     * Returns {@code true} if the given schema is a close-frame schema,
     * identified by the presence of the {@code x-close-frame} extension.
     *
     * @param schema the schema to check (may be {@code null})
     * @return {@code true} if the schema carries the close-frame extension
     */
    public static boolean isCloseFrameSchema(AsyncApiSchema schema) {
        if (schema == null || schema.extensions() == null) {
            return false;
        }
        return schema.extensions().containsKey(X_CLOSE_FRAME);
    }

    /**
     * Adds a type definition to a list only if no entry with the same type name already exists.
     *
     * @param typeName               the type name to check
     * @param typeDefNode            the node to conditionally add
     * @param typeDefinitionNodeList the target list
     */
    public static void updateTypeDefNodeList(String typeName, TypeDefinitionNode typeDefNode,
                                             List<TypeDefinitionNode> typeDefinitionNodeList) {
        boolean anyMatch = typeDefinitionNodeList.stream()
                .anyMatch(node -> node.typeName().text().trim().equals(typeName));
        if (!anyMatch) {
            typeDefinitionNodeList.add(typeDefNode);
        }
    }

    /**
     * Returns a stream generator class name derived from a return type string.
     *
     * @param returnType the Ballerina return type (may contain {@code |})
     * @return the PascalCase stream generator class name
     */
    public static String getStreamGeneratorName(String returnType) {
        if (returnType.contains("|")) {
            returnType = returnType.replaceAll("\\|", "");
        }
        return String.format("%s%s",
                returnType.substring(0, 1).toUpperCase(Locale.ENGLISH),
                returnType.substring(1));
    }

    /**
     * Builds a {@code while true { ... }} source string from a list of inner statements.
     *
     * @param innerStatements the statements inside the while body
     * @return the source text of the while-true block
     */
    public static String buildWhileTrue(List<StatementNode> innerStatements) {
        StringBuilder sb = new StringBuilder("while true {\n");
        for (StatementNode stmt : innerStatements) {
            sb.append(stmt.toSourceCode()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a token of the given kind with a trailing space so adjacent keywords
     * are properly separated in the generated source.
     *
     * @param kind the syntax kind of the token
     * @return the token with a trailing space minutia
     */
    public static Token tokenWithSpace(SyntaxKind kind) {
        MinutiaeList space = AbstractNodeFactory.createMinutiaeList(
                AbstractNodeFactory.createWhitespaceMinutiae(" "));
        return AbstractNodeFactory.createToken(kind,
                AbstractNodeFactory.createEmptyMinutiaeList(), space);
    }

    private static MinutiaeList getSingleWSMinutiae() {
        Minutiae whitespace = AbstractNodeFactory.createWhitespaceMinutiae(" ");
        return AbstractNodeFactory.createMinutiaeList(whitespace);
    }
}
