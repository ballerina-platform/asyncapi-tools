import ballerina/http;

@display {label: ""}
public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;

    public function init(int|http:Listener listenTo = 8090, *ListenerConfiguration configuration) returns error? {
        if listenTo is http:Listener {
            self.httpListener = listenTo;
        } else {
            json configJson = configuration.toJson();
            map<json> configMap = check configJson.cloneWithType();
            _ = configMap.remove("webhookSecret");
            http:ListenerConfiguration httpConfig = check configMap.cloneWithType();
            self.httpListener = check new (listenTo, httpConfig);
        }
        self.dispatcherService = new DispatcherService(configuration.webhookSecret);
    }

    public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
    }

    public isolated function detach(GenericServiceType serviceRef) returns error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.removeServiceRef(serviceTypeStr);
    }

    public isolated function 'start() returns error? {
        check self.httpListener.attach(self.dispatcherService, ());
        return self.httpListener.'start();
    }

    public isolated function gracefulStop() returns @tainted error? {
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        return self.httpListener.immediateStop();
    }

    private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string {
        if serviceRef is TicketService {
            return "TicketService";
        } else if serviceRef is CompanyService {
            return "CompanyService";
        } else if serviceRef is LineItemService {
            return "LineItemService";
        } else if serviceRef is ProductService {
            return "ProductService";
        } else if serviceRef is ConversationService {
            return "ConversationService";
        } else if serviceRef is DealService {
            return "DealService";
        } else if serviceRef is ContactService {
            return "ContactService";
        } else {
            panic error("Unrecognized service type attached to the listener");
        }
    }
}
