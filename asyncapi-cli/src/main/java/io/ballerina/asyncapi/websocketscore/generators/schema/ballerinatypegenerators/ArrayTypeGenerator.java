/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.schema.ballerinatypegenerators;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorConstants;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.schema.TypeGeneratorUtils;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.hasConstraints;
import static io.ballerina.asyncapi.websocketscore.generators.schema.TypeGeneratorUtils.getNullableType;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayDimensionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesisedTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;

/**
 * Generate TypeDefinitionNode and TypeDescriptorNode for array schemas.
 * -- ex:
 * Sample AsyncAPI :
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

    /**
     * Generate TypeDescriptorNode for array type schemas. If array type is not given, type will be `AnyData`
     * public type StringArray string[];
     */
    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws BallerinaAsyncApiExceptionWs {
        AsyncApi25SchemaImpl arraySchema = schema;
        AsyncApi25SchemaImpl items = (AsyncApi25SchemaImpl) arraySchema.getItems();
        boolean isConstraintsAvailable =
                hasConstraints(items) && typeName != null;
        TypeGenerator typeGenerator;
        if (isConstraintsAvailable) {
            String normalizedTypeName = typeName.replaceAll(GeneratorConstants.SPECIAL_CHARACTER_REGEX, "").trim();
            List<AnnotationNode> typeAnnotations = new ArrayList<>();
            AnnotationNode constraintNode = TypeGeneratorUtils.generateConstraintNode(typeName, items);
            if (constraintNode != null) {
                typeAnnotations.add(constraintNode);
            }
            typeName = GeneratorUtils.getValidName(
                    parentType != null ?
                            parentType + "-" + normalizedTypeName + "-Items-" + items.getType() :
                            normalizedTypeName + "-Items-" + items.getType(), true);
            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null);
            TypeDefinitionNode arrayItemWithConstraint = typeGenerator.generateTypeDefinitionNode(
                    createIdentifierToken(typeName), new ArrayList<>(), typeAnnotations);
            imports.addAll(typeGenerator.getImports());
            typeDefinitionNodeList.add(arrayItemWithConstraint);
        } else {
            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null);
        }
        TypeDescriptorNode typeDescriptorNode;
        typeDefinitionNodeList.addAll(typeGenerator.getTypeDefinitionNodeList());
        if ((typeGenerator instanceof PrimitiveTypeGenerator ||
                typeGenerator instanceof ArrayTypeGenerator) && isConstraintsAvailable) {
            typeDescriptorNode = NodeParser.parseTypeDescriptor(typeName);
        } else {
            typeDescriptorNode = typeGenerator.generateTypeDescriptorNode();
        }

        if (typeGenerator instanceof UnionTypeGenerator || (items.getEnum() != null && items.getEnum().size() > 0)) {
            typeDescriptorNode = createParenthesisedTypeDescriptorNode(
                    createToken(OPEN_PAREN_TOKEN), typeDescriptorNode, createToken(CLOSE_PAREN_TOKEN));
        }
        if (typeDescriptorNode instanceof OptionalTypeDescriptorNode) {
            Node node = ((OptionalTypeDescriptorNode) typeDescriptorNode).typeDescriptor();
            typeDescriptorNode = (TypeDescriptorNode) node;
        }

        if (arraySchema.getMaxItems() != null) {
            if (arraySchema.getMaxItems() > GeneratorConstants.MAX_ARRAY_LENGTH) {
                throw new BallerinaAsyncApiExceptionWs("Maximum item count defined in the definition exceeds the " +
                        "maximum ballerina array length.");
            }
        }
        NodeList<ArrayDimensionNode> arrayDimensions = createEmptyNodeList();
        if (typeDescriptorNode.kind() == ARRAY_TYPE_DESC) {
            ArrayTypeDescriptorNode innerArrayType = (ArrayTypeDescriptorNode) typeDescriptorNode;
            arrayDimensions = innerArrayType.dimensions();
            typeDescriptorNode = innerArrayType.memberTypeDesc();
        }

        ArrayDimensionNode arrayDimension = createArrayDimensionNode(
                createToken(OPEN_BRACKET_TOKEN), null,
                createToken(CLOSE_BRACKET_TOKEN));
        arrayDimensions = arrayDimensions.add(arrayDimension);
        ArrayTypeDescriptorNode arrayTypeDescriptorNode = createArrayTypeDescriptorNode(typeDescriptorNode
                , arrayDimensions);
        imports.addAll(typeGenerator.getImports());
        return getNullableType(arraySchema, arrayTypeDescriptorNode);
    }
}
