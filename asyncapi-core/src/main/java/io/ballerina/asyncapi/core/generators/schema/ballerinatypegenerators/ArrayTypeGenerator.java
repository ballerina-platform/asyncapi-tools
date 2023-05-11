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

package io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

/**
 * Generate TypeDefinitionNode and TypeDescriptorNode for array schemas.
 * -- ex:
 * Sample OpenAPI :
 * <pre>
 *     Pets:
 *       type: array
 *       items:
 *         $ref: "#/components/schemas/Pet"
 *  </pre>
 * Generated Ballerina type for the schema `Pet` :
 * <pre>
 *      public type Pets Pet[];
 * </pre>
 *
 * @since 1.3.0
 */
public class ArrayTypeGenerator extends TypeGenerator {
    private String parentType = null;

    public ArrayTypeGenerator(AsyncApi25SchemaImpl schema, String typeName, String parentType) {
        super(schema, typeName);
        this.parentType = parentType;
    }


    //TODO : Change this override method after using rc-2
    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiException {
        return null;
    }


//    /**
//     * Generate TypeDescriptorNode for array type schemas. If array type is not given, type will be `AnyData`
//     * public type StringArray string[];
//     */
//    @Override
//    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiException {
//
////        assert schema instanceof ArraySchema;
//        if(schema.getType().equals("array")){
//            JsonNode arraySchema= schema.getItems();
//
//        }
//        AsyncApi25SchemaImpl arraySchema = schema;
////        Schema<?> items = arraySchema.getItems();
//        boolean isConstraintsAvailable =
//                !GeneratorMetaData.getInstance().isNullable() && hasConstraints(items) && typeName != null;
//        TypeGenerator typeGenerator;
//        if (isConstraintsAvailable) {
//            String normalizedTypeName = typeName.replaceAll(GeneratorConstants.SPECIAL_CHARACTER_REGEX, "").trim();
//            typeName = GeneratorUtils.getValidName(
//                    parentType != null ?
//                            parentType + "-" + normalizedTypeName + "-Items-" + items.getType() :
//                            normalizedTypeName + "-Items-" + items.getType(),
//                    true);
//            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null);
//            List<AnnotationNode> typeAnnotations = new ArrayList<>();
//            AnnotationNode constraintNode = TypeGeneratorUtils.generateConstraintNode(items);
//            if (constraintNode != null) {
//                typeAnnotations.add(constraintNode);
//            }
//            TypeDefinitionNode arrayItemWithConstraint = typeGenerator.generateTypeDefinitionNode(
//                    createIdentifierToken(typeName),
//                    new ArrayList<>(),
//                    typeAnnotations);
//            typeDefinitionNodeList.add(arrayItemWithConstraint);
//        } else {
//            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null);
//        }
//
//        TypeDescriptorNode typeDescriptorNode;
//        typeDefinitionNodeList.addAll(typeGenerator.getTypeDefinitionNodeList());
//        if ((typeGenerator instanceof PrimitiveTypeGenerator ||
//                typeGenerator instanceof ArrayTypeGenerator) && isConstraintsAvailable) {
//            typeDescriptorNode = NodeParser.parseTypeDescriptor(typeName);
//        } else {
//            typeDescriptorNode = typeGenerator.generateTypeDescriptorNode();
//        }
//
//        if (typeGenerator instanceof UnionTypeGenerator) {
//            typeDescriptorNode = createParenthesisedTypeDescriptorNode(
//                    createToken(OPEN_PAREN_TOKEN), typeDescriptorNode, createToken(CLOSE_PAREN_TOKEN));
//        }
//        if (typeDescriptorNode instanceof OptionalTypeDescriptorNode) {
//            Node node = ((OptionalTypeDescriptorNode) typeDescriptorNode).typeDescriptor();
//            typeDescriptorNode = (TypeDescriptorNode) node;
//        }
//
//        if (arraySchema.getMaxItems() != null) {
//            if (arraySchema.getMaxItems() > GeneratorConstants.MAX_ARRAY_LENGTH) {
//                throw new BallerinaOpenApiException("Maximum item count defined in the definition exceeds the " +
//                        "maximum ballerina array length.");
//            }
//        }
//        NodeList<ArrayDimensionNode> arrayDimensions = NodeFactory.createEmptyNodeList();
//        if (typeDescriptorNode.kind() == SyntaxKind.ARRAY_TYPE_DESC) {
//            ArrayTypeDescriptorNode innerArrayType = (ArrayTypeDescriptorNode) typeDescriptorNode;
//            arrayDimensions = innerArrayType.dimensions();
//            typeDescriptorNode = innerArrayType.memberTypeDesc();
//        }
//
//        ArrayDimensionNode arrayDimension = NodeFactory.createArrayDimensionNode(
//                createToken(SyntaxKind.OPEN_BRACKET_TOKEN), null,
//                createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
//        arrayDimensions = arrayDimensions.add(arrayDimension);
//        ArrayTypeDescriptorNode arrayTypeDescriptorNode = createArrayTypeDescriptorNode(typeDescriptorNode
//                , arrayDimensions);
//
//        return getNullableType(arraySchema, arrayTypeDescriptorNode);
//    }
}
