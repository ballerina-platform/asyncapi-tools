public type Subscribe record {
    Additional_Array additionalArray?;
    string event;
    Additional_NestedArray additionalNestedArray?;
    Additional_Primitive additionalPrimitive?;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Additional_Primitive record {|
    string name?;
    decimal age?;
    string...;
|};

public type Additional_Array record {|
    boolean isArray?;
    string[]...;
|};

public type Additional_NestedArray record {|
    boolean isArray?;
    string[][]...;
|};
