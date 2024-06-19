import ballerina/lang.runtime;
import ballerina/log;
import ballerina/websocket;

import xlibb/pipe;
import ballerina/uuid;

public client isolated class PayloadVv1Client {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final PipesMap pipes;
    private boolean isActive;

    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector` 
    # + serviceUrl - URL of the target service 
    # + return - An error if connector initialization failed 
    # + headerParams - header parameters 
    public isolated function init(HeaderParams headerParams, websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/payloadV") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        string modifiedUrl = serviceUrl + string `/v1`;
        map<string> headerParam = {"offset": headerParams.offset.toString(), "lat": headerParams.lat, "lon": headerParams.lon, "exclude": headerParams.exclude, "units": headerParams.units.toString()};
        map<string> customHeaders = getCombineHeaders(clientConfig.customHeaders,headerParam);
        clientConfig.customHeaders = customHeaders;
        websocket:Client websocketEp = check new (modifiedUrl, clientConfig);
        self.clientEp = websocketEp;
        self.isActive = true;
        self.startMessageWriting();
        self.startMessageReading();
        return;
    }

    # Use to write messages to the websocket.
    #
    private isolated function startMessageWriting() {
        worker writeMessage {
            while true {
                lock {
                    if !self.isActive {
                        break;
                    }
                }
                Message|pipe:Error message = self.writeMessageQueue.consume(5);
                if message is pipe:Error {
                    if message.message() == "Operation has timed out" {
                        continue;
                    }
                    log:printError("[writeMessage]PipeError: " + message.message());
                    self.attemptToCloseConnection();
                    return;
                }
                websocket:Error? wsErr = self.clientEp->writeMessage(message);
                if wsErr is websocket:Error {
                    log:printError("[writeMessage]WsError: " + wsErr.message());
                    self.attemptToCloseConnection();
                    return;
                }
                runtime:sleep(0.01);
            }
        }
    }

    # Use to read messages from the websocket.
    #
    private isolated function startMessageReading() {
        worker readMessage {
            while true {
                lock {
                    if !self.isActive {
                        break;
                    }
                }
                Message|websocket:Error message = self.clientEp->readMessage(Message);
                if message is websocket:Error {
                    log:printError("[readMessage]WsError: " + message.message());
                    self.attemptToCloseConnection();
                    return;
                }
                pipe:Pipe pipe = self.pipes.getPipe(message.event);
                pipe:Error? pipeErr = pipe.produce(message, 5);
                if pipeErr is pipe:Error {
                    log:printError("[readMessage]PipeError: " + pipeErr.message());
                    self.attemptToCloseConnection();
                    return;
                }
                runtime:sleep(0.01);
            }
        }
    }

    # remote description
    #
    # + subscribe - subscribe description 
    # + timeout - waiting period to keep the event in the buffer in seconds 
    # + return - unsubscribe description 
    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns UnSubscribe|error {
        lock {
            if !self.isActive {
                return error("[doSubscribe]ConnectionError: Connection has been closed");
            }
        }
        pipe:Pipe subscribePipe = new (1);
        string id;
        lock {
            id = uuid:createType1AsString();
            subscribe.id = id;
        }
        self.pipes.addPipe(id, subscribePipe);
        Message|error message = subscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]DataBindingError: Error in cloning message");
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]PipeError: Error in producing message");
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("subscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]PipeError: Error in consuming message");
        }
        check subscribePipe.gracefulClose();
        UnSubscribe|error unSubscribe = responseMessage.cloneWithType();
        if unSubscribe is error {
            self.attemptToCloseConnection();
            return error("[doSubscribe]DataBindingError: Error in cloning message");
        }
        return unSubscribe;
    }

    isolated function attemptToCloseConnection() {
        error? connectionClose = self->connectionClose();
        if connectionClose is error {
            log:printError("ConnectionError: " + connectionClose.message());
        }
    }

    remote isolated function connectionClose() returns error? {
        lock {
            self.isActive = false;
            check self.writeMessageQueue.immediateClose();
            check self.pipes.removePipes();
            check self.clientEp->close();
        }
    };
}
