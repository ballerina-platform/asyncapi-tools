/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * ... (License header remains unchanged) ...
 */
package io.ballerina.asyncapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.deref.Dereferencer;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.refs.ReferenceResolverChain;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class AsyncApiParser {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ASYNCAPI = "asyncapi";
    private static final String V3_0_0 = "3.0.0";
    // Any 3.x.y, with the pre-release suffix AsyncAPI allows (3.1.0-rc1); 3.0.0 itself is unchanged by the rewrite
    private static final Pattern LATER_3X = Pattern.compile("3\\.\\d+\\.\\d+(-[0-9A-Za-z.-]+)?");

    private AsyncApiParser() {

    }

    /**
     * Parses an AsyncAPI specification from a JSON string.
     *
     * @param jsonString the AsyncAPI JSON content
     * @return the parsed {@link AsyncApiSpec}
     * @throws AsyncApiParserException if the JSON is blank, invalid, or fails validation
     */
    public static AsyncApiSpec parseFromJsonString(String jsonString) throws AsyncApiParserException {
        if (StringUtils.isBlank(jsonString)) {
            throw new AsyncApiParserException("AsyncAPI specification JSON cannot be null or empty.");
        }

        Document rootDocument;
        String version;
        try {
            JsonNode root = JSON.readTree(jsonString);
            if (!(root instanceof ObjectNode object)) {
                throw new AsyncApiParserException("An AsyncAPI specification must be a JSON object.");
            }
            version = object.path(ASYNCAPI).asText(null);
            // Apicurio reads up to 3.0.0, and later 3.x versions only add to it, so they are read as 3.0.0
            if (version != null && LATER_3X.matcher(version).matches()) {
                object.put(ASYNCAPI, V3_0_0);
            }
            rootDocument = Library.readDocument(object);
        } catch (AsyncApiParserException e) {
            throw e;
        } catch (Exception e) {
            throw new AsyncApiParserException(e.getMessage(), e);
        }

        validateDocument(rootDocument);
        AsyncApiDocument dereferencedDocument = getAsyncApiDocument(rootDocument);
        dereferencedDocument.setAsyncapi(version);
        try {
            return AsyncApiSpecCreator.create(dereferencedDocument);
        } catch (Exception e) {
            throw new AsyncApiParserException("Failed to build AsyncAPI spec model: " + e.getMessage(), e);
        }
    }

    /**
     * Maps an inline JSON Schema node, such as an {@code x-response} payload, to the schema model.
     *
     * @param schemaNode the JSON Schema object
     * @return the mapped schema, or {@code null} if {@code schemaNode} is not a JSON object
     */
    public static AsyncApiSchema parseSchema(JsonNode schemaNode) {
        return SchemaMapper.mapFromJsonNode(schemaNode);
    }

    private static void validateDocument(Document document) throws AsyncApiParserException {
        List<ValidationProblem> problems = Library.validate(document, null);
        if (problems == null || problems.isEmpty()) {
            return;
        }

        // Filter out Apicurio false positives: oneOf message containers have no direct
        // payload/headers — the individual oneOf items do. Also, Apicurio incorrectly
        // enforces 'location' as required on Parameter Objects; the AsyncAPI spec makes it optional.
        List<ValidationProblem> realProblems = problems.stream()
                .filter(p -> !p.message.contains("payload or headers"))
                .filter(p -> !p.message.contains("'location' property"))
                .collect(Collectors.toList());

        if (!realProblems.isEmpty()) {
            String errorMessages = realProblems.stream().map(p -> "[" + p.message + "]")
                    .collect(Collectors.joining(" | "));
            throw new AsyncApiParserException("AsyncAPI validation failed: " + errorMessages);
        }
    }

    private static AsyncApiDocument getAsyncApiDocument(Document rootDocument) throws AsyncApiParserException {
        if (!(rootDocument instanceof AsyncApiDocument document)) {
            throw new AsyncApiParserException("The provided JSON is not a valid AsyncAPI document. Detected type: " +
                            rootDocument.getClass().getSimpleName());
        }

        try {
            Dereferencer dereferencer = new Dereferencer(ReferenceResolverChain.getInstance(), false);
            return (AsyncApiDocument) dereferencer.dereference(document);
        } catch (Exception e) {
            throw new AsyncApiParserException("Failed to dereference AsyncAPI document: " + e.getMessage(), e);
        }
    }
}
