import ballerina/websocket;
import ballerina/log;

listener websocket:Listener ep0 = new (80);

service /hello on ep0 {
    resource function get hi() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer(username)
    }
}

service class ChatServer {
    *websocket:Service;

    remote function onOpen(websocket:Caller caller) returns error? {
        string welcomeMsg = "Hi " + self.username + "! You have successfully connected to the chat";
        check caller->writeMessage(welcomeMsg);
        string msg = self.username + " connected to chat";
        check broadcast(msg);
        caller.setAttribute(USERNAME, self.username);
        lock {
            connectionsMap[caller.getConnectionId()] = caller;
        }
    }

    remote function onMessage(websocket:Caller caller, string text) returns error? {
        string msg = check getUsername(caller, USERNAME) + ": " + text;
        io:println(msg);
        @strand {
            thread:"any"
        }
        worker broadcast returns error? {
            check broadcast(msg);
        }
    }

    remote function onClose(websocket:Caller caller, int statusCode, string reason) returns error? {
        lock {
            _ = connectionsMap.remove(caller.getConnectionId());
        }
        string msg = check getUsername(caller, USERNAME) + " left the chat";
        check broadcast(msg);
    }
}

// Function to perform the broadcasting of messages.
function broadcast(string|PlayerLeft|UserMove|Winner message) returns error? {
    foreach websocket:Caller con in connectionsMap {
        websocket:Error? err = con->writeMessage(message);
        if err is websocket:Error {
            io:println("Error sending message to the :" + check getUserSign(con, USER_SIGN) +
                        ". Reason: " + err.message());
        }
    }
}

function getUserSign(websocket:Caller ep, string key) returns string|error {
    return <string> check ep.getAttribute(key);
}

function calculateWinner() returns string? {
    int[][] lines = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]];
    foreach int[] i in lines {
        int[] block = i;
        int a = block[0];
        int b = block[1];
        int c = block[2];
        if squares[a] == squares[b] && squares[a] == squares[c] {
            return squares[a];
        }
    }
}

