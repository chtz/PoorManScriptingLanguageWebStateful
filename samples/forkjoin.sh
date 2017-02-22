#!/bin/bash

export DEF_ID=`curl -s --data-binary @forkjoin.pmsl -H'Content-Type:text/plain' http://localhost:7070/definitions`
echo Workflow definition created: $DEF_ID

export WF_ID=`curl -s --data-binary '{}' -H'Content-Type:application/json' http://localhost:7070/definitions/$DEF_ID`
echo Workflow instance $WF_ID started:
cat ../instanceStorage/$WF_ID.json | jq ".token"

export FIRST_CHILD_ID=$(cat ../instanceStorage/$WF_ID.json | jq -r ".token.children[0].vars.id")
IGNORE=$(curl -s --data-binary "{\"foo\":\"bar\"}" -H'Content-Type:application/json' http://localhost:7070/tokens/$WF_ID/$FIRST_CHILD_ID)
echo Child $FIRST_CHILD_ID signaled:
cat ../instanceStorage/$WF_ID.json | jq ".token"

export FIRST_CHILD_ID=$(cat ../instanceStorage/$WF_ID.json | jq -r ".token.children[0].vars.id")
IGNORE=$(curl -s --data-binary "{\"foo\":\"bar\"}" -H'Content-Type:application/json' http://localhost:7070/tokens/$WF_ID/$FIRST_CHILD_ID)
echo Child $FIRST_CHILD_ID signaled:
cat ../instanceStorage/$WF_ID.json | jq ".token"

export FIRST_CHILD_ID=$(cat ../instanceStorage/$WF_ID.json | jq -r ".token.children[0].vars.id")
IGNORE=$(curl -s --data-binary "{\"foo\":\"bar\"}" -H'Content-Type:application/json' http://localhost:7070/tokens/$WF_ID/$FIRST_CHILD_ID)
echo Child $FIRST_CHILD_ID signaled:
cat ../instanceStorage/$WF_ID.json | jq ".token"

export FIRST_CHILD_ID=$(cat ../instanceStorage/$WF_ID.json | jq -r ".token.children[0].vars.id")
IGNORE=$(curl -s --data-binary "{\"foo\":\"bar\"}" -H'Content-Type:application/json' http://localhost:7070/tokens/$WF_ID/$FIRST_CHILD_ID)
echo Child $FIRST_CHILD_ID signaled:
cat ../instanceStorage/$WF_ID.json | jq ".token"
