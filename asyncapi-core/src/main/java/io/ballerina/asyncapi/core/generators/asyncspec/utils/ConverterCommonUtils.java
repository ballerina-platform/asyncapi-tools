/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.asyncapi.core.generators.asyncspec.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Document;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.generators.asyncspec.Constants;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncAPIResult;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalBooleanSchema;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.runtime.api.utils.IdentifierUtils;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.BALLERINA;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.HYPHEN;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SLASH;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SPECIAL_CHAR_REGEX;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.UNDERSCORE;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.YAML_EXTENSION;

/**
 * Utilities used in Ballerina  to AsyncAPI converter.
 */
public class ConverterCommonUtils {

    /**
     * Retrieves a matching AsyncApi {@link Schema} for a provided ballerina type.
     *
     * @param type ballerina type name as a String
     * @return AsyncApi {@link Schema} for type defined by {@code type}
     */
    public static BalAsyncApi25SchemaImpl getAsyncApiSchema(String type) {
        BalAsyncApi25SchemaImpl schema = new BalAsyncApi25SchemaImpl();
        switch (type) {
            case Constants.STRING:
            case Constants.PLAIN:
                schema.setType(AsyncAPIType.STRING.toString());
                break;
            case Constants.BOOLEAN:
                schema.setType(AsyncAPIType.BOOLEAN.toString());
                break;
            case Constants.ARRAY:
            case Constants.TUPLE:
                schema.setType(AsyncAPIType.ARRAY.toString());
                break;
            case Constants.INT:
            case Constants.INTEGER:
                schema.setType(AsyncAPIType.INTEGER.toString());
                schema.setFormat("int64");
                break;
            case Constants.BYTE_ARRAY:
            case Constants.OCTET_STREAM:
                schema.setType(AsyncAPIType.STRING.toString());
                schema.setFormat("uuid");
                break;
            case Constants.NUMBER:
            case Constants.DECIMAL:
                schema.setType(AsyncAPIType.NUMBER.toString());
                schema.setFormat(Constants.DOUBLE);
                break;
            case Constants.FLOAT:
                schema.setType(AsyncAPIType.NUMBER.toString());
                schema.setFormat(Constants.FLOAT);
                break;
            case Constants.OBJECT:
                schema.setType(AsyncAPIType.OBJECT.toString());
                //TODO: If there are so many types like map<map<json>>, map<map<string>>,
                // map<map<int>> then those also need to be handle
                break;
            case Constants.MAP_JSON:
//            case Constants.MAP_STRING:
            case Constants.MAP:
//                objectSchema.setType(AsyncAPIType.RECORD.toString());
                schema.setType(AsyncAPIType.OBJECT.toString());
                schema.setAdditionalProperties(new BalBooleanSchema(true));
//                schema.setAdditionalProperties(true);
//                schema.setAdditionalProperties(true);
//                schema.additionalProperties(true);
                break;

            case Constants.TYPE_REFERENCE:
            case Constants.TYPEREFERENCE:
            case Constants.XML:
            case Constants.JSON:
            default:
                schema = new BalAsyncApi25SchemaImpl();  //If json is required
                break;
        }
        return schema;
    }

    /**
     * Retrieves a matching AsyncApi {@link Schema} for a provided ballerina type.
     *
     * @param type ballerina type with SYNTAX KIND
     * @return AsyncApi {@link Schema} for type defined by {@code type}
     */
    public static BalAsyncApi25SchemaImpl getAsyncApiSchema(SyntaxKind type) {
        BalAsyncApi25SchemaImpl schema = new BalAsyncApi25SchemaImpl();

        switch (type) {
            case STRING_TYPE_DESC:
                schema.setType("string");
                break;
            case BOOLEAN_TYPE_DESC:
                schema.setType("boolean");
                break;
            case ARRAY_TYPE_DESC:
                schema.setType("array");
                break;
            case INT_TYPE_DESC:
                schema.setType("integer");
                schema.setFormat("int64");
                break;
            case BYTE_TYPE_DESC:
                schema.setType("string");
                schema.setFormat("uuid");
                break;
            case DECIMAL_TYPE_DESC:
                schema.setType("number");
                schema.setFormat(Constants.DOUBLE);
                break;
            case FLOAT_TYPE_DESC:
                schema.setType("number");
                schema.setFormat(Constants.FLOAT);
                break;
            case MAP_TYPE_DESC:
                schema.setType("object");
                break;
            default:
                schema = new BalAsyncApi25SchemaImpl();
                break;
        }
        return schema;
    }

    /**
     * Parse and get the {@link AsyncApi25Document} for the given AsyncAPI contract.
     *
     * @param definitionURI URI for the AsyncAPI contract
     * @return {@link AsyncAPIResult}  AsyncAPI model
     */
    public static AsyncAPIResult parseAsyncAPIFile(String definitionURI) {
        List<AsyncAPIConverterDiagnostic> diagnostics = new ArrayList<>();
        Path contractPath = Paths.get(definitionURI);

        if (!Files.exists(contractPath)) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_103;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(),
                    error.getDescription(), null);
            diagnostics.add(diagnostic);
        }
        if (!(definitionURI.endsWith(Constants.YAML_EXTENSION) || definitionURI.endsWith(Constants.JSON_EXTENSION)
                || definitionURI.endsWith(Constants.YML_EXTENSION))) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_103;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(),
                    error.getDescription(), null);
            diagnostics.add(diagnostic);
        }
        String asyncAPIFileContent = null;
        try {
            asyncAPIFileContent = Files.readString(contractPath);
        } catch (IOException e) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_102;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(), error.getDescription(),
                    null, e.toString());
            diagnostics.add(diagnostic);
        }
        YAMLFactory factory1 = YAMLFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        new ObjectMapper();
        ObjectMapper mapper = new ObjectMapper(factory1);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        AsyncApi25Document yamldoc;
        try {
            ObjectNode yamlNodes = (ObjectNode) mapper.readTree(asyncAPIFileContent);
            yamldoc = (AsyncApi25Document) Library.readDocument(yamlNodes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<ValidationProblem> yamlprob = Library.validate(yamldoc, null);

        if (!yamlprob.isEmpty()) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_105;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(), error.getDescription(),
                    null);
            diagnostics.add(diagnostic);
            return new AsyncAPIResult(null, diagnostics);
        }
        AsyncApi25Document api = yamldoc;
        return new AsyncAPIResult(api, diagnostics);
    }
