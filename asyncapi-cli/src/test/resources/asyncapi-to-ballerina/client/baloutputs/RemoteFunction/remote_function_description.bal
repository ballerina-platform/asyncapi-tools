import ballerina/websocket;
import nuvindu/pipe;
import ballerina/lang.runtime;

public client isolated class PayloadVlocationsClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/payloadV") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        string modifiedUrl = serviceUrl + string `/locations`;
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
                string event = responseMessage.event;
                match (event) {
                    "UnSubscribe" => {
                        pipe:Pipe subscribePipe = self.pipes.getPipe("subscribe");
                        check subscribePipe.produce(responseMessage, 5);
                    }
                }
            }
        }
    }
    # remote description
    #
    # + subscribe - subscribe request description
    # + timeout - waiting period to keep the event in the buffer in seconds
    # + return - unsubscribe response description
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns UnSubscribe|error {
        pipe:Pipe subscribePipe = new (1);
        self.pipes.addPipe("subscribe", subscribePipe);
        check self.writeMessageQueue.produce(subscribe, timeout);
        anydata responseMessage = check subscribePipe.consume(timeout);
        UnSubscribe unSubscribe = check responseMessage.cloneWithType();
        check subscribePipe.immediateClose();
        return unSubscribe;
    }
}
