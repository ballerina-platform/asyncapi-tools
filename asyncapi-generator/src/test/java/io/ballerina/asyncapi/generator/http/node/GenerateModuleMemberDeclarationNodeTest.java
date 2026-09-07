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
package io.ballerina.asyncapi.generator.http.node;

import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link GenerateModuleMemberDeclarationNode}'s type-level doc-comment generation
 * (record, allOf record, type alias, enum, and the anydata fallback), mirroring the field-level
 * doc-comment coverage that already exists implicitly via other tests.
 */
public class GenerateModuleMemberDeclarationNodeTest {

    private String generate(String schemaName, AsyncApiSchema schema) throws GeneratorException {
        return generate(schemaName, schema, Map.of(schemaName, schema));
    }

    /**
     * Same as {@link #generate(String, AsyncApiSchema)}, but lets a test supply a richer
     * {@code allSchemas} map - needed for hoisting-collision tests, where the candidate hoisted
     * name must be checked against a pre-existing top-level schema.
     */
    private String generate(String schemaName, AsyncApiSchema schema, Map<String, AsyncApiSchema> allSchemas)
            throws GeneratorException {
        GenerateModuleMemberDeclarationNode gen = new GenerateModuleMemberDeclarationNode(
                Map.entry(schemaName, schema), allSchemas, new HashSet<>(allSchemas.keySet()));
        StringBuilder result = new StringBuilder(gen.generate().toString());
        for (TypeDefinitionNode hoisted : gen.getHoistedTypes()) {
            result.append(hoisted);
        }
        return result.toString();
    }

    @Test
    void testRecordWithTitleGetsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .title("A GitHub Actions workflow run")
                .properties(Map.of("id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        String result = generate("WorkflowRun", schema);
        Assert.assertTrue(result.contains("#A GitHub Actions workflow run"),
                "Record type should carry its title as a doc comment: " + result);
    }

    @Test
    void testRecordWithDescriptionOnlyGetsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .description("Payload for push events")
                .properties(Map.of("ref", AsyncApiSchema.builder().type("string").build()))
                .build();
        String result = generate("PushPayload", schema);
        Assert.assertTrue(result.contains("#Payload for push events"),
                "Record type should fall back to description when title is absent: " + result);
    }

