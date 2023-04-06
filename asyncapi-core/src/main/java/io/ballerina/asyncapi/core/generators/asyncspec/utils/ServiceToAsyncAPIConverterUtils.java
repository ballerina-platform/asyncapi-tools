/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.core.generators.asyncspec.utils;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.ModelType;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Document;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncAPIInfo;
import io.ballerina.asyncapi.core.generators.asyncspec.model.AsyncAPIResult;
import io.ballerina.asyncapi.core.generators.asyncspec.service.AsyncAPIEndpointMapper;
//import io.ballerina.asyncapi.core.generators.asyncspec.service.AsyncAPIServiceMapper;
import io.ballerina.asyncapi.core.generators.asyncspec.service.AsyncAPIServiceMapper;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.diagnostics.Location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.*;
import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.*;

/**
 * The ServiceToAsyncAPIConverterUtils provide API for convert ballerina service into asyncAPI specification.
 *
 * @since 2.0.0
 */
public class ServiceToAsyncAPIConverterUtils {

    /**
     * This method will generate asyncapi definition Map lists with ballerina code.
     *
     * @param syntaxTree    - Syntax tree the related to ballerina service
     * @param semanticModel - Semantic model related to ballerina module
     * @param serviceName   - Service name that need to generate the asyncAPI specification
     * @param needJson      - Flag for enabling the generated file format with json or YAML
     * @param inputPath     - Input file path for resolve the annotation details
     * @return - {@link Map} with asyncAPI definitions for service nodes
     */
    public static List<AsyncAPIResult> generateAsyncAPISpecDefinition(SyntaxTree syntaxTree, SemanticModel semanticModel,
                                                              String serviceName, Boolean needJson,
                                                              Path inputPath) {
        List<ListenerDeclarationNode> endpoints = new ArrayList<>();
        Map<String, ServiceDeclarationNode> servicesToGenerate = new LinkedHashMap<>();
        List<String> availableService = new ArrayList<>();
        List<AsyncAPIConverterDiagnostic> diagnostics = new ArrayList<>();
        List<AsyncAPIResult> outputs = new ArrayList<>();
        List<ClassDefinitionNode> classDefinitionNodes =new ArrayList<>();
        if (containErrors(semanticModel.diagnostics())) {
            DiagnosticMessages messages = DiagnosticMessages.AAS_CONVERTOR_100;
            ExceptionDiagnostic error = new ExceptionDiagnostic(messages.getCode(), messages.getDescription(),
                    null);
            diagnostics.add(error);
        } else {
            ModulePartNode modulePartNode = syntaxTree.rootNode();
            extractListenersAndServiceNodes(serviceName, availableService, servicesToGenerate,classDefinitionNodes, modulePartNode,
                    endpoints, semanticModel);
            // If there are no META-INF.services found for a given service name.
            if (serviceName != null && servicesToGenerate.isEmpty()) {
                DiagnosticMessages messages = DiagnosticMessages.AAS_CONVERTOR_101;
                ExceptionDiagnostic error = new ExceptionDiagnostic(messages.getCode(), messages.getDescription(),
                        null, serviceName, availableService.toString().trim());
                diagnostics.add(error);
            }
            // Generating asyncapi specification for selected META-INF.services
            for (Map.Entry<String, ServiceDeclarationNode> serviceNode : servicesToGenerate.entrySet()) {
                String asyncApiName = getAsyncApiFileName(syntaxTree.filePath(), serviceNode.getKey(), needJson);
                AsyncAPIResult asyncAPIDefinition = generateAsyncApiSpec(serviceNode.getValue(), endpoints,classDefinitionNodes, semanticModel, asyncApiName,
                        inputPath);
                asyncAPIDefinition.setServiceName(asyncApiName);
                outputs.add(asyncAPIDefinition);
            }
        }
        if (!diagnostics.isEmpty()) {
            AsyncAPIResult exceptions = new AsyncAPIResult(null, diagnostics);
            outputs.add(exceptions);
        }
        return outputs;
    }

