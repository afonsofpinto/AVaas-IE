terraform {
  required_version = ">= 1.0.0, < 2.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
  access_key  = "ASIAXB3NBADQVTLEQN6O"
  secret_key  = "bFvFEJdFRMFor1go0SROpqGK365tl+DoNUaZEf9b"
  token = "FwoGZXIvYXdzEEQaDL8RA97FSbkYSVZ8vyLHAX+xWflCDbu/vpDYwsvRFWO+aLCvgdWuxZT3ekcH+Q+RzNrgXG1EbSKm2Abj8TSKbdEl7XHeAdmRgrG8oXqTFD3Ya26suCbMVhXB68hheSbdds0Ah+8lTWTo0RAwAiPCwTDsnbhscf811ejC58Xhdk22zBqz0sSQCG0J/p6zT3atRAe1BUG0AVGrKVfgsio/Ao4pO0pbtMJMRVlillrMmBTvlcfa7sfulaSGLK7NoTAMvI7Buc5+K37wh4OOhXTFFOllDVFRCgIoo/aXowYyLRcOQ2tRDgLNqiYvf4Nr2TsFY3n4WvsqKaN7fVL9O6o4wneEBIaGPz8pw4Fhuw=="
}

variable "db_username" {
  description = "The username for the database"
  type        = string
  sensitive   = true
  default     = "afonsid"
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
  default     = "12345678"
}

variable "db_name" {
  description = "The name to use for the database"
  type        = string
  default     = "avaas"
}

resource "aws_db_instance" "example" {
  identifier_prefix   = "avaas"
  engine              = "mysql"
  allocated_storage   = 20
  instance_class      = "db.t2.micro"
  skip_final_snapshot = true
  publicly_accessible = true
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_name             = var.db_name
  username = var.db_username
  password = var.db_password
}

output "address" {
  value       = aws_db_instance.example.address
  description = "Connect to the database at this endpoint"
}

output "port" {
  value       = aws_db_instance.example.port
  description = "The port the database is listening on"
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