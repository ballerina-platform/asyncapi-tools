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

package io.ballerina.asyncapi.core.generators.schema;

import com.fasterxml.jackson.databind.node.BooleanNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.AllOfRecordTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.AnyDataTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.ArrayTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.JsonTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.MapTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.PrimitiveTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.RecordTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.ReferencedTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.TypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.UnionTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.model.GeneratorMetaData;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorUtils.convertAsyncAPITypeToBallerina;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAPPING_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * Contains util functions needed for schema generation.
 *
 * @since 1.3.0
 */
public class TypeGeneratorUtils {
    private static final List<String> primitiveTypeList =
            new ArrayList<>(Arrays.asList(GeneratorConstants.INTEGER, GeneratorConstants.NUMBER,
                    GeneratorConstants.STRING, GeneratorConstants.BOOLEAN));

    /**
     * Get SchemaType object relevant to the schema given.
     *
     * @param schemaValue Schema object
     * @param typeName    parameter name
     * @return Relevant SchemaType object
     */
    public static TypeGenerator getTypeGenerator(AsyncApi25SchemaImpl schemaValue, String typeName, String parentName) {

        if (schemaValue.get$ref() != null) {
            return new ReferencedTypeGenerator(schemaValue, typeName);

        } else if ((
                (schemaValue.getOneOf() != null || schemaValue.getAllOf() != null || schemaValue.getAnyOf() != null))) {
            if (schemaValue.getAllOf() != null) {
                return new AllOfRecordTypeGenerator(schemaValue, typeName);
            } else {
                return new UnionTypeGenerator(schemaValue, typeName);
            }
        } else if (schemaValue.getType()==null &&  schemaValue.getAdditionalProperties()!=null){
            return new JsonTypeGenerator(schemaValue,typeName);
        } else if (schemaValue.getType()!=null && schemaValue.getType().equals(GeneratorConstants.OBJECT) &&
                schemaValue.getAdditionalProperties()!=null &&
                  (schemaValue.getAdditionalProperties() instanceof AsyncApi25SchemaImpl ||
                          schemaValue.getAdditionalProperties() instanceof BooleanUnionValueImpl)) {


//            if (((AsyncApi25SchemaImpl) schemaValue.getAdditionalProperties()).getType() == null) {
//                return new MapTypeGenerator(schemaValue, typeName);


//            } else if (((AsyncApi25SchemaImpl) schemaValue.getAdditionalProperties()).getType() != null) {
                return new MapTypeGenerator(schemaValue, typeName);

//            }else if (){
//                return  null;
//            }

            //TODO: include mapschema here see openapi code
        } else if ((schemaValue.getType() != null && schemaValue.getType().equals(GeneratorConstants.OBJECT)) ||
                schemaValue.getProperties() != null) {
            return new RecordTypeGenerator(schemaValue, typeName);
        } else if (schemaValue.getType()!=null && schemaValue.getType().equals("array")) {
            return new ArrayTypeGenerator(schemaValue, typeName, parentName);
        } else if (schemaValue.getType() != null && primitiveTypeList.contains(schemaValue.getType())) {
            return new PrimitiveTypeGenerator(schemaValue, typeName);
        } else { // when schemaValue.type == null
            return new AnyDataTypeGenerator(schemaValue, typeName);
        }
    }

    /**
     * Generate proper type name considering the nullable configurations.
     * Scenario 1 : schema.getNullable() != null && schema.getNullable() == true && nullable == true -> string?
     * Scenario 2 : schema.getNullable() != null && schema.getNullable() == true && nullable == false -> string?
     * Scenario 3 : schema.getNullable() != null && schema.getNullable() == false && nullable == false -> string
     * Scenario 4 : schema.getNullable() != null && schema.getNullable() == false && nullable == true -> string
     * Scenario 5 : schema.getNullable() == null && nullable == true -> string?
     * Scenario 6 : schema.getNullable() == null && nullable == false -> string
     *
     * @param schema           Schema of the property
     * @param originalTypeDesc Type name
     * @return Final type of the field
     */
    public static TypeDescriptorNode getNullableType(AsyncApi25SchemaImpl schema, TypeDescriptorNode originalTypeDesc) {
        TypeDescriptorNode nillableType = originalTypeDesc;
        boolean nullable = GeneratorMetaData.getInstance().isNullable();
        if (schema.getExtensions() != null) {
            if (schema.getExtensions().get("x-nullable").equals(BooleanNode.TRUE)) {
                nillableType = createOptionalTypeDescriptorNode(originalTypeDesc, createToken(QUESTION_MARK_TOKEN));
            }
        } else if (nullable) {
            nillableType = createOptionalTypeDescriptorNode(originalTypeDesc, createToken(QUESTION_MARK_TOKEN));
        }
        return nillableType;
    }

