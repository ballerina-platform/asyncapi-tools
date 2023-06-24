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
    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector`
    # + serviceUrl - URL of the target service
    # + return - An error if connector initialization failed
    public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/chat") returns error? {
        self.pipes = new ();
        self.streamGenerators = new ();
        self.writeMessageQueue = new (1000);
        self.readMessageQueue = new (1000);
        websocket:Client websocketEp = check new (serviceUrl, clientConfig);
        self.clientEp = websocketEp;
        self.subscribeMessagePipe = ();
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
                }
            }
        }
    }
    #
    remote isolated function doSubscribeMessage(SubscribeMessage subscribeMessage, decimal timeout) returns stream<NextMessage|CompleteMessage|ErrorMessage,error?>|error {
        if self.writeMessageQueue.isClosed() {
            return error("connection closed");
        }
        pipe:Pipe subscribeMessagePipe;
        lock {
            self.subscribeMessagePipe = self.pipes.getPipe("subscribeMessage");
        }
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
    remote isolated function closeSubscribeMessagePipe() returns error? {
        lock {
            if self.subscribeMessagePipe !is () {
                pipe:Pipe subscribeMessagePipe = check self.subscribeMessagePipe.ensureType();
                check subscribeMessagePipe.gracefulClose();
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
