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
package io.ballerina.asyncapi.generator.http;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.extractor.SchemaExtractor;
import io.ballerina.asyncapi.generator.http.extractor.ServiceTypeExtractor;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.generator.DispatcherGenerator;
import io.ballerina.asyncapi.generator.http.generator.ListenerGenerator;
import io.ballerina.asyncapi.generator.http.generator.ServiceTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls the flow of Ballerina code generation from a parsed AsyncAPI specification.
 */
public class HttpCodeGenerator {

    private static final Logger LOG = LogManager.getLogger(HttpCodeGenerator.class);

    private final AsyncApiSpec asyncApiSpec;

    public HttpCodeGenerator(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Runs the full generation pipeline and writes Ballerina source files to {@code outputPath}.
     * Generates the four files:
     * {@code data_types.bal}, {@code service_types.bal}, {@code listener.bal}, and
     * {@code dispatcher_service.bal}.
     *
     * @param outputPath the directory to write generated files into
     * @throws GeneratorException if extraction or generation fails
     */
    public void generate(Path outputPath) throws GeneratorException {
        if (outputPath == null) {
            outputPath = Path.of(""); // defaults to current working directory
        }

        // Extract data from the parsed spec
        ServiceTypeExtractor serviceTypeExtractor = new ServiceTypeExtractor(asyncApiSpec);
        List<HttpServiceType> serviceTypes = serviceTypeExtractor.extract();
        Map<String, AsyncApiSchema> schemas = new HashMap<>(new SchemaExtractor(asyncApiSpec).extract());
        schemas.putAll(serviceTypeExtractor.getInlineSchemas());
        EventIdentifierConfig identifierConfig = new EventIdentifierExtractor(asyncApiSpec).extract();

        // Generate Ballerina source content
        String dataTypesContent = new DataTypesGenerator(schemas).generate();
        String serviceTypesContent = new ServiceTypesGenerator(serviceTypes).generate();
        String listenerContent = new ListenerGenerator(serviceTypes).generate();
        String dispatcherContent = new DispatcherGenerator(serviceTypes, identifierConfig).generate();

        // Write generated files to the output directory
        writeFile(outputPath.resolve(Constants.DATA_TYPES_BAL), dataTypesContent);
        writeFile(outputPath.resolve(Constants.SERVICE_TYPES_BAL), serviceTypesContent);
        writeFile(outputPath.resolve(Constants.LISTENER_BAL), listenerContent);
        writeFile(outputPath.resolve(Constants.DISPATCHER_SERVICE_BAL), dispatcherContent);
        LOG.info("Following files were created.\n-- {}\n-- {}\n-- {}\n-- {}",
                Constants.DATA_TYPES_BAL, Constants.SERVICE_TYPES_BAL,
                Constants.LISTENER_BAL, Constants.DISPATCHER_SERVICE_BAL);
    }

    /**
     * Writes generated content to a file at the given absolute path.
     *
     * @param filePath absolute path of the file to write
     * @param content  the Ballerina source content
     * @throws GeneratorException if the file cannot be written
     */
    private void writeFile(Path filePath, String content) throws GeneratorException {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GeneratorException("Could not write to file: " + filePath, e);
        }
    }
}
