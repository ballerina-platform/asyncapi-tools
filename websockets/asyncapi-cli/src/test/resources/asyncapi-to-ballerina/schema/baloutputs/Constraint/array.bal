import ballerina/constraint;

@constraint:String {maxLength: 23}
public type HobbyItemsString string;

@constraint:String {minLength: 7}
public type PersonDetailsItemsString string;

@constraint:Float {maxValue: 445.4}
public type PersonFeeItemsNumber float;

@constraint:Array {maxLength: 5, minLength: 2}
public type Hobby HobbyItemsString[];

public type Subscribe record {
    Person person?;
    string event;
    Address tag;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Person record {
    Hobby hobby?;
    @constraint:Array {maxLength: 5}
    PersonDetailsItemsString[] Details?;
    int id;
    PersonFeeItemsNumber[] fee?;
    # The maximum number of items in the response (as set in the query or by default).
    anydata 'limit?;
};

@constraint:String {minLength: 5}
public type Address string;
