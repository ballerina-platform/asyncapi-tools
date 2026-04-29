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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the static utility methods in {@link ConverterCommonUtils} and the
 * {@link ConverterCommonUtils.NullLocation} inner class.
 *
 * <p>All methods under test are pure static functions or simple constructors — no Ballerina
 * compilation, no mocking, and no Apicurio document parsing is required.
 */
class ConverterCommonUtilsTest {

    // -------------------------------------------------------------------------
    // normalizeTitle
    // -------------------------------------------------------------------------

    @Test
    void testNormalizeTitle_null_returnsNull() {
        Assert.assertNull(ConverterCommonUtils.normalizeTitle(null),
                "normalizeTitle(null) must return null");
    }

    @Test
    void testNormalizeTitle_empty_returnsEmpty() {
        Assert.assertEquals(ConverterCommonUtils.normalizeTitle(""), "",
                "normalizeTitle(\"\") must return the original empty string");
    }

    @Test
    void testNormalizeTitle_singleSegmentWithLeadingSlash_titleCasesSegment() {
        Assert.assertEquals(ConverterCommonUtils.normalizeTitle("/chat"), "Chat",
                "normalizeTitle(\"/chat\") must strip the leading slash and title-case the segment");
    }

    @Test
    void testNormalizeTitle_multiSegmentPath_titleCasesEachSegment() {
        Assert.assertEquals(ConverterCommonUtils.normalizeTitle("/user/signed"), "User Signed",
                "normalizeTitle(\"/user/signed\") must produce a space-joined title-cased string");
    }

    @Test
    void testNormalizeTitle_plainWord_uppercasesFirstChar() {
        Assert.assertEquals(ConverterCommonUtils.normalizeTitle("alreadyTitle"), "AlreadyTitle",
                "normalizeTitle(\"alreadyTitle\") must uppercase the first character with no other changes");
    }

    // -------------------------------------------------------------------------
    // getAsyncApiFileName
    // -------------------------------------------------------------------------

    @Test
    void testGetAsyncApiFileName_plainServiceName_yaml() {
        String result = ConverterCommonUtils.getAsyncApiFileName("service.bal", "chat", false);
        Assert.assertEquals(result, "chat_asyncapi.yaml",
                "plain service name without slash must produce <name>_asyncapi.yaml");
    }

    @Test
    void testGetAsyncApiFileName_serviceNameWithLeadingSlash_json() {
        String result = ConverterCommonUtils.getAsyncApiFileName("service.bal", "/chat", true);
        Assert.assertEquals(result, "chat_asyncapi.json",
                "service name with leading slash must strip the slash and produce <name>_asyncapi.json");
    }

    @Test
    void testGetAsyncApiFileName_slashOnly_fallsBackToFileStem() {
        String result = ConverterCommonUtils.getAsyncApiFileName("service.bal", "/", false);
        Assert.assertEquals(result, "service_asyncapi.yaml",
                "service name of \"/\" alone must fall back to the source file stem");
    }

    @Test
    void testGetAsyncApiFileName_emptyServiceName_fallsBackToFileStem() {
        String result = ConverterCommonUtils.getAsyncApiFileName("service.bal", "", false);
        Assert.assertEquals(result, "service_asyncapi.yaml",
                "blank service name must fall back to the source file stem");
    }

    @Test
    void testGetAsyncApiFileName_nestedPath_joinsSegmentsWithUnderscore() {
        String result = ConverterCommonUtils.getAsyncApiFileName("service.bal", "user/chat", false);
        Assert.assertEquals(result, "user_chat_asyncapi.yaml",
                "nested service path must replace slashes with underscores");
    }

    // -------------------------------------------------------------------------
    // getNormalizedFileName
    // -------------------------------------------------------------------------

    @Test
    void testGetNormalizedFileName_alphanumeric_unchanged() {
        Assert.assertEquals(ConverterCommonUtils.getNormalizedFileName("chat"), "chat",
                "alphanumeric input must pass through unchanged");
    }

    @Test
    void testGetNormalizedFileName_hyphenatedName_replacedWithUnderscore() {
        Assert.assertEquals(ConverterCommonUtils.getNormalizedFileName("user-chat"), "user_chat",
                "hyphens must be replaced with underscores");
    }

    @Test
    void testGetNormalizedFileName_multipleSpecialChars_joinedWithUnderscore() {
        Assert.assertEquals(ConverterCommonUtils.getNormalizedFileName("user.chat.test"), "user_chat_test",
                "dots must be treated as separators and segments joined with underscores");
    }

    // -------------------------------------------------------------------------
    // getAsyncApiSchema(String) — primitive types
    // -------------------------------------------------------------------------

