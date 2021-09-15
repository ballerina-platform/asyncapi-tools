import ballerina/http;

class SlackListener {
    private http:Listener httpListener;
    //private string verificationToken;

    public isolated function init(int port = 8090) returns error? { //receive verification token with listener config
        self.httpListener = check new (port); // param port
        //self.verificationToken = userConfig.verificationToken
    }

    public isolated function attach(GenericServiceType serviceRef, string | string [] attachPoint) returns @tainted error? {// Pass verification token 
        check self.httpListener.attach(check new DispatcherService(serviceRef), attachPoint);
    }

    public isolated function detach(service object {} s) returns error? {
        return self.httpListener.detach(s);
    }

    public isolated function 'start() returns error? {
        return self.httpListener.'start();
    }

    public isolated function gracefulStop() returns @tainted error? {
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        return self.httpListener.immediateStop();
    }
}