    /**
     * Filter all the end points and service nodes.
     */
    private static void extractListenersAndServiceNodes(String serviceName, List<String> availableService,
                                                        Map<String, ServiceDeclarationNode> servicesToGenerate, List<ClassDefinitionNode>classDefinitionNodes,
                                                        ModulePartNode modulePartNode,
                                                        List<ListenerDeclarationNode> endpoints,
                                                        SemanticModel semanticModel) {
        for (Node node : modulePartNode.members()) {
            SyntaxKind syntaxKind = node.kind();
            // Load a listen_declaration for the server part in the yaml spec
            if (syntaxKind.equals(SyntaxKind.LISTENER_DECLARATION)) {
                ListenerDeclarationNode listener = (ListenerDeclarationNode) node;
                endpoints.add(listener);
            }
            else if (syntaxKind.equals(SyntaxKind.SERVICE_DECLARATION)) {
                ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) node;
                if (isWebsocketService(serviceNode, semanticModel)) {
                    // Here check the service is related to the http
                    // module by checking listener type that attached to service endpoints.
                    Optional<Symbol> serviceSymbol = semanticModel.symbol(serviceNode);
                    if (serviceSymbol.isPresent() && serviceSymbol.get() instanceof ServiceDeclarationSymbol) {
                        String service = AsyncAPIEndpointMapper.ENDPOINT_MAPPER.getServiceBasePath(serviceNode);
                        String updateServiceName = service;
                        //`String updateServiceName` used to track the service
                        // name for service file contains multiple service node.
                        //example:
                        //<pre>
                        //    listener http:Listener ep1 = new (443, config = {host: "pets-tore.swagger.io"});
                        //    service /hello on ep1 {
                        //        resource function post hi(@http:Payload json payload) {
                        //       }
                        //    }
                        //    service /hello on new http:Listener(9090) {
                        //        resource function get hi() {
                        //        }
                        //    }
                        //</pre>
                        // Using absolute path we generate file name, therefore having same name may overwrite
                        // the file, due to this suppose to use hashcode as identity factor for the file name.
                        // Generated file name for above example -> hello_asyncapi.yaml, hello_45673_asyncapi
                        //.yaml
                        if (servicesToGenerate.containsKey(service)) {
                            updateServiceName = service + HYPHEN + serviceSymbol.get().hashCode();
                        }
                        if (serviceName != null) {
                            // Filtering by service name
                            availableService.add(service);
                            if (serviceName.equals(service)) {
                                servicesToGenerate.put(updateServiceName, serviceNode);
                            }
                        } else {
                            // To generate for all META-INF.services
                            servicesToGenerate.put(updateServiceName, serviceNode);
                        }
                    }
                }
            }
            else if (syntaxKind.equals(SyntaxKind.CLASS_DEFINITION)){
                ClassDefinitionNode serviceNode = (ClassDefinitionNode) node;
                classDefinitionNodes.add(serviceNode);


            }
        }
    }


    /**
     * Provides an instance of {@code AsyncAPIResult}, which contains the generated contract as well as
     * all the diagnostics information.
     *
     * @param serviceDefinition     Service Node related to ballerina service
     * @param endpoints             Listener endpoints that bind to service
     * @param semanticModel         Semantic model for given ballerina file
     * @param asyncApiFileName      AsyncAPI file name
     * @param ballerinaFilePath     Input ballerina file Path
     * @return {@code AsyncAPIResult}
     */
    public static AsyncAPIResult generateAsyncApiSpec(ServiceDeclarationNode serviceDefinition,
                                        List<ListenerDeclarationNode> endpoints,List<ClassDefinitionNode> classDefinitionNodes, SemanticModel semanticModel,
                                        String asyncApiFileName, Path ballerinaFilePath) {
        // 01.Fill the asyncAPI info section
        AsyncAPIResult asyncApiResult = fillAsyncAPIInfoSection(serviceDefinition, semanticModel, asyncApiFileName,
                ballerinaFilePath);
        if (asyncApiResult.getAsyncAPI().isPresent() && asyncApiResult.getDiagnostics().isEmpty()) {
            AsyncApi25DocumentImpl asyncapi = (AsyncApi25DocumentImpl) asyncApiResult.getAsyncAPI().get();
            if (asyncapi.getChannels() == null) {
                // Take base path of service
                AsyncAPIServiceMapper asyncAPIServiceMapper = new AsyncAPIServiceMapper(semanticModel);
                // 02. Filter and set the ServerURLs according to endpoints. Complete the server section in AsyncAPISpec
                asyncapi = AsyncAPIEndpointMapper.ENDPOINT_MAPPER.getServers(asyncapi, endpoints, serviceDefinition);
                // 03. Filter path and component sections in AsyncAPISpec.
//                 Generate asyncApi string for the mentioned service name.
                asyncapi = asyncAPIServiceMapper.convertServiceToAsyncAPI(serviceDefinition,classDefinitionNodes, asyncapi);
                List<ValidationProblem> modelProblems=Library.validate(asyncapi,null);
                if (!(modelProblems.isEmpty())){
                    List<AsyncAPIConverterDiagnostic>diagnostics=asyncAPIServiceMapper.getErrors();
                    DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTER_107;
                    ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(), error.getDescription(), null);
                    diagnostics.add(diagnostic);
                    return new AsyncAPIResult(null, diagnostics);

                }else{
                    return new AsyncAPIResult(asyncapi, asyncAPIServiceMapper.getErrors());
                }
            } else {
                return new AsyncAPIResult(asyncapi, asyncApiResult.getDiagnostics());
            }
        } else {
            return asyncApiResult;
        }
    }

    /**
     * This function is for completing the AsyncAPI info section with package details and annotation details.
     *
     * First check the given service node has metadata with annotation details with `asyncapi:serviceInfo`,
     * if it is there, then {@link #parseServiceInfoAnnotationAttachmentDetails(List, AnnotationNode, Path)}
     * function extracts the annotation details and store details in {@code AsyncAPIInfo} model using
     * {@link #updateAsyncAPIInfoModel(SeparatedNodeList)} function. If the annotation contains the valid contract
     * path then we complete given AsyncAPI specification using annotation details. if not we create new AsyncAPI
     * specification and fill asyncAPI info sections.
     * If the annotation is not in the given service, then we filled the AsyncAPI specification info section using
     * package details and title with service base path.
     * After completing these two process we normalized the AsyncAPI specification by checking all the info
     * details are completed, if in case not completed, we complete empty fields with default values.
     *
     * @param serviceNode   Service node for relevant service.
     * @param semanticModel Semantic model for relevant project.
     * @param asyncApiFileName AsyncAPI generated file name.
     * @param ballerinaFilePath Ballerina file path.
     * @return {@code AsyncAPIResult}
     */
    private static AsyncAPIResult fillAsyncAPIInfoSection(ServiceDeclarationNode serviceNode, SemanticModel semanticModel,
                                                          String asyncApiFileName, Path ballerinaFilePath) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        List<AsyncAPIConverterDiagnostic> diagnostics = new ArrayList<>();
        AsyncApi25Document asyncAPI=(AsyncApi25Document) Library.createDocument(ModelType.ASYNCAPI25);
        asyncAPI.setAsyncapi(ASYNC_API_VERSION);
        String currentServiceName = AsyncAPIEndpointMapper.ENDPOINT_MAPPER.getServiceBasePath(serviceNode);
        // 01. Set asyncAPI inFo section with package details
        String version = getContractVersion(serviceNode, semanticModel);
        if (metadata.isPresent() && !metadata.get().annotations().isEmpty()) {
            MetadataNode metadataNode = metadata.get();
            NodeList<AnnotationNode> annotations = metadataNode.annotations();
            for (AnnotationNode annotation : annotations) {
                if (annotation.annotReference().kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                    QualifiedNameReferenceNode ref = (QualifiedNameReferenceNode) annotation.annotReference();
                    String annotationName = ref.modulePrefix().text() + ":" + ref.identifier().text();
                    //TODO : This asyncApi annotation part not yet implemented
                    //FIXME: Create annotation and push it to ballerina central
                    if (annotationName.equals(ASYNCAPI_ANNOTATION)) {
                        AsyncAPIResult asyncApiResult = parseServiceInfoAnnotationAttachmentDetails(diagnostics, annotation,
                                ballerinaFilePath);
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

        return new AsyncAPIResult(asyncAPI, diagnostics);
    }

    // Finalize the asyncAPI info section
    private static AsyncAPIResult normalizeInfoSection(String asyncApiFileName, String currentServiceName, String version,
                                          AsyncAPIResult asyncApiResult) {
        if (asyncApiResult.getAsyncAPI().isPresent()) {
            AsyncApi25Document asyncAPI = asyncApiResult.getAsyncAPI().get();
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
                if (asyncAPI.getInfo().getTitle() == null) {
                    asyncAPI.getInfo().setTitle(normalizeTitle(currentServiceName));
                } else if (asyncAPI.getInfo().getTitle() != null && asyncAPI.getInfo().getTitle().equals(SLASH)) {
                    asyncAPI.getInfo().setTitle(normalizeTitle(asyncApiFileName));
                } else if (asyncAPI.getInfo().getTitle().isBlank()) {
                    asyncAPI.getInfo().setTitle(normalizeTitle(currentServiceName));
                } else if (asyncAPI.getInfo().getTitle() == null && currentServiceName.equals(SLASH)) {
                    asyncAPI.getInfo().setTitle(normalizeTitle(asyncApiFileName));
                }
                if (asyncAPI.getInfo().getVersion() == null || asyncAPI.getInfo().getVersion().isBlank()) {
                    asyncAPI.getInfo().setVersion(version);
                }
            }
            return new AsyncAPIResult(asyncAPI, asyncApiResult.getDiagnostics());
        } else {
            return asyncApiResult;
        }
    }

    // Set contract version by default using package version.
    private static String getContractVersion(ServiceDeclarationNode serviceDefinition, SemanticModel semanticModel) {
        Optional<Symbol> symbol = semanticModel.symbol(serviceDefinition);
        String version = "1.0.0";
        if (symbol.isPresent()) {
            Symbol serviceSymbol = symbol.get();
            Optional<ModuleSymbol> module = serviceSymbol.getModule();
            if (module.isPresent()) {
                version = module.get().id().version();
            }
        }
        return version;
    }

    private static String normalizeTitle(String title) {
        if (title != null) {
            String[] splits = (title.replaceFirst(SLASH, "")).split(SPECIAL_CHAR_REGEX);
            StringBuilder stringBuilder = new StringBuilder();
            if (splits.length > 1) {
                for (String piece : splits) {
                    if (piece.isBlank()) {
                        continue;
                    }
                    stringBuilder.append(piece.substring(0, 1).toUpperCase(Locale.ENGLISH)).append(piece.substring(1));
                    stringBuilder.append(" ");
                }
                title = stringBuilder.toString().trim();
            } else if (splits.length == 1 && !splits[0].isBlank()) {
                stringBuilder.append(splits[0].substring(0, 1).toUpperCase(Locale.ENGLISH))
                        .append(splits[0].substring(1));
                title = stringBuilder.toString().trim();
            }
            return title;
        }
        return null;
    }

    // Set annotation details  for info section.
    private static AsyncAPIResult parseServiceInfoAnnotationAttachmentDetails(List<AsyncAPIConverterDiagnostic> diagnostics,
                                                                         AnnotationNode annotation,
                                                                         Path ballerinaFilePath) {
        Location location = annotation.location();
        AsyncApi25Document asyncAPI=(AsyncApi25Document) Library.createDocument(ModelType.ASYNCAPI25);
        asyncAPI.setAsyncapi(ASYNC_API_VERSION);
        Optional<MappingConstructorExpressionNode> content = annotation.annotValue();
        // If contract path there
        if (content.isPresent()) {
           SeparatedNodeList<MappingFieldNode> fields = content.get().fields();
           if (!fields.isEmpty()) {
               AsyncAPIInfo asyncAPIInfo = updateAsyncAPIInfoModel(fields);
               // If in case ballerina file path is getting null, then asyncAPI specification will be generated for
               // given META-INF.services.
               if (asyncAPIInfo.getContractPath().isPresent() && ballerinaFilePath != null) {
                   return updateExistingContractAsyncAPI(diagnostics, location, asyncAPIInfo, ballerinaFilePath);
               } else if (asyncAPIInfo.getTitle().isPresent() && asyncAPIInfo.getVersion().isPresent()) {
                   Info info = asyncAPI.createInfo();
                   info.setVersion(asyncAPIInfo.getVersion().get());
                   info.setTitle(normalizeTitle
                           (asyncAPIInfo.getTitle().get()));
                   asyncAPI.setInfo(info);
               } else if (asyncAPIInfo.getVersion().isPresent()) {
                   Info info = asyncAPI.createInfo();
                   info.setVersion(asyncAPIInfo.getVersion().get());
                   asyncAPI.setInfo(info);
               } else if (asyncAPIInfo.getTitle().isPresent()) {
                   Info info = asyncAPI.createInfo();
                   info.setTitle(normalizeTitle(
                           asyncAPIInfo.getTitle().get()));
                   asyncAPI.setInfo(info);
               }
           }
        }
        return new AsyncAPIResult(asyncAPI, diagnostics);
    }

    private static AsyncAPIResult updateExistingContractAsyncAPI(List<AsyncAPIConverterDiagnostic> diagnostics,
                                                                 Location location, AsyncAPIInfo asyncAPIInfo,
                                                                 Path ballerinaFilePath) {

        AsyncAPIResult asyncAPIResult = resolveContractPath(diagnostics, location, asyncAPIInfo, ballerinaFilePath);
        Optional<AsyncApi25Document> contract = asyncAPIResult.getAsyncAPI();
        if (contract.isEmpty()) {
            return asyncAPIResult;
        }
        AsyncApi25Document asyncApi = contract.get();
        if (asyncAPIInfo.getVersion().isPresent() && asyncAPIInfo.getTitle().isPresent()) {
            // read the asyncapi
            asyncApi.getInfo().setVersion(asyncAPIInfo.getVersion().get());
            asyncApi.getInfo().setTitle(asyncAPIInfo.getTitle().get());
            diagnostics.addAll(asyncAPIResult.getDiagnostics());
            return new AsyncAPIResult(asyncApi, asyncAPIResult.getDiagnostics());
        } else if (asyncAPIInfo.getTitle().isPresent()) {
            asyncApi.getInfo().setTitle(asyncAPIInfo.getTitle().get());
            return new AsyncAPIResult(asyncApi, asyncAPIResult.getDiagnostics());
        } else if (asyncAPIInfo.getVersion().isPresent()) {
            asyncApi.getInfo().setVersion(asyncAPIInfo.getVersion().get());
            return new AsyncAPIResult(asyncApi, asyncAPIResult.getDiagnostics());
        } else {
            return asyncAPIResult;
        }
    }

    private static AsyncAPIInfo updateAsyncAPIInfoModel(SeparatedNodeList<MappingFieldNode> fields) {
        AsyncAPIInfo.AsyncAPIInfoBuilder infoBuilder = new AsyncAPIInfo.AsyncAPIInfoBuilder();
        for (MappingFieldNode field: fields) {
            String fieldName = ((SpecificFieldNode) field).fieldName().toString().trim();
            Optional<ExpressionNode> value = ((SpecificFieldNode) field).valueExpr();
            String fieldValue;
            if (value.isPresent()) {
                ExpressionNode expressionNode = value.get();
                if (!expressionNode.toString().trim().isBlank()) {
                    fieldValue = expressionNode.toString().trim().replaceAll("\"", "");
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
    private static AsyncAPIResult resolveContractPath(List<AsyncAPIConverterDiagnostic> diagnostics, Location location,
                                     AsyncAPIInfo asyncAPIInfo, Path ballerinaFilePath) {
        AsyncAPIResult asyncApiResult;
        AsyncApi25Document asyncApi = null;
        Path asyncApiPath = Paths.get(asyncAPIInfo.getContractPath().get().replaceAll("\"", "").trim());
        Path relativePath = null;
        if (asyncApiPath.toString().trim().isBlank()) {
            DiagnosticMessages error = DiagnosticMessages.AAS_CONVERTOR_103;
            ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode(),
                    error.getDescription(), location);
            diagnostics.add(diagnostic);
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
                    ExceptionDiagnostic diagnostic = new ExceptionDiagnostic(error.getCode()
                            , error.getDescription(), location, e.toString());
                    diagnostics.add(diagnostic);
                }
            }
        }
        if (relativePath != null && Files.exists(relativePath)) {
            asyncApiResult = ConverterCommonUtils.parseAsyncAPIFile(relativePath.toString());
            if (asyncApiResult.getAsyncAPI().isPresent()) {
                asyncApi= asyncApiResult.getAsyncAPI().get();
            }
            diagnostics.addAll(asyncApiResult.getDiagnostics());
        }
        return new AsyncAPIResult(asyncApi, diagnostics);
    }
}
