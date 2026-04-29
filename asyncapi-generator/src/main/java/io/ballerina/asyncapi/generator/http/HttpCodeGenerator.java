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
import io.ballerina.asyncapi.generator.http.extractor.WebhookAuthExtractor;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.generator.DispatcherGenerator;
import io.ballerina.asyncapi.generator.http.generator.ListenerGenerator;
import io.ballerina.asyncapi.generator.http.generator.ServiceTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controls the flow of Ballerina code generation from a parsed AsyncAPI specification.
 */
public class HttpCodeGenerator {

    private static final Logger LOG = LogManager.getLogger(HttpCodeGenerator.class);
    private static final String DATA_TYPES_BAL = "types.bal";
    private static final String SERVICE_TYPES_BAL = "service_types.bal";
    private static final String LISTENER_BAL = "listener.bal";
    private static final String DISPATCHER_SERVICE_BAL = "dispatcher_service.bal";

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
        Optional<WebhookAuthConfig> webhookAuthConfig = new WebhookAuthExtractor(asyncApiSpec).extract();

        // Generate Ballerina source content
        String dataTypesContent = new DataTypesGenerator(schemas).generate();
        String serviceTypesContent = new ServiceTypesGenerator(serviceTypes).generate();
        String listenerContent = new ListenerGenerator(serviceTypes, webhookAuthConfig).generate();
        String dispatcherContent = new DispatcherGenerator(serviceTypes, identifierConfig, webhookAuthConfig)
                .generate();

        // Write generated files to the output directory
        Path writtenDataTypes = writeFile(outputPath.resolve(DATA_TYPES_BAL), dataTypesContent);
        Path writtenServiceTypes = writeFile(outputPath.resolve(SERVICE_TYPES_BAL), serviceTypesContent);
        Path writtenListener = writeFile(outputPath.resolve(LISTENER_BAL), listenerContent);
        Path writtenDispatcher = writeFile(outputPath.resolve(DISPATCHER_SERVICE_BAL), dispatcherContent);
        LOG.info("Following files were created.\n-- {}\n-- {}\n-- {}\n-- {}",
                writtenDataTypes.getFileName(), writtenServiceTypes.getFileName(),
                writtenListener.getFileName(), writtenDispatcher.getFileName());
    }

    /**
     * Writes generated content to a file at the given absolute path.
     *
     * @param filePath absolute path of the file to write
     * @param content  the Ballerina source content
     * @throws GeneratorException if the file cannot be written
     */
    private Path writeFile(Path filePath, String content) throws GeneratorException {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Path targetPath = filePath;
            if (Files.exists(filePath)) {
                Console console = System.console();
                if (console != null) {
                    while (true) {
                        String answer = console.readLine(
                                "'%s' already exists. Overwrite? [y/n]: ",
                                filePath.getFileName());
                        if ("y".equalsIgnoreCase(answer)) {
                            break;
                        } else if ("n".equalsIgnoreCase(answer)) {
                            targetPath = resolveUniqueFile(filePath);
                            break;
                        }
                    }
                }
            }
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            return targetPath;
        } catch (IOException e) {
            throw new GeneratorException(String.format("Could not write to file: %s", filePath), e);
        }
    }

    /**
     * Returns a path with a numeric suffix that does not yet exist on disk.
     * For example, {@code foo.bal} becomes {@code foo.1.bal}, then {@code foo.2.bal}, etc.
     *
     * @param filePath the original file path that already exists
     * @return a non-existing sibling path with a numeric suffix inserted before the extension
     */
    private static Path resolveUniqueFile(Path filePath) {
        String name = filePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext = dot >= 0 ? name.substring(dot) : "";
        int counter = 1;
        Path candidate;
        do {
            candidate = filePath.resolveSibling(base + "." + counter + ext);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }
}
