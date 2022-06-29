package io.ballerina.asyncapi.codegenerator.usecase;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the extraction of header type event identifier from the AsyncAPI specification.
 */
public class ExtractHeaderTypeEventIdentifierFromSpecTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains the x-ballerina-event-identifier attribute in the channel " +
                    "but invalid type attribute value",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "header or body is not provided as the value of type attribute within " +
                    "the attribute x-ballerina-event-identifier in the Async API Specification"
    )
    public void testExtractWithIdentifierPathInvalidType() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-with-event-identifier-invalid-type.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractIdentifierPathFromSpec = new ExtractIdentifierPathFromSpec(asyncApiSpec);
        extractIdentifierPathFromSpec.extract();
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains the x-ballerina-event-identifier attribute in the channel " +
                    "and the value of type attribute as `header` "
    )
    public void testExtractWithIdentifierPathValidHeaderType() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-with-event-identifier-valid-header-type.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractIdentifierPathFromSpec = new ExtractIdentifierPathFromSpec(asyncApiSpec);
        String identifierPath = extractIdentifierPathFromSpec.extract();

        Assert.assertEquals(identifierPath, "event-name");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains the x-ballerina-event-identifier attribute in the channel " +
                    "and the value of type attribute as `header` " +
                    "but missing the name attribute inside it",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "name attribute is not found within the attribute " +
                    "x-ballerina-event-identifier in the Async API Specification"
    )
    public void testExtractWithIdentifierPathMissingHeaderName() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-with-event-identifier-missing-header-name.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractIdentifierPathFromSpec = new ExtractIdentifierPathFromSpec(asyncApiSpec);
        extractIdentifierPathFromSpec.extract();
    }
}
