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
package io.ballerina.asyncapi.core;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.deref.Dereferencer;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.refs.ReferenceResolverChain;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import org.apache.commons.lang3.StringUtils;

public final class AsyncApiParser {

    private AsyncApiParser() {

    }

    public static AsyncApiSpec parseFromJsonString(String jsonString) throws AsyncApiParserException {
        if (StringUtils.isBlank(jsonString)) {
            throw new AsyncApiParserException(
                    "AsyncAPI specification JSON cannot be null or empty.");
        }
        try {
            Document rootDocument = Library.readDocumentFromJSONString(jsonString);
            AsyncApiDocument dereferencedDocument = getAsyncApiDocument(rootDocument);
            return AsyncApiSpecCreator.create(dereferencedDocument);
        } catch (Exception e) {
            throw new AsyncApiParserException(e.getMessage(), e);
        }
    }

    private static AsyncApiDocument getAsyncApiDocument(Document rootDocument) throws AsyncApiParserException {
        if (!(rootDocument instanceof AsyncApiDocument document)) {
            throw new AsyncApiParserException(
                    "The provided JSON is not a valid AsyncAPI document. Detected type: " +
                            rootDocument.getClass().getSimpleName());
        }

        Dereferencer dereferencer = new Dereferencer(ReferenceResolverChain.getInstance(), false);
        return (AsyncApiDocument) dereferencer.dereference(document);
    }
}
