#!/bin/bash

terraform init
terraform apply -var-file="../aws-session.tf" -auto-approve
echo "RDS deployed!"