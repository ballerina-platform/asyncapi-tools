/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.asyncapi.core.generators.asyncspec.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.syntax.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;


/**
 * This class for the mapping ballerina headers with OAS header parameter sections.
 *
 * @since 2.0.0
 */
public class AsyncAPIHeaderMapper {
    private final Map<String, String> apidocs;

    public AsyncAPIHeaderMapper(Map<String, String> apidocs) {
        this.apidocs = apidocs;

    }

    /**
     * Handle header parameters in ballerina data type.
     *
     * @param headerParam    -  {@link RequiredParameterNode} type header parameter node
     */
    public void setHeaderParameter(RequiredParameterNode headerParam, AsyncApi25SchemaImpl bindingHeaderObject) {
//        List<Parameter> parameters = new ArrayList<>();
        String headerName = unescapeIdentifier(extractHeaderName(headerParam));
//        HeaderParameter headerParameter = new HeaderParameter();
        Node node = headerParam.typeName();
        AsyncApi25SchemaImpl headerTypeSchema = ConverterCommonUtils.getAsyncApiSchema(getHeaderType(headerParam));
        //TODO : If there "http:ServiceConfig", "treatNilableAsOptional" uncomment below codes and then implement it
//        NodeList<AnnotationNode> annotations = getAnnotationNodesFromServiceNode(headerParam);
//        String isOptional = Constants.TRUE;
//        if (!annotations.isEmpty()) {
//            Optional<String> values = ConverterCommonUtils.extractServiceAnnotationDetails(annotations,
//                    "http:ServiceConfig", "treatNilableAsOptional");
//            if (values.isPresent()) {
//                isOptional = values.get();
//            }
//        }
        enableHeaderRequiredOption(node, headerTypeSchema);
        if (apidocs != null && apidocs.containsKey(headerName)) {
            headerTypeSchema.setDescription(apidocs.get(headerName.trim()));
        }
        completeHeaderParameter(headerName,  headerTypeSchema, headerParam.annotations(),
                headerParam.typeName(),bindingHeaderObject);
//        return parameters;
    }

    private String extractHeaderName(ParameterNode headerParam) {
        if (headerParam instanceof DefaultableParameterNode) {
            return ((DefaultableParameterNode) headerParam).paramName().get().text().replaceAll("\\\\", "");
        }
        return ((RequiredParameterNode) headerParam).paramName().get().text().replaceAll("\\\\", "");
    }

    /**
     * Handle header parameters in ballerina data type.
     *
     * @param headerParam    -  {@link DefaultableParameterNode} type header parameter node
     */
    public void setHeaderParameter(DefaultableParameterNode headerParam,AsyncApi25SchemaImpl bindingHeaderObject ) {
//        List<Parameter> parameters = new ArrayList<>();
        String headerName = extractHeaderName(headerParam);
//        HeaderParameter headerParameter = new HeaderParameter();
        AsyncApi25SchemaImpl headerTypeSchema = ConverterCommonUtils.getAsyncApiSchema(getHeaderType(headerParam));
        String defaultValue = headerParam.expression().toString().trim();
        if (defaultValue.length() > 1 && defaultValue.charAt(0) == '"' &&
                defaultValue.charAt(defaultValue.length() - 1) == '"') {
            defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
        }
        List<SyntaxKind> allowedTypes = new ArrayList<>();
        allowedTypes.addAll(Arrays.asList(SyntaxKind.STRING_LITERAL, SyntaxKind.NUMERIC_LITERAL,
                SyntaxKind.BOOLEAN_LITERAL));
        if (allowedTypes.contains(headerParam.expression().kind())) {
            headerTypeSchema.setDefault(new TextNode(defaultValue));
        } else if (headerParam.expression().kind() == SyntaxKind.LIST_CONSTRUCTOR) {
            headerTypeSchema = new AsyncApi25SchemaImpl();
            headerTypeSchema.setDefault(new TextNode(defaultValue));
        }
        if (headerParam.typeName().kind() == SyntaxKind.OPTIONAL_TYPE_DESC) {
            headerTypeSchema.addExtension(X_NULLABLE,BooleanNode.TRUE);
        }
        if (apidocs != null && apidocs.containsKey(headerName)) {
            headerTypeSchema.setDescription(apidocs.get(headerName.trim()));
        }
        completeHeaderParameter(headerName, headerTypeSchema, headerParam.annotations(),
                headerParam.typeName(),bindingHeaderObject);
    }

