dockerUsername=$1
dockerPswd=$2
dockerArtifactID=$3
targetScript=$4

echo "#!/bin/bash
echo 'Starting...'
sudo yum install -y docker
sudo service docker start
sudo docker login -u '$dockerUsername' -p '$dockerPswd'
sudo docker pull $dockerUsername/$dockerArtifactID:1.0.0-SNAPSHOT
sudo docker run -d --name tryout2 -p 8080:8080 $dockerUsername/$dockerArtifactID:1.0.0-SNAPSHOT
echo Finished." > $targetScript