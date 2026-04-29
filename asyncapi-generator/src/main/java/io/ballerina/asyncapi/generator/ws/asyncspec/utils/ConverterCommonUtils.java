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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.models.union.BooleanUnionValueImpl;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.generator.ws.asyncspec.Constants;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.AsyncApiResult;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
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

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.AsyncAPIType;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.BALLERINA;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.HYPHEN;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SLASH;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SPECIAL_CHAR_REGEX;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.UNDERSCORE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.WEBSOCKET;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.YAML_EXTENSION;

/**
 * Common utilities shared across Ballerina-to-AsyncAPI 3.0 conversion mappers.
 * Corresponds to the legacy {@code ConverterCommonUtils}.
 */
public final class ConverterCommonUtils {

    private ConverterCommonUtils() {
    }

    /**
     * Returns a {@link BalAsyncApi30SchemaImpl} matching the given Ballerina type string.
     *
     * @param type Ballerina type name
     * @return schema representing the type
     */
    public static BalAsyncApi30SchemaImpl getAsyncApiSchema(String type) {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
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
                break;
            case Constants.MAP_JSON:
            case Constants.MAP:
                schema.setType(AsyncAPIType.OBJECT.toString());
                schema.setAdditionalProperties(new BooleanUnionValueImpl(true));
                break;
            case Constants.TYPE_REFERENCE:
            case Constants.TYPEREFERENCE:
            case Constants.XML:
            case Constants.JSON:
            default:
                schema = new BalAsyncApi30SchemaImpl();
                break;
        }
        return schema;
    }

    /**
     * Returns a {@link BalAsyncApi30SchemaImpl} matching the given Ballerina {@link SyntaxKind}.
     *
     * @param type syntax kind of the type descriptor
     * @return schema representing the type
     */
    public static BalAsyncApi30SchemaImpl getAsyncApiSchema(SyntaxKind type) {
        BalAsyncApi30SchemaImpl schema = new BalAsyncApi30SchemaImpl();
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
                schema = new BalAsyncApi30SchemaImpl();
                break;
        }
        return schema;
    }

    /**
     * Parses an AsyncAPI 3.0 contract file and returns it as an {@link AsyncApiResult}.
     *
     * @param definitionURI path to the AsyncAPI YAML or JSON file
     * @return result containing the parsed document or diagnostics on failure
     */
    public static AsyncApiResult parseAsyncAPIFile(String definitionURI) {
        List<AsyncApiConverterDiagnostic> diagnostics = new ArrayList<>();
        Path contractPath = Paths.get(definitionURI);

        if (!Files.exists(contractPath)) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_103;
            diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(), null));
        }
        if (!(definitionURI.endsWith(Constants.YAML_EXTENSION) || definitionURI.endsWith(Constants.JSON_EXTENSION)
                || definitionURI.endsWith(Constants.YML_EXTENSION))) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_104;
            diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(), null));
        }
        if (!diagnostics.isEmpty()) {
            return new AsyncApiResult(null, null, diagnostics);
        }

        String asyncAPIFileContent;
        try {
            asyncAPIFileContent = Files.readString(contractPath);
        } catch (IOException e) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_102;
            diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(), null, e.toString()));
            return new AsyncApiResult(null, null, diagnostics);
        }

        YAMLFactory factory = YAMLFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        AsyncApi30Document yamlDoc;
        try {
            ObjectNode yamlNodes = (ObjectNode) mapper.readTree(asyncAPIFileContent);
            yamlDoc = (AsyncApi30Document) Library.readDocument(yamlNodes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<ValidationProblem> problems = Library.validate(yamlDoc, null);
        if (!problems.isEmpty()) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_105;
            diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(), null));
            return new AsyncApiResult(null, null, diagnostics);
        }
        return new AsyncApiResult(null, yamlDoc, diagnostics);
    }

    /**
     * Normalises a service path or name to a human-readable title string.
     *
     * @param serviceName raw service name or path
     * @return title-cased string, or null if input is null
     */
    public static String normalizeTitle(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        String[] urlPaths = (serviceName.replaceFirst(SLASH, "")).split(SPECIAL_CHAR_REGEX);
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
     * Returns {@code true} if the given service is a Ballerina WebSocket service.
     *
     * @param serviceNode   service declaration node
     * @param semanticModel semantic model for the module
     * @return true if WebSocket service
     */
    public static boolean isWebsocketService(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
        if (serviceSymbol.isEmpty()) {
            return false;
        }
        ServiceDeclarationSymbol serviceNodeSymbol = (ServiceDeclarationSymbol) serviceSymbol.get();
        List<TypeSymbol> listenerTypes = serviceNodeSymbol.listenerTypes();
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
                    .anyMatch(typeReferenceTypeSymbol ->
                            isWebsocketModule(typeReferenceTypeSymbol.getModule().get()));
        }
        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isWebsocketModule(
                    ((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }
        return false;
    }

    private static boolean isWebsocketModule(ModuleSymbol moduleSymbol) {
        if (moduleSymbol.getName().isPresent()) {
            return WEBSOCKET.equals(moduleSymbol.getName().get())
                    && BALLERINA.equals(moduleSymbol.id().orgName());
        }
        return false;
    }

    /**
     * Generates the output AsyncAPI file name from the source file path and service name.
     *
     * @param servicePath source Ballerina file path
     * @param serviceName service base path
     * @param isJson      true to generate a {@code .json} name, false for {@code .yaml}
     * @return generated file name including extension
     */
    public static String getAsyncApiFileName(String servicePath, String serviceName, boolean isJson) {
        String asyncAPIFileName;
        if (serviceName.isBlank() || serviceName.equals(SLASH) || serviceName.startsWith(SLASH + HYPHEN)) {
            String[] fileName = serviceName.split(SLASH);
            if (fileName.length > 0 && !serviceName.isBlank()) {
                asyncAPIFileName = FilenameUtils.removeExtension(servicePath) + fileName[1];
            } else {
                asyncAPIFileName = FilenameUtils.removeExtension(servicePath);
            }
        } else if (serviceName.startsWith(HYPHEN)) {
            asyncAPIFileName = FilenameUtils.removeExtension(servicePath) + serviceName;
        } else {
            if (serviceName.startsWith(SLASH)) {
                serviceName = serviceName.substring(1);
            }
            asyncAPIFileName = serviceName.replaceAll(SLASH, UNDERSCORE);
        }
        return getNormalizedFileName(asyncAPIFileName) + Constants.ASYNC_API_SUFFIX
                + (isJson ? JSON_EXTENSION : YAML_EXTENSION);
    }

    /**
     * Removes special characters from a file name, joining parts with underscores.
     *
     * @param asyncAPIFileName raw file name
     * @return normalised file name
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

    /**
     * Returns {@code true} if the diagnostics list contains any ERROR-severity entry.
     *
     * @param diagnostics list of Ballerina diagnostics
     * @return true if errors present
     */
    public static boolean containErrors(List<Diagnostic> diagnostics) {
        return diagnostics != null && diagnostics.stream().anyMatch(diagnostic ->
                diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR);
    }

    /**
     * Removes Ballerina escape characters from an identifier.
     *
     * @param parameterName identifier text from the syntax tree
     * @return unescaped identifier
     */
    public static String unescapeIdentifier(String parameterName) {
        String unescapedParamName = IdentifierUtils.unescapeBallerina(parameterName);
        return unescapedParamName.trim().replaceAll("\\\\", "").replaceAll("'", "");
    }

    /**
     * Creates a new empty {@link ObjectNode} using the default {@link JsonNodeFactory}.
     *
     * @return new empty ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    /**
     * Creates an {@link ObjectMapper} configured for AsyncAPI serialisation.
     *
     * @return configured ObjectMapper
     */
    public static ObjectMapper callObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        return objectMapper;
    }

    /**
     * A {@code NullLocation} representing a placeholder location for diagnostics that have no source position.
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
