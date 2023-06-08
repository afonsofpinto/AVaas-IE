# CHANGE -----------
# docker
dockerUsername="ietest"
dockerPassword="123456789"

# paste here the output from RDS database terraform
dbAPILOTName="apilot"
dbCarName="car"
dbHostAPILOT="apilot20230608151020679600000003.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbHostCar="car20230608151020672500000001.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbHostUser="user20230608151020673000000002.chphl7tm5mbk.us-east-1.rds.amazonaws.com"
dbPassword="password"
dbUserName="user"
dbUsername="root"



# kafka brokers
kafkaBrokers="ec2-44-201-90-72.compute-1.amazonaws.com:9092,ec2-54-163-208-13.compute-1.amazonaws.com:9092,ec2-44-203-179-175.compute-1.amazonaws.com:9092"


# -- DO NOT CHANGE -----------------------
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
terraform destroy -var-file="../aws-session.tf"
terraform apply -var-file="../aws-session.tf"
