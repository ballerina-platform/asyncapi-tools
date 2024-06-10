import ballerina/websocket;
import xlibb/pipe;
import ballerina/lang.runtime;

public client isolated class ChatClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    private final StreamGeneratorsMap streamGenerators;
    private boolean isMessageWriting;
    private boolean isMessageReading;
    private boolean isPipeTriggering;
    private pipe:Pipe? subscribeMessagePipe;
    private pipe:Pipe? pingMessagePipe;
    private pipe:Pipe? connectionInitMessagePipe;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(string serviceUrl, websocket:ClientConfiguration clientConfig =  {}) returns error? {
        self.pipes = new ();
        self.streamGenerators = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.subscribeMessagePipe = ();
        self.pingMessagePipe = ();
        self.connectionInitMessagePipe = ();
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
                string 'type = message.'type;
                match ('type) {
                    "NextMessage"|"CompleteMessage"|"ErrorMessage" => {
                        pipe:Pipe subscribeMessagePipe = self.pipes.getPipe("subscribeMessage");
                        check subscribeMessagePipe.produce(message, 5);
                    }
                    "PongMessage" => {
                        pipe:Pipe pingMessagePipe = self.pipes.getPipe("pingMessage");
                        check pingMessagePipe.produce(message, 5);
                    }
                    "ConnectionAckMessage" => {
                        pipe:Pipe connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
                        check connectionInitMessagePipe.produce(message, 5);
                    }
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
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe subscribeMessagePipe;
        lock {
            self.subscribeMessagePipe = self.pipes.getPipe("subscribeMessage");
        }
        Message message = check subscribeMessage.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            subscribeMessagePipe = check self.subscribeMessagePipe.ensureType();
        }
        stream<NextMessage|CompleteMessage|ErrorMessage,error?> streamMessages;
        lock {
            NextMessageCompleteMessageErrorMessageStreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
            self.streamGenerators.addStreamGenerator(streamGenerator);
            streamMessages = new (streamGenerator);
        }
        return streamMessages;
    }
    #
    remote isolated function doPingMessage(PingMessage pingMessage, decimal timeout) returns PongMessage|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe pingMessagePipe;
        lock {
            self.pingMessagePipe = self.pipes.getPipe("pingMessage");
        }
        Message message = check pingMessage.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            pingMessagePipe = check self.pingMessagePipe.ensureType();
        }
        anydata responseMessage = check pingMessagePipe.consume(timeout);
        PongMessage pongMessage = check responseMessage.cloneWithType();
        return pongMessage;
    }
    #
    remote isolated function doPongMessage(PongMessage pongMessage, decimal timeout) returns error? {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        Message message = check pongMessage.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
    }
    #
    remote isolated function doConnectionInitMessage(ConnectionInitMessage connectionInitMessage, decimal timeout) returns ConnectionAckMessage|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe connectionInitMessagePipe;
        lock {
            self.connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
        }
        Message message = check connectionInitMessage.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
        lock {
            connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
        }
        anydata responseMessage = check connectionInitMessagePipe.consume(timeout);
        ConnectionAckMessage connectionAckMessage = check responseMessage.cloneWithType();
        return connectionAckMessage;
    }
    #
    remote isolated function doCompleteMessage(CompleteMessage completeMessage, decimal timeout) returns error? {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        Message message = check completeMessage.cloneWithType();
        check self.writeMessageQueue.produce(message, timeout);
    }
    remote isolated function closeSubscribeMessagePipe() returns error? {
        lock {
            if self.subscribeMessagePipe !is() {
                pipe:Pipe subscribeMessagePipe = check self.subscribeMessagePipe.ensureType();
                check subscribeMessagePipe.gracefulClose();
            }
        }
    };
    remote isolated function closePingMessagePipe() returns error? {
        lock {
            if self.pingMessagePipe !is() {
                pipe:Pipe pingMessagePipe = check self.pingMessagePipe.ensureType();
                check pingMessagePipe.gracefulClose();
            }
        }
    };
    remote isolated function closeConnectionInitMessagePipe() returns error? {
        lock {
            if self.connectionInitMessagePipe !is() {
                pipe:Pipe connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
                check connectionInitMessagePipe.gracefulClose();
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
            check self.streamGenerators.removeStreamGenerators();
            check self.clientEp->close();
        }
    };
}
