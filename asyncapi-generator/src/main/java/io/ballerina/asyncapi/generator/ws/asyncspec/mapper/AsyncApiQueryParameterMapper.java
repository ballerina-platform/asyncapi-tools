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

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.Arrays;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SCHEMA_REFERENCE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.X_NULLABLE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LIST_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAPPING_CONSTRUCTOR;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NIL_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NUMERIC_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPTIONAL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;

/**
 * Processes mapping query parameters in between Ballerina and AsyncApiSpec.
 * Corresponds to the legacy {@code AsyncApiQueryParameterMapper}.
 */
public class AsyncApiQueryParameterMapper {

    private final AsyncApi30ComponentsImpl components;
    private final SemanticModel semanticModel;
    private final Map<String, String> apidocs;
    private final SyntaxKind[] validExpressionKind = {STRING_LITERAL, NUMERIC_LITERAL, BOOLEAN_LITERAL,
            LIST_CONSTRUCTOR, NIL_LITERAL, MAPPING_CONSTRUCTOR};

    /**
     * Creates a new query parameter mapper.
     *
     * @param apidocs       map of parameter name to API doc description
     * @param components    AsyncAPI components for schema registration
     * @param semanticModel Ballerina semantic model
     */
    public AsyncApiQueryParameterMapper(Map<String, String> apidocs, AsyncApi30ComponentsImpl components,
                                        SemanticModel semanticModel) {
        this.apidocs = apidocs;
        this.components = components;
        this.semanticModel = semanticModel;
    }

