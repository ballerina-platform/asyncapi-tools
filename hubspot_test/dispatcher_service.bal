import ballerina/http;
import ballerinax/asyncapi.native.handler;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();

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
        json payload = check request.getJsonPayload();
        string eventType = (check payload.subscriptionType).toString();
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        check self.matchRemoteFunc(genericDataType, eventType);
        check caller->respond(http:STATUS_OK);
    }

    private function matchRemoteFunc(GenericDataType genericDataType, string eventType) returns error? {
        match eventType {
            "ticket" => {
                check self.matchRemoteFuncForTicket(genericDataType);
            }
            "company" => {
                check self.matchRemoteFuncForCompany(genericDataType);
            }
            "line_item" => {
                check self.matchRemoteFuncForLineItem(genericDataType);
            }
            "product" => {
                check self.matchRemoteFuncForProduct(genericDataType);
            }
            "conversation" => {
                check self.matchRemoteFuncForConversation(genericDataType);
            }
            "deal" => {
                check self.matchRemoteFuncForDeal(genericDataType);
            }
            "contact" => {
                check self.matchRemoteFuncForContact(genericDataType);
            }
        }
    }

    private function matchRemoteFuncForTicket(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "ticket.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "ticket.propertyChange", "TicketService", "onTicketPropertyChange");
            }
            "ticket.deletion" => {
                check self.executeRemoteFunc(genericDataType, "ticket.deletion", "TicketService", "onTicketDeletion");
            }
            "ticket.creation" => {
                check self.executeRemoteFunc(genericDataType, "ticket.creation", "TicketService", "onTicketCreation");
            }
            "ticket.merge" => {
                check self.executeRemoteFunc(genericDataType, "ticket.merge", "TicketService", "onTicketMerge");
            }
            "ticket.restore" => {
                check self.executeRemoteFunc(genericDataType, "ticket.restore", "TicketService", "onTicketRestore");
            }
            "ticket.associationChange" => {
                check self.executeRemoteFunc(genericDataType, "ticket.associationChange", "TicketService", "onTicketAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForCompany(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "company.deletion" => {
                check self.executeRemoteFunc(genericDataType, "company.deletion", "CompanyService", "onCompanyDeletion");
            }
            "company.restore" => {
                check self.executeRemoteFunc(genericDataType, "company.restore", "CompanyService", "onCompanyRestore");
            }
            "company.merge" => {
                check self.executeRemoteFunc(genericDataType, "company.merge", "CompanyService", "onCompanyMerge");
            }
            "company.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "company.propertyChange", "CompanyService", "onCompanyPropertyChange");
            }
            "company.creation" => {
                check self.executeRemoteFunc(genericDataType, "company.creation", "CompanyService", "onCompanyCreation");
            }
            "company.associationChange" => {
                check self.executeRemoteFunc(genericDataType, "company.associationChange", "CompanyService", "onCompanyAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForLineItem(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "line_item.merge" => {
                check self.executeRemoteFunc(genericDataType, "line_item.merge", "LineItemService", "onLineItemMerge");
            }
            "line_item.deletion" => {
                check self.executeRemoteFunc(genericDataType, "line_item.deletion", "LineItemService", "onLineItemDeletion");
            }
            "line_item.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "line_item.propertyChange", "LineItemService", "onLineItemPropertyChange");
            }
            "line_item.restore" => {
                check self.executeRemoteFunc(genericDataType, "line_item.restore", "LineItemService", "onLineItemRestore");
            }
            "line_item.associationChange" => {
                check self.executeRemoteFunc(genericDataType, "line_item.associationChange", "LineItemService", "onLineItemAssociationChange");
            }
            "line_item.creation" => {
                check self.executeRemoteFunc(genericDataType, "line_item.creation", "LineItemService", "onLineItemCreation");
            }
        }
    }

    private function matchRemoteFuncForProduct(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "product.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "product.propertyChange", "ProductService", "onProductPropertyChange");
            }
            "product.deletion" => {
                check self.executeRemoteFunc(genericDataType, "product.deletion", "ProductService", "onProductDeletion");
            }
            "product.merge" => {
                check self.executeRemoteFunc(genericDataType, "product.merge", "ProductService", "onProductMerge");
            }
            "product.restore" => {
                check self.executeRemoteFunc(genericDataType, "product.restore", "ProductService", "onProductRestore");
            }
            "product.creation" => {
                check self.executeRemoteFunc(genericDataType, "product.creation", "ProductService", "onProductCreation");
            }
        }
    }

    private function matchRemoteFuncForConversation(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "conversation.creation" => {
                check self.executeRemoteFunc(genericDataType, "conversation.creation", "ConversationService", "onConversationCreation");
            }
            "conversation.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "conversation.propertyChange", "ConversationService", "onConversationPropertyChange");
            }
            "conversation.privacyDeletion" => {
                check self.executeRemoteFunc(genericDataType, "conversation.privacyDeletion", "ConversationService", "onConversationPrivacyDeletion");
            }
            "conversation.newMessage" => {
                check self.executeRemoteFunc(genericDataType, "conversation.newMessage", "ConversationService", "onConversationNewMessage");
            }
            "conversation.deletion" => {
                check self.executeRemoteFunc(genericDataType, "conversation.deletion", "ConversationService", "onConversationDeletion");
            }
        }
    }

    private function matchRemoteFuncForDeal(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "deal.deletion" => {
                check self.executeRemoteFunc(genericDataType, "deal.deletion", "DealService", "onDealDeletion");
            }
            "deal.creation" => {
                check self.executeRemoteFunc(genericDataType, "deal.creation", "DealService", "onDealCreation");
            }
            "deal.merge" => {
                check self.executeRemoteFunc(genericDataType, "deal.merge", "DealService", "onDealMerge");
            }
            "deal.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "deal.propertyChange", "DealService", "onDealPropertyChange");
            }
            "deal.restore" => {
                check self.executeRemoteFunc(genericDataType, "deal.restore", "DealService", "onDealRestore");
            }
            "deal.associationChange" => {
                check self.executeRemoteFunc(genericDataType, "deal.associationChange", "DealService", "onDealAssociationChange");
            }
        }
    }

    private function matchRemoteFuncForContact(GenericDataType genericDataType) returns error? {
        match genericDataType.subscriptionType {
            "contact.creation" => {
                check self.executeRemoteFunc(genericDataType, "contact.creation", "ContactService", "onContactCreation");
            }
            "contact.associationChange" => {
                check self.executeRemoteFunc(genericDataType, "contact.associationChange", "ContactService", "onContactAssociationChange");
            }
            "contact.deletion" => {
                check self.executeRemoteFunc(genericDataType, "contact.deletion", "ContactService", "onContactDeletion");
            }
            "contact.privacyDeletion" => {
                check self.executeRemoteFunc(genericDataType, "contact.privacyDeletion", "ContactService", "onContactPrivacyDeletion");
            }
            "contact.propertyChange" => {
                check self.executeRemoteFunc(genericDataType, "contact.propertyChange", "ContactService", "onContactPropertyChange");
            }
            "contact.merge" => {
                check self.executeRemoteFunc(genericDataType, "contact.merge", "ContactService", "onContactMerge");
            }
            "contact.restore" => {
                check self.executeRemoteFunc(genericDataType, "contact.restore", "ContactService", "onContactRestore");
            }
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }
}
