import ballerina/log;
import ballerina/websocket;

import xlibb/pipe;

public client isolated class GraphqlOverWebsocketClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final PipesMap pipes;
    private boolean isActive;

    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector` 
    # + serviceUrl - URL of the target service 
    # + return - An error if connector initialization failed 
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/graphql_over_websocket") returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.isActive = true;
        self.startMessageWriting();
        self.startMessageReading();
        return;
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
                pipe:Pipe pipe = self.pipes.getPipe(message.'type);
                pipe:Error? pipeErr = pipe.produce(message, 5);
                if pipeErr is pipe:Error {
                    log:printError("PipeError: Failed to produce message to the pipe", pipeErr);
                    self.attemptToCloseConnection();
                    return;
                }
            }
        }
    }

    remote isolated function doConnectionInit(ConnectionInit connectionInit, decimal timeout) returns ConnectionAck|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = connectionInit.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("connectionInit").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        ConnectionAck|error connectionAck = responseMessage.cloneWithType();
        if connectionAck is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", connectionAck);
        }
        return connectionAck;
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

    remote isolated function doSubscribe(Subscribe subscribe, decimal timeout) returns Next|Complete|error {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = subscribe.cloneWithType();
        if message is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", message);
        }
        pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        if pipeErr is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in producing message", pipeErr);
        }
        Message|pipe:Error responseMessage = self.pipes.getPipe("subscribe").consume(timeout);
        if responseMessage is pipe:Error {
            self.attemptToCloseConnection();
            return error("PipeError: Error in consuming message", responseMessage);
        }
        Next|Complete|error unionResult = responseMessage.cloneWithType();
        if unionResult is error {
            self.attemptToCloseConnection();
            return error("DataBindingError: Error in cloning message", unionResult);
        }
        return unionResult;
    }

    remote isolated function doComplete(Complete complete, decimal timeout) returns error? {
        lock {
            if !self.isActive {
                return error("ConnectionError: Connection has been closed");
            }
        }
        Message|error message = complete.cloneWithType();
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
            check self.clientEp->close();
        }
    };
}
