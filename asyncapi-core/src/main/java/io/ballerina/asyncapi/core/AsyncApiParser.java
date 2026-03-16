/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * ... (License header remains unchanged) ...
 */
package io.ballerina.asyncapi.core;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.deref.Dereferencer;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.refs.ReferenceResolverChain;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class AsyncApiParser {

    private AsyncApiParser() {

    }

    public static AsyncApiSpec parseFromJsonString(String jsonString) throws AsyncApiParserException {
        if (StringUtils.isBlank(jsonString)) {
            throw new AsyncApiParserException("AsyncAPI specification JSON cannot be null or empty.");
        }

        Document rootDocument;
        try {
            rootDocument = Library.readDocumentFromJSONString(jsonString);
        } catch (Exception e) {
            throw new AsyncApiParserException(e.getMessage(), e);
        }

        validateDocument(rootDocument);
        AsyncApiDocument dereferencedDocument = getAsyncApiDocument(rootDocument);
        try {
            return AsyncApiSpecCreator.create(dereferencedDocument);
        } catch (Exception e) {
            throw new AsyncApiParserException("Failed to build AsyncAPI spec model: " + e.getMessage(), e);
        }
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
