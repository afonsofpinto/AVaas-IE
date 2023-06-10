#!/bin/bash

key_file="labsuser.pem"

echo 'Deploying RDS...'
cd RDS-Terraform
bash deploy_rds.sh &
cd ..

echo 'Deploying Kafka...'
cd Kafka-Terraform
bash deploy_kafka.sh $key_file &
cd ..

echo "Deploying Camunda..."
cd Camunda-Terraform
bash deploy_camunda.sh &
cd ..

wait %1 %2 %3

echo "Deploying Microservices..."
cd Microservices
bash deploy_microservices.sh
cd ..
echo "Deployed Microservices!"

echo "Deploying Konga..."
cd Kong-Terraform
bash deploy_kong.sh
cd ..
echo "Deployed Kong!"

echo "Finished."

