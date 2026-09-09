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

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumMemberNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ENUM_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates a {@link ModuleMemberDeclarationNode} (enum, record, or type alias) for
 * {@code data_types.bal} from a single {@link AsyncApiSchema} entry.
 */
public class GenerateModuleMemberDeclarationNode implements Generator {

    private static final Logger LOG = LogManager.getLogger(GenerateModuleMemberDeclarationNode.class);
    private static final String X_NULLABLE = "x-nullable";
    private static final String SCHEMA_TYPE_INTEGER = "integer";
    private static final String SCHEMA_TYPE_STRING = "string";
    private static final String SCHEMA_TYPE_BOOLEAN = "boolean";
    private static final String SCHEMA_TYPE_NUMBER = "number";
    private static final String SCHEMA_TYPE_DECIMAL = "decimal";
    private static final String SCHEMA_TYPE_FLOAT = "float";
    private static final String SCHEMA_TYPE_DOUBLE = "double";
    private static final String SCHEMA_TYPE_ARRAY = "array";
    private static final String SCHEMA_TYPE_OBJECT = "object";

    private final Map.Entry<String, AsyncApiSchema> entry;
    private final Map<String, AsyncApiSchema> allSchemas;
    private final Set<String> claimedTypeNames;
    private final List<TypeDefinitionNode> hoistedTypes = new ArrayList<>();

    /**
     * Creates a generator for a single schema entry.
     *
     * @param entry            the schema name and its definition
     * @param allSchemas       the full schema map, used to resolve {@code allOf} sub-schema stubs
     * @param claimedTypeNames the set of type names already in use across the whole generation run
     *                         (every top-level schema name, plus every name already hoisted by any
     *                         instance so far) - shared across every {@code GenerateModuleMemberDeclarationNode}
     *                         instance in one {@code DataTypesGenerator} run so two different schemas
     *                         hoisting an inline object with the same field name (e.g. two different
     *                         payloads each with an inline {@code workflow} object) can never produce
     *                         two colliding type definitions. Mutated in place as names are claimed.
     */
    public GenerateModuleMemberDeclarationNode(Map.Entry<String, AsyncApiSchema> entry,
                                               Map<String, AsyncApiSchema> allSchemas,
                                               Set<String> claimedTypeNames) {
        this.entry = entry;
        this.allSchemas = allSchemas;
        this.claimedTypeNames = claimedTypeNames;
    }

    /**
     * Returns any additional top-level type definitions hoisted out of inline object schemas
     * (see {@link #getTypeDescriptorNode}) while building this entry's own declaration. Only
     * meaningful after {@link #generate()} has been called.
     *
     * @return the hoisted type definitions, in the order they were encountered; empty if none
     */
    public List<TypeDefinitionNode> getHoistedTypes() {
        return hoistedTypes;
    }

    @Override
    public ModuleMemberDeclarationNode generate() throws GeneratorException {
        String schemaName = entry.getKey();
        AsyncApiSchema schema = entry.getValue();
        IdentifierToken typeName = AbstractNodeFactory.createIdentifierToken(
                CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(schemaName.trim()), true));

