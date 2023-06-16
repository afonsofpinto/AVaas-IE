#!/bin/bash
cd
# will later be used to get the id of the current instance
idBroker=$(cat id.txt)


# --------- Zookeeper ---------
# get and setup files
sudo wget https://dlcdn.apache.org/zookeeper/zookeeper-3.8.0/apache-zookeeper-3.8.0-bin.tar.gz
sudo tar -zxf apache-zookeeper-3.8.0-bin.tar.gz
sudo mv apache-zookeeper-3.8.0-bin /usr/local/zookeeper
sudo mkdir -p /var/lib/zookeeper

# create config file
echo "tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
maxClientCnxns=60
initLimit=10
syncLimit=5
servers" > /usr/local/zookeeper/conf/zoo.cfg  # servers will later be replaced

# set zookeeper servers
string_with_new_lines=$(echo ${zookeeper_config} | sed 's|,|\\n|g') # replace commas with \n
sed -i "s/servers/$string_with_new_lines/g" "/usr/local/zookeeper/conf/zoo.cfg" # replace servers with multiline servers' hostnames

sudo yum -y install java-1.8.0-openjdk.x86_64 
sudo bash -c "echo $idBroker > /var/lib/zookeeper/myid"

# ---------- kafka ----------
# get and setup files
sudo wget https://archive.apache.org/dist/kafka/3.3.2/kafka_2.13-3.3.2.tgz
sudo tar -zxf kafka_2.13-3.3.2.tgz
sudo mv kafka_2.13-3.3.2 /usr/local/kafka
sudo mkdir /tmp/kafka-logs
ip=`curl http://169.254.169.254/latest/meta-data/public-hostname`

# set properties file
sudo sed -i "s/#listeners=PLAINTEXT:\/\/:9092/listeners=PLAINTEXT:\/\/$ip:9092/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/broker.id=0/broker.id=$idBroker/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/offsets.topic.replication.factor=1/offsets.topic.replication.factor=${totalBrokers}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/transaction.state.log.replication.factor=1/transaction.state.log.replication.factor=${totalBrokers}/g" /usr/local/kafka/config/server.properties
sudo sed -i "s/transaction.state.log.min.isr=1/transaction.state.log.min.isr=${totalBrokers}/g" /usr/local/kafka/config/server.properties
# set zookeeper servers in kafka
sudo sed -i "s/zookeeper.connect=localhost:2181/zookeeper.connect=${kafka_config}/g" /usr/local/kafka/config/server.properties

# ---------- reboot & startup ----------
# start zookeeper on boot
sudo bash -c 'echo "sudo /usr/local/zookeeper/bin/zkServer.sh start" >> /etc/rc.local'
# start kafka on boot
sudo bash -c 'echo "wait 15" >> /etc/rc.local'
sudo bash -c 'echo "(sudo /usr/local/kafka/bin/kafka-server-start.sh -daemon /usr/local/kafka/config/server.properties) &" >> /etc/rc.local'
# set necessary kafka topics on boot (only one process does this)
if [ $idBroker -eq 1 ]; then
  sudo bash -c 'echo "sudo /usr/local/kafka/bin/kafka-topics.sh --create --bootstrap-server ${broker1}:9092 -replication-factor ${totalBrokers} --partitions ${totalBrokers*2} --topic vehicle-txns" >> /etc/rc.local'
  sudo bash -c 'echo "sudo /usr/local/kafka/bin/kafka-topics.sh --create --bootstrap-server ${broker1}:9092 -replication-factor ${totalBrokers} --partitions ${totalBrokers*2} --topic apilot-txns" >> /etc/rc.local'
  sudo bash -c 'echo "sudo /usr/local/kafka/bin/kafka-topics.sh --create --bootstrap-server ${broker1}:9092 -replication-factor ${totalBrokers} --partitions ${totalBrokers*2} --topic av-events" >> /etc/rc.local'
fi
sudo chmod +x /etc/rc.local   # make it executable
sudo systemctl enable rc-local.service
sudo systemctl start  rc-local.service
rm id.txt # delete tmp file for the id