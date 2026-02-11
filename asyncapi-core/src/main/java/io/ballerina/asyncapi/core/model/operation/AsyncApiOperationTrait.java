package io.ballerina.asyncapi.core.model.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Represents an Operation Trait Object that defines reusable operation properties.
 *
 * @param title        A human-friendly title for the operation.
 * @param summary      A short summary of the operation.
 * @param description  A verbose description of the operation.
 * @param security     A list of security mechanisms available for this operation.
 * @param tags         A list of tags for API documentation control.
 * @param externalDocs Additional external documentation.
 * @param bindings     Protocol-specific operation bindings.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperationTrait(
        String title,
        String summary,
        String description,
        List<AsyncApiSecurityScheme> security,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiOperationBindings bindings,
        Map<String, JsonNode> extensions
) {
}
