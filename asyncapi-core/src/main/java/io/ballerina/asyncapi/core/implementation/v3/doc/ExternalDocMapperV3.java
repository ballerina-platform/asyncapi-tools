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
package io.ballerina.asyncapi.core.implementation.v3.doc;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio {@link AsyncApiExternalDocumentation} to {@link AsyncApiExternalDocs} for AsyncAPI 3.0.
 */
public final class ExternalDocMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ExternalDocMapperV3.class);

    private ExternalDocMapperV3() {
    }

    /**
     * Maps an Apicurio {@link AsyncApiExternalDocumentation} to an {@link AsyncApiExternalDocs}.
     * Resolves $ref references if present.
     *
     * @param externalDocs the Apicurio external documentation object
     * @param components the Apicurio components, used for $ref resolution
     * @return the mapped AsyncApiExternalDocs, or null if externalDocs is null or resolution fails
     */
    public static AsyncApiExternalDocs map(AsyncApiExternalDocumentation externalDocs, AsyncApiComponents components) {
        if (externalDocs == null) {
            return null;
        }

        String $ref = externalDocs instanceof AsyncApiReferenceable referenceable
                ? referenceable.get$ref()
                : null;

        if ($ref != null) {
            AsyncApiExternalDocumentation resolved = resolveRef($ref, components);
            if (resolved == null) {
                return null;
            }
            return map(resolved, components);
        }

        Map<String, JsonNode> extensions = externalDocs instanceof AsyncApiExtensible extensible
                ? extensible.getExtensions()
                : null;

        return new AsyncApiExternalDocs(
                externalDocs.getDescription(),
                URIUtils.toUri(externalDocs.getUrl()),
                extensions
        );

    }

    /**
     * Resolves an externalDocs {@code $ref} string, following any chain of refs, to the
     * final concrete {@link AsyncApiExternalDocumentation}. Detects cyclic references.
     *
     * @param $ref       the initial $ref string (e.g., "#/components/externalDocs/myDocs")
     * @param components the AsyncAPI components used for lookup
     * @return the concrete externalDocs object, or {@code null} if resolution fails
     */
    private static AsyncApiExternalDocumentation resolveRef(String $ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.EXTERNAL_DOCS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping externalDocs.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping externalDocs.", current);
                return null;
            }
            String name = current.substring(Constants.EXTERNAL_DOCS_REF_PREFIX.length());
            AsyncApiExternalDocumentation resolved = null;

            // AsyncApi 3.x versions (eg: 3.0) do not expose getExternalDocs() on the parent
            // AsyncApiComponents interface. We must cast to the specific version type to access it.
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            if (components instanceof AsyncApi30Components typedComponents) {
                Map<String, ? extends AsyncApiExternalDocumentation> externalDocsMap =
                        typedComponents.getExternalDocs();
                if (externalDocsMap != null) {
                    resolved = externalDocsMap.get(name);
                }
            }
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching externalDocs found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                current = resolvedTyped.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
