terraform {
  required_version = ">= 1.0.0, < 2.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_token" {}

provider "aws" {
  region = "us-east-1"
  access_key  = var.aws_access_key
  secret_key  = var.aws_secret_key
  token = var.aws_token
}

variable "db_username" {
  description = "The username for the database"
  type        = string
  sensitive   = true
  default     = "root"
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
  default     = "password"
}

variable "apilot_db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "apilot"
}

variable "car_db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "car"
}

variable "user_db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "user"
}

resource "aws_db_instance" "apilot" {
  identifier_prefix   = "apilot"
  engine              = "mysql"
  allocated_storage   = 10
  instance_class      = "db.t2.micro"
  skip_final_snapshot = true
  publicly_accessible = true
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_name             = var.apilot_db_name
  username = var.db_username
  password = var.db_password
}

resource "aws_db_instance" "car" {
  identifier_prefix   = "car"
  engine              = "mysql"
  allocated_storage   = 10
  instance_class      = "db.t2.micro"
  skip_final_snapshot = true
  publicly_accessible = true
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_name             = var.car_db_name
  username = var.db_username
  password = var.db_password
}

resource "aws_db_instance" "user" {
  identifier_prefix   = "user"
  engine              = "mysql"
  allocated_storage   = 10
  instance_class      = "db.t2.micro"
  skip_final_snapshot = true
  publicly_accessible = true
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_name             = var.user_db_name
  username = var.db_username
  password = var.db_password
}

resource "aws_security_group" "rds" {
  name = var.security_group_name
  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

variable "security_group_name" {
  description = "The name of the security group"
  type        = string
  default     = "terraform-rds-instance"
}


# DONT CHANGE -- Used in the scripts

output "dbUsername" {
  value       = nonsensitive(var.db_username)
  sensitive = false
}
output "dbPassword" {
  value       = nonsensitive(var.db_password)
  sensitive = false
}
output "dbHostAPILOT" {
  value       = aws_db_instance.apilot.address
}
output "dbHostCar" {
  value       = aws_db_instance.car.address
}
output "dbHostUser" {
  value       = aws_db_instance.user.address
}
output "dbAPILOTName" {
  value       = var.apilot_db_name
}
output "dbCarName" {
  value       = var.car_db_name
}
output "dbUserName" {
  value       = var.user_db_name
}