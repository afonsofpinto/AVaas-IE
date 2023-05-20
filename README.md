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
```
Besides the username and password, you can also change the db name in the variables on that file (not required)

Then run the following commands:
```bash
cd RDS-Terraform
terraform init
terraform destroy -var-file="../aws-session.tf"
terraform apply -var-file="../aws-session.tf"
```
Save the output somewhere (it will be used in point **4.**)


## 3 - Launch Kafka cluster

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


## 4 - Launch Microservices

Edit the ```Microservices/deploy_microservices.sh``` file. Namely, copy the 
output of 2. (the databases info) and paste them here, and edit other fields:  
**WARNING** - the output comes with spaces between the variable, the equals sign and the value. Make sure
to delete the spaces to be exactly as it is below!
```bash
# docker
dockerUsername="dockerusername"
dockerPassword="dockerpassword"

# paste here the output from RDS database terraform
dbAPILOTName="apilot"
dbCarName="car"
dbHostAPILOT="apilot20230520112708447400000003.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbHostCar="car20230520112708446300000001.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbHostUser="user20230520112708446800000002.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbPassword="password"
dbUserName="user"
dbUsername="root"

# kafka
kafkaBrokers="ec2-18-233-8-92.compute-1.amazonaws.com:9092,ec2-18-206-245-148.compute-1.amazonaws.com:9092,ec2-54-173-82-93.compute-1.amazonaws.com:9092"
```
For the kafka brokers you can copy the output from **3.**

**IMPORTANT** - depending on your system's architecture, you may need to change the instance type
as well as the AMI in ```Microservices/create-microservices.tf```:
```Terraform
resource "aws_instance" "car" {
  ami = "ami-06a0cd9728546d178" # Amazon Linux x86 MAY NEED TO CHANGE THIS
  instance_type = "t2.medium"   # MAY NEED TO CHANGE THIS
  vpc_security_group_ids = [aws_security_group.microservice.id]
  key_name = "vockey"
  user_data = "${file("./scripts/car.sh")}"
  user_data_replace_on_change = true

  tags = {
    Name = "Car-microservice"
  }
}
```

Run the deployment script:
```
bash deploy_microservices.sh
```
Wait about 20s for the instances to install all the dependencies, and run the microservice.
Now you should be able to access each microservice in the following 
path: ```http://<microservice_hostname>:8080/q/swagger-ui```.  
You should replace ```<microservice_hostname>``` with the hostnames from the output of the
terraform file after running the script.
The microservices now should be connected to the respective databases
and kafka.


## 5 - Launch AVaaSSimulator
Copy the kafka_brokers variable from the output of **3.** and put them in the pom.xml:
```Xml
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
        <configuration>
          <mainClass>AVaaSMessageProvider</mainClass> 
          <arguments>
            <argument>--broker-list</argument>
            <argument>!!PASTE kafka_brokers HERE!!</argument> <!-- Replace kafka brokers here -->
            <argument>--filterprefix</argument>
            <argument>av-events</argument>
            <!-- Add more arguments as needed -->
          </arguments>
        </configuration>
      </plugin>
```
Then run the application with:
```
mvn exec:java
```

## 6 - Consume from kafka
To consume the events generated from either the AVaaSSimulator or the user microservice,
login into one of the microservices:
```bash
ssh -i keyfile.pem ec2-user@microservice-hostname
```
And issue the following command:
```bash
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server <microservice-hostname>:9092 --topic <topic> --from-beginning
```
Where **topic** can be one of the following:
- **av-events** (where AVaaSSimulator produces to)
- **vehicle-txns** (where User microservice produces to)
- **apilot-txns** (where User microservice produces to)
			