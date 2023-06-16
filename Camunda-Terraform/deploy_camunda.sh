#!/bin/bash

terraform init
terraform apply -var-file="../aws-session.tf" -auto-approve

# grant access to tasklist
curl -w "\n" -X POST -H "Content-Type: application/json" -d @authTaskList.json "http://$camundaHost:8080/engine-rest/authorization/create"


echo "Deployed Camunda!"