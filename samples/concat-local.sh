#!/bin/bash

export DEF_ID=`curl -s --data-binary @concatlocal.pmsl -H'Content-Type:text/plain' http://localhost:7070/definitions`
echo Workflow definition created: $DEF_ID

export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' http://localhost:7070/definitions/$DEF_ID`
echo Workflow instance started: $WF_ID

export WF_ID=`curl -s --data-binary "{\"email\":\"$1\"}" -H'Content-Type:application/json' http://localhost:7070/instances/$WF_ID`
echo Workflow instance signaled: $WF_ID with email: $1
