Narrative:
In order to develop an application that requires a stack efficiently
As a development team
I would like to use an interface and implementation in Java directly
 
 
Scenario:  Basic functionality of a Stack
 
Given an empty stack
When the string Java is added
And the string C++ is added
And the last element is removed again
Then the resulting element should be Java
 
Scenario:  Stack search
 
Given an empty stack
When the string Java is added
And the string C++ is added
And the string PHP is added
And the element Java is searched for
Then the position returned should be 3
