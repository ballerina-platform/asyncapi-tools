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
package io.ballerina.asyncapi.generator.ws.asyncspec.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.apicurio.datamodels.models.union.MultiFormatSchemaSchemaUnion;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ReadonlyTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.SingletonTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_REASON;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_REASON_DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_STATUS;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_STATUS_DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CUSTOM_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHERKEY_NULLABLE_EXCEPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHERKEY_OPTIONAL_EXCEPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DISPATCHER_KEY_TYPE_EXCEPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.DOUBLE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FLOAT;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FRAME_TYPE_CLOSE_NODE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.FRAME_TYPE_DESCRIPTION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.INTEGER;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.NUMBER;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.OBJECT;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.PREDEFINED_CLOSE_FRAME_TYPE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.STRING;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;

/**
 * Maps Ballerina type definitions to AsyncAPI component schemas.
 * Corresponds to the legacy {@code AsyncApiComponentMapper}.
 */
public class AsyncApiComponentMapper {

    private final AsyncApi30ComponentsImpl components;
    private final List<AsyncApiConverterDiagnostic> diagnostics;

    private static final Set<String> closeFrameTypes =
            Set.of(PREDEFINED_CLOSE_FRAME_TYPE, CUSTOM_CLOSE_FRAME_TYPE);

    /**
     * Constructs the mapper with the components object to populate.
     *
     * @param components the AsyncAPI 3.0 components to write schemas into
     */
    public AsyncApiComponentMapper(AsyncApi30ComponentsImpl components) {
        this.components = components;
        this.diagnostics = new ArrayList<>();
    }

