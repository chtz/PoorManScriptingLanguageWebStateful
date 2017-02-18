#!/bin/bash

export DEF_ID=`curl -s --data-binary @concat.pmsl -H'Content-Type:text/plain' https://pmw.furthermore.ch/definitions`
echo Workflow definition created: $DEF_ID

export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' https://pmw.furthermore.ch/definitions/$DEF_ID`
echo Workflow instance started: $WF_ID

export WF_ID=`curl -s --data-binary "{\"email\":\"$1\",\"prefix\":\"My Prefix\"}" -H'Content-Type:application/json' https://pmw.furthermore.ch/instances/$WF_ID`
echo Workflow instance signaled: $WF_ID with email: $1
