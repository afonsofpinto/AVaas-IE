# AVaas-IE

The autonomous vehicle as a service (AVaaS) is a cloudbased system that aims at the integration between car manufacturers, autopilot developers, vehicle users, and other external service providers to provide value added services of intelligence quotient (IQ), ethical quotient (EQ), and adversity quotient (AQ)1. IQ, EQ and AQ are symbolical ones. All of this services will be developed here.

# Deployment steps

## 1 - Set aws access information and keys
1.1 - Customize the following fields from the ```aws_session.tf``` file:
```Terraform
aws_access_key = "YOUR KEY"
aws_secret_key = "YOUR KEY"
aws_token = "YOUR TOKEN"
aws_keyname = "YOUR .pem KEY FILE NAME"
```

1.2 - Place the .pem key file in the root directory and set permissions:  

**Windows**: ```icacls <keyfile>.pem /inheritance:r /grant:r "<username>:(RX)"```, where username is the output of running the command ```whoami```  

**Linux/Unix OS**: ```chmod u=rwx,g=,o= <keyfile>.pem```


## 2 - Launch the databases [RDS-Terraform]

First, set the following parameters in the RDS .tf file: 
		
```Terraform
variable "db_username" {
  description = "The username for the database"
  type        = string
  sensitive   = true
  default     = "YOUR DB USERNAME"
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
  default     = "YOUR DB PASSWORD"
}

variable "db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "YOUR DB NAME"
}                         
```

Then run the following commands:
```bash
cd RDS-Terraform
terraform init
terraform destroy -var-file="../aws-session.tf"
terraform apply -var-file="../aws-session.tf"
```

## 2 - Launch Kafka cluster

From the root directory execute the following commands:
```bash
cd Kafka-Terraform
terraform init
terraform destroy -var-file="../aws-session.tf"
terraform apply -var-file="../aws-session.tf"
```

Go to your AWS dashboard and **reboot all created instances**.
Once they are rebooted, wait around 15s, and kafka should be working on the 
specified port at all brokers.

The broker's hostnames are printed on the terminal, so you can use those values to set 
the properties on the next section

## 3 - Launch Microservices
Set the following properties in the application.properties file in each microservice:
```yaml
quarkus.datasource.username=<your-username>
quarkus.datasource.password=<your-password>
quarkus.datasource.reactive.url=mysql://<your-hostname>:<your-port>/<your-database>
```


			