    /**
     * Returns the diagnostics produced during mapping.
     *
     * @return unmodifiable list of diagnostics
     */
    public List<AsyncApiConverterDiagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    /**
     * Maps a Ballerina type reference to an AsyncAPI component schema.
     *
     * @param typeSymbol      the type to map
     * @param dispatcherValue optional dispatcher key value for dispatcher-type validation
     */
    public void createComponentSchema(TypeSymbol typeSymbol, String dispatcherValue) {
        String componentName =
                ConverterCommonUtils.unescapeIdentifier(typeSymbol.getName().orElseThrow().trim());
        Map<String, ?> allSchemas = this.components.getSchemas();
        boolean isComponentContains = allSchemas != null && allSchemas.containsKey(componentName);

        if (allSchemas == null || !isComponentContains || dispatcherValue != null) {
            Map<String, String> apiDocs =
                    getRecordFieldsAPIDocsMap((TypeReferenceTypeSymbol) typeSymbol, componentName);
            String typeDoc = null;
            if (!apiDocs.isEmpty()) {
                typeDoc = apiDocs.get(typeSymbol.getName().get());
            }
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeSymbol;
            TypeSymbol type = typeRef.typeDescriptor();
            if (type.typeKind() == TypeDescKind.INTERSECTION) {
                type = excludeReadonlyIfPresent(type);
            }
            BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
            if (isCloseFrameRecordType(typeSymbol)) {
                this.components.addSchema(componentName,
                        (MultiFormatSchemaSchemaUnion) getCloseFrameSchema(type));
                return;
            }
            switch (type.typeKind()) {
                case RECORD:
                    handleRecordTypeSymbol((RecordTypeSymbol) type, componentName, apiDocs, dispatcherValue);
                    break;
                case TYPE_REFERENCE:
                    schema.setType(OBJECT);
                    schema.set$ref(
                            ConverterCommonUtils.unescapeIdentifier(type.getName().orElseThrow().trim()));
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) schema);
                    TypeReferenceTypeSymbol referredType = (TypeReferenceTypeSymbol) type;
                    createComponentSchema(referredType, dispatcherValue);
                    break;
                case STRING:
                    schema.setType(STRING);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) schema);
                    break;
                case INT:
                    schema.setType(INTEGER);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) schema);
                    break;
                case DECIMAL:
                    schema.setType(NUMBER);
                    schema.setFormat(DOUBLE);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) schema);
                    break;
                case FLOAT:
                    schema.setType(NUMBER);
                    schema.setFormat(FLOAT);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) schema);
                    break;
                case ARRAY:
                case TUPLE:
                    BalAsyncApi30SchemaImpl arraySchema = mapArrayToArraySchema(type, componentName);
                    arraySchema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) arraySchema);
                    break;
                case UNION:
                    BalAsyncApi30SchemaImpl unionSchema = handleUnionType(
                            (UnionTypeSymbol) type, new BalAsyncApi30SchemaImpl(),
                            componentName, null, null);
                    unionSchema.setDescription(typeDoc);
                    components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) unionSchema);
                    break;
                case MAP:
                    MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) type;
                    TypeSymbol typeParam = mapTypeSymbol.typeParam();
                    if (typeParam.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                        TypeReferenceTypeSymbol typeReferenceTypeSymbol =
                                (TypeReferenceTypeSymbol) typeParam;
                        BalAsyncApi30SchemaImpl objectSchema = new BalAsyncApi30SchemaImpl();
                        objectSchema.setType(AsyncAPIType.OBJECT.toString());
                        BalAsyncApi30SchemaImpl objectSchema2 = new BalAsyncApi30SchemaImpl();
                        objectSchema2.setType(AsyncAPIType.OBJECT.toString());
                        objectSchema2.set$ref(ConverterCommonUtils.unescapeIdentifier(
                                typeReferenceTypeSymbol.getName().orElseThrow().trim()));
                        objectSchema.setAdditionalProperties(objectSchema2);
                        components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) objectSchema);
                        createComponentSchema(typeReferenceTypeSymbol, dispatcherValue);
                    } else {
                        TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                        BalAsyncApi30SchemaImpl asyncApiSchema =
                                getAsyncApiSchema(typeDescKind.getName());
                        //TODO : have to check here asyncApiSchema.getType() == null ? true : asyncApiSchema
                        BalAsyncApi30SchemaImpl objectSchema = new BalAsyncApi30SchemaImpl();
                        objectSchema.setType(AsyncAPIType.OBJECT.toString());
                        objectSchema.setAdditionalProperties(asyncApiSchema.getType() == null
                                ? new BooleanUnionValueImpl(true) : asyncApiSchema);
                        components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) objectSchema);
                    }
                    break;
                default:
                    DiagnosticMessages errorMessage = DiagnosticMessages.AAS_CONVERTOR_106;
                    IncompatibleRemoteDiagnostic error = new IncompatibleRemoteDiagnostic(
                            errorMessage, typeRef.getLocation().get(), type.typeKind().getName());
                    diagnostics.add(error);
                    break;
            }
        }
    }

    private BalAsyncApi30SchemaImpl handleRecordTypeSymbol(RecordTypeSymbol recordTypeSymbol,
                                                           String componentName,
                                                           Map<String, String> apiDocs,
                                                           String dispatcherValue) {
        List<TypeSymbol> typeInclusions = recordTypeSymbol.typeInclusions();
        Map<String, RecordFieldSymbol> recordFields = recordTypeSymbol.fieldDescriptors();
        HashSet<String> unionKeys = new HashSet<>(recordFields.keySet());
        BalAsyncApi30SchemaImpl recordSchema = null;
        if (typeInclusions.isEmpty()) {
            recordSchema = generateObjectSchemaFromRecordFields(
                    componentName, recordFields, apiDocs, dispatcherValue);
        } else {
            mapTypeInclusionToAllOfSchema(
                    componentName, typeInclusions, recordFields, unionKeys, apiDocs, dispatcherValue);
        }
        return recordSchema;
    }

    /**
     * Creates API documentation map for record fields.
     */
    private Map<String, String> getRecordFieldsAPIDocsMap(TypeReferenceTypeSymbol typeSymbol,
                                                          String componentName) {
        Map<String, String> apiDocs = new LinkedHashMap<>();
        TypeDefinitionSymbol recordTypeDefinitionSymbol =
                (TypeDefinitionSymbol) (typeSymbol.definition());
        if (recordTypeDefinitionSymbol.typeDescriptor() instanceof RecordTypeSymbol) {
            RecordTypeSymbol recordType =
                    (RecordTypeSymbol) recordTypeDefinitionSymbol.typeDescriptor();
            if (recordTypeDefinitionSymbol.documentation().isPresent()) {
                apiDocs = recordTypeDefinitionSymbol.documentation().get().parameterMap();
            }
            Map<String, RecordFieldSymbol> recordFieldSymbols = recordType.fieldDescriptors();
            for (Map.Entry<String, RecordFieldSymbol> fields : recordFieldSymbols.entrySet()) {
                Optional<Documentation> fieldDoc = fields.getValue().documentation();
                if (fieldDoc.isPresent() && fieldDoc.get().description().isPresent()) {
                    String field = ConverterCommonUtils.unescapeIdentifier(fields.getKey());
                    if (!apiDocs.containsKey(field)) {
                        apiDocs.put(field, fieldDoc.get().description().get());
                    }
                }
            }
        }
        Symbol recordSymbol = typeSymbol.definition();
        Optional<Documentation> documentation = ((Documentable) recordSymbol).documentation();
        if (documentation.isPresent() && documentation.get().description().isPresent()) {
            Optional<String> description = documentation.get().description();
            apiDocs.put(componentName, description.get().trim());
        }
        return apiDocs;
    }

    /**
     * Maps Ballerina type inclusions to an AsyncAPI allOf composed schema.
     */
    private void mapTypeInclusionToAllOfSchema(String componentName,
                                               List<TypeSymbol> typeInclusions,
                                               Map<String, RecordFieldSymbol> recordFields,
                                               HashSet<String> unionKeys,
                                               Map<String, String> apiDocs,
                                               String dispatcherValue) {
        BalAsyncApi30SchemaImpl allOfSchema = new BalAsyncApi30SchemaImpl();
        for (TypeSymbol typeInclusion : typeInclusions) {
            BalAsyncApi30SchemaImpl referenceSchema = new BalAsyncApi30SchemaImpl();
            String typeInclusionName = typeInclusion.getName().orElseThrow();
            referenceSchema.set$ref(
                    SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeInclusionName));
            allOfSchema.addAllOf(referenceSchema);
            if (typeInclusion.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                TypeReferenceTypeSymbol typeRecord = (TypeReferenceTypeSymbol) typeInclusion;
                apiDocs.putAll(getRecordFieldsAPIDocsMap(typeRecord, typeInclusionName));
                if (typeRecord.typeDescriptor() instanceof RecordTypeSymbol typeInclusionRecord) {
                    Map<String, RecordFieldSymbol> tInFields =
                            typeInclusionRecord.fieldDescriptors();
                    unionKeys.addAll(tInFields.keySet());
                    unionKeys.removeAll(tInFields.keySet());
                    generateObjectSchemaFromRecordFields(typeInclusionName, tInFields, apiDocs, null);
                }
            }
        }
        Map<String, RecordFieldSymbol> filteredField = new LinkedHashMap<>();
        recordFields.forEach((key1, value) -> unionKeys.stream()
                .filter(key -> ConverterCommonUtils.unescapeIdentifier(key1.trim())
                        .equals(ConverterCommonUtils.unescapeIdentifier(key)))
                .forEach(key ->
                        filteredField.put(ConverterCommonUtils.unescapeIdentifier(key1), value)));
        BalAsyncApi30SchemaImpl objectSchema = generateObjectSchemaFromRecordFields(
                componentName, filteredField, apiDocs, dispatcherValue);
        allOfSchema.addAllOf(objectSchema);
        this.components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) allOfSchema);
    }

    /**
     * Maps Ballerina record fields to an AsyncAPI object schema.
     *
     * @param componentName   schema name in the components map
     * @param rfields         record fields to map
     * @param apiDocs         API documentation map for field descriptions
     * @param dispatcherValue optional dispatcher key for const-value injection
     * @return the generated object schema
     */
    public BalAsyncApi30SchemaImpl generateObjectSchemaFromRecordFields(
            String componentName, Map<String, RecordFieldSymbol> rfields,
            Map<String, String> apiDocs, String dispatcherValue) {
        BalAsyncApi30SchemaImpl componentSchema = new BalAsyncApi30SchemaImpl();
        componentSchema.setType(AsyncAPIType.OBJECT.toString());
        List<String> required = new ArrayList<>();
        boolean dispatcherValuePresent = false;
        componentSchema.setDescription(apiDocs.get(componentName));
        for (Map.Entry<String, RecordFieldSymbol> field : rfields.entrySet()) {
            String fieldName = ConverterCommonUtils.unescapeIdentifier(field.getKey().trim());
            TypeDescKind fieldTypeKind =
                    excludeReadonlyIfPresent(field.getValue().typeDescriptor()).typeKind();
            String fieldType = fieldTypeKind.toString().toLowerCase(Locale.ENGLISH).trim();
            BalAsyncApi30SchemaImpl property = getAsyncApiSchema(fieldType);
            boolean fieldIsOptional = field.getValue().isOptional();

            if (fieldTypeKind == TypeDescKind.TYPE_REFERENCE) {
                TypeReferenceTypeSymbol typeReference =
                        (TypeReferenceTypeSymbol) field.getValue().typeDescriptor();
                property = handleTypeReference(
                        typeReference, property, isSameRecord(componentName, typeReference));
            } else if (fieldTypeKind == TypeDescKind.UNION) {
                property = handleUnionType(
                        (UnionTypeSymbol) field.getValue().typeDescriptor(),
                        property, componentName, fieldName, dispatcherValue);
            } else if (fieldTypeKind == TypeDescKind.MAP) {
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) field.getValue().typeDescriptor();
                property = handleMapType(componentName, property, mapTypeSymbol);
            } else if (fieldTypeKind == TypeDescKind.RECORD) {
                property = handleRecordTypeSymbol(
                        (RecordTypeSymbol) field.getValue().typeDescriptor(),
                        null, new HashMap<>(), null);
            } else if (fieldTypeKind == TypeDescKind.JSON) {
                property.setAdditionalProperties(new BooleanUnionValueImpl(true));
            }
            if (property.getType() != null) {
                if (property.getType().equals(AsyncAPIType.ARRAY.toString())
                        && !((property).getItems() != null
                        && ((BalAsyncApi30SchemaImpl) (property).getItems()).getOneOf() != null)) {
                    BooleanNode booleanNode = null;
                    if (property.getExtensions() != null) {
                        booleanNode = (BooleanNode) (property.getExtensions().get(X_NULLABLE));
                    }
                    property = mapArrayToArraySchema(field.getValue().typeDescriptor(), componentName);
                    if (booleanNode != null) {
                        property.addExtension(X_NULLABLE, booleanNode);
                    }
                }
            }
            if (dispatcherValue != null && dispatcherValue.equals(fieldName)) {
                if (fieldType.equals(STRING)) {
                    if (!fieldIsOptional) {
                        dispatcherValuePresent = true;
                        property.setConst(new TextNode(componentName));
                    } else {
                        throw new NoSuchElementException(String.format(
                                DISPATCHERKEY_OPTIONAL_EXCEPTION, fieldName, componentName));
                    }
                } else {
                    throw new NoSuchElementException(
                            String.format(DISPATCHER_KEY_TYPE_EXCEPTION, dispatcherValue));
                }
            }
            if (!fieldIsOptional) {
                required.add(fieldName);
            }
            if (apiDocs.containsKey(fieldName)) {
                property.setDescription(apiDocs.get(fieldName));
            }
            componentSchema.addProperty(fieldName, property);
        }
        if (dispatcherValue != null && !dispatcherValuePresent) {
            throw new NoSuchElementException(String.format(
                    DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD, dispatcherValue, componentName));
        }
        if (!required.isEmpty()) {
            componentSchema.setRequired(required);
        }
        if (componentName != null) {
            this.components.addSchema(componentName, (MultiFormatSchemaSchemaUnion) componentSchema);
        }
        return componentSchema;
    }

    /**
     * Builds the fixed-shape AsyncAPI schema for a WebSocket close frame.
     *
     * @param typeSymbol the close-frame record type
     * @return the generated close-frame schema
     */
    public BalAsyncApi30SchemaImpl getCloseFrameSchema(TypeSymbol typeSymbol) {
        BalAsyncApi30SchemaImpl closeFrameSchema = new BalAsyncApi30SchemaImpl();
        closeFrameSchema.setType(AsyncAPIType.OBJECT.toString());
        List<String> requiredFields = new ArrayList<>();

        BalAsyncApi30SchemaImpl frameType = new BalAsyncApi30SchemaImpl();
        frameType.setType(STRING);
        frameType.setConst(FRAME_TYPE_CLOSE_NODE);
        frameType.setDescription(FRAME_TYPE_DESCRIPTION);
        requiredFields.add(FRAME_TYPE);
        closeFrameSchema.addProperty(FRAME_TYPE, frameType);

        BalAsyncApi30SchemaImpl statusCode = new BalAsyncApi30SchemaImpl();
        statusCode.setType(INTEGER);
        statusCode.setDescription(CLOSE_FRAME_STATUS_DESCRIPTION);
        if (typeSymbol instanceof RecordTypeSymbol recordTypeSymbol) {
            if (recordTypeSymbol.fieldDescriptors().containsKey(CLOSE_FRAME_STATUS)) {
                RecordFieldSymbol statusField =
                        recordTypeSymbol.fieldDescriptors().get(CLOSE_FRAME_STATUS);
                if (statusField.typeDescriptor() instanceof SingletonTypeSymbol singletonTypeSymbol) {
                    int status = Integer.parseInt(singletonTypeSymbol.signature());
                    statusCode.setConst(new IntNode(status));
                }
            }
        }
        requiredFields.add(CLOSE_FRAME_STATUS);
        closeFrameSchema.addProperty(CLOSE_FRAME_STATUS, statusCode);

        BalAsyncApi30SchemaImpl reason = new BalAsyncApi30SchemaImpl();
        reason.setType(STRING);
        reason.setDescription(CLOSE_FRAME_REASON_DESCRIPTION);
        if (typeSymbol instanceof RecordTypeSymbol recordTypeSymbol) {
            if (recordTypeSymbol.fieldDescriptors().containsKey(CLOSE_FRAME_REASON)) {
                RecordFieldSymbol reasonField =
                        recordTypeSymbol.fieldDescriptors().get(CLOSE_FRAME_REASON);
                if (!reasonField.isOptional()) {
                    requiredFields.add(CLOSE_FRAME_REASON);
                }
            }
        }
        closeFrameSchema.addProperty(CLOSE_FRAME_REASON, reason);

        closeFrameSchema.setRequired(requiredFields);
        closeFrameSchema.setDescription(CLOSE_FRAME_DESCRIPTION);
        return closeFrameSchema;
    }

    /**
     * Returns {@code true} if the type is a Ballerina WebSocket close-frame record type.
     *
     * @param typeSymbol type to inspect
     * @return true if close-frame record
     */
    public static boolean isCloseFrameRecordType(TypeSymbol typeSymbol) {
        String moduleName = typeSymbol.getModule().flatMap(Symbol::getName).orElse("");
        String symbolName = typeSymbol.getName().orElse("");
        if (WEBSOCKET.equals(moduleName) && closeFrameTypes.contains(symbolName)) {
            return true;
        }
        TypeDescKind typeDescKind = typeSymbol.typeKind();
        if (TypeDescKind.TYPE_REFERENCE.equals(typeDescKind)) {
            return isCloseFrameRecordType(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        } else if (TypeDescKind.RECORD.equals(typeDescKind)) {
            RecordTypeSymbol bRecordTypeSymbol = (RecordTypeSymbol) typeSymbol;
            if (bRecordTypeSymbol.fieldDescriptors().containsKey(CLOSE_FRAME_TYPE)) {
                return isCloseFrameRecordType(
                        bRecordTypeSymbol.fieldDescriptors().get(CLOSE_FRAME_TYPE).typeDescriptor());
            }
        } else if (TypeDescKind.INTERSECTION.equals(typeDescKind)) {
            return isCloseFrameRecordType(
                    ((IntersectionTypeSymbol) typeSymbol).effectiveTypeDescriptor());
        }
        return false;
    }

    /**
     * Returns the type symbol with any {@code readonly} intersection stripped.
     *
     * @param typeSymbol type to normalise
     * @return the non-readonly member of an intersection, or the original symbol
     */
    public TypeSymbol excludeReadonlyIfPresent(TypeSymbol typeSymbol) {
        if (!typeSymbol.typeKind().equals(TypeDescKind.INTERSECTION)) {
            return typeSymbol;
        }
        List<TypeSymbol> typeSymbols =
                ((IntersectionTypeSymbol) typeSymbol).memberTypeDescriptors();
        for (TypeSymbol symbol : typeSymbols) {
            if (!(symbol instanceof ReadonlyTypeSymbol)) {
                typeSymbol = symbol;
                break;
            }
        }
        return typeSymbol;
    }

    private BalAsyncApi30SchemaImpl handleMapType(String componentName,
                                                  BalAsyncApi30SchemaImpl property,
                                                  MapTypeSymbol mapTypeSymbol) {
        TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
        if (typeDescKind == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeReference = (TypeReferenceTypeSymbol) mapTypeSymbol.typeParam();
            BalAsyncApi30SchemaImpl reference = handleTypeReference(
                    typeReference, new BalAsyncApi30SchemaImpl(),
                    isSameRecord(componentName, typeReference));
            property.setAdditionalProperties(reference);
        } else if (typeDescKind == TypeDescKind.ARRAY) {
            BalAsyncApi30SchemaImpl arraySchema =
                    mapArrayToArraySchema(mapTypeSymbol.typeParam(), componentName);
            property.setAdditionalProperties(arraySchema);
        } else {
            BalAsyncApi30SchemaImpl asyncApiSchema = getAsyncApiSchema(typeDescKind.getName());
            property.setAdditionalProperties(asyncApiSchema.getType() == null
                    ? new BooleanUnionValueImpl(true) : asyncApiSchema);
        }
        return property;
    }

    /**
     * Handles a field whose type is a type reference (record, enum, or other named type).
     */
    private BalAsyncApi30SchemaImpl handleTypeReference(TypeReferenceTypeSymbol typeReferenceSymbol,
                                                        BalAsyncApi30SchemaImpl property,
                                                        boolean isCyclicRecord) {
        if (typeReferenceSymbol.definition().kind() == SymbolKind.ENUM) {
            EnumSymbol enumSymbol = (EnumSymbol) typeReferenceSymbol.definition();
            property = mapEnumValues(enumSymbol);
        } else {
            property.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(
                    typeReferenceSymbol.getName().orElseThrow().trim()));
            if (!isCyclicRecord) {
                createComponentSchema(typeReferenceSymbol, null);
            }
        }
        return property;
    }

    /**
     * Handles union type fields, generating oneOf or nullable schemas as appropriate.
     * <p>
     * TODO: uncomment after fixing ballerina lang union type handling issue
     */
    private BalAsyncApi30SchemaImpl handleUnionType(UnionTypeSymbol unionType,
                                                    BalAsyncApi30SchemaImpl property,
                                                    String parentComponentName,
                                                    String fieldName,
                                                    String dispatcherValue) {
        List<TypeSymbol> unionTypes = unionType.memberTypeDescriptors();
        List<BalAsyncApi30SchemaImpl> properties = new ArrayList<>();
        String nullable = FALSE;
        for (TypeSymbol union : unionTypes) {
            if (union.typeKind() == TypeDescKind.NIL
                    && fieldName != null && fieldName.equals(dispatcherValue)) {
                throw new NoSuchElementException(String.format(
                        DISPATCHERKEY_NULLABLE_EXCEPTION, fieldName, parentComponentName));
            } else if (union.typeKind() == TypeDescKind.NIL) {
                nullable = TRUE;
            } else if (union.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                property = getAsyncApiSchema(union.typeKind().getName().trim());
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) union;
                property = handleTypeReference(typeReferenceTypeSymbol, property,
                        isSameRecord(parentComponentName, typeReferenceTypeSymbol));
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.UNION) {
                property = handleUnionType(
                        (UnionTypeSymbol) union, property, parentComponentName, null, null);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.ARRAY || union.typeKind() == TypeDescKind.TUPLE) {
                property = mapArrayToArraySchema(union, parentComponentName);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.MAP) {
                if (parentComponentName != null) {
                    MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) union;
                    TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                    BalAsyncApi30SchemaImpl asyncApiSchema =
                            getAsyncApiSchema(typeDescKind.getName());
                    BalAsyncApi30SchemaImpl objectSchema =
                            getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                    objectSchema.setAdditionalProperties(asyncApiSchema.getType() == null
                            ? new BooleanUnionValueImpl(true) : asyncApiSchema);
                    property = objectSchema;
                    properties.add(property);
                    components.addSchema(parentComponentName,
                            (MultiFormatSchemaSchemaUnion) property);
                }
            } else {
                property = getAsyncApiSchema(union.typeKind().getName().trim());
                properties.add(property);
            }
        }
        property = generateOneOfSchema(property, properties);
        if (nullable.equals(TRUE)) {
            property.addExtension(X_NULLABLE, BooleanNode.TRUE);
        }
        return property;
    }

    private boolean isSameRecord(String parentComponentName,
                                 TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
        if (parentComponentName == null) {
            return false;
        }
        return typeReferenceTypeSymbol.getName().isPresent()
                && parentComponentName.equals(typeReferenceTypeSymbol.getName().get().trim());
    }

    /**
     * Generates a oneOf schema from a list of candidate schemas.
     */
    private BalAsyncApi30SchemaImpl generateOneOfSchema(BalAsyncApi30SchemaImpl property,
                                                        List<BalAsyncApi30SchemaImpl> properties) {
        boolean isTypeReference = properties.size() == 1;
        if (!isTypeReference) {
            BalAsyncApi30SchemaImpl oneOf = new BalAsyncApi30SchemaImpl();
            for (BalAsyncApi30SchemaImpl asyncApi30Schema : properties) {
                oneOf.addOneOf(asyncApi30Schema);
            }
            property = oneOf;
        }
        return property;
    }

    private BalAsyncApi30SchemaImpl mapEnumValues(EnumSymbol enumSymbol) {
        BalAsyncApi30SchemaImpl property = new BalAsyncApi30SchemaImpl();
        property.setType(AsyncAPIType.STRING.toString());
        List<JsonNode> enums = new ArrayList<>();
        List<ConstantSymbol> enumMembers = enumSymbol.members();
        for (int i = enumMembers.size() - 1; i >= 0; i--) {
            ConstantSymbol enumMember = enumMembers.get(i);
            if (enumMember.typeDescriptor().typeKind() == TypeDescKind.SINGLETON) {
                String signatureValue = enumMember.typeDescriptor().signature();
                if (signatureValue.startsWith("\"") && signatureValue.endsWith("\"")) {
                    signatureValue = signatureValue.substring(1, signatureValue.length() - 1);
                }
                enums.add(new TextNode(signatureValue));
            } else {
                enums.add(new TextNode(enumMember.constValue().toString().trim()));
            }
        }
        property.setEnum(enums);
        return property;
    }

    /**
     * Maps a Ballerina array or tuple type to an AsyncAPI array schema.
     */
    private BalAsyncApi30SchemaImpl mapArrayToArraySchema(TypeSymbol symbol, String componentName) {
        BalAsyncApi30SchemaImpl property = new BalAsyncApi30SchemaImpl();
        property.setType(AsyncAPIType.ARRAY.toString());
        int arrayDimensions = 0;
        while (symbol instanceof ArrayTypeSymbol) {
            arrayDimensions = arrayDimensions + 1;
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) symbol;
            symbol = arrayTypeSymbol.memberTypeDescriptor();
        }
        BalAsyncApi30SchemaImpl symbolProperty = getAsyncApiSchema(symbol.typeKind().getName());
        if (symbol.typeKind() == TypeDescKind.UNION) {
            symbolProperty = getSchemaForUnionType(
                    (UnionTypeSymbol) symbol, symbolProperty, componentName);
        }
        if (symbol.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
            symbolProperty = getSchemaForTypeReferenceSymbol(symbol, symbolProperty, componentName);
        }
        if (symbol.typeKind() == TypeDescKind.MAP) {
            MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) symbol;
            symbolProperty = handleMapType(componentName, symbolProperty, mapTypeSymbol);
        }
        if (symbol.typeKind().equals(TypeDescKind.TUPLE)) {
            TupleTypeSymbol tuple = (TupleTypeSymbol) symbol;
            BalAsyncApi30SchemaImpl composedSchema = new BalAsyncApi30SchemaImpl();
            for (TypeSymbol typeSymbol : tuple.memberTypeDescriptors()) {
                BalAsyncApi30SchemaImpl asyncApiSchema = getAsyncApiSchema(typeSymbol.signature());
                if (typeSymbol instanceof TypeReferenceTypeSymbol) {
                    asyncApiSchema.set$ref(SCHEMA_REFERENCE + typeSymbol.signature());
                    createComponentSchema(typeSymbol, null);
                }
                composedSchema.addOneOf(asyncApiSchema);
            }
            symbolProperty = composedSchema;
        }
        if (arrayDimensions > 1) {
            BalAsyncApi30SchemaImpl arraySchema = new BalAsyncApi30SchemaImpl();
            arraySchema.setType(AsyncAPIType.ARRAY.toString());
            property.setItems(handleArray(arrayDimensions - 1, symbolProperty, arraySchema));
        } else {
            property.setItems(symbolProperty);
        }
        return property;
    }

    /**
     * Maps a union type that wraps an array (e.g. {@code string[]? name}).
     * <p>
     * TODO: Map for different array type unions (ex:float|int[] ids, float|int[]? ids)
     */
    private BalAsyncApi30SchemaImpl getSchemaForUnionType(UnionTypeSymbol symbol,
                                                          BalAsyncApi30SchemaImpl symbolProperty,
                                                          String componentName) {
        List<TypeSymbol> typeSymbols = symbol.userSpecifiedMemberTypes();
        for (TypeSymbol typeSymbol : typeSymbols) {
            if (typeSymbol.typeKind() == TypeDescKind.ARRAY) {
                TypeSymbol arrayType = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
                if (arrayType.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                    symbolProperty = getSchemaForTypeReferenceSymbol(
                            arrayType, symbolProperty, componentName);
                } else {
                    symbolProperty = getAsyncApiSchema(arrayType.typeKind().getName());
                }
            } else if (typeSymbol.typeKind() != TypeDescKind.NIL) {
                symbolProperty = getAsyncApiSchema(typeSymbol.typeKind().getName());
            }
        }
        return symbolProperty;
    }

    /**
     * Handles a type reference that is the element type of an array field.
     */
    private BalAsyncApi30SchemaImpl getSchemaForTypeReferenceSymbol(TypeSymbol arrayType,
                                                                    BalAsyncApi30SchemaImpl symbolProperty,
                                                                    String componentName) {
        if (((TypeReferenceTypeSymbol) arrayType).definition().kind() == SymbolKind.ENUM) {
            TypeReferenceTypeSymbol typeRefEnum = (TypeReferenceTypeSymbol) arrayType;
            EnumSymbol enumSymbol = (EnumSymbol) typeRefEnum.definition();
            symbolProperty = mapEnumValues(enumSymbol);
        } else {
            symbolProperty.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(
                    arrayType.getName().orElseThrow().trim()));
            TypeReferenceTypeSymbol typeRecord = (TypeReferenceTypeSymbol) arrayType;
            if (!isSameRecord(componentName, typeRecord)) {
                createComponentSchema(typeRecord, null);
            }
        }
        return symbolProperty;
    }

    /**
     * Recursively handles nested array dimensions.
     * <p>
     * TODO: Here needs to check objectMapper.valueToTree(property) because it may contain entity:true
     */
    private BalAsyncApi30SchemaImpl handleArray(int arrayDimensions,
                                                BalAsyncApi30SchemaImpl property,
                                                BalAsyncApi30SchemaImpl arrayProperty) {
        if (arrayDimensions > 1) {
            BalAsyncApi30SchemaImpl nArray = new BalAsyncApi30SchemaImpl();
            nArray.setType(AsyncAPIType.ARRAY.toString());
            arrayProperty.setItems(handleArray(arrayDimensions - 1, property, nArray));
        } else if (arrayDimensions == 1) {
            arrayProperty.setItems(property);
        }
        return arrayProperty;
    }
}
