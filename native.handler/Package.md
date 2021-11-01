This is a wrapper, which is being used by triggers. 
In Ballerina, resource functions can not execute remote functions directly.
Java interop must be used to execute those kinds of calls.
This module contains a wrapper, which wraps a set of interop functions.
Triggers use this wrapper to execute calls from the resource functions to the remote functions.