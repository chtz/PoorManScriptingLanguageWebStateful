#!/bin/bash

export DEF_ID=`curl -s --data-binary @forkjoin2.pmsl -H'Content-Type:text/plain' https://pmw.furthermore.ch/definitions`
echo Workflow definition created: $DEF_ID

export WF_ID=`curl -s --data-binary "{\"mail\":\"$1\"}" -H'Content-Type:application/json' https://pmw.furthermore.ch/definitions/$DEF_ID`
echo Workflow instance started: $WF_ID
