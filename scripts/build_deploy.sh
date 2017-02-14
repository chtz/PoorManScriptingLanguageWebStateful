#!/bin/bash
mvn package docker:build
docker push dockerchtz/pmsl-web-stateful:latest
ssh root@pmsl '(docker rm -f pmsl-web-stateful; true) && (docker rmi dockerchtz/pmsl-web-stateful; true) && docker run -p 7070:7070 --name pmsl-web-stateful -d dockerchtz/pmsl-web-stateful'
