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
import io.ballerina.asyncapi.core.implementation.utils.JsonPointers;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AsyncApiParser {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ASYNCAPI = "asyncapi";
    private static final String V3_0_0 = "3.0.0";
    private static final String OPERATIONS = "operations";
    private static final String CHANNELS = "channels";
    private static final String COMPONENTS = "components";
    private static final String CHANNEL = "channel";
    private static final String MESSAGES = "messages";
    private static final String REPLY = "reply";
    private static final String REF = "$ref";
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
        ObjectNode object;
        try {
            JsonNode root = JSON.readTree(jsonString);
            if (!(root instanceof ObjectNode rootObject)) {
                throw new AsyncApiParserException("An AsyncAPI specification must be a JSON object.");
            }
            object = rootObject;
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

        validateDocument(rootDocument, object);
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

    private static final String DOCUMENT_ID_PROBLEM = "Document ID should be a valid URI";
    private static final String MISSING_ADDRESS_PROBLEM = "Channel is missing the 'address' property";
    // Apicurio checks these refs without following a $ref on the way or decoding ~1, so brokenRefs checks them
    private static final List<String> CHANNEL_AND_MESSAGE_REF_PROBLEMS = List.of(
            "Operation channel reference must point to valid channel",
            "Message references must point to valid messages",
            "Reply channel reference must point to valid channel",
            "Reply message references must point to valid messages");

    private static void validateDocument(Document document, ObjectNode root) throws AsyncApiParserException {
        List<ValidationProblem> problems = Library.validate(document, null);
        List<String> refProblems = brokenRefs(root);
        if ((problems == null || problems.isEmpty()) && refProblems.isEmpty()) {
            return;
        }
        if (problems == null) {
            problems = List.of();
        }

        // Filter out Apicurio false positives: oneOf message containers have no direct
        // payload/headers — the individual oneOf items do. Also, Apicurio incorrectly
        // enforces 'location' as required on Parameter Objects; the AsyncAPI spec makes it optional.
        // Apicurio checks the document id as a URL, but the spec only needs a URI, such as a urn:
        boolean idIsUri = document instanceof AsyncApiDocument asyncApiDocument
                && isAbsoluteUri(asyncApiDocument.getId());
        List<ValidationProblem> realProblems = problems.stream()
                .filter(p -> !p.message.contains("payload or headers"))
                .filter(p -> !p.message.contains("'location' property"))
                .filter(p -> !(idIsUri && p.message.contains(DOCUMENT_ID_PROBLEM)))
                // 3.0 makes a channel address optional; a missing or null one means it is dynamic
                .filter(p -> !p.message.contains(MISSING_ADDRESS_PROBLEM))
                .filter(p -> CHANNEL_AND_MESSAGE_REF_PROBLEMS.stream().noneMatch(p.message::contains))
                .collect(Collectors.toList());

        if (!realProblems.isEmpty() || !refProblems.isEmpty()) {
            String errorMessages = Stream.concat(realProblems.stream().map(p -> p.message), refProblems.stream())
                    .map(message -> "[" + message + "]")
                    .collect(Collectors.joining(" | "));
            throw new AsyncApiParserException("AsyncAPI validation failed: " + errorMessages);
        }
    }

    /**
     * Lists the 3.x channel and message refs, in channels and operations, that point nowhere in the document.
     */
    private static List<String> brokenRefs(ObjectNode root) {
        List<String> broken = new ArrayList<>();
        if (!root.path(ASYNCAPI).asText("").startsWith("3.")) {
            return broken;
        }
        checkChannels(root, root.path(CHANNELS), "channel", broken);
        checkChannels(root, root.path(COMPONENTS).path(CHANNELS), "component channel", broken);
        root.path(OPERATIONS).properties().forEach(entry -> {
            String operation = entry.getKey();
            JsonNode reply = entry.getValue().path(REPLY);
            checkRef(root, entry.getValue().path(CHANNEL), "operation '" + operation + "' channel", broken);
            entry.getValue().path(MESSAGES).forEach(message ->
                    checkRef(root, message, "operation '" + operation + "' message", broken));
            checkRef(root, reply.path(CHANNEL), "operation '" + operation + "' reply channel", broken);
            reply.path(MESSAGES).forEach(message ->
                    checkRef(root, message, "operation '" + operation + "' reply message", broken));
        });
        return broken;
    }

    private static void checkChannels(JsonNode root, JsonNode channels, String kind, List<String> broken) {
        channels.properties().forEach(channel -> {
            String what = kind + " '" + channel.getKey() + "'";
            checkRef(root, channel.getValue(), what, broken);
            channel.getValue().path(MESSAGES).properties().forEach(message ->
                    checkRef(root, message.getValue(), what + " message '" + message.getKey() + "'", broken));
        });
    }

    private static void checkRef(JsonNode root, JsonNode reference, String what, List<String> broken) {
        JsonNode ref = reference.path(REF);
        if (ref.isTextual() && JsonPointers.resolve(root, ref.asText()) == null) {
            broken.add(what + " reference '" + ref.asText() + "' doesn't point to anything in the document");
        }
    }

    private static boolean isAbsoluteUri(String value) {
        if (value == null) {
            return false;
        }
        try {
            URI uri = new URI(value);
            // java.net.URI lets [ and ] into an opaque part; RFC 3986 allows them only around an IPv6 host
            return uri.isAbsolute() && !(uri.isOpaque() && StringUtils.containsAny(value, '[', ']'));
        } catch (URISyntaxException e) {
            return false;
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
