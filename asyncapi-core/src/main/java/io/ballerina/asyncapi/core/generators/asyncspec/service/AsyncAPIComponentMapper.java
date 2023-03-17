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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleResourceDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.symbols.*;

import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;


/**
 * This util class for processing the mapping in between ballerina record and openAPI object schema.
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
     * This function for doing the mapping with ballerina record to object schema.
     *
     * @param typeSymbol     Record Name as a TypeSymbol
     */
    public void createComponentSchema( TypeSymbol typeSymbol) {
        String componentName = ConverterCommonUtils.unescapeIdentifier(typeSymbol.getName().orElseThrow().trim());
        Map<String, String> apiDocs = getRecordFieldsAPIDocsMap((TypeReferenceTypeSymbol) typeSymbol, componentName);
        String typeDoc = null;
        if (apiDocs.size() > 0) {
            typeDoc = apiDocs.get(typeSymbol.getName().get());
        }
        TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeSymbol;
        TypeSymbol type = typeRef.typeDescriptor();
        // Handle record type request body
        if (type.typeKind() == TypeDescKind.INTERSECTION) {
            List<TypeSymbol> typeSymbols = ((IntersectionTypeSymbol) type).memberTypeDescriptors();
            for (TypeSymbol symbol: typeSymbols) {
                if (!(symbol instanceof ReadonlyTypeSymbol)) {
                    type = symbol;
                    break;
                }
            }
        }
        AsyncApi25SchemaImpl schema= new AsyncApi25SchemaImpl();
        switch (type.typeKind()) {
            case RECORD:
                // Handle typeInclusions with allOf type binding
                handleRecordTypeSymbol((RecordTypeSymbol) type, componentName, apiDocs);
                break;
            case STRING:
                schema.setType(STRING);
                schema.setDescription(typeDoc);
                components.addSchema(componentName,schema);
                break;
            case INT:
                schema.setType(INTEGER);
                schema.setDescription(typeDoc);
                components.addSchema(componentName,schema);
                break;
            case DECIMAL:
                schema.setType(NUMBER);
                schema.setFormat(DOUBLE);
                schema.setDescription(typeDoc);
                components.addSchema(componentName,schema);
                break;
            case FLOAT:
                schema.setType(NUMBER);
                schema.setFormat(FLOAT);
                schema.setDescription(typeDoc);
                components.addSchema(componentName,schema);
                break;
            case ARRAY:
            case TUPLE:
                AsyncApi25SchemaImpl arraySchema = mapArrayToArraySchema( type, componentName);
                arraySchema.setDescription(typeDoc);
                components.addSchema(componentName,arraySchema );
                break;
            case UNION:
                AsyncApi25SchemaImpl unionSchema = handleUnionType((UnionTypeSymbol) type, new AsyncApi25SchemaImpl(), componentName);
                unionSchema.setDescription(typeDoc);
                components.addSchema(componentName,unionSchema);
                break;
            case MAP:
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) type;
                TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                AsyncApi25SchemaImpl openApiSchema = ConverterCommonUtils.getAsyncApiSchema(typeDescKind.getName());
                //TODO : have to check here openApiSchema.getType() == null ? true : openApiSchema
                AsyncApi25SchemaImpl objectSchema= new AsyncApi25SchemaImpl();
                objectSchema.setType(AsyncAPIType.RECORD.toString());
//                schema.put(componentName,
//                        new ObjectSchema().additionalProperties(
//                                openApiSchema.getType() == null ? true : openApiSchema)
//                                .description(typeDoc));
//                Map<String, Schema> schemas = components.getSchemas();
//                if (schemas != null) {
//                    schemas.putAll(schema);
//                } else {
                objectSchema.setDescription(typeDoc);
                objectSchema.setAdditionalProperties(openApiSchema);
                components.addSchema(componentName,schema);
//                }
                break;
            default:
                // Diagnostic for currently unsupported data types.
                DiagnosticMessages errorMessage = DiagnosticMessages.AAS_CONVERTOR_106;
                IncompatibleResourceDiagnostic error = new IncompatibleResourceDiagnostic(errorMessage,
                        typeRef.getLocation().get(), type.typeKind().getName());
                diagnostics.add(error);
                break;
        }
    }

    private void handleRecordTypeSymbol(RecordTypeSymbol recordTypeSymbol,
                                        String componentName, Map<String, String> apiDocs) {
        // Handle typeInclusions with allOf type binding
        List<TypeSymbol> typeInclusions = recordTypeSymbol.typeInclusions();
        Map<String, RecordFieldSymbol> recordFields = recordTypeSymbol.fieldDescriptors();
        HashSet<String> unionKeys = new HashSet<>(recordFields.keySet());
        if (typeInclusions.isEmpty()) {
            generateObjectSchemaFromRecordFields(componentName, recordFields, apiDocs);
        } else {
            mapTypeInclusionToAllOfSchema(componentName, typeInclusions, recordFields, unionKeys, apiDocs);
        }
    }

    /**
     * Creating API docs related to given record fields.
     */
    private Map<String, String> getRecordFieldsAPIDocsMap(TypeReferenceTypeSymbol typeSymbol, String componentName) {

        Map<String, String> apiDocs =  new LinkedHashMap<>();
        Symbol recordSymbol = typeSymbol.definition();
        Optional<Documentation> documentation = ((Documentable) recordSymbol).documentation();
        if (documentation.isPresent() && documentation.get().description().isPresent()) {
            Optional<String> description = (documentation.get().description());
            apiDocs.put(componentName, description.get().trim());
        }
        // Record field apidoc mapping
        TypeDefinitionSymbol recordTypeDefinitionSymbol = (TypeDefinitionSymbol) ((typeSymbol).definition());
        if (recordTypeDefinitionSymbol.typeDescriptor() instanceof RecordTypeSymbol) {
            RecordTypeSymbol recordType = (RecordTypeSymbol) recordTypeDefinitionSymbol.typeDescriptor();
            Map<String, RecordFieldSymbol> recordFieldSymbols = recordType.fieldDescriptors();
            for (Map.Entry<String , RecordFieldSymbol> fields: recordFieldSymbols.entrySet()) {
                Optional<Documentation> fieldDoc = (fields.getValue()).documentation();
                if (fieldDoc.isPresent() && fieldDoc.get().description().isPresent()) {
                    apiDocs.put(ConverterCommonUtils.unescapeIdentifier(fields.getKey()),
                            fieldDoc.get().description().get());
                }
            }
        }
        return apiDocs;
    }

    /**
     * This function is to map the ballerina typeInclusion to OAS allOf composedSchema.
     */
    private void mapTypeInclusionToAllOfSchema(
                                               String componentName, List<TypeSymbol> typeInclusions, Map<String,
            RecordFieldSymbol> recordFields, HashSet<String> unionKeys, Map<String, String> apiDocs) {

        // Map to allOF need to check the status code inclusion there
        AsyncApi25SchemaImpl allOfSchema =new AsyncApi25SchemaImpl();
        // Set schema
        for (TypeSymbol typeInclusion: typeInclusions) {
            AsyncApi25SchemaImpl referenceSchema = new AsyncApi25SchemaImpl();
            String typeInclusionName = typeInclusion.getName().orElseThrow();
            referenceSchema.set$ref(ConverterCommonUtils.unescapeIdentifier(typeInclusionName));

            allOfSchema.addAllOf(referenceSchema);
            if (typeInclusion.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                TypeReferenceTypeSymbol typeRecord = (TypeReferenceTypeSymbol) typeInclusion;
                if (typeRecord.typeDescriptor() instanceof RecordTypeSymbol) {
                    RecordTypeSymbol typeInclusionRecord = (RecordTypeSymbol) typeRecord.typeDescriptor();
                    Map<String, RecordFieldSymbol> tInFields = typeInclusionRecord.fieldDescriptors();
                    unionKeys.addAll(tInFields.keySet());
                    unionKeys.removeAll(tInFields.keySet());
                    generateObjectSchemaFromRecordFields( typeInclusionName, tInFields, apiDocs);
                    // Update the schema value

                }
            }
        }
        Map<String, RecordFieldSymbol> filteredField = new LinkedHashMap<>();
        recordFields.forEach((key1, value) -> unionKeys.stream().filter(key ->
                ConverterCommonUtils.unescapeIdentifier(key1.trim()).
                        equals(ConverterCommonUtils.unescapeIdentifier(key))).forEach(key ->
                filteredField.put(ConverterCommonUtils.unescapeIdentifier(key1), value)));
        AsyncApi25SchemaImpl objectSchema = generateObjectSchemaFromRecordFields( null, filteredField, apiDocs);
//        allOfSchemaList.add(objectSchema);
        allOfSchema.addAllOf(objectSchema);
//        allOfSchema.addAllOf(allOfSchemaList);
//        if (schema != null && !schema.containsKey(componentName)) {
//            // Set properties for the schema
//            schema.put(componentName, allOfSchema);
        this.components.addSchema(componentName,allOfSchema);
//        } else if (schema == null) {
//            schema = new LinkedHashMap<>();
//            schema.put(componentName, allOfSchema);
//            this.components.addSchema(componentName,allOfSchema);
//        }
    }

    /**
     * This function is to map ballerina record type fields to AsyncAPI objectSchema fields.
     */
    private AsyncApi25SchemaImpl generateObjectSchemaFromRecordFields(String componentName,Map<String, RecordFieldSymbol> rfields,
                                                              Map<String, String> apiDocs) {
        AsyncApi25SchemaImpl componentSchema=new AsyncApi25SchemaImpl();
        componentSchema.setType(AsyncAPIType.RECORD.toString());
        List<String> required = new ArrayList<>();
        componentSchema.setDescription(apiDocs.get(componentName));
        for (Map.Entry<String, RecordFieldSymbol> field: rfields.entrySet()) {
            String fieldName = ConverterCommonUtils.unescapeIdentifier(field.getKey().trim());
            if (!field.getValue().isOptional()) {  // Check if the field is optional or not
                required.add(fieldName);
            }
            TypeDescKind fieldTypeKind = field.getValue().typeDescriptor().typeKind();
            String type = fieldTypeKind.toString().toLowerCase(Locale.ENGLISH);
            AsyncApi25SchemaImpl property = ConverterCommonUtils.getAsyncApiSchema(type);

            if (fieldTypeKind == TypeDescKind.TYPE_REFERENCE) { // ex:- Cat schema field inside Pet schema
                TypeReferenceTypeSymbol typeReference = (TypeReferenceTypeSymbol) field.getValue().typeDescriptor();
                property = handleTypeReference(typeReference, property, isSameRecord(componentName,
                        typeReference));

            } else if (fieldTypeKind == TypeDescKind.UNION) { // ex:-  Cat|Dog Schema fields inside Pet schema
                property = handleUnionType((UnionTypeSymbol) field.getValue().typeDescriptor(), property,
                        componentName);

            } else if (fieldTypeKind == TypeDescKind.MAP) {  // map<json> field inside Pet schema
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) field.getValue().typeDescriptor();
                property = handleMapType(componentName, property, mapTypeSymbol);

            }
            //TODO : Have to check && !(property.getItems() instanceof ObjectNode)
            if (property.getType().equals(AsyncAPIType.ARRAY.toString()) ) {
                BooleanNode booleanNode= (BooleanNode) property.getExtensions().get("x-nullable");
                property = mapArrayToArraySchema( field.getValue().typeDescriptor(), componentName);
                property.addExtension(X_NULLABLE,booleanNode);
            }
            // Add API documentation for record field
            if (apiDocs.containsKey(fieldName)) {
                property.setDescription(apiDocs.get(fieldName));
            }
            componentSchema.addProperty(fieldName,property);
        }
        componentSchema.setRequired(required);
        if (componentName != null) {
            // Set properties for the schema
            this.components.addSchema(componentName, componentSchema);
        }
        return componentSchema;
    }

    //TODO : Have to include a comment

    private AsyncApi25SchemaImpl handleMapType( String componentName, AsyncApi25SchemaImpl property,
                             MapTypeSymbol mapTypeSymbol) {

        TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
        if (typeDescKind == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeReference = (TypeReferenceTypeSymbol) mapTypeSymbol.typeParam();
            AsyncApi25SchemaImpl reference = handleTypeReference( typeReference, new AsyncApi25SchemaImpl(),
                    isSameRecord(componentName, typeReference));
            property.setAdditionalProperties(reference);
        } else if (typeDescKind == TypeDescKind.ARRAY) {
            AsyncApi25SchemaImpl arraySchema = mapArrayToArraySchema(mapTypeSymbol.typeParam(), componentName);
            property.setAdditionalProperties(arraySchema);
        } else {
            AsyncApi25SchemaImpl asyncApiSchema = ConverterCommonUtils.getAsyncApiSchema(typeDescKind.getName());
            //TODO : Have to consider about asyncApiSchema.getType() == null ? true : asyncApiSchema in addtionalProperties
            property.setAdditionalProperties(asyncApiSchema);
        }
        return property;
    }

    /**
     * This function uses to handle the field datatype has TypeReference(ex: Record or Enum). ex: Cat Record or Enum type inside Pet Record
     */
    private AsyncApi25SchemaImpl handleTypeReference( TypeReferenceTypeSymbol typeReferenceSymbol,
                                       AsyncApi25SchemaImpl property, boolean isCyclicRecord) {
        if (typeReferenceSymbol.definition().kind() == SymbolKind.ENUM) {
            EnumSymbol enumSymbol = (EnumSymbol) typeReferenceSymbol.definition();
            property = mapEnumValues(enumSymbol);
        } else {
            property.set$ref( SCHEMA_REFERENCE+ConverterCommonUtils.unescapeIdentifier(
                    typeReferenceSymbol.getName().orElseThrow().trim()));
            if (!isCyclicRecord) {
                createComponentSchema( typeReferenceSymbol);
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
    private AsyncApi25SchemaImpl handleUnionType(UnionTypeSymbol unionType, AsyncApi25SchemaImpl property, String parentComponentName) {
        List<TypeSymbol> unionTypes = unionType.memberTypeDescriptors();
        List<AsyncApi25SchemaImpl> properties = new ArrayList<>();
        String nullable = FALSE;
        for (TypeSymbol union: unionTypes) {
            if (union.typeKind() == TypeDescKind.NIL) {
                nullable = TRUE;
            } else if (union.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                property = ConverterCommonUtils.getAsyncApiSchema(union.typeKind().getName().trim());
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) union;
                property = handleTypeReference(typeReferenceTypeSymbol, property,
                        isSameRecord(parentComponentName, typeReferenceTypeSymbol));
                properties.add(property);
                // TODO: uncomment after fixing ballerina lang union type handling issue
            } else if (union.typeKind() == TypeDescKind.UNION) {
                property = handleUnionType((UnionTypeSymbol) union, property, parentComponentName);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.ARRAY || union.typeKind() == TypeDescKind.TUPLE) {
                property = mapArrayToArraySchema( union, parentComponentName);
                properties.add(property);
            } else if (union.typeKind() == TypeDescKind.MAP) {
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) union;
                TypeDescKind typeDescKind = mapTypeSymbol.typeParam().typeKind();
                AsyncApi25SchemaImpl openApiSchema = ConverterCommonUtils.getAsyncApiSchema(typeDescKind.getName());
                AsyncApi25SchemaImpl objectSchema= new AsyncApi25SchemaImpl();
                objectSchema.setType(AsyncAPIType.RECORD.toString());
                objectSchema.setAdditionalProperties(openApiSchema);
                property = objectSchema;
                properties.add(property);
                components.addSchema(parentComponentName,property);

            } else {
                property = ConverterCommonUtils.getAsyncApiSchema(union.typeKind().getName().trim());
                properties.add(property);
            }
        }

        property = generateOneOfSchema(property, properties);

        if (nullable.equals(TRUE)) {
            property.addExtension(X_NULLABLE,BooleanNode.TRUE);
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
    private AsyncApi25SchemaImpl generateOneOfSchema(AsyncApi25SchemaImpl property, List<AsyncApi25SchemaImpl> properties) {
        boolean isTypeReference = properties.size() == 1 && properties.get(0).get$ref() == null;
        if (!isTypeReference) {
            AsyncApi25SchemaImpl oneOf=new AsyncApi25SchemaImpl();
            for (AsyncApi25SchemaImpl asyncApi25Schema: properties){
                oneOf.addOneOf(asyncApi25Schema);

            }
            property = oneOf;
        }
        return property;
    }

    private AsyncApi25SchemaImpl mapEnumValues(EnumSymbol enumSymbol) {

//        Schema property;
        AsyncApi25SchemaImpl property=new AsyncApi25SchemaImpl();
        property.setType(AsyncAPIType.STRING.toString());
        List<JsonNode> enums = new ArrayList<>();
        List<ConstantSymbol> enumMembers = enumSymbol.members();
        for (ConstantSymbol enumMember : enumMembers) {
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
     * Generate arraySchema for ballerina record  as array type.
     */
    private AsyncApi25SchemaImpl mapArrayToArraySchema( TypeSymbol symbol,
                                       String componentName) {
        AsyncApi25SchemaImpl property=new AsyncApi25SchemaImpl();
        property.setType(AsyncAPIType.ARRAY.toString());
        int arrayDimensions = 0;
        while (symbol instanceof ArrayTypeSymbol) {
            arrayDimensions = arrayDimensions + 1;
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) symbol;
            symbol = arrayTypeSymbol.memberTypeDescriptor();
        }
        // Handle record fields have reference record array type (ex: Tag[] tags)
        AsyncApi25SchemaImpl symbolProperty  = ConverterCommonUtils.getAsyncApiSchema(symbol.typeKind().getName());
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
            symbolProperty = handleMapType( componentName, symbolProperty, mapTypeSymbol);
        }

        // Handle the tuple type
        if (symbol.typeKind().equals(TypeDescKind.TUPLE)) {
            // Add all the schema related to typeSymbols into the list. Then the list can be mapped into oneOf
            // type.
            TupleTypeSymbol tuple = (TupleTypeSymbol) symbol;
            AsyncApi25SchemaImpl composedSchema=new AsyncApi25SchemaImpl();
            for (TypeSymbol typeSymbol : tuple.memberTypeDescriptors()) {
                AsyncApi25SchemaImpl asyncApiSchema = ConverterCommonUtils.getAsyncApiSchema(typeSymbol.signature());
                if (typeSymbol instanceof TypeReferenceTypeSymbol) {
                    asyncApiSchema.set$ref(typeSymbol.signature());
                    createComponentSchema(typeSymbol);
                }
                composedSchema.addOneOf(asyncApiSchema);
            }

            symbolProperty = composedSchema;
        }
        // Handle nested array type
        if (arrayDimensions > 1) {
            AsyncApi25SchemaImpl arraySchema= new AsyncApi25SchemaImpl();
            arraySchema.setType(AsyncAPIType.ARRAY.toString());
            property.setItems(handleArray(arrayDimensions - 1, symbolProperty, arraySchema));
        } else {

            property.setItems(new ObjectMapper().valueToTree(symbolProperty));
        }
        return property;
    }

    /**
     * This function is used to map union type of BUNION type (ex: string[]? name).
     *
     * TODO: Map for different array type unions (ex:float|int[] ids, float|int[]? ids)
     * `string[]? name` here it takes union member types as array and nil,fix should do with array type and map to
     * oneOf OAS.
     */
    private AsyncApi25SchemaImpl getSchemaForUnionType(UnionTypeSymbol symbol, AsyncApi25SchemaImpl symbolProperty, String componentName) {
        List<TypeSymbol> typeSymbols = symbol.userSpecifiedMemberTypes();
        for (TypeSymbol typeSymbol: typeSymbols) {
            if (typeSymbol.typeKind() == TypeDescKind.ARRAY) {
                TypeSymbol arrayType = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
                // Set the record model to the definition
                if (arrayType.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                    symbolProperty = getSchemaForTypeReferenceSymbol(arrayType, symbolProperty, componentName);
                } else {
                    symbolProperty = ConverterCommonUtils.getAsyncApiSchema(arrayType.typeKind().getName());
                }
            } else if (typeSymbol.typeKind() != TypeDescKind.NIL) {
                symbolProperty = ConverterCommonUtils.getAsyncApiSchema(typeSymbol.typeKind().getName());
            }
        }
        return symbolProperty;
    }

    /**
     * This util function is to handle the type reference symbol is record type or enum type.
     */
    private AsyncApi25SchemaImpl getSchemaForTypeReferenceSymbol(TypeSymbol arrayType, AsyncApi25SchemaImpl symbolProperty, String componentName) {

        if (((TypeReferenceTypeSymbol) arrayType).definition().kind() == SymbolKind.ENUM) {
            TypeReferenceTypeSymbol typeRefEnum = (TypeReferenceTypeSymbol) arrayType;
            EnumSymbol enumSymbol = (EnumSymbol) typeRefEnum.definition();
            symbolProperty = mapEnumValues(enumSymbol);
        } else {
            symbolProperty.set$ref(SCHEMA_REFERENCE+ConverterCommonUtils.unescapeIdentifier(
                    arrayType.getName().orElseThrow().trim()));
            TypeReferenceTypeSymbol typeRecord = (TypeReferenceTypeSymbol) arrayType;
            if (!isSameRecord(componentName, typeRecord)) {
                createComponentSchema( typeRecord);
            }
        }
        return symbolProperty;
    }

    /**
     * Handle nested array.
     */
    //TODO : Here needs to check objectMapper.valueToTree(property) because it may contatins entity:true
    private JsonNode handleArray(int arrayDimensions, Schema property, AsyncApi25SchemaImpl arrayProperty) {
        ObjectMapper objectMapper= ConverterCommonUtils.callObjectMapper();

        if (arrayDimensions > 1) {
            AsyncApi25SchemaImpl narray = new AsyncApi25SchemaImpl();
            narray.setType(AsyncAPIType.ARRAY.toString());
            arrayProperty.setItems(handleArray(arrayDimensions - 1, property,  narray));
        } else if (arrayDimensions == 1) {

            arrayProperty.setItems(objectMapper.valueToTree(property));
        }
        return objectMapper.valueToTree(arrayProperty);
    }

}
