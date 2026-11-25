/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * ... (License header remains unchanged) ...
 */
package io.ballerina.asyncapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.deref.Dereferencer;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.refs.ReferenceResolverChain;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.implementation.common.SchemaFormats;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.utils.JsonPointers;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
    private static final String REPLIES = "replies";
    private static final String LOCAL_REF = "#";
    private static final String ROOT_CHANNELS_REF = "#/channels/";
    private static final String COMPONENT_CHANNELS_REF = "#/components/channels/";
    private static final String CHANNEL_MESSAGES = "/messages/";
    // The kind brokenRefs gives the operations under the root "operations"
    private static final String OPERATION_KIND = "operation";
    private static final String REF = "$ref";
    private static final String SCHEMAS = "schemas";
    private static final String PAYLOAD = "payload";
    private static final String HEADERS = "headers";
    private static final String SCHEMA_FORMAT = "schemaFormat";
    private static final String BUNDLE_FIX = "Bundle the spec into one file, such as with 'asyncapi bundle', and "
            + "generate again.";
    private static final String COMPONENT_SCHEMAS_REF = "#/components/schemas/";
    // A ref to a message object itself, rather than to a schema inside it
    private static final Pattern MESSAGE_REF = Pattern.compile(
            "#/(components/messages|channels/[^/]+/messages|components/channels/[^/]+/messages)/[^/]+");
    // Keys whose values are data, so a $ref in them isn't a reference
    private static final Set<String> SCHEMA_DATA_KEYS = Set.of("examples", "example", "default", "const", "enum");
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
        return parseFromJsonString(jsonString, warning -> { });
    }

    /**
     * Parses an AsyncAPI specification from a JSON string, correcting common 3.x mistakes whose meaning is clear,
     * such as a scheme in a server host.
     *
     * @param jsonString the AsyncAPI JSON content
     * @param warnings   receives one warning per correction, saying how to fix the spec
     * @return the parsed {@link AsyncApiSpec}
     * @throws AsyncApiParserException if the JSON is blank, invalid, or fails validation
     */
    public static AsyncApiSpec parseFromJsonString(String jsonString, Consumer<SpecWarning> warnings)
            throws AsyncApiParserException {
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
            if (V3_0_0.equals(object.path(ASYNCAPI).asText())) {
                LenientReads.apply(object, warnings);
            }
            rootDocument = Library.readDocument(object);
        } catch (AsyncApiParserException e) {
            throw e;
        } catch (Exception e) {
            throw new AsyncApiParserException(e.getMessage(), e);
        }

        validateDocument(rootDocument, object, warnings);
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

    private static void validateDocument(Document document, ObjectNode root, Consumer<SpecWarning> warnings)
            throws AsyncApiParserException {
        List<ValidationProblem> problems = Library.validate(document, null);
        boolean v3 = root.path(ASYNCAPI).asText("").startsWith("3.");
        List<String> refProblems = new ArrayList<>(v3 ? brokenRefs(root) : List.of());
        refProblems.addAll(schemaRefProblems(root, warnings));
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
                .filter(p -> !v3 || CHANNEL_AND_MESSAGE_REF_PROBLEMS.stream().noneMatch(p.message::contains))
                .collect(Collectors.toList());

        if (!realProblems.isEmpty() || !refProblems.isEmpty()) {
            String errorMessages = Stream.concat(realProblems.stream().map(p -> p.message), refProblems.stream())
                    .map(message -> "[" + message + "]")
                    .collect(Collectors.joining(" | "));
            throw new AsyncApiParserException("AsyncAPI validation failed: " + errorMessages);
        }
    }

    /**
     * Lists the local channel, message, operation and reply refs of a 3.x document that point nowhere in it.
     */
    private static List<String> brokenRefs(ObjectNode root) {
        List<String> broken = new ArrayList<>();
        JsonNode components = root.path(COMPONENTS);
        checkChannels(root, root.path(CHANNELS), "channel", broken);
        checkChannels(root, components.path(CHANNELS), "component channel", broken);
        checkOperations(root, root.path(OPERATIONS), OPERATION_KIND, broken);
        checkOperations(root, components.path(OPERATIONS), "component operation", broken);
        components.path(REPLIES).properties().forEach(reply -> checkReply(root, reply.getValue(),
                MissingNode.getInstance(), "component reply '" + reply.getKey() + "'", broken));
        return broken;
    }

    /**
     * Lists the schema refs to a message whose type would be missing, in any AsyncAPI version, and warns once per
     * file about refs into another file, which isn't read. A payload in another schema format is left out.
     */
    private static List<String> schemaRefProblems(ObjectNode root, Consumer<SpecWarning> warnings) {
        List<String> problems = new ArrayList<>();
        Map<String, List<String>> otherFiles = new LinkedHashMap<>();
        String schemasAt = "/" + COMPONENTS + "/" + SCHEMAS;
        root.path(COMPONENTS).path(SCHEMAS).properties().forEach(schema -> checkSchemaRefs(root, schema.getValue(),
                schemasAt + "/" + pointerToken(schema.getKey()), problems, otherFiles));
        checkPayloads(root, root, "", schemasAt, problems, otherFiles);
        // A type it names may still be defined here, so the spec is read with a warning
        otherFiles.forEach((file, refs) -> warnings.accept(new SpecWarning(refs.get(0), refs.size() == 1
                ? String.format("the schema reference at %s points into another file, '%s', which isn't read, so "
                        + "a type it names may be missing", refs.get(0), file)
                : String.format("%d schema references point into another file, '%s', which isn't read, so a type "
                        + "they name may be missing; the first is at %s", refs.size(), file, refs.get(0)),
                BUNDLE_FIX)));
        return problems;
    }

    // Each message's payload and headers schemas, wherever the message is written
    private static void checkPayloads(JsonNode root, JsonNode node, String at, String schemasAt,
                                      List<String> problems, Map<String, List<String>> otherFiles) {
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                checkPayloads(root, node.get(i), at + "/" + i, schemasAt, problems, otherFiles);
            }
            return;
        }
        // The component schemas are checked by name, and a 2.x message in another format has no JSON Schema
        if (!node.isObject() || at.equals(schemasAt) || !isJsonSchemaFormat(node)) {
            return;
        }
        node.properties().forEach(entry -> {
            String child = at + "/" + pointerToken(entry.getKey());
            if ((PAYLOAD.equals(entry.getKey()) || HEADERS.equals(entry.getKey())) && entry.getValue().isObject()) {
                // A payload that is itself a ref to a message has no schema, so its message is left out as such
                if (isJsonSchemaFormat(entry.getValue()) && !isMessageRef(entry.getValue())) {
                    checkSchemaRefs(root, entry.getValue(), child, problems, otherFiles);
                }
            } else {
                checkPayloads(root, entry.getValue(), child, schemasAt, problems, otherFiles);
            }
        });
    }

    private static boolean isMessageRef(JsonNode node) {
        return node.path(REF).isTextual() && MESSAGE_REF.matcher(node.path(REF).asText()).matches();
    }

    private static boolean isJsonSchemaFormat(JsonNode node) {
        JsonNode format = node.path(SCHEMA_FORMAT);
        return !format.isTextual() || SchemaFormats.isJsonSchema(format.asText());
    }

    // Reports each $ref in a schema that points at a message, which has no type to give, or into another file
    private static void checkSchemaRefs(JsonNode root, JsonNode schema, String at, List<String> problems,
                                        Map<String, List<String>> otherFiles) {
        if (schema.isArray()) {
            for (int i = 0; i < schema.size(); i++) {
                checkSchemaRefs(root, schema.get(i), at + "/" + i, problems, otherFiles);
            }
            return;
        }
        if (!schema.isObject()) {
            return;
        }
        JsonNode ref = schema.path(REF);
        String fix = ref.isTextual() && MESSAGE_REF.matcher(ref.asText()).matches()
                ? messageSchemaFix(root, ref.asText()) : null;
        if (fix != null) {
            problems.add(String.format("the schema reference at %s points to a message, '%s', not a schema; %s", at,
                    ref.asText(), fix));
        } else if (ref.isTextual() && !ref.asText().startsWith(LOCAL_REF)) {
            String file = ref.asText().contains("#") ? ref.asText().substring(0, ref.asText().indexOf('#'))
                    : ref.asText();
            otherFiles.computeIfAbsent(file, key -> new ArrayList<>()).add(at);
        }
        schema.properties().forEach(entry -> {
            if (!REF.equals(entry.getKey()) && !SCHEMA_DATA_KEYS.contains(entry.getKey())) {
                checkSchemaRefs(root, entry.getValue(), at + "/" + pointerToken(entry.getKey()), problems,
                        otherFiles);
            }
        });
    }

    private static String pointerToken(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }

    /**
     * The fix for a schema ref to a message, or {@code null} when the type it names exists anyway: an inline payload
     * is named after its message, and so is a payload schema of the same name.
     */
    private static String messageSchemaFix(JsonNode root, String messageRef) {
        JsonNode message = JsonPointers.resolve(root, messageRef);
        if (message == null) {
            return null;
        }
        String messageName = messageRef.substring(messageRef.lastIndexOf('/') + 1);
        JsonNode payload = message.path(PAYLOAD);
        JsonNode payloadRef = payload.path(REF);
        if (payloadRef.isTextual() && payloadRef.asText().startsWith(COMPONENT_SCHEMAS_REF)) {
            return payloadRef.asText().equals(COMPONENT_SCHEMAS_REF + messageName) ? null
                    : "point it at the message's payload schema '" + payloadRef.asText() + "'";
        }
        return payload.isObject() ? null : "point it at a schema instead, as the message has no JSON Schema payload";
    }

    private static void checkOperations(JsonNode root, JsonNode operations, String kind, List<String> broken) {
        operations.properties().forEach(entry -> {
            String what = kind + " '" + entry.getKey() + "'";
            JsonNode operation = entry.getValue();
            checkRef(root, operation, what, broken);
            checkRef(root, operation.path(CHANNEL), what + " channel", broken);
            checkRootChannelRef(operation.path(CHANNEL), kind, what, broken);
            operation.path(MESSAGES).forEach(message -> checkRef(root, message, what + " message", broken));
            checkMessagesOfChannel(root, operation.path(CHANNEL), operation.path(MESSAGES), what, broken);
            checkReply(root, operation.path(REPLY), operation.path(CHANNEL), what + " reply", broken);
        });
    }

    // A reply without a channel of its own uses its operation's channel
    private static void checkReply(JsonNode root, JsonNode reply, JsonNode operationChannel, String what,
                                   List<String> broken) {
        checkRef(root, reply, what, broken);
        checkRef(root, reply.path(CHANNEL), what + " channel", broken);
        reply.path(MESSAGES).forEach(message -> checkRef(root, message, what + " message", broken));
        JsonNode resolved = reply.path(REF).isTextual() ? JsonPointers.resolve(root, reply.get(REF).asText()) : reply;
        if (resolved != null) {
            JsonNode channel = resolved.has(CHANNEL) ? resolved.path(CHANNEL) : operationChannel;
            checkMessagesOfChannel(root, channel, resolved.path(MESSAGES), what, broken);
        }
    }

    // A root operation's channel is one of the root channels; a component operation's can be anywhere
    private static void checkRootChannelRef(JsonNode channelRef, String kind, String what, List<String> broken) {
        JsonNode ref = channelRef.path(REF);
        if (OPERATION_KIND.equals(kind) && ref.isTextual() && ref.asText().startsWith(LOCAL_REF)
                && !ref.asText().startsWith(ROOT_CHANNELS_REF)) {
            broken.add(String.format("%s channel reference '%s' isn't one of the channels under 'channels'", what,
                    ref.asText()));
        }
    }

    /**
     * Reports each message an operation or reply lists that isn't a message of its channel, as 3.0 requires.
     * A message given as {@code <channel>/messages/<key>} must name the operation's channel itself; any other
     * message must be one the channel lists. A channel or message in another file can't be checked.
     */
    private static void checkMessagesOfChannel(JsonNode root, JsonNode channelRef, JsonNode messages, String what,
                                               List<String> broken) {
        JsonNode ref = channelRef.path(REF);
        JsonNode channel = ref.isTextual() ? JsonPointers.resolve(root, ref.asText()) : null;
        if (channel == null || JsonPointers.isExternalRef(channel)) {
            return;
        }
        // Compared after following refs, so a component message the channel lists under any key matches
        Set<JsonNode> ofChannel = Collections.newSetFromMap(new IdentityHashMap<>());
        channel.path(MESSAGES).forEach(message -> ofChannel.add(target(root, message)));
        messages.forEach(message -> {
            JsonNode messageRef = message.path(REF);
            if (!messageRef.isTextual()) {
                return;
            }
            int messagesAt = messageRef.asText().lastIndexOf(CHANNEL_MESSAGES);
            // Only a channel's messages are checked by their channel; #/components/messages/X is a message itself
            String channelPart = messagesAt > 0 ? messageRef.asText().substring(0, messagesAt) : "";
            if (channelPart.startsWith(ROOT_CHANNELS_REF) || channelPart.startsWith(COMPONENT_CHANNELS_REF)) {
                // Its channel part, not the message it ends at, so another channel listing the same message fails
                JsonNode messageChannel = JsonPointers.resolve(root, channelPart);
                if (messageChannel != null && !JsonPointers.isExternalRef(messageChannel)
                        && messageChannel != channel) {
                    broken.add(String.format("%s message reference '%s' isn't one of the messages of its channel "
                            + "'%s'", what, messageRef.asText(), ref.asText()));
                }
                return;
            }
            JsonNode target = JsonPointers.resolve(root, messageRef.asText());
            if (target != null && !JsonPointers.isExternalRef(target) && !ofChannel.contains(target)) {
                broken.add(String.format("%s message reference '%s' isn't one of the messages of its channel '%s'",
                        what, messageRef.asText(), ref.asText()));
            }
        });
    }

    private static JsonNode target(JsonNode root, JsonNode node) {
        JsonNode ref = node.path(REF);
        JsonNode resolved = ref.isTextual() && ref.asText().startsWith(LOCAL_REF)
                ? JsonPointers.resolve(root, ref.asText()) : node;
        return resolved != null ? resolved : node;
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
        // A ref to another file isn't in this document, so it isn't checked here
        if (ref.isTextual() && ref.asText().startsWith(LOCAL_REF)
                && JsonPointers.resolve(root, ref.asText()) == null) {
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
