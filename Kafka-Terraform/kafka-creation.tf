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

variable "nBroker" {
  description = "number of brokers"
  type = number
  default = 3
}

variable "zookeeper_kafka_port" {
  description = "Port of zookeeper that kafka uses"
  type = number
  default = 2181
}

variable "zookeeper_zookeeper_port" {
  description = "Port of zookeeper that kafka uses"
  type = number
  default = 2888
}

variable "zookeeper_leader_port" {
  description = "Port of zookeeper that kafka uses"
  type = number
  default = 3888
}

variable "kafka_port" {
  description = "Port of kafka"
  type = number
  default = 9092
}

variable "security_group_name" {
  description = "The name of the security group"
  type = string
  default = "kafka-group"
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

resource "aws_instance" "exampleCluster" {
  ami = "ami-06a0cd9728546d178"
  instance_type = "t2.small"
  count = var.nBroker
  vpc_security_group_ids = [aws_security_group.instance.id]
  key_name = "vockey"

  tags = {
    Name = "kafka-${count.index}"
  }
}

# list of hostnames for zookeeper & kafka config, comma separated
locals {
  instance_hostnames = [for i in range(var.nBroker) : element(aws_instance.exampleCluster, i).public_dns]
  kafka_config = join(",", [for i in range(var.nBroker) : "${local.instance_hostnames[i]}:${var.zookeeper_kafka_port}"])
  kafka_brokers = join(",", [for i in range(var.nBroker) : "${local.instance_hostnames[i]}:${var.kafka_port}"])
  zookeeper_config = join(",", [for i in range(var.nBroker) : "server.${i+1}=${local.instance_hostnames[i]}:${var.zookeeper_zookeeper_port}:${var.zookeeper_leader_port}"])
}

# Config script template
data "template_file" "config_script" {
    depends_on = [aws_instance.exampleCluster]
    template = file("${path.module}/config.sh")

    vars = {
        totalBrokers = var.nBroker
        kafka_config = local.kafka_config
        zookeeper_config = local.zookeeper_config
        broker1 = local.instance_hostnames[0]
    }
}

# run the script commands in all instances AFTER all are created
resource "null_resource" "updateConfigs" {
  depends_on = [ aws_instance.exampleCluster ]
  count = "${var.nBroker}"

  connection {
    host        = local.instance_hostnames[count.index]
    type        = "ssh"
    user        = "ec2-user"
    private_key = file("../${var.aws_keyname}")
  }

  provisioner "remote-exec" {
    inline = [
      "cd ; echo ${count.index + 1} > id.txt",
      data.template_file.config_script.rendered,
    ]
  }
}


output "instance_hostnames" {
  value = local.instance_hostnames
}

output "kafka_brokers" {
  value = local.kafka_brokers
}

resource "aws_security_group" "instance" {
  name = var.security_group_name
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = var.zookeeper_kafka_port
    to_port = var.zookeeper_kafka_port
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = var.zookeeper_zookeeper_port
    to_port = var.zookeeper_zookeeper_port
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = var.zookeeper_leader_port
    to_port = var.zookeeper_leader_port
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 9092
    to_port = 9092
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

