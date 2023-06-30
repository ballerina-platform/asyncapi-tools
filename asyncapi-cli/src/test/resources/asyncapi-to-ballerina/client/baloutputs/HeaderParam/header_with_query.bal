import ballerina/websocket;
import xlibb/pipe;
import ballerina/lang.runtime;
import ballerina/uuid;

public client isolated class PayloadVv1Client {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    private boolean isMessageWriting;
    private boolean isMessageReading;
    private boolean isPipeTriggering;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    # + queryParams - query parameters
    # + headerParams - header parameters
    public isolated function init(HeaderParams headerParams, QueryParams queryParams, websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/payloadV") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        string modifiedUrl = serviceUrl + string `/v1`;
        map<anydata> queryParam = {"offset": queryParams.offset, "lat": queryParams.lat, "lon": queryParams.lon, "exclude": queryParams.exclude, "units": queryParams.units};
        modifiedUrl = modifiedUrl + check getPathForQueryParam(queryParam);
        map<string> headerParam = {"offset": headerParams.offset.toString(), "lat": headerParams.lat, "lon": headerParams.lon, "exclude": headerParams.exclude, "units": headerParams.units.toString()};
        map<string> customHeaders = getCombineHeaders(clientConfig.customHeaders,headerParam);
        clientConfig.customHeaders = customHeaders;
        websocket:Client websocketEp = check new (modifiedUrl, clientConfig);
        self.clientEp = websocketEp;
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
            while self.isMessageWriting {
                anydata requestMessage = check self.writeMessageQueue.consume(5);
                check self.clientEp->writeMessage(requestMessage);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage returns error? {
            while self.isMessageReading {
                Message message = check self.clientEp->readMessage();
                check self.readMessageQueue.produce(message, 5);
                runtime:sleep(0.01);
            }
        }
    }
    # Use to map received message responses into relevant requests.
    #
    private isolated function startPipeTriggering() {
        worker pipeTrigger returns error? {
            while self.isPipeTriggering {
                Message message = check self.readMessageQueue.consume(5);
                if message.hasKey("id") {
                    MessageWithId messageWithId = check message.cloneWithType();
                    string id = messageWithId.id;
                    pipe:Pipe idPipe = self.pipes.getPipe(id);
                    check idPipe.produce(messageWithId, 5);
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
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe subscribePipe = new (1);
        string id;
        lock {
            id = uuid:createType1AsString();
            subscribe.id = id;
        }
        self.pipes.addPipe(id, subscribePipe);
        Message message = check subscribe.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        anydata responseMessage = check subscribePipe.consume(timeout);
        check subscribePipe.gracefulClose();
        UnSubscribe unSubscribe = check responseMessage.cloneWithType();
        return unSubscribe;
    }
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
