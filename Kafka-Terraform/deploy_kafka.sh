#!/bin/bash

terraform init
terraform apply -var-file="../aws-session.tf" -auto-approve

# reboot the instances
instance_hostnames=$(cat kafka_hostnames.tmp)
for hostname in $instance_hostnames; do
  ssh -o StrictHostKeyChecking=no -i "../$1" ec2-user@$hostname "sudo reboot"
done
sleep 10 # wait for reboot
echo "Kafka deployed!"