    @Test
    void testGetAsyncApiSchemaString_string_returnsStringType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("string");
        Assert.assertEquals(schema.getType(), "string");
        Assert.assertNull(schema.getFormat(), "\"string\" must have no format");
    }

    @Test
    void testGetAsyncApiSchemaString_plain_returnsStringType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("plain");
        Assert.assertEquals(schema.getType(), "string",
                "\"plain\" must map to type \"string\"");
    }

    @Test
    void testGetAsyncApiSchemaString_boolean_returnsBooleanType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("boolean");
        Assert.assertEquals(schema.getType(), "boolean");
        Assert.assertNull(schema.getFormat(), "\"boolean\" must have no format");
    }

    @Test
    void testGetAsyncApiSchemaString_array_returnsArrayType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("array");
        Assert.assertEquals(schema.getType(), "array");
        Assert.assertNull(schema.getFormat(), "\"array\" must have no format");
    }

    @Test
    void testGetAsyncApiSchemaString_tuple_returnsArrayType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("tuple");
        Assert.assertEquals(schema.getType(), "array",
                "\"tuple\" must map to type \"array\"");
    }

    @Test
    void testGetAsyncApiSchemaString_int_returnsIntegerWithInt64Format() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("int");
        Assert.assertEquals(schema.getType(), "integer");
        Assert.assertEquals(schema.getFormat(), "int64");
    }

    @Test
    void testGetAsyncApiSchemaString_integer_returnsIntegerWithInt64Format() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("integer");
        Assert.assertEquals(schema.getType(), "integer",
                "\"integer\" must map to type \"integer\" with format \"int64\"");
        Assert.assertEquals(schema.getFormat(), "int64");
    }

    @Test
    void testGetAsyncApiSchemaString_byteArray_returnsStringWithUuidFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("byte[]");
        Assert.assertEquals(schema.getType(), "string");
        Assert.assertEquals(schema.getFormat(), "uuid");
    }

    @Test
    void testGetAsyncApiSchemaString_octetStream_returnsStringWithUuidFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("octet-stream");
        Assert.assertEquals(schema.getType(), "string",
                "\"octet-stream\" must map to type \"string\" with format \"uuid\"");
        Assert.assertEquals(schema.getFormat(), "uuid");
    }

    @Test
    void testGetAsyncApiSchemaString_number_returnsNumberWithDoubleFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("number");
        Assert.assertEquals(schema.getType(), "number");
        Assert.assertEquals(schema.getFormat(), "double");
    }

    @Test
    void testGetAsyncApiSchemaString_decimal_returnsNumberWithDoubleFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("decimal");
        Assert.assertEquals(schema.getType(), "number",
                "\"decimal\" must map to type \"number\" with format \"double\"");
        Assert.assertEquals(schema.getFormat(), "double");
    }

    @Test
    void testGetAsyncApiSchemaString_float_returnsNumberWithFloatFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("float");
        Assert.assertEquals(schema.getType(), "number");
        Assert.assertEquals(schema.getFormat(), "float");
    }

    @Test
    void testGetAsyncApiSchemaString_object_returnsObjectType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("object");
        Assert.assertEquals(schema.getType(), "object");
        Assert.assertNull(schema.getAdditionalProperties(), "plain \"object\" must have no additionalProperties");
    }

    @Test
    void testGetAsyncApiSchemaString_mapJson_returnsObjectWithAdditionalProperties() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("map<json>");
        Assert.assertEquals(schema.getType(), "object");
        Assert.assertNotNull(schema.getAdditionalProperties(),
                "\"map<json>\" must set additionalProperties to signal open schema");
    }

    @Test
    void testGetAsyncApiSchemaString_map_returnsObjectWithAdditionalProperties() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("map");
        Assert.assertEquals(schema.getType(), "object",
                "\"map\" must map to type \"object\" with additionalProperties");
        Assert.assertNotNull(schema.getAdditionalProperties());
    }

    @Test
    void testGetAsyncApiSchemaString_typeReference_returnsEmptySchema() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("type_reference");
        Assert.assertNull(schema.getType(),
                "\"type_reference\" must return an empty schema with no type set");
    }

    @Test
    void testGetAsyncApiSchemaString_xml_returnsEmptySchema() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("xml");
        Assert.assertNull(schema.getType(), "\"xml\" must return an empty schema");
    }

    @Test
    void testGetAsyncApiSchemaString_unknown_returnsEmptySchema() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema("unknownType");
        Assert.assertNull(schema.getType(),
                "an unrecognised type string must fall through to the default and return an empty schema");
    }

    // -------------------------------------------------------------------------
    // getAsyncApiSchema(SyntaxKind)
    // -------------------------------------------------------------------------

    @Test
    void testGetAsyncApiSchemaSyntaxKind_stringTypeDesc_returnsStringType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.STRING_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "string");
        Assert.assertNull(schema.getFormat());
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_booleanTypeDesc_returnsBooleanType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.BOOLEAN_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "boolean");
        Assert.assertNull(schema.getFormat());
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_arrayTypeDesc_returnsArrayType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.ARRAY_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "array");
        Assert.assertNull(schema.getFormat());
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_intTypeDesc_returnsIntegerWithInt64Format() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.INT_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "integer");
        Assert.assertEquals(schema.getFormat(), "int64");
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_byteTypeDesc_returnsStringWithUuidFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.BYTE_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "string");
        Assert.assertEquals(schema.getFormat(), "uuid");
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_decimalTypeDesc_returnsNumberWithDoubleFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.DECIMAL_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "number");
        Assert.assertEquals(schema.getFormat(), "double");
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_floatTypeDesc_returnsNumberWithFloatFormat() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.FLOAT_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "number");
        Assert.assertEquals(schema.getFormat(), "float");
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_mapTypeDesc_returnsObjectType() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.MAP_TYPE_DESC);
        Assert.assertEquals(schema.getType(), "object");
        Assert.assertNull(schema.getFormat(), "MAP_TYPE_DESC must have no format set");
    }

    @Test
    void testGetAsyncApiSchemaSyntaxKind_default_returnsEmptySchema() {
        BalAsyncApi30SchemaImpl schema = ConverterCommonUtils.getAsyncApiSchema(SyntaxKind.OBJECT_TYPE_DESC);
        Assert.assertNull(schema.getType(),
                "OBJECT_TYPE_DESC falls through to default and must return an empty schema");
    }

    // -------------------------------------------------------------------------
    // createObjectNode
    // -------------------------------------------------------------------------

    @Test
    void testCreateObjectNode_returnsNonNullObjectNode() {
        ObjectNode node = ConverterCommonUtils.createObjectNode();
        Assert.assertNotNull(node, "createObjectNode() must return a non-null ObjectNode");
    }

    @Test
    void testCreateObjectNode_isEmpty() {
        ObjectNode node = ConverterCommonUtils.createObjectNode();
        Assert.assertEquals(node.size(), 0, "createObjectNode() must return an empty node");
    }

    @Test
    void testCreateObjectNode_isObject() {
        ObjectNode node = ConverterCommonUtils.createObjectNode();
        Assert.assertTrue(node.isObject(), "createObjectNode() result must report isObject() = true");
    }

    // -------------------------------------------------------------------------
    // callObjectMapper
    // -------------------------------------------------------------------------

    @Test
    void testCallObjectMapper_returnsNonNull() {
        ObjectMapper mapper = ConverterCommonUtils.callObjectMapper();
        Assert.assertNotNull(mapper, "callObjectMapper() must return a non-null ObjectMapper");
    }

    @Test
    void testCallObjectMapper_canSerializeSimpleObject() throws Exception {
        ObjectMapper mapper = ConverterCommonUtils.callObjectMapper();
        String json = mapper.writeValueAsString(java.util.Collections.singletonMap("key", "value"));
        Assert.assertNotNull(json, "callObjectMapper() result must be able to serialise a simple map");
        Assert.assertTrue(json.contains("key"), "serialised JSON must contain the map key");
    }

    // -------------------------------------------------------------------------
    // unescapeIdentifier
    // -------------------------------------------------------------------------

    @Test
    void testUnescapeIdentifier_plainIdentifier_unchanged() {
        Assert.assertEquals(ConverterCommonUtils.unescapeIdentifier("hello"), "hello",
                "a plain identifier must pass through unescapeIdentifier unchanged");
    }

    @Test
    void testUnescapeIdentifier_identifierWithSurroundingWhitespace_trimmed() {
        Assert.assertEquals(ConverterCommonUtils.unescapeIdentifier(" hello "), "hello",
                "unescapeIdentifier must trim surrounding whitespace");
    }

    @Test
    void testUnescapeIdentifier_identifierWithSingleQuote_quoteRemoved() {
        String result = ConverterCommonUtils.unescapeIdentifier("'hello");
        Assert.assertFalse(result.contains("'"),
                "unescapeIdentifier must strip single-quote Ballerina keyword-escape prefix");
    }

    // -------------------------------------------------------------------------
    // NullLocation
    // -------------------------------------------------------------------------

    @Test
    void testNullLocation_lineRange_isNonNull() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertNotNull(location.lineRange(), "NullLocation.lineRange() must return a non-null LineRange");
    }

    @Test
    void testNullLocation_lineRange_startLineIsZero() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertEquals(location.lineRange().startLine().line(), 0,
                "NullLocation.lineRange().startLine().line() must be 0");
        Assert.assertEquals(location.lineRange().startLine().offset(), 0,
                "NullLocation.lineRange().startLine().offset() must be 0");
    }

    @Test
    void testNullLocation_lineRange_endLineIsZero() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertEquals(location.lineRange().endLine().line(), 0,
                "NullLocation.lineRange().endLine().line() must be 0");
    }

    @Test
    void testNullLocation_textRange_isNonNull() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertNotNull(location.textRange(), "NullLocation.textRange() must return a non-null TextRange");
    }

    @Test
    void testNullLocation_textRange_startOffsetIsZero() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertEquals(location.textRange().startOffset(), 0,
                "NullLocation.textRange().startOffset() must be 0");
    }

    @Test
    void testNullLocation_implementsLocation() {
        ConverterCommonUtils.NullLocation location = new ConverterCommonUtils.NullLocation();
        Assert.assertTrue(location instanceof io.ballerina.tools.diagnostics.Location,
                "NullLocation must implement io.ballerina.tools.diagnostics.Location");
    }
}
