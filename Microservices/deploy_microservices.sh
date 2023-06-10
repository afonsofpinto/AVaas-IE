# CHANGE -----------
# docker
dockerUsername="ietest"
dockerPassword="123456789"

# (optional)
dbAPILOTName="apilot"
dbCarName="car"
dbPassword="password"
dbUserName="user"
dbUsername="root"


# -- DO NOT CHANGE -----------------------
# kafka brokers
kafkaBrokers=$(cat ../Kafka-Terraform/kafka_brokers.tmp)

dbHostAPILOT=$(cat ../RDS-Terraform/apilot_addr.tmp)
dbHostCar=$(cat ../RDS-Terraform/car_addr.tmp)
dbHostUser=$(cat ../RDS-Terraform/user_addr.tmp)

propertiesPath="src/main/resources/application.properties"

full_db_hostname() {
  local hostnamePort="$1"
  local dbName="$2"
  echo "mysql://$hostnamePort/$dbName?verifyServerCertificate=false&useSSL=false"
}

# Update application.properties file on each microservice
bash ./scripts/updateProperties.sh "APILOT/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$(full_db_hostname $dbHostAPILOT $dbAPILOTName)"
bash ./scripts/updateProperties.sh "Car/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$(full_db_hostname $dbHostCar $dbCarName)"
bash ./scripts/updateProperties.sh "User/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$(full_db_hostname $dbHostUser $dbUserName)" "$kafkaBrokers"

docker login -u "$dockerUsername" -p "$dockerPassword"

bash ./scripts/buildContainers.sh

# build scripts for each microservice to execute when deployed
bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "apilot" "./scripts/apilot.sh"
bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "car" "./scripts/car.sh"
bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "user" "./scripts/user.sh"

# deploy microservices
terraform init
terraform apply -var-file="../aws-session.tf" -auto-approve
