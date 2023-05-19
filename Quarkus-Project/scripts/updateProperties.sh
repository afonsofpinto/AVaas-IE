path=$1
dockerUsername=$2
dbUsername=$3
dbPassword=$4
dbHostname=$5

sed -i "/^quarkus.datasource.username=/ s/.*/quarkus.datasource.username=$dbUsername/" $path
sed -i "/^quarkus.datasource.password=/ s/.*/quarkus.datasource.password=$dbPassword/" $path
sed -i "/^quarkus.datasource.reactive.url=/ s/.*/quarkus.datasource.reactive.url=$dbHostname/" $path
sed -i "/^quarkus.container-image.group=/ s/.*/quarkus.container-image.group=$dockerUsername/" $path