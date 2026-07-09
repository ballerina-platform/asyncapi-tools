import ballerina/crypto;
import ballerina/http;
import ballerina/log;
import ballerinax/asyncapi.native.handler;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();
    private string webhookSecret;

    function init(string webhookSecret) {
        self.webhookSecret = webhookSecret;
    }

    isolated function addServiceRef(string serviceType, GenericServiceType genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
            return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericService;
    }

    isolated function removeServiceRef(string serviceType) returns error? {
        if (!self.services.hasKey(serviceType)) {
            return error("Cannot detach the service of type " + serviceType + ". Service has not been attached to the listener before");
        }
        _ = self.services.remove(serviceType);
    }

    resource function post .(http:Caller caller, http:Request request) returns error? {
        error? verifyResult = self.verifyWebhookSignature(request, self.webhookSecret);
        if verifyResult is error {
            http:Response r = new;
            r.statusCode = http:STATUS_UNAUTHORIZED;
            check caller->respond(r);
            return;
        }
        log:printInfo("DISPATCHER_ENTERED");
        json payload = check request.getJsonPayload();
        string eventType = (check payload.subscriptionType).toString();
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        check self.matchRemoteFunc(genericDataType, eventType);
        check caller->respond(http:STATUS_OK);
    }

    private function verifyWebhookSignature(http:Request request, string webhookSecret) returns error? {
        if !request.hasHeader("X-HubSpot-Signature-v3") {
            return error("Unauthorized: Missing Signature Header");
        }
        string receivedHeader = let var headerValue = trap request.getHeader("X-HubSpot-Signature-v3") in (headerValue is string ? headerValue : "");
        map<string> extractedHeaderValues = {};
        int headerCursor = 0;
        extractedHeaderValues["signature"] = receivedHeader.substring(headerCursor);
        headerCursor = receivedHeader.length();
        if !extractedHeaderValues.hasKey("signature") {
            return error("Unauthorized: Missing Header Component: signature");
        }
        string signature = extractedHeaderValues["signature"] ?: "";
        if !extractedHeaderValues.hasKey("signature") {
            return error("Unauthorized: Missing Signature Value");
        }
        string extractedSignature = extractedHeaderValues["signature"] ?: "";
        string payloadToHash = string `${request.method}${request.rawPath}${check request.getTextPayload()}${let var headerValue = trap request.getHeader("X-HubSpot-Request-Timestamp") in (headerValue is string ? headerValue : "")}`;
        byte[] computedHmac = check crypto:hmacSha256(payloadToHash.toBytes(), webhookSecret.toBytes());
        string computedSignature = computedHmac.toBase64();
        string expectedHeader = string `${computedSignature}`;
        if !crypto:equalConstantTime(receivedHeader.toBytes(), expectedHeader.toBytes()) {
            return error("Unauthorized: Signature Mismatch");
        }
        log:printInfo("SIGNATURE_VERIFIED");
        return;
    }

    private function matchRemoteFunc(GenericDataType genericDataType, string eventType) returns error? {
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForTicket(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForCompany(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForLineItem(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForProduct(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForConversation(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForDeal(genericDataType);
        log:printDebug("MATCH_LEVEL_1_hubspot_test", eventType = eventType);
        check self.matchRemoteFuncForContact(genericDataType);
    }

    private function matchRemoteFuncForTicket(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "ticket.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.propertyChange");
                check self.executeRemoteFunc(genericDataType, "ticket.propertyChange", "TicketService", "onTicketPropertyChange");
            }
            "ticket.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.deletion");
                check self.executeRemoteFunc(genericDataType, "ticket.deletion", "TicketService", "onTicketDeletion");
            }
            "ticket.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.creation");
                check self.executeRemoteFunc(genericDataType, "ticket.creation", "TicketService", "onTicketCreation");
            }
            "ticket.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.merge");
                check self.executeRemoteFunc(genericDataType, "ticket.merge", "TicketService", "onTicketMerge");
            }
            "ticket.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.restore");
                check self.executeRemoteFunc(genericDataType, "ticket.restore", "TicketService", "onTicketRestore");
            }
            "ticket.associationChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "ticket.associationChange");
                check self.executeRemoteFunc(genericDataType, "ticket.associationChange", "TicketService", "onTicketAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForCompany(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "company.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.deletion");
                check self.executeRemoteFunc(genericDataType, "company.deletion", "CompanyService", "onCompanyDeletion");
            }
            "company.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.restore");
                check self.executeRemoteFunc(genericDataType, "company.restore", "CompanyService", "onCompanyRestore");
            }
            "company.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.merge");
                check self.executeRemoteFunc(genericDataType, "company.merge", "CompanyService", "onCompanyMerge");
            }
            "company.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.propertyChange");
                check self.executeRemoteFunc(genericDataType, "company.propertyChange", "CompanyService", "onCompanyPropertyChange");
            }
            "company.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.creation");
                check self.executeRemoteFunc(genericDataType, "company.creation", "CompanyService", "onCompanyCreation");
            }
            "company.associationChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "company.associationChange");
                check self.executeRemoteFunc(genericDataType, "company.associationChange", "CompanyService", "onCompanyAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForLineItem(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "line_item.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.merge");
                check self.executeRemoteFunc(genericDataType, "line_item.merge", "LineItemService", "onLineItemMerge");
            }
            "line_item.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.deletion");
                check self.executeRemoteFunc(genericDataType, "line_item.deletion", "LineItemService", "onLineItemDeletion");
            }
            "line_item.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.propertyChange");
                check self.executeRemoteFunc(genericDataType, "line_item.propertyChange", "LineItemService", "onLineItemPropertyChange");
            }
            "line_item.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.restore");
                check self.executeRemoteFunc(genericDataType, "line_item.restore", "LineItemService", "onLineItemRestore");
            }
            "line_item.associationChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.associationChange");
                check self.executeRemoteFunc(genericDataType, "line_item.associationChange", "LineItemService", "onLineItemAssociationChange");
            }
            "line_item.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "line_item.creation");
                check self.executeRemoteFunc(genericDataType, "line_item.creation", "LineItemService", "onLineItemCreation");
            }
        }
    }

    private function matchRemoteFuncForProduct(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "product.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "product.propertyChange");
                check self.executeRemoteFunc(genericDataType, "product.propertyChange", "ProductService", "onProductPropertyChange");
            }
            "product.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "product.deletion");
                check self.executeRemoteFunc(genericDataType, "product.deletion", "ProductService", "onProductDeletion");
            }
            "product.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "product.merge");
                check self.executeRemoteFunc(genericDataType, "product.merge", "ProductService", "onProductMerge");
            }
            "product.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "product.restore");
                check self.executeRemoteFunc(genericDataType, "product.restore", "ProductService", "onProductRestore");
            }
            "product.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "product.creation");
                check self.executeRemoteFunc(genericDataType, "product.creation", "ProductService", "onProductCreation");
            }
        }
    }

    private function matchRemoteFuncForConversation(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "conversation.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "conversation.creation");
                check self.executeRemoteFunc(genericDataType, "conversation.creation", "ConversationService", "onConversationCreation");
            }
            "conversation.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "conversation.propertyChange");
                check self.executeRemoteFunc(genericDataType, "conversation.propertyChange", "ConversationService", "onConversationPropertyChange");
            }
            "conversation.privacyDeletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "conversation.privacyDeletion");
                check self.executeRemoteFunc(genericDataType, "conversation.privacyDeletion", "ConversationService", "onConversationPrivacyDeletion");
            }
            "conversation.newMessage" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "conversation.newMessage");
                check self.executeRemoteFunc(genericDataType, "conversation.newMessage", "ConversationService", "onConversationNewMessage");
            }
            "conversation.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "conversation.deletion");
                check self.executeRemoteFunc(genericDataType, "conversation.deletion", "ConversationService", "onConversationDeletion");
            }
        }
    }

    private function matchRemoteFuncForDeal(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "deal.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.deletion");
                check self.executeRemoteFunc(genericDataType, "deal.deletion", "DealService", "onDealDeletion");
            }
            "deal.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.creation");
                check self.executeRemoteFunc(genericDataType, "deal.creation", "DealService", "onDealCreation");
            }
            "deal.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.merge");
                check self.executeRemoteFunc(genericDataType, "deal.merge", "DealService", "onDealMerge");
            }
            "deal.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.propertyChange");
                check self.executeRemoteFunc(genericDataType, "deal.propertyChange", "DealService", "onDealPropertyChange");
            }
            "deal.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.restore");
                check self.executeRemoteFunc(genericDataType, "deal.restore", "DealService", "onDealRestore");
            }
            "deal.associationChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "deal.associationChange");
                check self.executeRemoteFunc(genericDataType, "deal.associationChange", "DealService", "onDealAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForContact(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "contact.creation" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.creation");
                check self.executeRemoteFunc(genericDataType, "contact.creation", "ContactService", "onContactCreation");
            }
            "contact.associationChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.associationChange");
                check self.executeRemoteFunc(genericDataType, "contact.associationChange", "ContactService", "onContactAssociationChange");
            }
            "contact.deletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.deletion");
                check self.executeRemoteFunc(genericDataType, "contact.deletion", "ContactService", "onContactDeletion");
            }
            "contact.privacyDeletion" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.privacyDeletion");
                check self.executeRemoteFunc(genericDataType, "contact.privacyDeletion", "ContactService", "onContactPrivacyDeletion");
            }
            "contact.propertyChange" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.propertyChange");
                check self.executeRemoteFunc(genericDataType, "contact.propertyChange", "ContactService", "onContactPropertyChange");
            }
            "contact.merge" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.merge");
                check self.executeRemoteFunc(genericDataType, "contact.merge", "ContactService", "onContactMerge");
            }
            "contact.restore" => {
                log:printInfo("MATCH_LEVEL_2_hubspot_test", matchedEvent = "contact.restore");
                check self.executeRemoteFunc(genericDataType, "contact.restore", "ContactService", "onContactRestore");
            }
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            log:printInfo("HANDLER_EXECUTED_hubspot_test", eventName = eventName);
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }
}
