dockerUsername="ietest"
dockerPassword="123456789"

dbUsername="root"
dbPassword="password"
dbHostAPILOT="localhost:3036"
dbHostCar=""
dbHostUser=""


propertiesPath="src/main/resources/application.properties"

bash ./scripts/updateProperties.sh "APILOT/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$dbHostAPILOT"
#bash ./scripts/updateProperties.sh "car/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$dbHostAPILOT"
#bash ./scripts/updateProperties.sh "user/$propertiesPath" "$dockerUsername" "$dbUsername" "$dbPassword" "$dbHostAPILOT"

docker login -u "$dockerUsername" -p "$dockerPassword"

bash ./scripts/buildContainers.sh

# build scripts for each microservice to execute when deployed
bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "apilot" "./scripts/apilot.sh"
#bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "car" "./scripts/car.sh"
#bash ./scripts/microservice_builder.sh "$dockerUsername" "$dockerPassword" "user" "./scripts/user.sh"

# deploy microservices
terraform init
terraform destroy -var-file="../aws-session.tf"
terraform apply -var-file="../aws-session.tf"


