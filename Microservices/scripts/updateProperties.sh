path=$1
dockerUsername=$2
dbUsername=$3
dbPassword=$4
dbHostname=$5
kafkaBrokers=$6

# Update database properties
sed -i "/^quarkus.datasource.username=/ s/.*/quarkus.datasource.username=$dbUsername/" $path
sed -i "/^quarkus.datasource.password=/ s/.*/quarkus.datasource.password=$dbPassword/" $path
# escape "/" in database hostname
escapedDbHostname=$(echo "$dbHostname" | sed 's/\//\\\//g') # escape slashes
sed -i "/^quarkus\.datasource\.reactive.url=/ s/.*/quarkus.datasource.reactive.url=$escapedDbHostname:3306/" $path

# Update docker properties
sed -i "/^quarkus\.container-image\.group=/ s/.*/quarkus.container-image.group=$dockerUsername/" $path

# Update kafka brokers
sed -i "/^kafka\.bootstrap\.servers=/ s/.*/kafka.bootstrap.servers=$kafkaBrokers/" $path