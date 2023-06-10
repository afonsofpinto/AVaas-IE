#!/bin/bash

terraform init
terraform apply -var-file="../aws-session.tf" -auto-approve
sleep 45
bash setServicesNRoutes.sh