    public static void updateRecordFieldList(List<String> required,
                                             List<Node> recordFieldList,
                                             Map.Entry<String, Schema> field,
                                             AsyncApi25SchemaImpl fieldSchema,
                                             NodeList<Node> schemaDocNodes,
                                             IdentifierToken fieldName,
                                             TypeDescriptorNode fieldTypeName) {

        updateRecordFieldList(required, recordFieldList, field, fieldSchema, schemaDocNodes, fieldName,
                fieldTypeName, System.err);
    }

    public static void updateRecordFieldList(List<String> required,
                                             List<Node> recordFieldList,
                                             Map.Entry<String, Schema> field,
                                             AsyncApi25SchemaImpl fieldSchema,
                                             NodeList<Node> schemaDocNodes,
                                             IdentifierToken fieldName,
                                             TypeDescriptorNode fieldTypeName,
                                             PrintStream outStream) {


        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(schemaDocNodes);
        //Generate constraint annotation.
//        AnnotationNode constraintNode = generateConstraintNode(fieldSchema);
//        MetadataNode metadataNode;
//        boolean isConstraintSupport =
//                constraintNode != null && fieldSchema.getExtensions()!=null & fieldSchema.
//                        getExtensions().get("x-nullable") != null ||
//                        (((fieldSchema.getProperties() != null &&
//                                (fieldSchema.getOneOf() != null || fieldSchema.getAllOf() != null ||
//                                        fieldSchema.getAnyOf() != null))) && ( fieldSchema.getOneOf() != null ||
//                                fieldSchema.getAnyOf() != null));
//        boolean nullable = GeneratorMetaData.getInstance().isNullable();
//        if (nullable) {
//            constraintNode = null;
//        } else if (isConstraintSupport) {
//            outStream.printf("WARNING: constraints in the OpenAPI contract will be ignored for the " +
//                            "field `%s`, as constraints are not supported on Ballerina union types%n",
//                    fieldName.toString().trim());
//            constraintNode = null;
//        }
//        if (constraintNode == null) {
//            metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
//        } else {
        MetadataNode metadataNode = createMetadataNode(documentationNode, createNodeList(new ArrayList<>()));
//        }

        if (required != null) {
            setRequiredFields(required, recordFieldList, field, fieldSchema, fieldName, fieldTypeName, metadataNode);
        } else {
            RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                    fieldTypeName, fieldName, createToken(QUESTION_MARK_TOKEN), createToken(SEMICOLON_TOKEN));
            recordFieldList.add(recordFieldNode);
        }
    }

    private static void setRequiredFields(List<String> required, List<Node> recordFieldList,
                                          Map.Entry<String, Schema> field,
                                          AsyncApi25SchemaImpl fieldSchema,
                                          IdentifierToken fieldName,
                                          TypeDescriptorNode fieldTypeName,
                                          MetadataNode metadataNode) {

//        if (!required.contains(field.getKey().trim())) {
//            if (fieldSchema.getDefault() != null) {
//                RecordFieldWithDefaultValueNode defaultNode =
//                        getRecordFieldWithDefaultValueNode(fieldSchema, fieldName, fieldTypeName, metadataNode);
//                recordFieldList.add(defaultNode);
//            } else {
//                RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
//                        fieldTypeName, fieldName, createToken(QUESTION_MARK_TOKEN),
//                        createToken(SEMICOLON_TOKEN));
//                recordFieldList.add(recordFieldNode);
//            }
//        } else {
//            RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
//                    fieldTypeName, fieldName, null, createToken(SEMICOLON_TOKEN));
//            recordFieldList.add(recordFieldNode);
//        }
        if (!required.contains(field.getKey().trim())) {
            if (fieldSchema.getDefault() != null) {
                RecordFieldWithDefaultValueNode defaultNode =
                        getRecordFieldWithDefaultValueNode(fieldSchema, fieldName, fieldTypeName, metadataNode);
                recordFieldList.add(defaultNode);
            } else {
                RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, createToken(QUESTION_MARK_TOKEN),
                        createToken(SEMICOLON_TOKEN));
                recordFieldList.add(recordFieldNode);
            }
        } else {
            if (fieldSchema.getDefault() != null) {
                RecordFieldWithDefaultValueNode defaultNode =
                        getRecordFieldWithDefaultValueNode(fieldSchema, fieldName, fieldTypeName, metadataNode);
                recordFieldList.add(defaultNode);
            }else{
                RecordFieldNode recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, null, createToken(SEMICOLON_TOKEN));
                recordFieldList.add(recordFieldNode);

            }

        }
    }

    private static RecordFieldWithDefaultValueNode getRecordFieldWithDefaultValueNode(AsyncApi25SchemaImpl fieldSchema,
                                                                                      IdentifierToken fieldName,
                                                                                      TypeDescriptorNode fieldTypeName,
                                                                                      MetadataNode metadataNode) {

        Token defaultValueToken;
        String defaultValue = fieldSchema.getDefault().asText().trim();
        if (fieldSchema.getType().equals("string")) {
            if (defaultValue.equals("\"")) {
                defaultValueToken = AbstractNodeFactory.createIdentifierToken("\"" + "\\" +
                        fieldSchema.getDefault().asText() + "\"");
            } else {
                defaultValueToken = AbstractNodeFactory.createIdentifierToken(
                        fieldSchema.getDefault().toString());
            }
        } else if (!defaultValue.matches("^[0-9]*$") && !defaultValue.matches("^(\\d*\\.)?\\d+$")
                && !(defaultValue.startsWith("[") && defaultValue.endsWith("]")) &&
                !(fieldSchema.getType().equals("boolean"))) {
            //This regex was added due to avoid adding quotes for default values which are numbers and array values.
            //Ex: default: 123
            defaultValueToken = AbstractNodeFactory.createIdentifierToken(
                    fieldSchema.getDefault().asText());
        } else {

            defaultValueToken = AbstractNodeFactory.createIdentifierToken(fieldSchema.getDefault().asText().trim().replaceAll("\\\\", ""));
        }
        ExpressionNode expressionNode = createRequiredExpressionNode(defaultValueToken);
        return NodeFactory.createRecordFieldWithDefaultValueNode
                (metadataNode, null, fieldTypeName, fieldName, createToken(EQUAL_TOKEN),
                        expressionNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * This util is to set the constraint validation for given data type in the record field and user define type.
     * <p>
     * //     * @param fieldSchema Schema for data type
     *
     * @return {@link MetadataNode}
     */
    public static AnnotationNode generateConstraintNode(AsyncApi25SchemaImpl fieldSchema) {

        if (fieldSchema.getType().equals("string")) {
            AsyncApi25SchemaImpl stringSchema = fieldSchema;
            // Attributes : maxLength, minLength
            return generateStringConstraint(stringSchema);
        } else if (fieldSchema.getType().equals("integer") || fieldSchema.getType().equals("number")) {
            // Attribute : minimum, maximum, exclusiveMinimum, exclusiveMaximum
            return generateNumberConstraint(fieldSchema);
        } else if (fieldSchema.getType().equals("array")) {
            AsyncApi25SchemaImpl arraySchema = fieldSchema;
            // Attributes: maxItems, minItems
            return generateArrayConstraint(arraySchema);
        }
        // Ignore Object, Map and Composed schemas.
        return null;
    }

    /**
     * Generate constraint for numbers : int, float, decimal.
     */
    private static AnnotationNode generateNumberConstraint(AsyncApi25SchemaImpl fieldSchema) {

        List<String> fields = getNumberAnnotFields(fieldSchema);
        if (fields.isEmpty()) {
            return null;
        }
        String annotBody = GeneratorConstants.OPEN_BRACE + String.join(GeneratorConstants.COMMA, fields) +
                GeneratorConstants.CLOSE_BRACE;
        AnnotationNode annotationNode;
        if (fieldSchema.getType().equals("number")) {
            if (fieldSchema.getFormat() != null && fieldSchema.getFormat().equals(GeneratorConstants.FLOAT)) {
                annotationNode = createAnnotationNode(GeneratorConstants.CONSTRAINT_FLOAT, annotBody);
            } else {
                annotationNode = createAnnotationNode(GeneratorConstants.CONSTRAINT_NUMBER, annotBody);
            }
        } else {
            annotationNode = createAnnotationNode(GeneratorConstants.CONSTRAINT_INT, annotBody);
        }
        return annotationNode;
    }

    /**
     * Generate constraint for string.
     */
    private static AnnotationNode generateStringConstraint(AsyncApi25SchemaImpl stringSchema) {

        List<String> fields = getStringAnnotFields(stringSchema);
        if (fields.isEmpty()) {
            return null;
        }
        String annotBody = GeneratorConstants.OPEN_BRACE + String.join(GeneratorConstants.COMMA, fields) +
                GeneratorConstants.CLOSE_BRACE;
        return createAnnotationNode(GeneratorConstants.CONSTRAINT_STRING, annotBody);
    }

    /**
     * Generate constraint for array.
     */
    private static AnnotationNode generateArrayConstraint(AsyncApi25SchemaImpl arraySchema) {

        List<String> fields = getArrayAnnotFields(arraySchema);
        if (fields.isEmpty()) {
            return null;
        }
        String annotBody = GeneratorConstants.OPEN_BRACE + String.join(GeneratorConstants.COMMA, fields) +
                GeneratorConstants.CLOSE_BRACE;
        return createAnnotationNode(GeneratorConstants.CONSTRAINT_ARRAY, annotBody);
    }

    private static List<String> getNumberAnnotFields(AsyncApi25SchemaImpl numberSchema) {

        List<String> fields = new ArrayList<>();
        boolean isInt = numberSchema.getType().equals("integer");
        if (numberSchema.getMinimum() != null &&
                BigDecimal.ZERO.compareTo((BigDecimal) numberSchema.getMinimum()) != 0
                && numberSchema.getExclusiveMinimum() == null) {
            String value = numberSchema.getMinimum().toString();
            String fieldRef = GeneratorConstants.MINIMUM + GeneratorConstants.COLON +
                    (isInt ? numberSchema.getMinimum().intValue() : value);
            fields.add(fieldRef);
        }
        if (numberSchema.getMaximum() != null &&
                BigDecimal.ZERO.compareTo((BigDecimal) numberSchema.getMaximum()) != 0
                && numberSchema.getExclusiveMaximum() == null) {
            String value = numberSchema.getMaximum().toString();
            String fieldRef = GeneratorConstants.MAXIMUM + GeneratorConstants.COLON +
                    (isInt ? numberSchema.getMaximum().intValue() : value);
            fields.add(fieldRef);
        }
        if (numberSchema.getExclusiveMinimum() != null && numberSchema.getMinimum() != null &&
                BigDecimal.ZERO.compareTo((BigDecimal) numberSchema.getMinimum()) != 0) {
            String value = numberSchema.getMinimum().toString();
            String fieldRef = GeneratorConstants.EXCLUSIVE_MIN + GeneratorConstants.COLON +
                    (isInt ? numberSchema.getMinimum().intValue() : value);
            fields.add(fieldRef);
        }
        if (numberSchema.getExclusiveMaximum() != null &&
                numberSchema.getMaximum() != null && BigDecimal.ZERO.compareTo((BigDecimal)
                numberSchema.getMaximum()) != 0) {
            String value = numberSchema.getMaximum().toString();
            String fieldRef = GeneratorConstants.EXCLUSIVE_MAX + GeneratorConstants.COLON +
                    (isInt ? numberSchema.getMaximum().intValue() : value);
            fields.add(fieldRef);
        }
        //TODO: This will be enable once constraint package gives this support.
//        if (numberSchema.getMultipleOf() != null) {
//            String value = numberSchema.getMultipleOf().toString();
//            String fieldRef = "multipleOf:" + value;
//            fields.add(fieldRef);
//        }
        return fields;
    }

    private static List<String> getStringAnnotFields(AsyncApi25SchemaImpl stringSchema) {

        List<String> fields = new ArrayList<>();
        if (stringSchema.getMaxLength() != null && stringSchema.getMaxLength() != 0) {
            String value = stringSchema.getMaxLength().toString();
            String fieldRef = GeneratorConstants.MAX_LENGTH + GeneratorConstants.COLON + value;
            fields.add(fieldRef);
        }
        if (stringSchema.getMinLength() != null && stringSchema.getMinLength() != 0) {
            String value = stringSchema.getMinLength().toString();
            String fieldRef = GeneratorConstants.MIN_LENGTH + GeneratorConstants.COLON + value;
            fields.add(fieldRef);
        }
        //TODO: This will be enable once constraint package gives this support.
//        if (stringSchema.getPattern() != null) {
//            String value = stringSchema.getPattern();
//            String fieldRef = "pattern:" + "\"" + value + "\"";
//            fields.add(fieldRef);
//        }
        return fields;
    }

    private static List<String> getArrayAnnotFields(AsyncApi25SchemaImpl arraySchema) {

        List<String> fields = new ArrayList<>();
        if (arraySchema.getMaxItems() != null && arraySchema.getMaxItems() != 0) {
            String value = arraySchema.getMaxItems().toString();
            String fieldRef = GeneratorConstants.MAX_LENGTH + GeneratorConstants.COLON + value;
            fields.add(fieldRef);
        }
        if (arraySchema.getMinItems() != null && arraySchema.getMinItems() != 0) {
            String value = arraySchema.getMinItems().toString();
            String fieldRef = GeneratorConstants.MIN_LENGTH + GeneratorConstants.COLON + value;
            fields.add(fieldRef);
        }
        return fields;
    }

    /**
     * This util create any annotation node by providing annotation reference and annotation body content.
     *
     * @param annotationReference Annotation reference value
     * @param annotFields         Annotation body content fields with single string
     * @return {@link AnnotationNode}
     */
    private static AnnotationNode createAnnotationNode(String annotationReference, String annotFields) {

        MappingConstructorExpressionNode annotationBody = null;
        SimpleNameReferenceNode annotReference = createSimpleNameReferenceNode(
                createIdentifierToken(annotationReference));
        ExpressionNode expressionNode = NodeParser.parseExpression(annotFields);
        if (expressionNode.kind() == MAPPING_CONSTRUCTOR) {
            annotationBody = (MappingConstructorExpressionNode) expressionNode;
        }
        return NodeFactory.createAnnotationNode(
                createToken(AT_TOKEN),
                annotReference,
                annotationBody);
    }

    /**
     * Creates API documentation for record fields.
     *
     * @param field Schema of the field to generate
     * @return Documentation node list
     */
    public static List<Node> getFieldApiDocs(AsyncApi25SchemaImpl field) {

        List<Node> schemaDoc = new ArrayList<>();
        if (field.getDescription() != null) {
            schemaDoc.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    field.getDescription(), false));
        } else if (field.get$ref() != null) {
            String[] split = field.get$ref().trim().split("/");
            String componentName = GeneratorUtils.getValidName(split[split.length - 1], true);
            AsyncApi25DocumentImpl openAPI = GeneratorMetaData.getInstance().getAsyncAPI();
            if (openAPI.getComponents().getSchemas().get(componentName) != null) {
                AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) openAPI.getComponents()
                        .getSchemas().get(componentName);
                if (schema.getDescription() != null) {
                    schemaDoc.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                            schema.getDescription(), false));
                }
            }
        }
        return schemaDoc;
    }

    /**
     * Creates record documentation.
     *
     * @param documentation Documentation node list
     * @param schemaValue   OpenAPI schema
     *                      //     * @param typeAnnotations Annotation list of the record
     */
    public static void getRecordDocs(List<Node> documentation, AsyncApi25SchemaImpl schemaValue
    ) throws BallerinaAsyncApiException {

        if (schemaValue.getDescription() != null) {
            documentation.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    schemaValue.getDescription(), false));
        } else if (schemaValue.get$ref() != null) {
            String typeName = GeneratorUtils.getValidName(GeneratorUtils.extractReferenceType(
                    schemaValue.get$ref()), true);
            AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) GeneratorMetaData.getInstance().getAsyncAPI().
                    getComponents().getSchemas().get(typeName);
            if (refSchema.getDescription() != null) {
                documentation.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                        refSchema.getDescription(), false));
            }
        }
//        if (schemaValue.getDeprecated() != null && schemaValue.getDeprecated()) {
//            DocCommentsGenerator.extractDeprecatedAnnotation(schemaValue.getExtensions(),
//                    documentation, typeAnnotations);
//        }
    }
}
