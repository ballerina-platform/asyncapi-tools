/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package io.ballerina.asyncapi.websocketscore.generators.asyncspec.service;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.AsyncAPIType;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;

/**
 * This class for the mapping ballerina headers with AsyncApiSpec header parameter sections.
 */
public class AsyncAPIHeaderMapper {
    private final Map<String, String> apiDocs;

    public AsyncAPIHeaderMapper(Map<String, String> apiDocs) {
        this.apiDocs = apiDocs;
    }

    /**
     * Handle header parameters in ballerina data type.
     *
     * @param headerParam -  {@link RequiredParameterNode} type header parameter node
     */
    public void setHeaderParameter(RequiredParameterNode headerParam, BalAsyncApi25SchemaImpl bindingHeaderObject) {
        String extractedHeaderName = extractHeaderName(headerParam);
        if (extractedHeaderName != null) {
            String headerName = unescapeIdentifier(extractedHeaderName);
            Node node = headerParam.typeName();
            BalAsyncApi25SchemaImpl headerTypeSchema =
                    ConverterCommonUtils.getAsyncApiSchema(getHeaderType(headerParam));
            enableHeaderRequiredOption(node, headerTypeSchema);
            if (apiDocs != null && apiDocs.containsKey(headerName)) {
                headerTypeSchema.setDescription(apiDocs.get(headerName.trim()));
            }
            completeHeaderParameter(headerName, headerTypeSchema,
                    headerParam.annotations(), headerParam.typeName(), bindingHeaderObject);
        }
    }

    private String extractHeaderName(ParameterNode headerParam) {
        if (headerParam instanceof DefaultableParameterNode) {
            if (((DefaultableParameterNode) headerParam).paramName().isPresent()) {
                return ((DefaultableParameterNode) headerParam).paramName().get().text().
                        replaceAll("\\\\", "");
            }
        }
        if (((RequiredParameterNode) headerParam).paramName().isPresent()) {
            return ((RequiredParameterNode) headerParam).paramName().get().text().
                    replaceAll("\\\\", "");
        }
        return null;
    }

    /**
     * Handle header parameters in ballerina data type.
     *
     * @param headerParam -  {@link DefaultableParameterNode} type header parameter node
     */
    public void setHeaderParameter(DefaultableParameterNode headerParam, BalAsyncApi25SchemaImpl bindingHeaderObject) {
        String headerName = extractHeaderName(headerParam);
        BalAsyncApi25SchemaImpl headerTypeSchema = ConverterCommonUtils.getAsyncApiSchema(getHeaderType(headerParam));
        String defaultValue = headerParam.expression().toString().trim();
        if (defaultValue.length() > 1 &&
                defaultValue.charAt(0) == '"' &&
                defaultValue.charAt(defaultValue.length() - 1) == '"') {
            defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
        }
        List<SyntaxKind> allowedTypes = new ArrayList<>();
        allowedTypes.addAll(Arrays.asList(SyntaxKind.STRING_LITERAL,
                SyntaxKind.NUMERIC_LITERAL, SyntaxKind.BOOLEAN_LITERAL));
        if (allowedTypes.contains(headerParam.expression().kind())) {
            headerTypeSchema.setDefault(new TextNode(defaultValue));
        } else if (headerParam.expression().kind() == SyntaxKind.LIST_CONSTRUCTOR) {
            headerTypeSchema = new BalAsyncApi25SchemaImpl();
            headerTypeSchema.setDefault(new TextNode(defaultValue));
        }
        if (headerParam.typeName().kind() == SyntaxKind.OPTIONAL_TYPE_DESC) {
            headerTypeSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
        }
        if (apiDocs != null && apiDocs.containsKey(headerName)) {
            headerTypeSchema.setDescription(apiDocs.get(headerName.trim()));
        }
        completeHeaderParameter(headerName, headerTypeSchema,
                headerParam.annotations(), headerParam.typeName(), bindingHeaderObject);
    }

    /**
     * Extract header type by removing its optional and array types.
     */
    private String getHeaderType(ParameterNode headerParam) {
        if (headerParam instanceof DefaultableParameterNode) {
            return ((DefaultableParameterNode) headerParam).typeName().toString().
                    replaceAll("\\?", "").replaceAll("\\[", "").
                    replaceAll("\\]", "").trim();
        }
        return ((RequiredParameterNode) headerParam).typeName().toString().
                replaceAll("\\?", "").replaceAll("\\[", "").
                replaceAll("\\]", "").trim();
    }

    /**
     * Assign header values to AsyncApiSpec header parameter.
     */
    private void completeHeaderParameter(String headerName, BalAsyncApi25SchemaImpl headerSchema,
                                         NodeList<AnnotationNode> annotations, Node node,
                                         BalAsyncApi25SchemaImpl bindingHeaderObject) {

        if (!annotations.isEmpty()) {
            AnnotationNode annotationNode = annotations.get(0);
            headerName = getHeaderName(headerName, annotationNode);
        }
        if (node instanceof ArrayTypeDescriptorNode) {
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) node;
            BalAsyncApi25SchemaImpl arraySchema = new BalAsyncApi25SchemaImpl();
            arraySchema.setType(AsyncAPIType.ARRAY.toString());
            SyntaxKind kind = arrayNode.memberTypeDesc().kind();
            BalAsyncApi25SchemaImpl itemSchema = ConverterCommonUtils.getAsyncApiSchema(kind);
            if (headerSchema.getDefault() != null) {
                arraySchema.setDefault(headerSchema.getDefault());
            }
            arraySchema.setItems(itemSchema);
            bindingHeaderObject.addProperty(headerName, arraySchema);
        } else {
            bindingHeaderObject.addProperty(headerName, headerSchema);
        }
    }

    private void enableHeaderRequiredOption(Node node, BalAsyncApi25SchemaImpl headerSchema) {
        if (node.kind() == SyntaxKind.OPTIONAL_TYPE_DESC) {
            headerSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
        }
    }

    /**
     * Extract header name from header annotation value.
     *
     * @param headerName     - Header name
     * @param annotationNode - Related annotation for extract details
     * @return - Updated header name
     */
    private String getHeaderName(String headerName, AnnotationNode annotationNode) {
        if (annotationNode.annotValue().isPresent()) {
            MappingConstructorExpressionNode fieldNode = annotationNode.annotValue().get();
            SeparatedNodeList<MappingFieldNode> fields = fieldNode.fields();
            for (MappingFieldNode field : fields) {
                SpecificFieldNode sField = (SpecificFieldNode) field;
                if (sField.fieldName().toString().trim().equals("name") && sField.valueExpr().isPresent()) {
                    return sField.valueExpr().get().toString().trim().replaceAll("\"", "");
                }
            }
        }
        return headerName;
    }
}
