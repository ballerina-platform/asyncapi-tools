/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.asyncspec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25MessageImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalBooleanSchema;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DISPATCHERKEY_NULLABLE_EXCEPTION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DISPATCHERKEY_OPTIONAL_EXCEPTION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DISPATCHER_KEY_TYPE_EXCEPTION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.DOUBLE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FALSE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.FLOAT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.INTEGER;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.NUMBER;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.OBJECT;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.STRING;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.TRUE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.callObjectMapper;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.getAsyncApiSchema;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_FIELD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;


/**
 * This util class for processing the mapping in between ballerina record and asyncAPI object schema.
 *
 * @since 2.0.0
 */
public class AsyncAPIComponentMapper {
    private final AsyncApi25ComponentsImpl components;
    private final List<AsyncAPIConverterDiagnostic> diagnostics;


    public AsyncAPIComponentMapper(AsyncApi25ComponentsImpl components) {
        this.components = components;
        this.diagnostics = new ArrayList<>();
    }

    public List<AsyncAPIConverterDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    /**
     * This function for doing the mapping with ballerina type references.
     *
     * @param typeSymbol Type reference name as the TypeSymbol
     */
    public void createComponentSchema(TypeSymbol typeSymbol, String dispatcherValue) {
        String componentName = ConverterCommonUtils.unescapeIdentifier(typeSymbol.getName().orElseThrow().trim());
        //Check schema has created before, then skip recreating it
        boolean isComponentContains = false;

        Map<String, Schema> allSchemas = this.components.getSchemas();

        if (allSchemas != null) {
            isComponentContains = allSchemas.containsKey(componentName);
        }

        if (allSchemas == null || !isComponentContains || dispatcherValue != null) {


            Map<String, String> apiDocs = getRecordFieldsAPIDocsMap((TypeReferenceTypeSymbol) typeSymbol,
                    componentName);
            String typeDoc = null;
            if (apiDocs.size() > 0) {
                typeDoc = apiDocs.get(typeSymbol.getName().get());
            }
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeSymbol;
            TypeSymbol type = typeRef.typeDescriptor();
            // Handle record type request body

            if (type.typeKind() == TypeDescKind.INTERSECTION) {
                // Check if Read only present and then avoid it
                type = excludeReadonlyIfPresent(type);

            }
            BalAsyncApi25SchemaImpl schema = new BalAsyncApi25SchemaImpl();
            switch (type.typeKind()) {
                case RECORD:
                    // Handle typeInclusions with allOf type binding
                    handleRecordTypeSymbol((RecordTypeSymbol) type, componentName, apiDocs, dispatcherValue);
                    break;

                case TYPE_REFERENCE:
                    schema.setType(OBJECT);
                    schema.set$ref(ConverterCommonUtils.unescapeIdentifier(
                            type.getName().orElseThrow().trim()));
//                    schema.put(componentName, new ObjectSchema().$ref(ConverterCommonUtils.unescapeIdentifier(
//                            type.getName().orElseThrow().trim())));
                    components.addSchema(componentName, schema);
//                    components.setSchemas(schema);
                    TypeReferenceTypeSymbol referredType = (TypeReferenceTypeSymbol) type;
                    createComponentSchema(referredType, dispatcherValue);
                    break;
                case STRING:
                    schema.setType(STRING);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, schema);
                    break;
                case INT:
                    schema.setType(INTEGER);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, schema);
                    break;
                case DECIMAL:
                    schema.setType(NUMBER);
                    schema.setFormat(DOUBLE);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, schema);
                    break;
                case FLOAT:
                    schema.setType(NUMBER);
                    schema.setFormat(FLOAT);
                    schema.setDescription(typeDoc);
                    components.addSchema(componentName, schema);
                    break;
                case ARRAY:
                case TUPLE:
                    BalAsyncApi25SchemaImpl arraySchema = mapArrayToArraySchema(type, componentName);
                    arraySchema.setDescription(typeDoc);
                    components.addSchema(componentName, arraySchema);
                    break;
                case UNION:
                    BalAsyncApi25SchemaImpl unionSchema = handleUnionType((UnionTypeSymbol) type,
                            new BalAsyncApi25SchemaImpl(), componentName, null, null);
                    unionSchema.setDescription(typeDoc);
                    components.addSchema(componentName, unionSchema);
                    break;
                case MAP:
                    MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) type;
                    TypeSymbol typeParam = mapTypeSymbol.typeParam();
                    if (typeParam.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                        TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeParam;
                        BalAsyncApi25SchemaImpl objectSchema = new BalAsyncApi25SchemaImpl();
                        objectSchema.setType(AsyncAPIType.OBJECT.toString());
                        BalAsyncApi25SchemaImpl objectSchema2 = new BalAsyncApi25SchemaImpl();
                        objectSchema2.setType(AsyncAPIType.OBJECT.toString());
                        objectSchema2.set$ref(ConverterCommonUtils.unescapeIdentifier(
                                typeReferenceTypeSymbol.getName().orElseThrow().trim()));
                        objectSchema.setAdditionalProperties(objectSchema2);
                        components.addSchema(componentName, objectSchema);
//                        schema.put(componentName, new ObjectSchema().additionalProperties(new ObjectSchema()
//                                .$ref(ConverterCommonUtils.unescapeIdentifier(
//                                        typeReferenceTypeSymbol.getName().orElseThrow().trim()))));
                        createComponentSchema(typeReferenceTypeSymbol, dispatcherValue);
                    } else {

                        TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                        BalAsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(typeDescKind.getName());
                        //TODO : have to check here asyncApiSchema.getType() == null ? true : asyncApiSchema
                        BalAsyncApi25SchemaImpl objectSchema = new BalAsyncApi25SchemaImpl();
                        objectSchema.setType(AsyncAPIType.OBJECT.toString());
//                schema.put(componentName,
//                        new ObjectSchema().additionalProperties(
//                                asyncApiSchema.getType() == null ? true : asyncApiSchema)
//                                .description(typeDoc));
//                Map<String, Schema> schemas = components.getSchemas();
//                if (schemas != null) {
//                    schemas.putAll(schema);
//                } else {
//                    schema.setType(AsyncAPIType.OBJECT.toString());
//                    schema.setDescription(typeDoc);
//                    schema.setAdditionalProperties(asyncApiSchema.getType() == null ?
//                            new BooleanUnionValueImpl(true) : asyncApiSchema);
                        objectSchema.setAdditionalProperties(asyncApiSchema.getType() == null ?
                                new BalBooleanSchema(true) : asyncApiSchema);
                        components.addSchema(componentName, objectSchema);
//                }
                    }
                    break;
                default:
                    // Diagnostic for currently unsupported data types.
                    DiagnosticMessages errorMessage = DiagnosticMessages.AAS_CONVERTOR_106;
                    IncompatibleRemoteDiagnostic error = new IncompatibleRemoteDiagnostic(errorMessage,
                            typeRef.getLocation().get(), type.typeKind().getName());
                    diagnostics.add(error);
                    break;
            }
        }
    }

    private BalAsyncApi25SchemaImpl handleRecordTypeSymbol(RecordTypeSymbol recordTypeSymbol,
                                        String componentName, Map<String, String> apiDocs, String dispatcherValue) {
        // Handle typeInclusions with allOf type binding
        List<TypeSymbol> typeInclusions = recordTypeSymbol.typeInclusions();
        Map<String, RecordFieldSymbol> recordFields = recordTypeSymbol.fieldDescriptors();
        HashSet<String> unionKeys = new HashSet<>(recordFields.keySet());
        BalAsyncApi25SchemaImpl recordSchema =null;
        if (typeInclusions.isEmpty()) {
            recordSchema=generateObjectSchemaFromRecordFields(componentName, recordFields, apiDocs, dispatcherValue);
        } else {
            mapTypeInclusionToAllOfSchema(componentName, typeInclusions, recordFields,
                    unionKeys, apiDocs, dispatcherValue);
        }
        return recordSchema;
    }

    /**
     * Creating API docs related to given record fields.
     */
    private Map<String, String> getRecordFieldsAPIDocsMap(TypeReferenceTypeSymbol typeSymbol, String componentName) {
        Map<String, String> apiDocs = new LinkedHashMap<>();


        // Record field apidoc mapping
        TypeDefinitionSymbol recordTypeDefinitionSymbol = (TypeDefinitionSymbol) ((typeSymbol).definition());
        if (recordTypeDefinitionSymbol.typeDescriptor() instanceof RecordTypeSymbol) {
            RecordTypeSymbol recordType = (RecordTypeSymbol) recordTypeDefinitionSymbol.typeDescriptor();

            //Take record field parameter descriptions
            if (recordTypeDefinitionSymbol.documentation().isPresent()) {
                apiDocs = recordTypeDefinitionSymbol.documentation().get().parameterMap();
            }
            Map<String, RecordFieldSymbol> recordFieldSymbols = recordType.fieldDescriptors();

            //Take record each field descriptions (If there is a description using parameter
            // description then this will be not taken)
            for (Map.Entry<String, RecordFieldSymbol> fields : recordFieldSymbols.entrySet()) {
                Optional<Documentation> fieldDoc = (fields.getValue()).documentation();
                if (fieldDoc.isPresent() && fieldDoc.get().description().isPresent()) {
                    String field = ConverterCommonUtils.unescapeIdentifier(fields.getKey());
                    if (!apiDocs.containsKey(field)) {
                        apiDocs.put(field, fieldDoc.get().description().get());
                    }
                }
            }
        }

        // Take Record description
        Symbol recordSymbol = typeSymbol.definition();
        Optional<Documentation> documentation = ((Documentable) recordSymbol).documentation();
        if (documentation.isPresent() && documentation.get().description().isPresent()) {
            Optional<String> description = (documentation.get().description());
            apiDocs.put(componentName, description.get().trim());
        }
        return apiDocs;
    }

    /**
     * This function is to map the ballerina typeInclusion to AsyncApiSpec allOf composedSchema.
     */
    private void mapTypeInclusionToAllOfSchema(
            String componentName, List<TypeSymbol> typeInclusions, Map<String,
            RecordFieldSymbol> recordFields, HashSet<String> unionKeys, Map<String, String> apiDocs,
            String dispatcherValue) {

        BalAsyncApi25SchemaImpl allOfSchema = new BalAsyncApi25SchemaImpl();
        // Set schema
        for (TypeSymbol typeInclusion : typeInclusions) {
            BalAsyncApi25SchemaImpl referenceSchema = new BalAsyncApi25SchemaImpl();
            String typeInclusionName = typeInclusion.getName().orElseThrow();
            referenceSchema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(typeInclusionName));

            allOfSchema.addAllOf(referenceSchema);
            if (typeInclusion.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                TypeReferenceTypeSymbol typeRecord = (TypeReferenceTypeSymbol) typeInclusion;

                apiDocs.putAll(getRecordFieldsAPIDocsMap(typeRecord, typeInclusionName));
                if (typeRecord.typeDescriptor() instanceof RecordTypeSymbol) {
                    RecordTypeSymbol typeInclusionRecord = (RecordTypeSymbol) typeRecord.typeDescriptor();
                    Map<String, RecordFieldSymbol> tInFields = typeInclusionRecord.fieldDescriptors();
                    unionKeys.addAll(tInFields.keySet());
                    unionKeys.removeAll(tInFields.keySet());
                    generateObjectSchemaFromRecordFields(typeInclusionName, tInFields, apiDocs, null);

                }
            }
        }
        Map<String, RecordFieldSymbol> filteredField = new LinkedHashMap<>();
        recordFields.forEach((key1, value) -> unionKeys.stream().filter(key ->
                ConverterCommonUtils.unescapeIdentifier(key1.trim()).
                        equals(ConverterCommonUtils.unescapeIdentifier(key))).forEach(key ->
                filteredField.put(ConverterCommonUtils.unescapeIdentifier(key1), value)));
        BalAsyncApi25SchemaImpl objectSchema = generateObjectSchemaFromRecordFields(componentName, filteredField,
                apiDocs, dispatcherValue);
        allOfSchema.addAllOf(objectSchema);
        this.components.addSchema(componentName, allOfSchema);

    }

    /**
     * This function is to map ballerina record type fields to AsyncAPI objectSchema fields.
     */
    public BalAsyncApi25SchemaImpl generateObjectSchemaFromRecordFields(String componentName,
                                                                        Map<String, RecordFieldSymbol> rfields,
                                                                        Map<String, String> apiDocs,
                                                                        String dispatcherValue) {
        BalAsyncApi25SchemaImpl componentSchema = new BalAsyncApi25SchemaImpl();
        componentSchema.setType(AsyncAPIType.OBJECT.toString());
        List<String> required = new ArrayList<>();
        boolean dispatcherValuePresent = false;
        componentSchema.setDescription(apiDocs.get(componentName));
        for (Map.Entry<String, RecordFieldSymbol> field : rfields.entrySet()) {
            String fieldName = ConverterCommonUtils.unescapeIdentifier(field.getKey().trim());
            TypeDescKind fieldTypeKind = field.getValue().typeDescriptor().typeKind();
            String fieldType = fieldTypeKind.toString().toLowerCase(Locale.ENGLISH).trim();
            BalAsyncApi25SchemaImpl property = getAsyncApiSchema(fieldType);

            boolean fieldIsOptional = field.getValue().isOptional();


            if (fieldTypeKind == TypeDescKind.TYPE_REFERENCE) { // ex:- public type Subscribe string;  Cat
                TypeReferenceTypeSymbol typeReference = (TypeReferenceTypeSymbol) field.getValue().typeDescriptor();
                property = handleTypeReference(typeReference, property, isSameRecord(componentName, typeReference));

            } else if (fieldTypeKind == TypeDescKind.UNION) { // ex:-  Cat|Dog Schema fields inside Pet schema , string?
                property = handleUnionType((UnionTypeSymbol) field.getValue().typeDescriptor(), property, componentName,
                        fieldName, dispatcherValue);

            } else if (fieldTypeKind == TypeDescKind.MAP) {  // map<json> field inside Pet schema
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) field.getValue().typeDescriptor();
                property = handleMapType(componentName, property, mapTypeSymbol);

            } else if (fieldTypeKind==TypeDescKind.RECORD) {
              property=  handleRecordTypeSymbol((RecordTypeSymbol) field.getValue().typeDescriptor(),
                      null,new HashMap<>(),null);

            }
            //TODO : Have to check && !(property.getItems() instanceof ObjectNode)
//            String check=property.getType();
            if (property.getType() != null) {
                if (property.getType().equals(AsyncAPIType.ARRAY.toString()) && !((property).getItems() != null &&
                        ((BalAsyncApi25SchemaImpl) (property).getItems().asSchema()).getOneOf() != null)) {
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
            // Add API documentation for record field
            if (dispatcherValue != null && dispatcherValue.equals(fieldName)) {
                if (fieldType.equals(STRING)) {
                    if (!fieldIsOptional) {
                        dispatcherValuePresent = true;
                        property.setConst(new TextNode(componentName));
                    } else {
                        throw new NoSuchElementException(String.format(DISPATCHERKEY_OPTIONAL_EXCEPTION,
                                fieldName, componentName));
                    }
                } else {
                    throw new NoSuchElementException(String.format(DISPATCHER_KEY_TYPE_EXCEPTION, dispatcherValue));

                }
            }
            if (!fieldIsOptional) {  // Check if the field is optional or not
                required.add(fieldName);
            }

            if (apiDocs.containsKey(fieldName)) {
                property.setDescription(apiDocs.get(fieldName));
            }
            componentSchema.addProperty(fieldName, property);
        }
        if (dispatcherValue != null && !dispatcherValuePresent) {
            throw new NoSuchElementException(String.format(DISPATCHERKEY_NOT_PRESENT_IN_RECORD_FIELD,
                    dispatcherValue, componentName));
        }
        if (!required.isEmpty()) {
            componentSchema.setRequired(required);
        }
        if (componentName != null) {
            // Set properties for the schema
            this.components.addSchema(componentName, componentSchema);
        }
        return componentSchema;
    }

    private BalAsyncApi25SchemaImpl handleMapType(String componentName, BalAsyncApi25SchemaImpl property,
                                                  MapTypeSymbol mapTypeSymbol) {

        TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
        if (typeDescKind == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeReference = (TypeReferenceTypeSymbol) mapTypeSymbol.typeParam();
            BalAsyncApi25SchemaImpl reference = handleTypeReference(typeReference, new BalAsyncApi25SchemaImpl(),
                    isSameRecord(componentName, typeReference));
            property.setAdditionalProperties(reference);
        } else if (typeDescKind == TypeDescKind.ARRAY) {
            BalAsyncApi25SchemaImpl arraySchema = mapArrayToArraySchema(mapTypeSymbol.typeParam(), componentName);
            property.setAdditionalProperties(arraySchema);
        } else {
            BalAsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(typeDescKind.getName());
            //TODO : This is not sure but for now we are using this, if there is an additionalProperties=true

//            BalAsyncApi25SchemaImpl objectSchema= new BalAsyncApi25SchemaImpl();
//            objectSchema.setType(AsyncAPIType.RECORD.toString());
            //TODO : Have to consider about asyncApiSchema.getType() == null ? true :
            // asyncApiSchema in addtionalProperties
            property.setAdditionalProperties(asyncApiSchema.getType() == null ?
                    new BalBooleanSchema(true) : asyncApiSchema);
        }
        return property;
    }

    /**
     * This function uses to handle the field datatype has TypeReference(ex: Record or Enum).
     */
    private BalAsyncApi25SchemaImpl handleTypeReference(TypeReferenceTypeSymbol typeReferenceSymbol,
                                                        BalAsyncApi25SchemaImpl property, boolean isCyclicRecord) {
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
     * This function uses to generate schema when field has union type as data type.
     * <pre>
     *     type Pet record {
     *         Dog|Cat type;
     *     };
     * </pre>
     */
    private BalAsyncApi25SchemaImpl handleUnionType(UnionTypeSymbol unionType, BalAsyncApi25SchemaImpl property,
                                                    String parentComponentName, String fieldName,
                                                    String dispatcherValue) {
        List<TypeSymbol> unionTypes = unionType.memberTypeDescriptors();
        List<BalAsyncApi25SchemaImpl> properties = new ArrayList<>();
        String nullable = FALSE;
        for (TypeSymbol union : unionTypes) {
            if (union.typeKind() == TypeDescKind.NIL && fieldName != null && fieldName.equals(dispatcherValue)) {
                throw new NoSuchElementException(String.format(DISPATCHERKEY_NULLABLE_EXCEPTION,
                        fieldName, parentComponentName));
            } else if (union.typeKind() == TypeDescKind.NIL) {
                nullable = TRUE;
            } else if (union.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                property = getAsyncApiSchema(union.typeKind().getName().trim());
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) union;
                property = handleTypeReference(typeReferenceTypeSymbol, property,
                        isSameRecord(parentComponentName, typeReferenceTypeSymbol));
                properties.add(property);
                // TODO: uncomment after fixing ballerina lang union type handling issue
            } else if (union.typeKind() == TypeDescKind.UNION) {
                property = handleUnionType((UnionTypeSymbol) union, property, parentComponentName,
                        null, null);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.ARRAY || union.typeKind() == TypeDescKind.TUPLE) {
                property = mapArrayToArraySchema(union, parentComponentName);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.MAP) {
                if(parentComponentName!=null) {
                    MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) union;
                    TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                    BalAsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(typeDescKind.getName());
                    BalAsyncApi25SchemaImpl objectSchema = getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
                    objectSchema.setAdditionalProperties(
                            asyncApiSchema.getType() == null ?
                                    new BalBooleanSchema(true) : asyncApiSchema
                    );
                    property = objectSchema;
                    properties.add(property);
                    components.addSchema(parentComponentName, property);
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

    private boolean isSameRecord(String parentComponentName, TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
        if (parentComponentName == null) {
            return false;
        }
        return typeReferenceTypeSymbol.getName().isPresent() &&
                parentComponentName.equals(typeReferenceTypeSymbol.getName().get().trim());
    }

    /**
     * This function generate oneOf composed schema for record fields.
     */
    private BalAsyncApi25SchemaImpl generateOneOfSchema(BalAsyncApi25SchemaImpl property,
                                                        List<BalAsyncApi25SchemaImpl> properties) {
        //TODO:  Uncomment below line after checking if count? count field has only one reference
        // then there no need to be oneOF
//        boolean isTypeReference = properties.size() == 1 && properties.get(0).get$ref() == null;
        boolean isTypeReference = properties.size() == 1;

        if (!isTypeReference) {
            BalAsyncApi25SchemaImpl oneOf = new BalAsyncApi25SchemaImpl();
            for (BalAsyncApi25SchemaImpl asyncApi25Schema : properties) {
                oneOf.addOneOf(asyncApi25Schema);

            }
            property = oneOf;
        }
        return property;
    }

    private BalAsyncApi25SchemaImpl mapEnumValues(EnumSymbol enumSymbol) {

//        Schema property;
        BalAsyncApi25SchemaImpl property = new BalAsyncApi25SchemaImpl();
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
     * Generate arraySchema for ballerina array type.
     */
    private BalAsyncApi25SchemaImpl mapArrayToArraySchema(TypeSymbol symbol,
                                                          String componentName) {
        BalAsyncApi25SchemaImpl property = new BalAsyncApi25SchemaImpl();
        property.setType(AsyncAPIType.ARRAY.toString());
        int arrayDimensions = 0;
        while (symbol instanceof ArrayTypeSymbol) {
            arrayDimensions = arrayDimensions + 1;
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) symbol;
            symbol = arrayTypeSymbol.memberTypeDescriptor();
        }
        // Handle record fields have reference record array type (ex: Tag[] tags)
        BalAsyncApi25SchemaImpl symbolProperty = getAsyncApiSchema(symbol.typeKind().getName());
        // Handle record fields have union type array (ex: string[]? name)
        if (symbol.typeKind() == TypeDescKind.UNION) {
            symbolProperty = getSchemaForUnionType((UnionTypeSymbol) symbol, symbolProperty, componentName);
        }
        // Set the record model to the definition
        if (symbol.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
            symbolProperty = getSchemaForTypeReferenceSymbol(symbol, symbolProperty, componentName);
        }
        // Handle record fields have union type array (ex: map<string>[] name)
        if (symbol.typeKind() == TypeDescKind.MAP) {
            MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) symbol;
            symbolProperty = handleMapType(componentName, symbolProperty, mapTypeSymbol);
        }

        // Handle the tuple type
        if (symbol.typeKind().equals(TypeDescKind.TUPLE)) {
            // Add all the schema related to typeSymbols into the list. Then the list can be mapped into oneOf
            // type.
            TupleTypeSymbol tuple = (TupleTypeSymbol) symbol;
            BalAsyncApi25SchemaImpl composedSchema = new BalAsyncApi25SchemaImpl();
            for (TypeSymbol typeSymbol : tuple.memberTypeDescriptors()) {
                BalAsyncApi25SchemaImpl asyncApiSchema = getAsyncApiSchema(typeSymbol.signature());
                if (typeSymbol instanceof TypeReferenceTypeSymbol) {
                    asyncApiSchema.set$ref(SCHEMA_REFERENCE + typeSymbol.signature());
                    createComponentSchema(typeSymbol, null);
                }
                composedSchema.addOneOf(asyncApiSchema);
            }

            symbolProperty = composedSchema;
        }
        // Handle nested array type
        if (arrayDimensions > 1) {
            BalAsyncApi25SchemaImpl arraySchema = new BalAsyncApi25SchemaImpl();
            arraySchema.setType(AsyncAPIType.ARRAY.toString());
            property.setItems(handleArray(arrayDimensions - 1, symbolProperty, arraySchema));
        } else {
            property.setItems(symbolProperty);
        }
        return property;
    }

    /**
     * This function is used to map union type of BUNION type (ex: string[]? name).
     * <p>
     * TODO: Map for different array type unions (ex:float|int[] ids, float|int[]? ids)
     * `string[]? name` here it takes union member types as array and nil,fix should do with array type and map to
     * oneOf AsyncApiSpec.
     */
    private BalAsyncApi25SchemaImpl getSchemaForUnionType(UnionTypeSymbol symbol,
                                                          BalAsyncApi25SchemaImpl symbolProperty,
                                                          String componentName) {
        List<TypeSymbol> typeSymbols = symbol.userSpecifiedMemberTypes();
        for (TypeSymbol typeSymbol : typeSymbols) {
            if (typeSymbol.typeKind() == TypeDescKind.ARRAY) {
                TypeSymbol arrayType = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
                // Set the record model to the definition
                if (arrayType.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                    symbolProperty = getSchemaForTypeReferenceSymbol(arrayType, symbolProperty, componentName);
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
     * This util function is to handle the type reference symbol is record type or enum type.
     */
    private BalAsyncApi25SchemaImpl getSchemaForTypeReferenceSymbol(TypeSymbol arrayType,
                                                                    BalAsyncApi25SchemaImpl symbolProperty,
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
     * Handle nested array.
     */
    //TODO : Here needs to check objectMapper.valueToTree(property) because it may contatins entity:true
    private BalAsyncApi25SchemaImpl handleArray(int arrayDimensions, BalAsyncApi25SchemaImpl property,
                                                BalAsyncApi25SchemaImpl arrayProperty) {

        if (arrayDimensions > 1) {
            BalAsyncApi25SchemaImpl nArray = new BalAsyncApi25SchemaImpl();
            nArray.setType(AsyncAPIType.ARRAY.toString());
            arrayProperty.setItems(handleArray(arrayDimensions - 1, property, nArray));
        } else if (arrayDimensions == 1) {

            arrayProperty.setItems(property);
        }
        return arrayProperty;
    }

    public TypeSymbol excludeReadonlyIfPresent(TypeSymbol typeSymbol) {
        List<TypeSymbol> typeSymbols = ((IntersectionTypeSymbol) typeSymbol).memberTypeDescriptors();
        for (TypeSymbol symbol : typeSymbols) {
            if (!(symbol instanceof ReadonlyTypeSymbol)) {
                typeSymbol = symbol;
                break;
            }
        }
        return typeSymbol;
    }

}
