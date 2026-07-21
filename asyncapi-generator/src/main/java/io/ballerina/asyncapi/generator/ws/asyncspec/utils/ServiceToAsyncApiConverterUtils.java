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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Document;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.generator.ws.asyncspec.mapper.AsyncApiEndpointMapper;
import io.ballerina.asyncapi.generator.ws.asyncspec.mapper.AsyncApiServiceMapper;
import io.ballerina.asyncapi.generator.ws.asyncspec.model.AsyncApiResult;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ASYNCAPI_ANNOTATION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.ASYNC_API_VERSION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.CONTRACT;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.HYPHEN;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.SLASH;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.TITLE;
import static io.ballerina.asyncapi.generator.ws.asyncspec.Constants.VERSION;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.containErrors;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.getAsyncApiFileName;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.isWebsocketService;
import static io.ballerina.asyncapi.generator.ws.asyncspec.utils.ConverterCommonUtils.normalizeTitle;

/**
 * Entry-point utilities for converting Ballerina service syntax trees to AsyncAPI 3.0.0 spec definitions.
 * Corresponds to the legacy {@code ServiceToAsyncApiConverterUtils}.
 */
public final class ServiceToAsyncApiConverterUtils {

    private ServiceToAsyncApiConverterUtils() {
    }

    /**
     * Generates AsyncAPI spec definitions for all (or a named) service in the given syntax tree.
     *
     * @param syntaxTree    the parsed Ballerina syntax tree
     * @param semanticModel the semantic model for type resolution
     * @param serviceName   optional service name filter (null to export all)
     * @param needJson      {@code true} to produce JSON, {@code false} for YAML
     * @param inputPath     path of the source file (used to resolve relative imports)
     * @return list of conversion results, one per exported service
     */
    public static List<AsyncApiResult> generateAsyncAPISpecDefinition(SyntaxTree syntaxTree,
                                                                       SemanticModel semanticModel,
                                                                       String serviceName,
                                                                       Boolean needJson,
                                                                       Path inputPath) {
        List<ListenerDeclarationNode> endpoints = new ArrayList<>();
        Map<String, ServiceDeclarationNode> servicesToGenerate = new LinkedHashMap<>();
        List<String> availableService = new ArrayList<>();
        List<AsyncApiConverterDiagnostic> diagnostics = new ArrayList<>();
        List<AsyncApiResult> outputs = new ArrayList<>();
        List<ClassDefinitionNode> classDefinitionNodes = new ArrayList<>();
        if (containErrors(semanticModel.diagnostics())) {
            DiagnosticMessages messages = DiagnosticMessages.AAS_CONVERTOR_100;
            ExceptionDiagnostic error = new ExceptionDiagnostic(messages.getCode(),
                    messages.getDescription(), null);
            diagnostics.add(error);
        } else {
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            extractListenersAndServiceNodes(serviceName, availableService, servicesToGenerate,
                    classDefinitionNodes, modulePartNode, endpoints, semanticModel);
            if (serviceName != null && servicesToGenerate.isEmpty()) {
                DiagnosticMessages messages = DiagnosticMessages.AAS_CONVERTOR_101;
                ExceptionDiagnostic error = new ExceptionDiagnostic(messages.getCode(),
                        messages.getDescription(), null, serviceName, availableService.toString().trim());
                diagnostics.add(error);
            }
            for (Map.Entry<String, ServiceDeclarationNode> serviceNode : servicesToGenerate.entrySet()) {
                String asyncApiName = getAsyncApiFileName(syntaxTree.filePath(), serviceNode.getKey(), needJson);
                AsyncApiResult asyncAPIDefinition = generateAsyncApiSpec(serviceNode.getValue(),
                        endpoints, classDefinitionNodes, semanticModel, asyncApiName, inputPath);
                outputs.add(asyncAPIDefinition);
            }
        }
        if (!diagnostics.isEmpty()) {
            AsyncApiResult exceptions = new AsyncApiResult(null, null, diagnostics);
            outputs.add(exceptions);
        }
        return outputs;
    }

