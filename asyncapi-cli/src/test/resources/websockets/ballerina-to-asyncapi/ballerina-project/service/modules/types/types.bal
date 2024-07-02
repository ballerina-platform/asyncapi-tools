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

# Represents a product
#
# + name - Name of the product
# + description - Product description
# + price - Product price
public type Product record {|
    string id?;
    string name;
    string description;
    Price price;
|};

public enum Currency {
    USD,
    LKR,
    SGD,
    GBP
}

public type Price record {|
    Currency currency;
    float amount;
    string event;
|};

public type Error record {|
    string code;
    string message;
|};

public type ErrorResponse record {|
    Error 'error;
|};

public type BadRequest record {|

    ErrorResponse body;
|};

public const string TEXT_HTML = "text/html";
