import ballerina/websocket;
import nuvindu/pipe;
import ballerina/lang.runtime;
import ballerina/uuid;

public client isolated class PayloadVv1Client {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    # + headerParams - header parameters
    public isolated function init(HeaderParams headerParams, websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/payloadV") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        string modifiedUrl = serviceUrl + string `/v1`;
        map<string> headerParam = {"offset": headerParams.offset.toString(), "lat": headerParams.lat, "lon": headerParams.lon, "exclude": headerParams.exclude, "units": headerParams.units.toString()};
        map<string> customHeaders = getCombineHeaders(clientConfig.customHeaders,headerParam);
        clientConfig.customHeaders=customHeaders;
        websocket:Client websocketEp = check new (modifiedUrl, clientConfig);
        self.clientEp = websocketEp;
        self.startMessageWriting();
        self.startMessageReading();
        self.startPipeTriggering();
        return;
    }
    # Use to write messages to the websocket.
    #
    private isolated function startMessageWriting() {
        worker writeMessage returns error {
            while true {
                anydata requestMessage = check self.writeMessageQueue.consume(5);
                check self.clientEp->writeMessage(requestMessage);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage returns error {
            while true {
                ResponseMessage responseMessage = check self.clientEp->readMessage();
                check self.readMessageQueue.produce(responseMessage, 5);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to map received message responses into relevant requests.
    #
    private isolated function startPipeTriggering() {
        worker pipeTrigger returns error {
            while true {
                ResponseMessage responseMessage = check self.readMessageQueue.consume(5);
                if responseMessage.hasKey("id") {
                    ResponseMessageWithId responseMessagWithId = check responseMessage.cloneWithType();
                    string id = responseMessagWithId.id;
                    pipe:Pipe idPipe = self.pipes.getPipe(id);
                    check idPipe.produce(responseMessagWithId, 5);
                }
            }
        }
    }
    # remote description
    #
    # + subscribe - subscribe description
    # + timeout - waiting period to keep the event in the buffer in seconds
    # + return - unsubscribe description
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns UnSubscribe|error {
        pipe:Pipe subscribePipe = new (1);
        string id;
        lock {
            id = uuid:createType1AsString();
        }
        self.pipes.addPipe(id, subscribePipe);
        subscribe["id"] = id;
        check self.writeMessageQueue.produce(subscribe, timeout);
        anydata responseMessage = check subscribePipe.consume(timeout);
        UnSubscribe unSubscribe = check responseMessage.cloneWithType();
        check subscribePipe.immediateClose();
        return unSubscribe;
    }
}