        if (schema.enumValue() != null && !schema.enumValue().isEmpty()) {
            return generateEnum(typeName, schema);
        } else if (schema.properties() != null && !schema.properties().isEmpty()) {
            return generateRecord(typeName, schema);
        } else if (schema.allOf() != null && !schema.allOf().isEmpty()) {
            return generateAllOfRecord(typeName, schema);
        } else if (schema.type() != null) {
            return generateTypeAlias(typeName, schema);
        } else {
            Token anydata = AbstractNodeFactory.createIdentifierToken("anydata");
            MetadataNode metadataNode = createMetadataNode(
                    createMarkdownDocumentationNode(createNodeList(buildDocLines(schema))), createEmptyNodeList());
            return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                    typeName, createBuiltinSimpleNameReferenceNode(null, anydata), createToken(SEMICOLON_TOKEN));
        }
    }

    private ModuleMemberDeclarationNode generateEnum(IdentifierToken typeName, AsyncApiSchema schema) {
        List<Node> enums = new ArrayList<>();
        List<JsonNode> enumValues = schema.enumValue();
        for (int i = 0; i < enumValues.size(); i++) {
            if (i > 0) {
                enums.add(createToken(SyntaxKind.COMMA_TOKEN));
            }
            enums.add(createEnumMemberNode(
                    null,                                                   // no metadata/annotations
                    createIdentifierToken(enumValues.get(i).asText()),
                    null,                                                   // no explicit value assignment (= ...)
                    null));                                                 // no constant expression value
        }
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(buildDocLines(schema))), createEmptyNodeList());
        return createEnumDeclarationNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(ENUM_KEYWORD),
                typeName, createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(enums),
                createToken(CLOSE_BRACE_TOKEN), null);
    }

    private TypeDefinitionNode generateRecord(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        return buildRecordTypeDefinition(typeName, schema);
    }

    /**
     * Builds a {@code public type <name> record {...};} declaration from an object schema's
     * properties. Shared by {@link #generateRecord} (a top-level schema entry) and
     * {@link #getTypeDescriptorNode} (a hoisted inline object schema) - the two cases differ only
     * in where the resulting {@link TypeDefinitionNode} ends up (returned directly vs. collected
     * into {@link #hoistedTypes}), not in how it's built.
     *
     * <p>When the schema explicitly declares {@code additionalProperties: true} (or a schema)
     * alongside real {@code properties} - "these known fields, plus possibly more" - the record
     * is instead built as {@code record {| ...knownFields; json...; |}}: an inclusive ({@code {|
     * |}}) record with an explicit {@code json} rest field. This is not a correctness fix (a
     * plain {@code record { ... }} is open by default in Ballerina and already tolerates and
     * preserves extra fields via {@code cloneWithType}/index access) - it's purely so the "extra
     * fields may exist" contract the spec declares is visible in the generated type itself,
     * instead of relying on a reader already knowing Ballerina's implicit-open-record default.
     */
    private TypeDefinitionNode buildRecordTypeDefinition(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        return buildRecordTypeDefinition(typeName, schema, "");
    }

    /**
     * Same as {@link #buildRecordTypeDefinition(IdentifierToken, AsyncApiSchema)}, but threads
     * {@code fieldNamePrefix} down into {@link #buildRecordFields} so that any of this record's own
     * fields which in turn need hoisting (a nested inline object) get a name derived from this
     * record's own identity rather than just the bare field key. Used by {@link #hoistOneOfUnion}
     * so that, e.g., a {@code MergeQueue} branch's {@code parameters} field hoists to
     * {@code MergeQueueParameters} instead of colliding with every other branch's own generically-
     * named {@code parameters} field. The two-arg overload is the default (empty prefix, i.e. no
     * behavior change) used everywhere else.
     */
    private TypeDefinitionNode buildRecordTypeDefinition(
            IdentifierToken typeName, AsyncApiSchema schema, String fieldNamePrefix) throws GeneratorException {
        List<String> required = schema.required() != null ? schema.required() : List.of();
        NodeList<Node> fieldNodes = createNodeList(buildRecordFields(schema.properties(), required, fieldNamePrefix));
        RecordTypeDescriptorNode recordType;
        if (isExplicitlyOpen(schema)) {
            io.ballerina.compiler.syntax.tree.RecordRestDescriptorNode restDescriptor =
                    NodeFactory.createRecordRestDescriptorNode(
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("json")),
                            createToken(SyntaxKind.ELLIPSIS_TOKEN), createToken(SEMICOLON_TOKEN));
            recordType = createRecordTypeDescriptorNode(
                    createToken(SyntaxKind.RECORD_KEYWORD), createToken(SyntaxKind.OPEN_BRACE_PIPE_TOKEN),
                    fieldNodes, restDescriptor, createToken(SyntaxKind.CLOSE_BRACE_PIPE_TOKEN));
        } else {
            recordType = createRecordTypeDescriptorNode(
                    createToken(SyntaxKind.RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                    fieldNodes, null, createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        }
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(buildDocLines(schema))), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode generateAllOfRecord(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        Map<String, AsyncApiSchema> merged = new LinkedHashMap<>();
        List<String> mergedRequired = new ArrayList<>();
        for (AsyncApiSchema sub : schema.allOf()) {
            AsyncApiSchema resolved = sub;
            if (sub.name() != null && sub.type() == null && sub.properties() == null && sub.allOf() == null) {
                resolved = allSchemas.get(sub.name());
                if (resolved == null) {
                    LOG.warn("Could not resolve allOf sub-schema reference: {} in schema: {}",
                            sub.name(), entry.getKey());
                    continue;
                }
            }
            if (resolved.properties() != null) {
                merged.putAll(resolved.properties());
            }
            if (resolved.required() != null) {
                mergedRequired.addAll(resolved.required());
            }
        }
        NodeList<Node> fieldNodes = createNodeList(buildRecordFields(merged, mergedRequired));
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(
                createToken(SyntaxKind.RECORD_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                fieldNodes, null, createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(buildDocLines(schema))), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode generateTypeAlias(IdentifierToken typeName, AsyncApiSchema schema)
            throws GeneratorException {
        TypeDescriptorNode fieldTypeName = getTypeDescriptorNode(schema, entry.getKey());
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(buildDocLines(schema))), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, createOptionalTypeDescriptorNode(fieldTypeName, createToken(QUESTION_MARK_TOKEN)),
                createToken(SEMICOLON_TOKEN));
    }

    private List<Node> buildRecordFields(Map<String, AsyncApiSchema> properties, List<String> required)
            throws GeneratorException {
        return buildRecordFields(properties, required, "");
    }

    /**
     * Same as {@link #buildRecordFields(Map, List)}, but hoists any field that itself needs a
     * top-level type using {@code fieldNamePrefix + <field name>} as the name hint instead of the
     * bare field name, when {@code fieldNamePrefix} is non-empty. See
     * {@link #buildRecordTypeDefinition(IdentifierToken, AsyncApiSchema, String)}.
     */
    private List<Node> buildRecordFields(
            Map<String, AsyncApiSchema> properties, List<String> required, String fieldNamePrefix)
            throws GeneratorException {
        List<Node> fields = new ArrayList<>();
        for (Map.Entry<String, AsyncApiSchema> field : properties.entrySet()) {
            String rawKey = field.getKey().trim();
            String fieldName;
            NodeList<AnnotationNode> annotations;
            if (CodegenUtils.requiresHeaderAnnotation(rawKey)) {
                fieldName = CodegenUtils.toCamelCase(rawKey);
                AnnotationNode headerAnnotation = NodeParser.parseAnnotation(
                        String.format("@http:Header { name: \"%s\" }", rawKey));
                annotations = createNodeList(headerAnnotation);
            } else {
                fieldName = CodegenUtils.escapeIdentifier(rawKey);
                annotations = createEmptyNodeList();
            }
            IdentifierToken fieldNameToken = AbstractNodeFactory.createIdentifierToken(fieldName);
            String fieldNameHint = fieldNamePrefix.isEmpty() ? rawKey : fieldNamePrefix + capitalize(rawKey);
            TypeDescriptorNode fieldType = getTypeDescriptorNode(field.getValue(), fieldNameHint);
            boolean isOptional = !required.contains(rawKey);
            Token questionMark = isOptional ? createToken(QUESTION_MARK_TOKEN) : null;
            Token semicolon = createToken(SEMICOLON_TOKEN);
            MetadataNode fieldMetadata = createMetadataNode(
                    createMarkdownDocumentationNode(createNodeList(buildDocLines(field.getValue()))), annotations);
            RecordFieldNode recordField = createRecordFieldNode(fieldMetadata,
                    null, // no readonly keyword
                    fieldType, fieldNameToken, questionMark, semicolon);
            fields.add(recordField);
        }
        return fields;
    }

    /**
     * Builds the {@code #} doc-comment lines for a schema's {@code title} (preferred) or
     * {@code description}, if either is present. Shared by both the type-level generators
     * ({@link #generateRecord}, {@link #generateAllOfRecord}, {@link #generateTypeAlias},
     * {@link #generateEnum}, and the {@code anydata} fallback in {@link #generate()}) and
     * {@link #buildRecordFields}, which applies the same extraction per field.
     *
     * @param schema the schema whose {@code title}/{@code description} becomes the doc comment
     * @return the doc-comment line nodes, empty if the schema has neither
     */
    private List<Node> buildDocLines(AsyncApiSchema schema) {
        List<Node> docLines = new ArrayList<>();
        String docText = schema.title() != null ? schema.title() : schema.description();
        if (docText != null) {
            for (String line : docText.split("\n")) {
                docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                        createToken(HASH_TOKEN), createNodeList(createIdentifierToken(line))));
            }
        }
        return docLines;
    }

    /**
     * Resolves the type descriptor for a field (or array-item, or type-alias) schema.
     *
     * <p>A schema resolved via {@code $ref} (has {@code schema.name() != null}) already has a
     * reusable name, so it's referenced directly. An inline object schema (has {@code properties()}
     * but no {@code $ref} name) has no such name - rather than embedding it as a long anonymous
     * {@code record {...}} inline (the previous behavior), it's always hoisted into its own
     * top-level named type instead, exactly like a {@code $ref}'d schema would be, so the shape of
     * the generated code no longer depends on whether the spec author happened to extract a given
     * object into a reusable component or left it inline - the generator no longer mirrors that
     * spec-authoring inconsistency.
     *
     * @param schema   the field/item/alias-target schema to resolve
     * @param nameHint a name to derive a hoisted type's name from if this schema turns out to be an
     *                 inline object (typically the field key, or the array field's key for an
     *                 array's item schema) - unused for every other case
     */
    private TypeDescriptorNode getTypeDescriptorNode(AsyncApiSchema schema, String nameHint)
            throws GeneratorException {
        TypeDescriptorNode typeDesc;
        if (schema.oneOf() != null && !schema.oneOf().isEmpty()) {
            typeDesc = hoistOneOfUnion(schema, nameHint);
        } else if (schema.properties() != null && !schema.properties().isEmpty()) {
            typeDesc = hoistInlineObject(schema, nameHint);
        } else if (schema.type() != null) {
            typeDesc = getTypeDescriptorForPrimitive(schema, nameHint);
        } else if (schema.name() != null) {
            String typeName = CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(schema.name()), true);
            typeDesc = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(typeName));
        } else {
            typeDesc = createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken("anydata"));
        }
        return applyNullable(schema, typeDesc);
    }

    private TypeDescriptorNode getTypeDescriptorForPrimitive(AsyncApiSchema schema, String nameHint)
            throws GeneratorException {
        return switch (schema.type()) {
            case SCHEMA_TYPE_INTEGER ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("int"));
            case SCHEMA_TYPE_STRING ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string"));
            case SCHEMA_TYPE_BOOLEAN ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("boolean"));
            case SCHEMA_TYPE_NUMBER, SCHEMA_TYPE_DECIMAL -> {
                if (schema.format() != null
                        && (schema.format().equals("float") || schema.format().equals("double"))) {
                    yield createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("float"));
                }
                yield createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("decimal"));
            }
            case SCHEMA_TYPE_FLOAT, SCHEMA_TYPE_DOUBLE ->
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("float"));
            case SCHEMA_TYPE_ARRAY -> getArrayTypeDescriptor(schema, nameHint);
            case SCHEMA_TYPE_OBJECT -> {
                if (schema.properties() != null && !schema.properties().isEmpty()) {
                    yield hoistInlineObject(schema, nameHint);
                }
                if (isExplicitlyOpen(schema)) {
                    yield NodeFactory.createMapTypeDescriptorNode(
                            createToken(SyntaxKind.MAP_KEYWORD),
                            NodeFactory.createTypeParameterNode(
                                    createToken(SyntaxKind.LT_TOKEN),
                                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("json")),
                                    createToken(SyntaxKind.GT_TOKEN)));
                }
                yield createRecordTypeDescriptorNode(
                        AbstractNodeFactory.createIdentifierToken("record"),
                        AbstractNodeFactory.createIdentifierToken("{"),
                        createEmptyNodeList(), null,
                        AbstractNodeFactory.createIdentifierToken("}"));
            }
            default -> createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("anydata"));
        };
    }

    /**
     * Returns whether a {@code type: object} schema is explicitly declared open (arbitrary,
     * caller-defined keys allowed beyond any declared {@code properties}) rather than simply
     * never having been filled in. Only an explicit {@code additionalProperties: true} (or a
     * schema constraining the value type) counts -- the field being merely absent must NOT be
     * treated as open, since JSON Schema's abstract default for a missing {@code
     * additionalProperties} is "open" but here it almost always means the spec just hasn't been
     * written yet, not that the field is genuinely free-form. Used both for property-less schemas
     * (which fall back to {@code map<json>}, see {@link #getTypeDescriptorForPrimitive}) and for
     * schemas with real properties plus this marker (which get an explicit {@code json} rest
     * field, see {@link #buildRecordTypeDefinition}).
     *
     * @param schema the object schema to check
     * @return {@code true} only if {@code additionalProperties} is explicitly {@code true} or a
     *         schema; {@code false} if absent or explicitly {@code false}
     */
    private boolean isExplicitlyOpen(AsyncApiSchema schema) {
        Object additionalProperties = schema.additionalProperties();
        return Boolean.TRUE.equals(additionalProperties) || additionalProperties instanceof AsyncApiSchema;
    }

    private TypeDescriptorNode getArrayTypeDescriptor(AsyncApiSchema schema, String nameHint)
            throws GeneratorException {
        if (schema.items() == null) {
            throw new GeneratorException("Array schema is missing the 'items' attribute");
        }
        ArrayDimensionNode dimension = NodeFactory.createArrayDimensionNode(
                createToken(OPEN_BRACKET_TOKEN), null, createToken(CLOSE_BRACKET_TOKEN));
        TypeDescriptorNode memberType;
        if (schema.items() instanceof AsyncApiSchema itemSchema) {
            memberType = getTypeDescriptorNode(itemSchema, nameHint + "Item");
        } else {
            memberType = createBuiltinSimpleNameReferenceNode(null,
                    AbstractNodeFactory.createIdentifierToken("anydata"));
        }
        return createArrayTypeDescriptorNode(memberType, createNodeList(dimension));
    }

    /**
     * Hoists an inline object schema into its own top-level {@code public type} declaration
     * (collected into {@link #hoistedTypes}), returning a reference to it - see
     * {@link #getTypeDescriptorNode} for why this always happens rather than embedding the object
     * as an anonymous inline record.
     */
    private TypeDescriptorNode hoistInlineObject(AsyncApiSchema schema, String nameHint)
            throws GeneratorException {
        IdentifierToken hoistedTypeName = createIdentifierToken(resolveHoistedTypeName(nameHint));
        hoistedTypes.add(buildRecordTypeDefinition(hoistedTypeName, schema));
        return createBuiltinSimpleNameReferenceNode(null, hoistedTypeName);
    }

    /**
     * Hoists a {@code oneOf} schema into a top-level {@code public type <name> Branch1|Branch2|...;}
     * union, one member per branch, and returns a reference to that union.
     *
     * <p>Each branch goes back through {@link #getTypeDescriptorNode}, so a branch with real
     * {@code properties} is hoisted via the exact same {@link #hoistInlineObject}/{@link
     * #buildRecordTypeDefinition} path every other object schema in this generator uses - meaning
     * every union member is an open record (Ballerina's default for a bare {@code record { ... }},
     * confirmed empirically - {@code cloneWithType} tolerates and preserves fields beyond what's
     * declared) exactly like everywhere else, not a special narrower case for {@code oneOf}
     * branches specifically.
     *
     * <p>Each branch's hoisted name is derived from its own discriminator, when there is one - a
     * sibling {@code type} property declaring a single-value {@code enum} (the common "tagged
     * union" shape, e.g. a ruleset rule's {@code type: "merge_queue"}) - falling back to a plain
     * positional name otherwise. This is purely for readability; nothing here depends on actually
     * discriminating between branches at runtime (that's what the union's own member types do).
     *
     * @param schema   the {@code oneOf} schema to hoist
     * @param nameHint a name to derive the union type's own name from
     * @return a reference to the newly-hoisted union type
     */
    private TypeDescriptorNode hoistOneOfUnion(AsyncApiSchema schema, String nameHint) throws GeneratorException {
        List<TypeDescriptorNode> branchTypes = new ArrayList<>();
        List<AsyncApiSchema> branches = schema.oneOf();
        for (int i = 0; i < branches.size(); i++) {
            AsyncApiSchema branch = branches.get(i);
            String branchHint = capitalize(nameHint) + capitalize(branchDiscriminatorHint(branch, i));
            if (branch.properties() != null && !branch.properties().isEmpty()) {
                // Hoisted directly (rather than via the generic getTypeDescriptorNode/
                // hoistInlineObject dispatch) so the branch's own name-prefix reaches its nested
                // fields too - see buildRecordTypeDefinition's 3-arg overload.
                IdentifierToken branchTypeName = createIdentifierToken(resolveHoistedTypeName(branchHint));
                hoistedTypes.add(buildRecordTypeDefinition(branchTypeName, branch, branchHint));
                TypeDescriptorNode branchRef = createBuiltinSimpleNameReferenceNode(null, branchTypeName);
                branchTypes.add(applyNullable(branch, branchRef));
            } else {
                branchTypes.add(getTypeDescriptorNode(branch, branchHint));
            }
        }
        String unionName = resolveHoistedTypeName(nameHint);
        hoistedTypes.add(new GenerateUnionDescriptorNode(branchTypes, unionName).generate());
        return createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(unionName));
    }

    /**
     * Returns a name fragment for one {@code oneOf} branch, preferring its own discriminator value
     * (a sibling {@code type} property's single-value {@code enum}, e.g. {@code "merge_queue"})
     * over a plain positional fallback ({@code "Branch0"}, {@code "Branch1"}, ...).
     *
     * @param branch the branch schema
     * @param index  the branch's position, used only in the positional fallback
     * @return a name fragment identifying this branch, before {@link #resolveHoistedTypeName}'s
     *         own collision handling is applied
     */
    private String branchDiscriminatorHint(AsyncApiSchema branch, int index) {
        if (branch.properties() != null) {
            AsyncApiSchema typeProperty = branch.properties().get("type");
            if (typeProperty != null && typeProperty.enumValue() != null && typeProperty.enumValue().size() == 1) {
                return typeProperty.enumValue().get(0).asText();
            }
        }
        return "Branch" + index;
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Names that collide with a compiler-builtin symbol even after {@link CodegenUtils#escapeIdentifier}
     * quote-escaping (e.g. {@code 'error} is still the same symbol as {@code error} - the quote is an
     * escape marker for using a keyword as an identifier, not a way to get a distinct name), or that
     * are builtin symbols without being lexical keywords at all (e.g. {@code Thread} - confirmed by a
     * real {@code bal build} rejecting a hoisted type generated with that name). Checked in addition
     * to {@link #claimedTypeNames}, since neither {@link CodegenUtils#escapeIdentifier} nor
     * {@link CodegenUtils#getValidName} know about this class of collision - they only reason about
     * lexical keywords, not the compiler's builtin symbol table.
     */
    private static final Set<String> RESERVED_BUILTIN_TYPE_NAMES = Set.of(
            "error", "anydata", "any", "never", "readonly", "handle", "future", "typedesc",
            "stream", "table", "map", "record", "object", "service", "function", "var",
            "int", "string", "boolean", "float", "decimal", "byte", "xml", "json", "thread");

    /**
     * Picks a unique type name for a newly-hoisted inline object, trying the field name itself
     * first, then that name prefixed with the enclosing top-level schema's name, then a numeric
     * suffix - checked and reserved against {@link #claimedTypeNames} (shared across every
     * {@code GenerateModuleMemberDeclarationNode} instance in the run, so this can never collide
     * with a pre-existing schema name or a type hoisted while processing a different entry) and
     * {@link #RESERVED_BUILTIN_TYPE_NAMES}.
     *
     * <p>The parent-prefixed fallback is built by capitalizing the hint's first letter and
     * concatenating it onto the raw parent name, then escaping/validating that combined string
     * once - not by concatenating two already-escaped fragments, which can embed a leading
     * {@code '} escape marker in the middle of the result (e.g. {@code "StatusPayload" + "'commit"}
     * would produce the invalid identifier {@code StatusPayload'commit}). Capitalizing the hint
     * fragment before concatenating also means the combined raw string is essentially never itself
     * a reserved keyword even when the bare hint was (only bare {@code "commit"} is a keyword,
     * {@code "StatusPayloadCommit"} isn't), so this naturally avoids needing the quote-escape at
     * all in the common case, as a side effect of producing cleaner PascalCase names.
     */
    private String resolveHoistedTypeName(String nameHint) {
        String candidate = buildTypeName(nameHint);
        if (isAvailable(candidate)) {
            claimedTypeNames.add(candidate);
            return candidate;
        }
        String capitalizedHint = nameHint.isEmpty() ? nameHint
                : nameHint.substring(0, 1).toUpperCase(Locale.ROOT) + nameHint.substring(1);
        String parentPrefixed = buildTypeName(entry.getKey().trim() + capitalizedHint);
        if (isAvailable(parentPrefixed)) {
            claimedTypeNames.add(parentPrefixed);
            return parentPrefixed;
        }
        int suffix = 2;
        String numbered;
        do {
            numbered = parentPrefixed + suffix;
            suffix++;
        } while (!isAvailable(numbered));
        claimedTypeNames.add(numbered);
        return numbered;
    }

    private static String buildTypeName(String rawName) {
        return CodegenUtils.getValidName(CodegenUtils.escapeIdentifier(rawName), true);
    }

    private boolean isAvailable(String candidateTypeName) {
        return !claimedTypeNames.contains(candidateTypeName)
                && !RESERVED_BUILTIN_TYPE_NAMES.contains(candidateTypeName.toLowerCase(Locale.ROOT))
                && !RESERVED_BUILTIN_TYPE_NAMES.contains(
                        candidateTypeName.replaceFirst("^'", "").toLowerCase(Locale.ROOT));
    }

    /**
     * Applies the standard AsyncAPI/OpenAPI {@code nullable} keyword by widening the type
     * descriptor to a {@code T?} union, if the schema declares it.
     *
     * <p>Also checks the legacy {@code x-nullable} extension key as a fallback, in case any
     * existing spec relies on it - real specs almost always use the standard {@code nullable}
     * field, which {@link AsyncApiSchema#nullable()} now carries directly (see
     * {@code SchemaMapper#mapNullable} in {@code asyncapi-core} for how it's extracted).
     */
    private TypeDescriptorNode applyNullable(AsyncApiSchema schema, TypeDescriptorNode typeDesc) {
        boolean nullable = Boolean.TRUE.equals(schema.nullable()) || isLegacyExtensionNullable(schema);
        if (nullable) {
            return createOptionalTypeDescriptorNode(typeDesc, createToken(QUESTION_MARK_TOKEN));
        }
        return typeDesc;
    }

    private boolean isLegacyExtensionNullable(AsyncApiSchema schema) {
        Map<String, JsonNode> extensions = schema.extensions();
        return extensions != null && extensions.containsKey(X_NULLABLE)
                && "true".equals(extensions.get(X_NULLABLE).asText());
    }
}
