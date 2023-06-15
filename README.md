# AVaas-IE

The autonomous vehicle as a service (AVaaS) is a cloudbased system that aims at the integration between car manufacturers, autopilot developers, vehicle users, and other external service providers to provide value added services of intelligence quotient (IQ), ethical quotient (EQ), and adversity quotient (AQ)1. IQ, EQ and AQ are symbolical ones. All of this services will be developed here.

# 1 - Auto deployment steps
1.1 - Customize the following fields from the ```aws_session.tf``` file:
```Terraform
aws_access_key = "YOUR KEY"
aws_secret_key = "YOUR KEY"
aws_token = "YOUR TOKEN"
aws_keyname = "YOUR .pem KEY FILE NAME"
```
Also set the key file name in the ```deploy_automated.sh``` script

1.2 - Place the .pem key file in the root directory and set permissions:

**Windows**: ```icacls <keyfile>.pem /inheritance:r /grant:r "<username>:(RX)"```, where username is the output of running the command ```whoami```

**Linux/Unix OS**: ```chmod u=rwx,g=,o= <keyfile>.pem```

Set you docker account in the ```Microservices/deploy_microservices.sh``` script:
```bash
# docker
dockerUsername="ietest"
dockerPassword="123456789"
```

Run the auto deployment script
```
bash deploy_automated.sh
```

# 2 - Manual deployment (debug)
## 2.1 - Set aws access information and keys
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


## 2.2 - Launch the databases [RDS-Terraform]

(Optional) Set the Username and Password for the databases in the RDS .tf file: 
		
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
You can also change the db name in the variables on that file (not required)

Then run the following commands:
```bash
cd RDS-Terraform
bash deploy_rds.sh
```
Save the output somewhere (it will be used in point **4.**)


## 2.3 - Launch Kafka cluster
#### !IMPORTANT! - Use LF as the line terminator on all scripts. Else some scripts will not execute in the AWS instances!
From the root directory execute the following commands:
```bash
cd Kafka-Terraform
deploy_kafka.sh
```

Go to your AWS dashboard and **reboot all created instances**.
Once they are rebooted, wait around 15s, and kafka should be working on the 
specified port at all brokers.

The broker's hostnames are printed on the terminal, so you can use those values to set 
the properties on the next section


## 2.4 - Launch Microservices + Camunda + Kong

Edit the ```Microservices/deploy_microservices.sh``` file. Namely, copy the 
output of 2. (the databases info) and paste them here, and edit other fields:  
**WARNING** - the output comes with spaces between the variable, the equals sign and the value. Make sure
to delete the spaces to be exactly as it is below!
```bash
# docker
dockerUsername="dockerusername"
dockerPassword="dockerpassword"

# CHANGE ONLY if you changed the default database names/username/password
dbAPILOTName="apilot"
dbCarName="car"
dbPassword="password"
dbUserName="user"
dbUsername="root"
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


## 2.5 - Launch AVaaSSimulator
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

## 2.6 - Deploy Camunda engine
Run:
```
cd Camunda-Terraform
bash deploy_camunda.sh
```

## 2.7 - Deploy Kong
Run:
```
cd Kong-Terraform
bash deploy_kong.sh
```


## 2.8 - Consume from kafka
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


# 3 - BPMN
To run the bpmn processes, you will need to set the hostname of every existing connectors of
every automated task.
### BPMN Use cases configuration
#### B1 - User subscribing/unsubscribing
You need to **pass an initial variable** to the process: 
- Name: kong, 
- Type: string
- Value: <kong-hostname>

#### B2 - Car manufacturer entering/updating/removing cars
You need to **pass an initial variable** to the process:
- Name: kong,
- Type: string
- Value: <kong-hostname>