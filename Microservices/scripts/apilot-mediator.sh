#!/bin/bash
echo 'Starting...'
sudo yum install -y docker
sudo service docker start
sudo docker login -u 'ietest' -p '123456789'
sudo docker pull ietest/apilot-mediator:1.0.0-SNAPSHOT
sudo docker run -d --name tryout2 -p 8080:8080 ietest/apilot-mediator:1.0.0-SNAPSHOT
echo Finished.