    /**
     * Handle function query parameters for required parameters.
     *
     * @param queryParam    required parameter node
     * @param bindingObject target schema to add query property to
     */
    public void createQueryParameter(RequiredParameterNode queryParam, BalAsyncApi30SchemaImpl bindingObject) {
        String queryParamName = unescapeIdentifier(queryParam.paramName().get().text());
        if (queryParam.typeName() instanceof BuiltinSimpleNameReferenceNode) {
            BalAsyncApi30SchemaImpl asyncApiQueryParamSchema =
                    ConverterCommonUtils.getAsyncApiSchema(queryParam.typeName().toString().trim());
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);
        } else if (queryParam.typeName().kind() == OPTIONAL_TYPE_DESC) {
            BalAsyncApi30SchemaImpl asyncApiQueryParamSchema = setOptionalQueryParameter(queryParamName,
                    ((OptionalTypeDescriptorNode) queryParam.typeName()));
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);
        } else if (queryParam.typeName().kind() == SyntaxKind.ARRAY_TYPE_DESC) {
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) queryParam.typeName();
            BalAsyncApi30SchemaImpl asyncApiQueryParamSchema = handleArrayTypeQueryParameter(queryParamName, arrayNode);
            bindingObject.addProperty(queryParamName, asyncApiQueryParamSchema);
        } else if (queryParam.typeName() instanceof SimpleNameReferenceNode) {
            SimpleNameReferenceNode queryNode = (SimpleNameReferenceNode) queryParam.typeName();
            AsyncApiComponentMapper componentMapper = new AsyncApiComponentMapper(components);
            TypeSymbol typeSymbol = (TypeSymbol) semanticModel.symbol(queryNode).orElseThrow();
            componentMapper.createComponentSchema(typeSymbol, null);
            BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
            schema.set$ref(SCHEMA_REFERENCE + ConverterCommonUtils.unescapeIdentifier(queryNode.name().text().trim()));
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                schema.setDescription(apidocs.get(queryParamName.trim()));
            }
            bindingObject.addProperty(queryParamName, schema);
        } else {
            BalAsyncApi30SchemaImpl schema = createContentTypeForMapJson(queryParamName, false);
            if (!apidocs.isEmpty() && queryParam.paramName().isPresent() && apidocs.containsKey(queryParamName)) {
                schema.setDescription(apidocs.get(queryParamName.trim()));
            }
            bindingObject.addProperty(queryParamName, schema);
        }
    }

    /**
     * Create AsyncAPISpec query parameter for default query parameters.
     *
     * @param defaultableQueryParam defaultable parameter node
     * @param bindingQueryObject    target schema to add query property to
     */
    public void createQueryParameter(DefaultableParameterNode defaultableQueryParam,
                                     BalAsyncApi30SchemaImpl bindingQueryObject) {
        String queryParamName = defaultableQueryParam.paramName().get().text();
        BalAsyncApi30SchemaImpl asyncApiQueryParamDefaultSchema = null;
        if (defaultableQueryParam.typeName() instanceof BuiltinSimpleNameReferenceNode) {
            asyncApiQueryParamDefaultSchema = ConverterCommonUtils.getAsyncApiSchema(
                    defaultableQueryParam.typeName().toString().trim());
            if (!apidocs.isEmpty() && defaultableQueryParam.paramName().isPresent()
                    && apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamDefaultSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
        } else if (defaultableQueryParam.typeName().kind() == OPTIONAL_TYPE_DESC) {
            asyncApiQueryParamDefaultSchema = setOptionalQueryParameter(queryParamName,
                    ((OptionalTypeDescriptorNode) defaultableQueryParam.typeName()));
        } else if (defaultableQueryParam.typeName() instanceof ArrayTypeDescriptorNode) {
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) defaultableQueryParam.typeName();
            asyncApiQueryParamDefaultSchema = handleArrayTypeQueryParameter(queryParamName, arrayNode);
        } else {
            asyncApiQueryParamDefaultSchema = createContentTypeForMapJson(queryParamName, false);
            if (!apidocs.isEmpty() && defaultableQueryParam.paramName().isPresent()
                    && apidocs.containsKey(queryParamName)) {
                asyncApiQueryParamDefaultSchema.setDescription(apidocs.get(queryParamName.trim()));
            }
        }

        if (Arrays.stream(validExpressionKind).anyMatch(syntaxKind -> syntaxKind
                == defaultableQueryParam.expression().kind())) {
            String defaultValue = defaultableQueryParam.expression().toString().trim()
                    .replaceAll("\"", "");
            if (defaultableQueryParam.expression().kind() == NIL_LITERAL) {
                defaultValue = null;
            }
            asyncApiQueryParamDefaultSchema.setDefault(new TextNode(defaultValue));
            bindingQueryObject.addProperty(queryParamName, asyncApiQueryParamDefaultSchema);
        } else {
            bindingQueryObject.addProperty(queryParamName, asyncApiQueryParamDefaultSchema);
        }
    }

    /**
     * Handle array type query parameter.
     */
    private BalAsyncApi30SchemaImpl handleArrayTypeQueryParameter(String queryParamName,
                                                                   ArrayTypeDescriptorNode arrayNode) {
        BalAsyncApi30SchemaImpl arraySchema = new BalAsyncApi30SchemaImpl();
        arraySchema.setType(AsyncAPIType.ARRAY.toString());
        TypeDescriptorNode itemTypeNode = arrayNode.memberTypeDesc();
        BalAsyncApi30SchemaImpl itemSchema;
        if (arrayNode.memberTypeDesc().kind() == OPTIONAL_TYPE_DESC) {
            itemSchema = ConverterCommonUtils.getAsyncApiSchema(
                    ((OptionalTypeDescriptorNode) itemTypeNode).typeDescriptor().toString().trim());
            itemSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
        } else {
            itemSchema = ConverterCommonUtils.getAsyncApiSchema(itemTypeNode.toString().trim());
        }
        arraySchema.setItems(itemSchema);
        if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
            arraySchema.setDescription(apidocs.get(queryParamName));
        }
        return arraySchema;
    }

    /**
     * Handle optional query parameter.
     */
    private BalAsyncApi30SchemaImpl setOptionalQueryParameter(String queryParamName,
                                                               OptionalTypeDescriptorNode typeNode) {
        Node node = typeNode.typeDescriptor();
        if (node.kind() == SyntaxKind.ARRAY_TYPE_DESC) {
            BalAsyncApi30SchemaImpl arraySchema = ConverterCommonUtils.getAsyncApiSchema(AsyncAPIType.ARRAY.toString());
            arraySchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
            ArrayTypeDescriptorNode arrayNode = (ArrayTypeDescriptorNode) node;
            TypeDescriptorNode itemTypeNode = arrayNode.memberTypeDesc();
            BalAsyncApi30SchemaImpl itemSchema = ConverterCommonUtils.getAsyncApiSchema(itemTypeNode.toString().trim());
            arraySchema.setItems(itemSchema);
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                arraySchema.setDescription(apidocs.get(queryParamName));
            }
            return arraySchema;
        } else if (node.kind() == SyntaxKind.MAP_TYPE_DESC) {
            BalAsyncApi30SchemaImpl mapJsonSchema = createContentTypeForMapJson(queryParamName, true);
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                mapJsonSchema.setDescription(apidocs.get(queryParamName));
            }
            return mapJsonSchema;
        } else {
            BalAsyncApi30SchemaImpl asyncApiSchema = ConverterCommonUtils.getAsyncApiSchema(node.toString().trim());
            asyncApiSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
            if (!apidocs.isEmpty() && apidocs.containsKey(queryParamName)) {
                asyncApiSchema.setDescription(apidocs.get(queryParamName));
            }
            return asyncApiSchema;
        }
    }

    private BalAsyncApi30SchemaImpl createContentTypeForMapJson(String queryParamName, boolean nullable) {
        BalAsyncApi30SchemaImpl objectSchema = ConverterCommonUtils.getAsyncApiSchema(AsyncAPIType.OBJECT.toString());
        BalAsyncApi30SchemaImpl emptyObjectSchema = ConverterCommonUtils.getAsyncApiSchema(queryParamName);
        if (nullable) {
            objectSchema.addExtension(X_NULLABLE, BooleanNode.TRUE);
        }
        objectSchema.setAdditionalProperties(emptyObjectSchema);
        return objectSchema;
    }
}
