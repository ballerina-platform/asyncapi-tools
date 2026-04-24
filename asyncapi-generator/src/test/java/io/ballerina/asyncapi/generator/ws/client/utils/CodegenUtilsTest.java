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

import com.fasterxml.jackson.databind.node.BooleanNode;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link CodegenUtils} covering identifier escaping, name sanitisation,
 * reference type extraction, URL building, schema helpers, and AST utilities.
 */
public class CodegenUtilsTest {

    // -------------------------------------------------------------------------
    // escapeIdentifier
    // -------------------------------------------------------------------------

    @Test
    void testEscapeIdentifier_keyword() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("type"), "'type",
                "Ballerina keyword 'type' should be escaped with a leading apostrophe");
    }

    @Test
    void testEscapeIdentifier_specialChars() {
        String result = CodegenUtils.escapeIdentifier("my-func");
        Assert.assertTrue(result.startsWith("'"),
                "Identifier containing a hyphen must be prefixed with an apostrophe");
        Assert.assertTrue(result.contains("-"),
                "The hyphen character should still be present (escaped) in the result");
    }

    @Test
    void testEscapeIdentifier_plain() {
        Assert.assertEquals(CodegenUtils.escapeIdentifier("myFunc"), "myFunc",
                "Plain alphanumeric identifier should be returned unchanged");
    }

    // -------------------------------------------------------------------------
    // getValidName
    // -------------------------------------------------------------------------

    @Test
    void testGetValidName_capitalizeTrue() {
        Assert.assertEquals(CodegenUtils.getValidName("myEvent", true), "MyEvent",
                "capitalizeFirstChar=true should uppercase the first character");
    }

    @Test
    void testGetValidName_capitalizeFalse() {
        String result = CodegenUtils.getValidName("MyEvent", false);
        Assert.assertTrue(result.startsWith("m"),
                "capitalizeFirstChar=false should lowercase the first character");
    }

    @Test
    void testGetValidName_slashSeparated() {
        Assert.assertEquals(CodegenUtils.getValidName("user/signup", true), "UserSignup",
                "Slash-separated path segments should be joined as PascalCase");
    }

    @Test
    void testGetValidName_underscoreSeparated() {
        Assert.assertEquals(CodegenUtils.getValidName("event_type", true), "EventType",
                "Underscore-separated identifier should be converted to PascalCase");
    }

    // -------------------------------------------------------------------------
    // extractReferenceType
    // -------------------------------------------------------------------------

    @Test
    void testExtractReferenceType_local() throws GeneratorException {
        String result = CodegenUtils.extractReferenceType("#/components/schemas/Foo");
        Assert.assertEquals(result, "Foo",
                "Local $ref should resolve to the last path segment");
    }

    @Test
    void testExtractReferenceType_nonLocalThrows() {
        try {
            CodegenUtils.extractReferenceType("http://example.com/schemas/Foo");
            Assert.fail("Expected GeneratorException for a non-local $ref");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null for non-local reference");
        }
    }

    // -------------------------------------------------------------------------
    // buildUrl
    // -------------------------------------------------------------------------

    @Test
    void testBuildUrl_substitutesVariables() {
        AsyncApiServerVariable versionVar = new AsyncApiServerVariable(null, "v2", null, null, null);
        Map<String, AsyncApiServerVariable> variables = Map.of("version", versionVar);
        String result = CodegenUtils.buildUrl("ws://example.com/{version}", "/chat", variables);
        Assert.assertEquals(result, "ws://example.com/v2/chat",
                "Server variable {version} should be replaced with its default value 'v2'");
    }

    @Test
    void testBuildUrl_noVariables() {
        String result = CodegenUtils.buildUrl("ws://example.com", "/events", null);
        Assert.assertEquals(result, "ws://example.com/events",
                "With no variables the URL should be host concatenated with pathname");
    }

    // -------------------------------------------------------------------------
    // isCloseFrameSchema
    // -------------------------------------------------------------------------

    @Test
    void testIsCloseFrameSchema_trueCase() {
        Map<String, com.fasterxml.jackson.databind.JsonNode> extensions =
                Map.of("x-close-frame", BooleanNode.TRUE);
        AsyncApiSchema closeSchema = AsyncApiSchema.builder()
                .type("object")
                .extensions(extensions)
                .build();
        Assert.assertTrue(CodegenUtils.isCloseFrameSchema(closeSchema),
                "Schema carrying x-close-frame extension should be identified as a close-frame schema");
    }

    @Test
    void testIsCloseFrameSchema_falseCase() {
        AsyncApiSchema normalSchema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("id", AsyncApiSchema.builder().type("string").build()))
                .build();
        Assert.assertFalse(CodegenUtils.isCloseFrameSchema(normalSchema),
                "Schema without x-close-frame extension should not be identified as a close-frame schema");
    }

    // -------------------------------------------------------------------------
    // getStreamGeneratorName
    // -------------------------------------------------------------------------

    @Test
    void testGetStreamGeneratorName_withPipeChar() {
        String result = CodegenUtils.getStreamGeneratorName("Foo|Bar");
        Assert.assertEquals(result, "FooBar",
                "Pipe character separating union type names should be removed to form the generator class name");
    }

    // -------------------------------------------------------------------------
    // buildWhileTrue
    // -------------------------------------------------------------------------

    @Test
    void testBuildWhileTrue_producesBlock() {
        StatementNode stmt = NodeParser.parseStatement("int x = 1;");
        List<StatementNode> stmts = new ArrayList<>();
        stmts.add(stmt);
        String result = CodegenUtils.buildWhileTrue(stmts);
        Assert.assertTrue(result.startsWith("while true {"),
                "buildWhileTrue result should open with 'while true {'");
        Assert.assertTrue(result.endsWith("}"),
                "buildWhileTrue result should close with '}'");
        Assert.assertTrue(result.contains("int x = 1"),
                "buildWhileTrue result should contain the inner statement");
    }

    // -------------------------------------------------------------------------
    // updateTypeDefNodeList
    // -------------------------------------------------------------------------

    @Test
    void testUpdateTypeDefNodeList_skipsDuplicate() {
        TypeDescriptorNode typeDesc = NodeParser.parseTypeDescriptor("string");
        TypeDefinitionNode typeDef = NodeFactory.createTypeDefinitionNode(
                null,
                AbstractNodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD),
                AbstractNodeFactory.createToken(SyntaxKind.TYPE_KEYWORD),
                AbstractNodeFactory.createIdentifierToken("MyAlias"),
                typeDesc,
                AbstractNodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        List<TypeDefinitionNode> list = new ArrayList<>();
        CodegenUtils.updateTypeDefNodeList("MyAlias", typeDef, list);
        Assert.assertEquals(list.size(), 1,
                "First call should insert the type definition node");
        CodegenUtils.updateTypeDefNodeList("MyAlias", typeDef, list);
        Assert.assertEquals(list.size(), 1,
                "Second call with the same type name should be a no-op (duplicate skipped)");
    }
}