    /**
     * Extract header type by removing its optional and array types.
     */
    private String getHeaderType(ParameterNode headerParam) {
        if (headerParam instanceof DefaultableParameterNode) {
            return ((DefaultableParameterNode) headerParam).typeName().toString().replaceAll("\\?", "").
                    replaceAll("\\[", "").replaceAll("\\]", "").trim();
        }
        return ((RequiredParameterNode) headerParam).typeName().toString().replaceAll("\\?", "").
                replaceAll("\\[", "").replaceAll("\\]", "").trim();
    }

    /**
     * Assign header values to OAS header parameter.
     */
    private void completeHeaderParameter(String headerName,
                                         AsyncApi25SchemaImpl headerSchema, NodeList<AnnotationNode> annotations, Node node, AsyncApi25SchemaImpl  bindingHeaderObject) {

        if (!annotations.isEmpty()) {
            AnnotationNode annotationNode = annotations.get(0);
            headerName = getHeaderName(headerName, annotationNode);
        }
        if (node instanceof ArrayTypeDescriptorNode) {
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) node;
            AsyncApi25SchemaImpl arraySchema = new AsyncApi25SchemaImpl();
            arraySchema.setType("array");
            SyntaxKind kind = arrayNode.memberTypeDesc().kind();
            AsyncApi25SchemaImpl itemSchema = ConverterCommonUtils.getAsyncApiSchema(kind);
            if (headerSchema.getDefault() != null) {
                arraySchema.setDefault(headerSchema.getDefault());
            }
            //TODO : Decide whether this will be another object , because of the field entity:true
            ObjectMapper test= new ObjectMapper();
            test.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
            ObjectNode obj=test.valueToTree(itemSchema);
//            ((ObjectNode)obj.get("properties").get("offset")).remove("entity");
//        obj.remove;
            obj.remove("entity");
            arraySchema.setItems(obj);
            bindingHeaderObject.addProperty(headerName,arraySchema);
//            headerParameter.schema(arraySchema);
//            headerParameter.setName(headerName);
//            parameters.add(headerParameter);
        } else {
            bindingHeaderObject.addProperty(headerName,headerSchema);

//            headerParameter.schema(headerSchema);
//            headerParameter.setName(headerName);
//            parameters.add(headerParameter);
        }
    }

    private void enableHeaderRequiredOption( Node node, AsyncApi25SchemaImpl headerSchema) {
        //TODO : After setting treatNilableAsOptional:true then change this also
        if (node.kind() == SyntaxKind.OPTIONAL_TYPE_DESC) {
            headerSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
//            if (isOptional.equals(Constants.FALSE)) {
//                headerParameter.setRequired(true);
//            }
//        } else {
//            headerParameter.setRequired(true);
//        }
        }
    }

    /**
     * Extract header name from header annotation value.
     *
     * @param headerName        - Header name
     * @param annotationNode    - Related annotation for extract details
     * @return                  - Updated header name
     */
    private String getHeaderName(String headerName, AnnotationNode annotationNode) {
        if (annotationNode.annotValue().isPresent()) {
            MappingConstructorExpressionNode fieldNode = annotationNode.annotValue().get();
            SeparatedNodeList<MappingFieldNode> fields = fieldNode.fields();
            for (MappingFieldNode field: fields) {
                SpecificFieldNode sField = (SpecificFieldNode) field;
                if (sField.fieldName().toString().trim().equals("name") && sField.valueExpr().isPresent()) {
                    return sField.valueExpr().get().toString().trim().replaceAll("\"", "");
                }
            }
        }
        return headerName;
    }
}
