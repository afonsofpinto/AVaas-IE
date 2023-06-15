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

resource "aws_instance" "Kong" {
  ami                     = "ami-06a0cd9728546d178"
  instance_type           = "t2.small"
  vpc_security_group_ids  = [aws_security_group.instance.id]
  key_name                = "vockey"
  user_data = file("deploy.sh")
  user_data_replace_on_change = true

  tags = {
    Name = "Kong"
  }
}

resource "aws_security_group" "instance" {
  name = var.security_group_name
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
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
  default     = "terraform-kong-instance5"
}

resource "local_file" "kong_host" {
  filename = "${path.module}/kong_addr.tmp"
  content  = aws_instance.Kong.public_dns
}


