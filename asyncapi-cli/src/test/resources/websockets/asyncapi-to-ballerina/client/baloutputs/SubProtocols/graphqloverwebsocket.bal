import ballerina/lang.regexp;
import ballerina/log;
import ballerina/websocket;

import xlibb/pipe;

public client isolated class ChatClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final PipesMap pipes;
    private final StreamGeneratorsMap streamGenerators;
    private boolean isActive;
    private final readonly & map<string> responseMap = {
        "CompleteMessage": "subscribeMessage",
        "ConnectionAckMessage": "connectionInitMessage",
        "PongMessage": "pingMessage",
        "NextMessage": "subscribeMessage",
        "ErrorMessage": "subscribeMessage"
    };

    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/chat") returns error? {
        self.pipes = new ();
        self.streamGenerators = new ();
        self.writeMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.isActive = true;
        self.startMessageWriting();
        self.startMessageReading();
        return;
    }

    private isolated function getRecordName(string dispatchingValue) returns string {
        string[] words = regexp:split(re `[\W_]+`, dispatchingValue);
        string result = "";
        foreach string word in words {
            result += word.substring(0, 1).toUpperAscii() + word.substring(1).toLowerAscii();
        }
        return result;
    }

    private isolated function getPipeName(string responseType) returns string {
        string responseRecordType = self.getRecordName(responseType);
        if self.responseMap.hasKey(responseRecordType) {
            return self.responseMap.get(responseRecordType);
        }
        return responseType;
    }

    # Used to write messages to the websocket.
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
                    log:printError("PipeError: Failed to consume message from the pipe", message);
                    self.attemptToCloseConnection();
                    return;
                }
                websocket:Error? wsErr = self.clientEp->writeMessage(message);
                if wsErr is websocket:Error {
                    log:printError("WsError: Failed to write message to the client", wsErr);
                    self.attemptToCloseConnection();
                    return;
                }
            }
        }
    }

    # Used to read messages from the websocket.
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
                    log:printError("WsError: Failed to read message from the client", message);
                    self.attemptToCloseConnection();
                    return;
                }
                pipe:Pipe pipe;
                MessageWithId|error messageWithId = message.cloneWithType(MessageWithId);
                if messageWithId is MessageWithId {
                    pipe = self.pipes.getPipe(messageWithId.id);
                } else {
                    string pipeName = self.getPipeName(message.'type);
                    pipe = self.pipes.getPipe(pipeName);
                }
                pipe:Error? pipeErr = pipe.produce(message, 5);
                if pipeErr is pipe:Error {
                    log:printError("PipeError: Failed to produce message to the pipe", pipeErr);
                    self.attemptToCloseConnection();
                    return;
                }
            }
        }
    }

    # subscribemessage description
    #
    # + subscribeMessage - subscribe payload description
    # + timeout - waiting period to keep the event in the buffer in seconds
    # + return - subscribe response description
    remote isolated function doSubscribeMessage(SubscribeMessage subscribeMessage, decimal timeout) returns stream<NextMessage|CompleteMessage|ErrorMessage,error?>|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = subscribeMessage.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        stream<NextMessage|CompleteMessage|ErrorMessage,error?> streamMessages;
        lock {
            NextMessageCompleteMessageErrorMessageStreamGenerator streamGenerator = new (self.pipes, subscribeMessage.id, timeout);
            self.streamGenerators.addStreamGenerator(streamGenerator);
            streamMessages = new (streamGenerator);
        }
        return streamMessages;
    }

    remote isolated function doPingMessage(PingMessage pingMessage, decimal timeout) returns PongMessage|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = pingMessage.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("pingMessage").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        PongMessage|error pongMessage = responseMessage.cloneWithType();
        if pongMessage is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", pongMessage);
        }
        return pongMessage;
    }

    remote isolated function doPongMessage(PongMessage pongMessage, decimal timeout) returns error? {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = pongMessage.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
    }

    remote isolated function doConnectionInitMessage(ConnectionInitMessage connectionInitMessage, decimal timeout) returns ConnectionAckMessage|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = connectionInitMessage.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("connectionInitMessage").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        ConnectionAckMessage|error connectionAckMessage = responseMessage.cloneWithType();
        if connectionAckMessage is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", connectionAckMessage);
        }
        return connectionAckMessage;
    }

    remote isolated function doCompleteMessage(CompleteMessage completeMessage, decimal timeout) returns error? {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = completeMessage.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
    }

    isolated function attemptToCloseConnection() {
        error? connectionClose = self->connectionClose();
        if connectionClose is error {
            log:printError("ConnectionError", connectionClose);
        }
    }

    remote isolated function connectionClose() returns error? {
        lock {
            self.isActive = false;
            check self.writeMessageQueue.immediateClose();
            check self.pipes.removePipes();
            check self.streamGenerators.removeStreamGenerators();
            check self.clientEp->close();
        }
    };
}
