public type TicketService service object {
    remote function onTicketPropertyChange(WebhookEvent event) returns error?;
    remote function onTicketDeletion(WebhookEvent event) returns error?;
    remote function onTicketCreation(WebhookEvent event) returns error?;
    remote function onTicketMerge(WebhookEvent event) returns error?;
    remote function onTicketRestore(WebhookEvent event) returns error?;
    remote function onTicketAssociationChange(WebhookEvent event) returns error?;
};

public type CompanyService service object {
    remote function onCompanyDeletion(WebhookEvent event) returns error?;
    remote function onCompanyRestore(WebhookEvent event) returns error?;
    remote function onCompanyMerge(WebhookEvent event) returns error?;
    remote function onCompanyPropertyChange(WebhookEvent event) returns error?;
    remote function onCompanyCreation(WebhookEvent event) returns error?;
    remote function onCompanyAssociationChange(WebhookEvent event) returns error?;
};

public type LineItemService service object {
    remote function onLineItemMerge(WebhookEvent event) returns error?;
    remote function onLineItemDeletion(WebhookEvent event) returns error?;
    remote function onLineItemPropertyChange(WebhookEvent event) returns error?;
    remote function onLineItemRestore(WebhookEvent event) returns error?;
    remote function onLineItemAssociationChange(WebhookEvent event) returns error?;
    remote function onLineItemCreation(WebhookEvent event) returns error?;
};

public type ProductService service object {
    remote function onProductPropertyChange(WebhookEvent event) returns error?;
    remote function onProductDeletion(WebhookEvent event) returns error?;
    remote function onProductMerge(WebhookEvent event) returns error?;
    remote function onProductRestore(WebhookEvent event) returns error?;
    remote function onProductCreation(WebhookEvent event) returns error?;
};

public type ConversationService service object {
    remote function onConversationCreation(WebhookEvent event) returns error?;
    remote function onConversationPropertyChange(WebhookEvent event) returns error?;
    remote function onConversationPrivacyDeletion(WebhookEvent event) returns error?;
    remote function onConversationNewMessage(WebhookEvent event) returns error?;
    remote function onConversationDeletion(WebhookEvent event) returns error?;
};

public type DealService service object {
    remote function onDealDeletion(WebhookEvent event) returns error?;
    remote function onDealCreation(WebhookEvent event) returns error?;
    remote function onDealMerge(WebhookEvent event) returns error?;
    remote function onDealPropertyChange(WebhookEvent event) returns error?;
    remote function onDealRestore(WebhookEvent event) returns error?;
    remote function onDealAssociationChange(WebhookEvent event) returns error?;
};

public type ContactService service object {
    remote function onContactCreation(WebhookEvent event) returns error?;
    remote function onContactAssociationChange(WebhookEvent event) returns error?;
    remote function onContactDeletion(WebhookEvent event) returns error?;
    remote function onContactPrivacyDeletion(WebhookEvent event) returns error?;
    remote function onContactPropertyChange(WebhookEvent event) returns error?;
    remote function onContactMerge(WebhookEvent event) returns error?;
    remote function onContactRestore(WebhookEvent event) returns error?;
};

public type GenericServiceType TicketService|CompanyService|LineItemService|ProductService|ConversationService|DealService|ContactService;

