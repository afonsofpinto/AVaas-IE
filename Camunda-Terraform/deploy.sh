#!/bin/bash
echo "Starting..."

sudo yum install -y docker

sudo service docker start

sudo docker pull camunda/camunda-bpm-platform:latest

sudo docker run -d --name camunda -p 8080:8080 camunda/camunda-bpm-platform:latest
