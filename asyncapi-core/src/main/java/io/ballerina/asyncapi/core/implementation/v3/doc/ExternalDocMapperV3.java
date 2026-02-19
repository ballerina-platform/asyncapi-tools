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

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.ExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ExternalDocumentation;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.Constants;

import java.io.PrintStream;
import java.util.Map;

/**
 * Maps Apicurio {@link ExternalDocumentation} to {@link AsyncApiExternalDocs} for AsyncAPI 3.0.
 */
public final class ExternalDocMapperV3 {

    private static final PrintStream outStream = System.err;

    private ExternalDocMapperV3() {
    }

    public static AsyncApiExternalDocs map(ExternalDocumentation externalDocs, AsyncApiComponents components) {
        if (externalDocs == null) {
            return null;
        }

        if (externalDocs instanceof AsyncApi30ExternalDocumentation typedDocs) {
            String $ref = typedDocs.get$ref();
            if ($ref != null) {
                ExternalDocumentation resolved = resolveRef($ref, components);
                if (resolved == null) {
                    outStream.println("Could not resolve $ref: " + $ref + ". Skipping externalDocs.");
                    return null;
                }
                if (resolved instanceof AsyncApi30ExternalDocumentation resolvedTyped
                        && resolvedTyped.get$ref() != null) {
                    outStream.println("Resolved $ref points to another $ref: " + resolvedTyped.get$ref() + ". Skipping externalDocs.");
                    return null;
                }
                return map(resolved, components);
            }
            return new AsyncApiExternalDocs(
                    typedDocs.getDescription(),
                    URIUtils.toUri(typedDocs.getUrl()),
                    typedDocs.getExtensions()
            );
        }

        return new AsyncApiExternalDocs(
                externalDocs.getDescription(),
                URIUtils.toUri(externalDocs.getUrl()),
                null
        );
    }

    private static ExternalDocumentation resolveRef(String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.EXTERNAL_DOCS_REF_PREFIX)) {
            outStream.println("Unsupported $ref format: " + $ref + ". Skipping externalDocs.");
            return null;
        }
        String name = $ref.substring(Constants.EXTERNAL_DOCS_REF_PREFIX.length());

        if (components == null) {
            return null;
        }

        if (components instanceof AsyncApi30Components typedComponents) {
            Map<String, AsyncApi30ExternalDocumentation> externalDocsMap = typedComponents.getExternalDocs();
            if (externalDocsMap == null) {
                return null;
            }
            return externalDocsMap.get(name);
        }

        return null;
    }
}
