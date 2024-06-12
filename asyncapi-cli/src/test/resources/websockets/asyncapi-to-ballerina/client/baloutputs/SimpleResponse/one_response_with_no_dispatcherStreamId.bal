import ballerina/websocket;
import xlibb/pipe;
import ballerina/lang.runtime;

public client isolated class PayloadVlocationsClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    private boolean isMessageWriting;
    private boolean isMessageReading;
    private boolean isPipeTriggering;
    private pipe:Pipe? subscribePipe;
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
        self.subscribePipe = ();
        self.isMessageWriting = true;
        self.isMessageReading = true;
        self.isPipeTriggering = true;
        self.startMessageWriting();
        self.startMessageReading();
        self.startPipeTriggering();
        return;
    }
    # Use to write messages to the websocket.
    #
    private isolated function startMessageWriting() {
        worker writeMessage returns error? {
            lock {
                while self.isMessageWriting {
                    anydata requestMessage = check self.writeMessageQueue.consume(5);
                    check self.clientEp->writeMessage(requestMessage);
                    runtime:sleep(0.01);
                }
            }
        }
    }
    # Use to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage returns error? {
            lock {
                while self.isMessageReading {
                    Message message = check self.clientEp->readMessage();
                    check self.readMessageQueue.produce(message, 5);
                    runtime:sleep(0.01);
                }
            }
        }
    }
    # Use to map received message responses into relevant requests.
    #
    private isolated function startPipeTriggering() {
        worker pipeTrigger returns error? {
            lock {
                while self.isPipeTriggering {
                    Message message = check self.readMessageQueue.consume(5);
                    string event = message.event;
                    match (event) {
                        "UnSubscribe" => {
                            pipe:Pipe subscribePipe = self.pipes.getPipe("subscribe");
                            check subscribePipe.produce(message, 5);
                        }
                    }
                }
            }
        }
    }
    #
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns UnSubscribe|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe subscribePipe;
        lock {
            self.subscribePipe = self.pipes.getPipe("subscribe");
        }
        Message message = check subscribe.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            subscribePipe = check self.subscribePipe.ensureType();
        }
        anydata responseMessage = check subscribePipe.consume(timeout);
        UnSubscribe unSubscribe = check responseMessage.cloneWithType();
        return unSubscribe;
    }
    remote isolated function closeSubscribePipe() returns error? {
        lock {
            if self.subscribePipe !is() {
                pipe:Pipe subscribePipe = check self.subscribePipe.ensureType();
                check subscribePipe.gracefulClose();
            }
        }
    };
    remote isolated function connectionClose() returns error? {
        lock {
            self.isMessageReading = false;
            self.isMessageWriting = false;
            self.isPipeTriggering = false;
            check self.writeMessageQueue.immediateClose();
            check self.readMessageQueue.immediateClose();
            check self.pipes.removePipes();
            check self.clientEp->close();
        }
    };
}