    /**
     * Filters all endpoint and service nodes from the module part node.
     *
     * @param serviceName          optional service name filter
     * @param availableService     accumulator for available service paths
     * @param servicesToGenerate   accumulator for services matching the filter
     * @param classDefinitionNodes accumulator for class definition nodes
     * @param modulePartNode       root module node
     * @param endpoints            accumulator for listener declaration nodes
     * @param semanticModel        semantic model for type resolution
     */
    private static void extractListenersAndServiceNodes(String serviceName,
                                                        List<String> availableService,
                                                        Map<String, ServiceDeclarationNode> servicesToGenerate,
                                                        List<ClassDefinitionNode> classDefinitionNodes,
                                                        ModulePartNode modulePartNode,
                                                        List<ListenerDeclarationNode> endpoints,
                                                        SemanticModel semanticModel) {
        for (Node node : modulePartNode.members()) {
            SyntaxKind syntaxKind = node.kind();
            if (syntaxKind.equals(SyntaxKind.LISTENER_DECLARATION)) {
                endpoints.add((ListenerDeclarationNode) node);
            } else if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                if (isWebsocketService(serviceNode, semanticModel)) {
                    Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
                    if (serviceSymbol.isPresent() && serviceSymbol.get() instanceof ServiceDeclarationSymbol) {
                        String service = AsyncApiEndpointMapper.ENDPOINT_MAPPER.getServiceBasePath(serviceNode);
                        String serviceClassName = resolveServiceClassName(serviceNode);
                        String updateServiceName = service;
                        if (servicesToGenerate.containsKey(service)) {
                            updateServiceName = service + HYPHEN + serviceSymbol.get().hashCode();
                        }
                        if (serviceName != null) {
                            availableService.add(serviceClassName.isEmpty()
                                    ? service : service + " (" + serviceClassName + ")");
                            if (serviceName.equals(service) || serviceName.equals(serviceClassName)) {
                                servicesToGenerate.put(updateServiceName, serviceNode);
                            }
                        } else {
                            servicesToGenerate.put(updateServiceName, serviceNode);
                        }
                    }
                }
            } else if (syntaxKind.equals(SyntaxKind.CLASS_DEFINITION)) {
                classDefinitionNodes.add((ClassDefinitionNode) node);
            }
        }
    }

    /**
     * Resolves the class-based service name for an anonymous service declaration by inspecting
     * its upgrade resource (e.g. {@code resource function get .()}) for a {@code return new
     * ChatService();} expression. Anonymous services matched only by base path (e.g. {@code /chat})
     * have no name of their own from a caller's perspective - the class they upgrade into is what
     * a {@code --service} filter is actually expected to match against.
     *
     * @param serviceNode the anonymous service declaration node
     * @return the resolved class name, or an empty string if none could be resolved
     */
    private static String resolveServiceClassName(ServiceDeclarationNode serviceNode) {
        for (Node member : serviceNode.members()) {
            if (member.kind().equals(SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)) {
                String serviceClassName =
                        ConverterCommonUtils.getServiceClassName((FunctionDefinitionNode) member);
                if (!serviceClassName.isEmpty()) {
                    return serviceClassName;
                }
            }
        }
        return "";
    }

    /**
     * Builds a single AsyncAPI 3.0.0 document for the given service node.
     *
     * <p>Phase 2 implementation fills the info section. Server mapping (Phase 3) and
     * channel/operation/component mapping (Phase 5) will be added in subsequent phases.
     *
     * @param serviceDefinition  service node
     * @param endpoints          available listener endpoints
     * @param classDefinitionNodes class definition nodes for dispatcher resolution
     * @param semanticModel      semantic model
     * @param asyncApiFileName   target file name (used as serviceName on the result)
     * @param ballerinaFilePath  source file path for annotation contract resolution
     * @return conversion result with the partially or fully populated document
     */
    private static AsyncApiResult generateAsyncApiSpec(ServiceDeclarationNode serviceDefinition,
                                                       List<ListenerDeclarationNode> endpoints,
                                                       List<ClassDefinitionNode> classDefinitionNodes,
                                                       SemanticModel semanticModel,
                                                       String asyncApiFileName,
                                                       Path ballerinaFilePath) {
        AsyncApiResult infoResult = fillAsyncApiInfoSection(serviceDefinition, semanticModel,
                asyncApiFileName, ballerinaFilePath);
        if (infoResult.getAsyncAPI().isEmpty()) {
            return new AsyncApiResult(null, null, infoResult.getDiagnostics());
        }
        if (!infoResult.getDiagnostics().isEmpty()) {
            return infoResult;
        }
        AsyncApi30Document asyncapi = infoResult.getAsyncAPI().get();
        AsyncApiEndpointMapper.ENDPOINT_MAPPER.getServers(asyncapi, endpoints, serviceDefinition);
        // Phase 5: channel/operation/component mapping
        AsyncApiServiceMapper serviceMapper = new AsyncApiServiceMapper(semanticModel, asyncapi);
        asyncapi = serviceMapper.convertServiceToAsyncApi(serviceDefinition, classDefinitionNodes, asyncapi);

        List<AsyncApiConverterDiagnostic> allDiagnostics = new ArrayList<>(infoResult.getDiagnostics());
        List<ValidationProblem> validationProblems = Library.validate(asyncapi, null);
        if (validationProblems != null && !validationProblems.isEmpty()) {
            for (ValidationProblem problem : validationProblems) {
                // AAM-001 fires because payload is stored via Jackson extensions rather than
                // setPayload(); suppress until the tool adopts the MultiFormatSchemaSchemaUnion API.
                if ("AAM-001".equals(problem.errorCode)) {
                    continue;
                }
                DiagnosticMessages messages = DiagnosticMessages.AAS_CONVERTER_107;
                ExceptionDiagnostic error = new ExceptionDiagnostic(
                        messages.getCode(), messages.getDescription(),
                        messages.getSeverity(), null, problem.message);
                allDiagnostics.add(error);
            }
        }
        return new AsyncApiResult(asyncApiFileName, asyncapi, allDiagnostics);
    }

    /**
     * Fills the {@code info} section of an AsyncAPI 3.0.0 document for the given service node.
     *
     * <p>If the service has an {@code asyncapi:ServiceInfo} annotation with a contract path, the
     * existing contract is loaded and its info section overridden. Otherwise, a new document is
     * created and the info section is populated from the package version and service name.
     *
     * @param serviceNode       service node
     * @param semanticModel     semantic model for version extraction
     * @param asyncApiFileName  file name (used as title fallback for {@code /} services)
     * @param ballerinaFilePath source file path for contract path resolution
     * @return result holding the document with info section set, or diagnostics on failure
     */
    private static AsyncApiResult fillAsyncApiInfoSection(ServiceDeclarationNode serviceNode,
                                                          SemanticModel semanticModel,
                                                          String asyncApiFileName,
                                                          Path ballerinaFilePath) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        List<AsyncApiConverterDiagnostic> diagnostics = new ArrayList<>();
        AsyncApi30Document asyncAPI = (AsyncApi30Document) Library.createDocument(ModelType.ASYNCAPI30);
        asyncAPI.setAsyncapi(ASYNC_API_VERSION);
        String currentServiceName = AsyncApiEndpointMapper.ENDPOINT_MAPPER.getServiceBasePath(serviceNode);
        String version = getContractVersion(serviceNode, semanticModel);
        if (metadata.isPresent() && !metadata.get().annotations().isEmpty()) {
            MetadataNode metadataNode = metadata.get();
            for (AnnotationNode annotation : metadataNode.annotations()) {
                if (annotation.annotReference().kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                    QualifiedNameReferenceNode ref = (QualifiedNameReferenceNode) annotation.annotReference();
                    String annotationName = ref.modulePrefix().text() + ":" + ref.identifier().text();
                    if (annotationName.equals(ASYNCAPI_ANNOTATION)) {
                        AsyncApiResult asyncApiResult = parseServiceInfoAnnotationAttachmentDetails(diagnostics,
                                annotation, ballerinaFilePath);
                        return normalizeInfoSection(asyncApiFileName, currentServiceName, version, asyncApiResult);
                    } else {
                        Info info = asyncAPI.createInfo();
                        info.setVersion(version);
                        info.setTitle(normalizeTitle(currentServiceName));
                        asyncAPI.setInfo(info);
                    }
                }
            }
        } else if (currentServiceName.equals(SLASH) || currentServiceName.isBlank()) {
            Info info = asyncAPI.createInfo();
            info.setVersion(version);
            info.setTitle(normalizeTitle(asyncApiFileName));
            asyncAPI.setInfo(info);
        } else {
            Info info = asyncAPI.createInfo();
            info.setVersion(version);
            info.setTitle(normalizeTitle(currentServiceName));
            asyncAPI.setInfo(info);
        }
        return new AsyncApiResult(null, asyncAPI, diagnostics);
    }

    /**
     * Normalises missing or blank fields in the info section of the result document.
     *
     * @param asyncApiFileName  file name used as title fallback
     * @param currentServiceName service base path
     * @param version           contract version
     * @param asyncApiResult    result whose info section should be normalised
     * @return result with a complete info section
     */
    private static AsyncApiResult normalizeInfoSection(String asyncApiFileName, String currentServiceName,
                                                       String version, AsyncApiResult asyncApiResult) {
        if (asyncApiResult.getAsyncAPI().isPresent()) {
            AsyncApi30Document asyncAPI = asyncApiResult.getAsyncAPI().get();
            if (asyncAPI.getInfo() == null) {
                String title = normalizeTitle(currentServiceName);
                if (currentServiceName.equals(SLASH)) {
                    title = normalizeTitle(asyncApiFileName);
                }
                Info info = asyncAPI.createInfo();
                info.setVersion(version);
                info.setTitle(title);
                asyncAPI.setInfo(info);
            } else {
                Info info = asyncAPI.getInfo();
                if (info.getTitle() == null) {
                    info.setTitle(normalizeTitle(currentServiceName));
                } else if (info.getTitle().equals(SLASH)) {
                    info.setTitle(normalizeTitle(asyncApiFileName));
                } else if (info.getTitle().isBlank()) {
                    info.setTitle(normalizeTitle(currentServiceName));
                }
                if (info.getVersion() == null || info.getVersion().isBlank()) {
                    info.setVersion(version);
                }
            }
            return new AsyncApiResult(null, asyncAPI, asyncApiResult.getDiagnostics());
        }
        return asyncApiResult;
    }

    /**
     * Extracts the contract version from the module containing the given service.
     *
     * @param serviceDefinition service node
     * @param semanticModel     semantic model
     * @return version string, defaulting to {@code "1.0.0"}
     */
    private static String getContractVersion(ServiceDeclarationNode serviceDefinition,
                                             SemanticModel semanticModel) {
        Optional<Symbol> symbol = semanticModel.symbol(serviceDefinition);
        String version = "1.0.0";
        if (symbol.isPresent()) {
            Optional<ModuleSymbol> module = symbol.get().getModule();
            if (module.isPresent()) {
                version = module.get().id().version();
            }
        }
        return version;
    }

    /**
     * Parses the {@code asyncapi:ServiceInfo} annotation and produces an {@link AsyncApiResult} with
     * the info section partially populated, or with the contract loaded from a file path.
     *
     * @param diagnostics       accumulator for conversion diagnostics
     * @param annotation        the annotation node to parse
     * @param ballerinaFilePath source file path for contract path resolution
     * @return result with document or diagnostics
     */
    private static AsyncApiResult parseServiceInfoAnnotationAttachmentDetails(
            List<AsyncApiConverterDiagnostic> diagnostics, AnnotationNode annotation, Path ballerinaFilePath) {
        Location location = annotation.location();
        AsyncApi30Document asyncAPI = (AsyncApi30Document) Library.createDocument(ModelType.ASYNCAPI30);
        asyncAPI.setAsyncapi(ASYNC_API_VERSION);
        Optional<MappingConstructorExpressionNode> content = annotation.annotValue();
        if (content.isPresent()) {
            SeparatedNodeList<MappingFieldNode> fields = content.get().fields();
            if (!fields.isEmpty()) {
                AsyncApiInfo asyncAPIInfo = buildAsyncApiInfoModel(fields);
                if (asyncAPIInfo.getContractPath().isPresent() && ballerinaFilePath != null) {
                    return updateExistingContractAsyncAPI(diagnostics, location, asyncAPIInfo, ballerinaFilePath);
                } else if (asyncAPIInfo.getTitle().isPresent() && asyncAPIInfo.getVersion().isPresent()) {
                    Info info = asyncAPI.createInfo();
                    info.setVersion(asyncAPIInfo.getVersion().get());
                    info.setTitle(asyncAPIInfo.getTitle().get());
                    asyncAPI.setInfo(info);
                } else if (asyncAPIInfo.getVersion().isPresent()) {
                    Info info = asyncAPI.createInfo();
                    info.setVersion(asyncAPIInfo.getVersion().get());
                    asyncAPI.setInfo(info);
                } else if (asyncAPIInfo.getTitle().isPresent()) {
                    Info info = asyncAPI.createInfo();
                    info.setTitle(asyncAPIInfo.getTitle().get());
                    asyncAPI.setInfo(info);
                }
            }
        }
        return new AsyncApiResult(null, asyncAPI, diagnostics);
    }

    /**
     * Loads an existing contract file and overrides its info section from the annotation.
     *
     * @param diagnostics       accumulator for conversion diagnostics
     * @param location          annotation source location for error reporting
     * @param asyncAPIInfo      annotation-extracted info fields
     * @param ballerinaFilePath source file path for relative contract path resolution
     * @return result with the loaded and overridden contract
     */
    private static AsyncApiResult updateExistingContractAsyncAPI(List<AsyncApiConverterDiagnostic> diagnostics,
                                                                 Location location, AsyncApiInfo asyncAPIInfo,
                                                                 Path ballerinaFilePath) {
        AsyncApiResult asyncAPIResult = resolveContractPath(diagnostics, location, asyncAPIInfo, ballerinaFilePath);
        Optional<AsyncApi30Document> contract = asyncAPIResult.getAsyncAPI();
        if (contract.isEmpty()) {
            return asyncAPIResult;
        }
        AsyncApi30Document asyncApi = contract.get();
        if (asyncAPIInfo.getVersion().isPresent() && asyncAPIInfo.getTitle().isPresent()) {
            asyncApi.getInfo().setVersion(asyncAPIInfo.getVersion().get());
            asyncApi.getInfo().setTitle(asyncAPIInfo.getTitle().get());
            diagnostics.addAll(asyncAPIResult.getDiagnostics());
            return new AsyncApiResult(null, asyncApi, asyncAPIResult.getDiagnostics());
        } else if (asyncAPIInfo.getTitle().isPresent()) {
            asyncApi.getInfo().setTitle(asyncAPIInfo.getTitle().get());
            return new AsyncApiResult(null, asyncApi, asyncAPIResult.getDiagnostics());
        } else if (asyncAPIInfo.getVersion().isPresent()) {
            asyncApi.getInfo().setVersion(asyncAPIInfo.getVersion().get());
            return new AsyncApiResult(null, asyncApi, asyncAPIResult.getDiagnostics());
        }
        return asyncAPIResult;
    }

    /**
     * Builds an {@link AsyncApiInfo} model from the annotation field nodes.
     *
     * @param fields mapping fields of the annotation value
     * @return populated info model
     */
    private static AsyncApiInfo buildAsyncApiInfoModel(SeparatedNodeList<MappingFieldNode> fields) {
        AsyncApiInfo.Builder infoBuilder = new AsyncApiInfo.Builder();
        for (MappingFieldNode field : fields) {
            String fieldName = ((SpecificFieldNode) field).fieldName().toString().trim();
            Optional<ExpressionNode> value = ((SpecificFieldNode) field).valueExpr();
            if (value.isPresent()) {
                ExpressionNode expressionNode = value.get();
                if (!expressionNode.toString().trim().isBlank()) {
                    String fieldValue = expressionNode.toString().trim().replaceAll("\"", "");
                    if (!fieldValue.isBlank()) {
                        switch (fieldName) {
                            case CONTRACT:
                                infoBuilder.contractPath(fieldValue);
                                break;
                            case TITLE:
                                infoBuilder.title(fieldValue);
                                break;
                            case VERSION:
                                infoBuilder.version(fieldValue);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return infoBuilder.build();
    }

    /**
     * Resolves and loads an AsyncAPI contract from the path declared in the annotation.
     *
     * @param diagnostics       accumulator for conversion diagnostics
     * @param location          annotation source location for error reporting
     * @param asyncAPIInfo      annotation-extracted info fields (provides contract path)
     * @param ballerinaFilePath source file path for relative contract path resolution
     * @return result with the loaded contract or diagnostics on failure
     */
    private static AsyncApiResult resolveContractPath(List<AsyncApiConverterDiagnostic> diagnostics,
                                                      Location location, AsyncApiInfo asyncAPIInfo,
                                                      Path ballerinaFilePath) {
        AsyncApi30Document asyncApi = null;
        Path asyncApiPath = Paths.get(asyncAPIInfo.getContractPath().get().replaceAll("\"", "").trim());
        Path relativePath = null;
        if (asyncApiPath.toString().trim().isBlank()) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_103;
            diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(), location));
        } else {
            Path path = Paths.get(asyncApiPath.toString());
            if (path.isAbsolute()) {
                relativePath = path;
            } else {
                File file = new File(ballerinaFilePath.toString());
                File parentFolder = new File(file.getParent());
                File asyncApiContract = new File(parentFolder, asyncApiPath.toString());
                try {
                    relativePath = Paths.get(asyncApiContract.getCanonicalPath());
                } catch (IOException e) {
                    DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_102;
                    diagnostics.add(new ExceptionDiagnostic(error.getCode(), error.getDescription(),
                            location, e.toString()));
                }
            }
        }
        if (relativePath != null && Files.exists(relativePath)) {
            AsyncApiResult result = ConverterCommonUtils.parseAsyncAPIFile(relativePath.toString());
            if (result.getAsyncAPI().isPresent()) {
                asyncApi = result.getAsyncAPI().get();
            }
            diagnostics.addAll(result.getDiagnostics());
        }
        return new AsyncApiResult(null, asyncApi, diagnostics);
    }

    /**
     * Holds the fields extracted from an {@code asyncapi:ServiceInfo} annotation.
     */
    private static final class AsyncApiInfo {

        private final String title;
        private final String version;
        private final String contractPath;

        private AsyncApiInfo(String title, String version, String contractPath) {
            this.title = title;
            this.version = version;
            this.contractPath = contractPath;
        }

        /**
         * Returns the normalised title, or empty if not present.
         *
         * @return optional title
         */
        public Optional<String> getTitle() {
            return Optional.ofNullable(normalizeTitle(this.title));
        }

        /**
         * Returns the version string, or empty if not present.
         *
         * @return optional version
         */
        public Optional<String> getVersion() {
            return Optional.ofNullable(this.version);
        }

        /**
         * Returns the contract path string, or empty if not present.
         *
         * @return optional contract path
         */
        public Optional<String> getContractPath() {
            return Optional.ofNullable(this.contractPath);
        }

        /**
         * Builder for {@link AsyncApiInfo}.
         */
        private static final class Builder {

            private String title;
            private String version;
            private String contractPath;

            /**
             * Sets the title.
             *
             * @param title title value
             * @return this builder
             */
            public Builder title(String title) {
                this.title = title;
                return this;
            }

            /**
             * Sets the version.
             *
             * @param version version value
             * @return this builder
             */
            public Builder version(String version) {
                this.version = version;
                return this;
            }

            /**
             * Sets the contract path.
             *
             * @param contractPath path to the existing contract file
             * @return this builder
             */
            public Builder contractPath(String contractPath) {
                this.contractPath = contractPath;
                return this;
            }

            /**
             * Builds and returns the {@link AsyncApiInfo}.
             *
             * @return new info instance
             */
            public AsyncApiInfo build() {
                return new AsyncApiInfo(title, version, contractPath);
            }
        }
    }
}
