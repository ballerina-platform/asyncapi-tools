// Copyright (c) 2023 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/websocket;

configurable int port = 8080;

type Album readonly & record {|
    readonly & string title;
    string id;
    string artist;
    decimal price;
    string event;
|};

type Testing string[];

table<Album> key(id) albums = table [
        {event: "Album",id: "1", title: "Blue Train", artist: "John Coltrane", price: 56.99},
        {event:"Album",id: "2", title: "Jeru", artist: "Gerry Mulligan", price: 17.99},
        {event:"Album",id: "3", title: "Sarah Vaughan and Clifford Brown", artist: "Sarah Vaughan", price: 39.99}
    ];

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(port) {
    resource function get albums/[string id]() returns websocket:Service| websocket:UpgradeError{
        return new ChatServer();

    }

}

service class ChatServer{
    *websocket:Service;


    remote function onAlbum(Album message, websocket:Caller caller) returns Album[]{
        return albums.toArray();

    }


}



