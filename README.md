# AVaas-IE

The autonomous vehicle as a service (AVaaS) is a cloudbased system that aims at the integration between car manufacturers, autopilot developers, vehicle users, and other external service providers to provide value added services of intelligence quotient (IQ), ethical quotient (EQ), and adversity quotient (AQ)1. IQ, EQ and AQ are symbolical ones. All of this services will be developed here.

# RDS-Terraform

 Parameters to update: 
		
```
provider "aws"{
 region = "us-east-1"
 access_key  = "YOUR ACCESS KEY"
 secret_key  = "YOUR SECRET KEY"
 token = "YOUR TOKEN"
}

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


			