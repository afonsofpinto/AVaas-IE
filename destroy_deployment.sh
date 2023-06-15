#!/bin/bash

echo 'Destroyng RDS...'
cd RDS-Terraform
terraform destroy -var-file="../aws-session.tf" -auto-approve &
cd ..

echo "Destroying Kafka..."
cd Kafka-Terraform
terraform destroy -var-file="../aws-session.tf" -auto-approve &
cd ..

echo "Destroying Microsevices..."
cd Microservices
terraform destroy -var-file="../aws-session.tf" -auto-approve &
cd ..

echo "Destroying Camunda..."
cd Camunda-Terraform
terraform destroy -var-file="../aws-session.tf" -auto-approve &
cd ..

echo "Destroying Kong..."
cd Kong-Terraform
terraform destroy -var-file="../aws-session.tf" -auto-approve &
cd ..

wait %1 %2 %3 %4 %5
echo "All resources were destroyed."