    @Test
    void testRecordWithNeitherTitleNorDescriptionOmitsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        String result = generate("Bare", schema);
        Assert.assertFalse(result.contains("#"),
                "Record type with neither title nor description should have no doc comment: " + result);
    }

    @Test
    void testExplicitlyOpenObjectRendersAsMapJson() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .nullable(true)
                .additionalProperties(true)
                .description("Arbitrary caller-supplied JSON")
                .build();
        String result = generate("ClientPayload", schema);
        Assert.assertTrue(result.contains("map<json>"),
                "A property-less object schema with additionalProperties: true should render as "
                        + "map<json>, not an empty record: " + result);
        Assert.assertFalse(result.contains("record{}"),
                "Should not fall back to the closed-empty-record placeholder: " + result);
    }

    @Test
    void testUnspecifiedObjectStillRendersAsEmptyRecord() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .nullable(true)
                .description("Never filled in by the spec author")
                .build();
        String result = generate("StillBlank", schema);
        Assert.assertTrue(result.contains("record{}"),
                "A property-less object schema with no additionalProperties marker must keep "
                        + "falling back to record {} - only an explicit additionalProperties: true "
                        + "should change behavior, not mere absence of properties: " + result);
        Assert.assertFalse(result.contains("map<json>"),
                "Must not treat every unfilled object as open just because flattening happened "
                        + "elsewhere: " + result);
    }

    @Test
    void testAllOfRecordWithTitleGetsDocComment() throws GeneratorException {
        AsyncApiSchema sub = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .title("Merged event shape")
                .allOf(List.of(sub))
                .build();
        String result = generate("MergedEvent", schema);
        Assert.assertTrue(result.contains("#Merged event shape"),
                "allOf-merged record should carry the outer schema's title as a doc comment: " + result);
    }

    @Test
    void testTypeAliasWithTitleGetsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("string")
                .title("An opaque identifier")
                .build();
        String result = generate("OpaqueId", schema);
        Assert.assertTrue(result.contains("#An opaque identifier"),
                "Type alias should carry its title as a doc comment: " + result);
    }

    @Test
    void testEnumWithTitleGetsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .title("The action that was performed")
                .enumValue(List.of(new TextNode("created"), new TextNode("deleted")))
                .build();
        String result = generate("ActionEnum", schema);
        Assert.assertTrue(result.contains("#The action that was performed"),
                "Enum type should carry its title as a doc comment: " + result);
    }

    @Test
    void testAnydataFallbackWithTitleGetsDocComment() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .title("Unstructured payload data")
                .build();
        String result = generate("Unstructured", schema);
        Assert.assertTrue(result.contains("#Unstructured payload data"),
                "The anydata-fallback branch should also carry a title as a doc comment: " + result);
    }

    @Test
    void testInlineObjectFieldIsHoistedIntoNamedType() throws GeneratorException {
        // WorkflowRunPayload.workflow, mirroring the real spec: an inline object (no $ref/name)
        // that should now become its own named type instead of a long anonymous inline record.
        AsyncApiSchema workflow = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "id", AsyncApiSchema.builder().type("integer").build(),
                        "name", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema payload = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("workflow", workflow))
                .build();
        String result = generate("WorkflowRunPayload", payload);

        Assert.assertTrue(result.contains("Workflowworkflow?;"),
                "The field should reference a named 'Workflow' type, not an inline record: " + result);
        Assert.assertTrue(result.contains("publictypeWorkflowrecord"),
                "A top-level 'Workflow' type definition should be hoisted out: " + result);
    }

    @Test
    void testHoistedTypeNameCollisionIsDisambiguatedWithParentPrefix() throws GeneratorException {
        // A pre-existing top-level schema already named "Workflow" - the naive hoisted name would
        // collide with it, so the hoister must fall back to a parent-prefixed name instead.
        AsyncApiSchema existingWorkflow = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        AsyncApiSchema inlineWorkflow = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("name", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema payload = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("workflow", inlineWorkflow))
                .build();
        Map<String, AsyncApiSchema> allSchemas = Map.of(
                "Workflow", existingWorkflow,
                "WorkflowRunPayload", payload);

        String result = generate("WorkflowRunPayload", payload, allSchemas);

        Assert.assertTrue(result.contains("WorkflowRunPayloadWorkflowworkflow?;"),
                "On a name collision, the hoisted type should be disambiguated with the parent's "
                        + "name as a prefix: " + result);
    }

    @Test
    void testHoistedTypeNameCollisionOnKeywordDoesNotEmbedStrayQuote() throws GeneratorException {
        // Real bug found via a live regen: "commit" is a Ballerina keyword, so the bare candidate
        // name escapes to 'commit. When a second, different parent's inline "commit" field collides
        // with that already-claimed name, the parent-prefixed fallback must NOT concatenate the
        // pre-escaped "'commit" onto the parent name (producing the invalid identifier
        // "StatusPayload'commit") - it must capitalize+concatenate the RAW hint first, then escape
        // the combined string once, giving the valid "StatusPayloadCommit".
        AsyncApiSchema firstCommit = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("sha", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema firstParent = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("commit", firstCommit))
                .build();
        AsyncApiSchema secondCommit = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("sha", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema secondParent = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("commit", secondCommit))
                .build();
        Map<String, AsyncApiSchema> allSchemas = new LinkedHashMap<>();
        allSchemas.put("BranchesItem", firstParent);
        allSchemas.put("StatusPayload", secondParent);
        Set<String> claimedTypeNames = new HashSet<>(allSchemas.keySet());

        StringBuilder combined = new StringBuilder();
        for (Map.Entry<String, AsyncApiSchema> entry : allSchemas.entrySet()) {
            GenerateModuleMemberDeclarationNode gen =
                    new GenerateModuleMemberDeclarationNode(entry, allSchemas, claimedTypeNames);
            combined.append(gen.generate());
            for (TypeDefinitionNode hoisted : gen.getHoistedTypes()) {
                combined.append(hoisted);
            }
        }
        String result = combined.toString();

        // The exact corrupted type name the real bug produced - "public type" immediately
        // followed by a name with an embedded quote is unambiguous (an escaped identifier only
        // ever appears as a complete standalone token, e.g. field name 'commit, never fused into
        // a type declaration's own name like this).
        Assert.assertFalse(result.contains("publictypeStatusPayload'commitrecord"),
                "The type name must never have a stray escape-quote embedded in the middle: " + result);
        Assert.assertTrue(result.contains("publictype'commitrecord"),
                "The first 'commit' field should hoist to the escaped bare keyword name: " + result);
        Assert.assertTrue(result.contains("publictypeStatusPayloadCommitrecord"),
                "The second, colliding 'commit' field must fall back to a clean parent-prefixed "
                        + "name, not an invalid identifier with an embedded quote: " + result);
    }

    @Test
    void testHoistedTypeNameAvoidsBuiltinSymbolCollision() throws GeneratorException {
        // Real bug found via a live regen: an inline "thread" field naively hoists to "Thread",
        // which is a Ballerina compiler-builtin symbol - bal build rejected it as "redeclared
        // builtin symbol 'Thread'" even though "thread" isn't a lexical keyword (escapeIdentifier
        // has no reason to touch it). The hoister must recognize this collision on its own and fall
        // back to the parent-prefixed name instead.
        AsyncApiSchema thread = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("node_id", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema payload = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("thread", thread))
                .build();
        String result = generate("PullRequestReviewThreadPayload", payload);

        Assert.assertFalse(result.contains("publictypeThreadrecord"),
                "Must not hoist a bare 'Thread' type - it collides with the compiler builtin: " + result);
        Assert.assertTrue(result.contains("publictypePullRequestReviewThreadPayloadThreadrecord"),
                "Should fall back to the parent-prefixed name instead: " + result);
    }

    @Test
    void testArrayItemInlineObjectIsHoisted() throws GeneratorException {
        AsyncApiSchema item = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("name", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema arraySchema = AsyncApiSchema.builder()
                .type("array")
                .items(item)
                .build();
        AsyncApiSchema payload = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("labels", arraySchema))
                .build();
        String result = generate("LabeledPayload", payload);

        Assert.assertTrue(result.contains("LabelsItem[]labels"),
                "The array's inline item schema should be hoisted and referenced by name: " + result);
        Assert.assertTrue(result.contains("publictypeLabelsItemrecord"),
                "A top-level 'LabelsItem' type definition should be hoisted out for the array item: "
                        + result);
    }

    @Test
    void testStandardNullableFieldBecomesOptionalType() throws GeneratorException {
        // The standard AsyncAPI 'nullable' keyword (schema.nullable(), as opposed to the legacy
        // x-nullable extension) must widen the field's type to T? - this is what a real spec author
        // actually writes (confirmed: docs/spec/asyncapi.yml uses plain "nullable: true").
        AsyncApiSchema nullableField = AsyncApiSchema.builder().type("string").nullable(true).build();
        AsyncApiSchema nonNullableField = AsyncApiSchema.builder().type("string").build();
        AsyncApiSchema payload = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("starredAt", nullableField, "action", nonNullableField))
                .build();
        String result = generate("StarPayload", payload);

        Assert.assertTrue(result.contains("string?starredAt"),
                "A field with nullable() == true should get a T? type: " + result);
        Assert.assertTrue(result.contains("stringaction"),
                "A field without 'nullable' should keep its plain type: " + result);
    }
}
