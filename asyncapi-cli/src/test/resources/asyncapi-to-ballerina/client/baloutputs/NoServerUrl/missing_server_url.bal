import ballerina/websocket;
import nuvindu/pipe;
import ballerina/lang.runtime;

public client isolated class ChatClient {
    private final websocket:Client clientEp;
    private final pipe:Pipe writeMessageQueue;
    private final pipe:Pipe readMessageQueue;
    private final PipesMap pipes;
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(string serviceUrl, websocket:ClientConfiguration clientConfig =  {}) returns error? {
        self.pipes = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
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
                string 'type = responseMessage.'type;
                match ('type) {
                    "NextMessage"|"CompleteMessage"|"ErrorMessage" => {
                        pipe:Pipe subscribeMessagePipe = self.pipes.getPipe("subscribeMessage");
                        check subscribeMessagePipe.produce(responseMessage, 5);
                    }
                    "PongMessage" => {
                        pipe:Pipe pingMessagePipe = self.pipes.getPipe("pingMessage");
                        check pingMessagePipe.produce(responseMessage, 5);
                    }
                    "ConnectionAckMessage" => {
                        pipe:Pipe connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
                        check connectionInitMessagePipe.produce(responseMessage, 5);
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
        pipe:Pipe subscribeMessagePipe = new (10000);
        self.pipes.addPipe("subscribeMessage", subscribeMessagePipe);
        check self.writeMessageQueue.produce(subscribeMessage, timeout);
        stream<NextMessage|CompleteMessage|ErrorMessage,error?> streamMessages;
        lock {
            NextMessageCompleteMessageErrorMessageStreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
            streamMessages = new (streamGenerator);
        }
        return streamMessages;
    }
    #
    remote isolated function doPingMessage(PingMessage pingMessage, decimal timeout) returns PongMessage|error {
        pipe:Pipe pingMessagePipe = new (1);
        self.pipes.addPipe("pingMessage", pingMessagePipe);
        check self.writeMessageQueue.produce(pingMessage, timeout);
        anydata responseMessage = check pingMessagePipe.consume(timeout);
        PongMessage pongMessage = check responseMessage.cloneWithType();
        check pingMessagePipe.immediateClose();
        return pongMessage;
    }
    #
    remote isolated function doPongMessage(PongMessage pongMessage, decimal timeout) returns error? {
        check self.writeMessageQueue.produce(pongMessage, timeout);
    }
    #
    remote isolated function doConnectionInitMessage(ConnectionInitMessage connectionInitMessage, decimal timeout) returns ConnectionAckMessage|error {
        pipe:Pipe connectionInitMessagePipe = new (1);
        self.pipes.addPipe("connectionInitMessage", connectionInitMessagePipe);
        check self.writeMessageQueue.produce(connectionInitMessage, timeout);
        anydata responseMessage = check connectionInitMessagePipe.consume(timeout);
        ConnectionAckMessage connectionAckMessage = check responseMessage.cloneWithType();
        check connectionInitMessagePipe.immediateClose();
        return connectionAckMessage;
    }
    #
    remote isolated function doCompleteMessage(CompleteMessage completeMessage, decimal timeout) returns error? {
        check self.writeMessageQueue.produce(completeMessage, timeout);
    }
}
