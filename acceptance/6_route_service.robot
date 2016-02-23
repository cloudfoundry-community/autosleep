*** Settings ***
Resource        Keywords.robot
Documentation   Test the route service feature
Force Tags      Route service


*** Test Cases ***
Auto-Stopped app should be bound to a route service
    #copy inactivity detection test, and test if route service binded at the end

Incoming traffic should trigger start
    #copy inactivity detection test, curl the test app, wait for app to start. Check that app start and no route service registered

Incoming traffic should be forwarded
    #check that we don't loose calls. But how?

Bind route on unkown app should fail

Unbind app should also unbind route
