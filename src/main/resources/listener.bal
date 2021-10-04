import ballerina/http;

public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;

    public function init(int|http:Listener listenOn = 8090) returns error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
        self.dispatcherService = new DispatcherService();
    }

    public isolated function attach(GenericService serviceRef, () attachPoint) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
    }

    public isolated function detach(GenericService serviceRef) returns error? {
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

    private isolated function getServiceTypeStr(GenericService serviceRef) returns string {}
}
