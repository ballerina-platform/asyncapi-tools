public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Pet record {
    int id;
    string name;
    string tag?;
    string 'type?;
};

public type Dog record {
    *Pet;
    boolean bark?;
};

public type Pets Pet[];

public type SimpleType int;

public type ReferredSimpleType SimpleType;

public type TestPet Pet;

public type TestDog Dog;