//

    public static String normalizeTitle(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        String[] urlPaths = (serviceName.replaceFirst(Constants.SLASH, "")).split(SPECIAL_CHAR_REGEX);
        StringBuilder stringBuilder = new StringBuilder();
        String title = serviceName;
        if (urlPaths.length > 1) {
            for (String path : urlPaths) {
                if (path.isBlank()) {
                    continue;
                }
                stringBuilder.append(path.substring(0, 1).toUpperCase(Locale.ENGLISH));
                stringBuilder.append(path.substring(1));
                stringBuilder.append(" ");
            }
            title = stringBuilder.toString().trim();
        } else if (urlPaths.length == 1 && !urlPaths[0].isBlank()) {
            stringBuilder.append(urlPaths[0].substring(0, 1).toUpperCase(Locale.ENGLISH));
            stringBuilder.append(urlPaths[0].substring(1));
            title = stringBuilder.toString().trim();
        }
        return title;
    }

    /**
     * This util function is to check the given service is a websocket service.
     *
     * @param serviceNode   Service node for analyse
     * @param semanticModel Semantic model
     * @return boolean output
     */
    public static boolean isWebsocketService(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
        if (serviceSymbol.isEmpty()) {
            return false;
        }

        ServiceDeclarationSymbol serviceNodeSymbol = (ServiceDeclarationSymbol) serviceSymbol.get();
        List<TypeSymbol> listenerTypes = (serviceNodeSymbol).listenerTypes();
        for (TypeSymbol listenerType : listenerTypes) {
            if (isWebsocketListener(listenerType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWebsocketListener(TypeSymbol listenerType) {
        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isWebsocketModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isWebsocketModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private static boolean isWebsocketModule(ModuleSymbol moduleSymbol) {
        if (moduleSymbol.getName().isPresent()) {
            return WEBSOCKET.equals(moduleSymbol.getName().get()) && BALLERINA.equals(moduleSymbol.id().orgName());

        } else {
            return false;
        }

    }

    /**
     * Generate file name with service basePath.
     */
    public static String getAsyncApiFileName(String servicePath, String serviceName, boolean isJson) {
        String asyncAPIFileName;
        if (serviceName.isBlank() || serviceName.equals(SLASH) || serviceName.startsWith(SLASH + HYPHEN)) {
            String[] fileName = serviceName.split(SLASH);
            // This condition is to handle `service on ep1 {} ` multiple scenarios
            if (fileName.length > 0 && !serviceName.isBlank()) {
                asyncAPIFileName = FilenameUtils.removeExtension(servicePath) + fileName[1];
            } else {
                asyncAPIFileName = FilenameUtils.removeExtension(servicePath);
            }
        } else if (serviceName.startsWith(HYPHEN)) {
            // serviceName -> service on ep1 {} has multiple service ex: "-33456"
            asyncAPIFileName = FilenameUtils.removeExtension(servicePath) + serviceName;
        } else {
            // Remove starting path separate if exists
            if (serviceName.startsWith(SLASH)) {
                serviceName = serviceName.substring(1);
            }
            // Replace rest of the path separators with underscore
            asyncAPIFileName = serviceName.replaceAll(SLASH, "_");
        }

        return getNormalizedFileName(asyncAPIFileName) + Constants.ASYNC_API_SUFFIX +
                (isJson ? JSON_EXTENSION : YAML_EXTENSION);
    }

    /**
     * Remove special characters from the given file name.
     */
    public static String getNormalizedFileName(String asyncAPIFileName) {

        String[] splitNames = asyncAPIFileName.split("[^a-zA-Z0-9]");
        if (splitNames.length > 0) {
            return Arrays.stream(splitNames)
                    .filter(namePart -> !namePart.isBlank())
                    .collect(Collectors.joining(UNDERSCORE));
        }
        return asyncAPIFileName;
    }

    public static boolean containErrors(List<Diagnostic> diagnostics) {
        return diagnostics != null && diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
    }

    public static String unescapeIdentifier(String parameterName) {
        String unescapedParamName = IdentifierUtils.unescapeBallerina(parameterName);
        return unescapedParamName.trim().replaceAll("\\\\", "").replaceAll("'", "");
    }

    public static ObjectNode createObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    public static ObjectMapper callObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        return objectMapper;
    }

    /**
     * This {@code NullLocation} represents the null location allocation for scenarios which has not location.
     */
    public static class NullLocation implements Location {

        @Override
        public LineRange lineRange() {
            LinePosition from = LinePosition.from(0, 0);
            return LineRange.from("", from, from);
        }

        @Override
        public TextRange textRange() {
            return TextRange.from(0, 0);
        }
    }
}
