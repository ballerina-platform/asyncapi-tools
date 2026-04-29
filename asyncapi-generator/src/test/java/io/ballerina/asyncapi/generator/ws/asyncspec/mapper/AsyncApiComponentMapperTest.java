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
package io.ballerina.asyncapi.generator.ws.asyncspec.mapper;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.BalAsyncApi30SchemaImpl;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unit tests for {@link AsyncApiComponentMapper} covering construction, diagnostics,
 * and the one public method that can be driven with pure Java / Apicurio inputs:
 * {@link AsyncApiComponentMapper#generateObjectSchemaFromRecordFields} with an empty
 * field map (no {@link RecordFieldSymbol} instance is ever constructed — the loop body
 * is unreachable and the entire return path runs on {@code String} and Apicurio types).
 *
 * <p>Tier-3 gap (covered by integration tests in Phase 5):
 * {@code createComponentSchema(TypeSymbol, String)},
 * {@code isCloseFrameRecordType(TypeSymbol)},
 * {@code excludeReadonlyIfPresent(TypeSymbol)}, and
 * {@code getCloseFrameSchema(TypeSymbol)} all require a live Ballerina
 * {@code TypeSymbol} from the Compiler API and cannot be exercised here.
 */
class AsyncApiComponentMapperTest {

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static AsyncApi30Document buildDoc() {
        return (AsyncApi30Document) Library.createDocument(ModelType.ASYNCAPI30);
    }

    private static AsyncApi30ComponentsImpl attachComponents(AsyncApi30Document doc) {
        AsyncApi30ComponentsImpl components = (AsyncApi30ComponentsImpl) doc.createComponents();
        doc.setComponents(components);
        return components;
    }

    // -------------------------------------------------------------------------
    // tests
    // -------------------------------------------------------------------------

    @Test
    void testConstructor_doesNotThrow() {
        AsyncApi30Document doc = buildDoc();
        AsyncApi30ComponentsImpl components = attachComponents(doc);
        AsyncApiComponentMapper mapper = new AsyncApiComponentMapper(components);
        Assert.assertNotNull(mapper,
                "AsyncApiComponentMapper constructor must not throw and must return a non-null instance");
    }

    @Test
    void testGetDiagnostics_freshInstance_returnsNonNullEmptyList() {
        AsyncApi30Document doc = buildDoc();
        AsyncApi30ComponentsImpl components = attachComponents(doc);
        AsyncApiComponentMapper mapper = new AsyncApiComponentMapper(components);
        Assert.assertNotNull(mapper.getDiagnostics(),
                "getDiagnostics() must not return null on a fresh instance");
        Assert.assertTrue(mapper.getDiagnostics().isEmpty(),
                "getDiagnostics() must be empty before any mapping has been performed");
    }

    @Test
    void testGenerateObjectSchemaFromRecordFields_emptyFields_createsObjectSchemaInComponents() {
        // generateObjectSchemaFromRecordFields is the only public method whose full return
        // path runs without requiring a live TypeSymbol — when the field map is empty the
        // loop body is never entered, so RecordFieldSymbol is used only as a type parameter.
        AsyncApi30Document doc = buildDoc();
        AsyncApi30ComponentsImpl components = attachComponents(doc);
        AsyncApiComponentMapper mapper = new AsyncApiComponentMapper(components);

        Map<String, RecordFieldSymbol> emptyFields = new LinkedHashMap<>();
        Map<String, String> emptyApiDocs = new LinkedHashMap<>();
        BalAsyncApi30SchemaImpl schema =
                mapper.generateObjectSchemaFromRecordFields("TestSchema", emptyFields, emptyApiDocs, null);

        Assert.assertNotNull(schema,
                "generateObjectSchemaFromRecordFields must return a non-null schema");
        Assert.assertEquals(schema.getType(), "object",
                "an empty record must produce type \"object\"");
        Assert.assertNotNull(components.getSchemas(),
                "components.getSchemas() must not be null after a schema has been added");
        Assert.assertTrue(components.getSchemas().containsKey("TestSchema"),
                "the generated schema must be registered under the supplied component name");
    }

    @Test
    void testDocComponents_isSameInstance_noDefensiveCopy() {
        // Confirms that the mapper mutates the exact components instance that the document
        // holds — there is no defensive copy between doc.setComponents() and doc.getComponents().
        AsyncApi30Document doc = buildDoc();
        AsyncApi30ComponentsImpl components = attachComponents(doc);
        new AsyncApiComponentMapper(components);
        Assert.assertSame(doc.getComponents(), components,
                "doc.getComponents() must be the same object reference that was passed to setComponents()");
    }
}
