terraform {
  required_version = ">= 1.0.0, < 2.0.0"

  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = "~> 4.0"
    }
    null = {
      source = "hashicorp/null"
      version = "3.1.0"
    }
  }
}

variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_token" {}
variable "aws_keyname" {}

# changes every tim e new lab session is started
provider "aws" {
  region = "us-east-1"
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
  token = var.aws_token
}



resource "aws_instance" "apilot" {
  ami = "ami-06a0cd9728546d178" # Amazon Linux x86 CHANGE THIS
  instance_type = "t2.medium"
  vpc_security_group_ids = [aws_security_group.microservice.id]
  key_name = "vockey"
  user_data = "${file("./scripts/apilot.sh")}"
  user_data_replace_on_change = true

  tags = {
  Name = "APILOT-microservice"
  }
}

resource "aws_instance" "car" {
  ami = "ami-06a0cd9728546d178" # Amazon Linux x86 CHANGE THIS
  instance_type = "t2.medium"
  vpc_security_group_ids = [aws_security_group.microservice.id]
  key_name = "vockey"
  user_data = "${file("./scripts/car.sh")}"
  user_data_replace_on_change = true

  tags = {
    Name = "Car-microservice"
  }
}

resource "aws_instance" "user" {
  ami = "ami-06a0cd9728546d178" # Amazon Linux x86 CHANGE THIS
  instance_type = "t2.medium"
  vpc_security_group_ids = [aws_security_group.microservice.id]
  key_name = "vockey"
  user_data = "${file("./scripts/user.sh")}"
  user_data_replace_on_change = true

  tags = {
    Name = "User-microservice"
  }
}

# TODO deploy other microservices

resource "aws_security_group" "microservice" {
  name = var.security_group_name
  ingress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

variable "security_group_name" {
  description = "The name of the security group"
  type = string
  default = "terraform-Quarkus-instance"
}


output "apilot_microservice" {
  value = aws_instance.apilot.public_dns
  description = "Address of the Quarkus EC2 machine"
}

output "car_microservice" {
  value = aws_instance.car.public_dns
  description = "Address of the Quarkus EC2 machine"
}

output "user_microservice" {
  value = aws_instance.user.public_dns
  description = "Address of the Quarkus EC2 machine"
}


