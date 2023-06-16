package org.ie;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class APILOTSimulator {
    Properties properties;
    Producer<String, String> producer;
    KafkaConsumer<String, String> consumer;
    AdminClient adminClient;

    Set<String> subscribedTopics = new HashSet<>();

    public APILOTSimulator() {
        // Read properties from properties file
        Properties prop = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Properties properties = new Properties();
        properties.put("bootstrap.servers", prop.getProperty("kafka.bootstrap.servers"));
        properties.put("key.deserializer", prop.getProperty("kafka.key.deserializer"));
        properties.put("value.deserializer", prop.getProperty("kafka.value.deserializer"));
        properties.put("key.serializer", prop.getProperty("kafka.key.serializer"));
        properties.put("value.serializer", prop.getProperty("kafka.value.serializer"));
        properties.put("group.id", prop.getProperty("kafka.group.id"));


        producer = new KafkaProducer<>(properties);
        adminClient = AdminClient.create(properties);
        consumer = new KafkaConsumer<>(properties);
    }

    public void start() throws IOException {
        Pattern topicPattern = Pattern.compile("APILOT_.*");


        try {
            while (true) {
                try {
                    ListTopicsOptions options = new ListTopicsOptions().listInternal(false);
                    ListTopicsResult topicsResult = adminClient.listTopics(options);

                    for(TopicListing topicListing : topicsResult.listings().get()) {
                        String topicName = topicListing.name();
                        if (!topicPattern.matcher(topicName).matches()) continue;
                        if (!subscribedTopics.contains(topicName)) {
                            consumer.subscribe(Collections.singletonList(topicName));
                            subscribedTopics.add(topicName);
                        }
                        ConsumerRecords<String, String> records = consumer.poll(100);
                        for (ConsumerRecord<String, String> record : records) {
                            System.out.println("Consuming from topic: " + topicName);
                            parseEvent(record.value(), topicName);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            consumer.close();
            producer.close();
        }
    }

    private void parseEvent(String jsonString, String topic) {
        JSONObject processedEvent = processEvent(jsonString);
        if (processedEvent != null) sendMessage(processedEvent, topic);
    }

    private JSONObject processEvent(String event) {
        // old event
        JSONObject jsonEvent = new JSONObject(event);
        System.out.println("Event: " + jsonEvent.toString());
        if (!jsonEvent.has("APILOT_Event")) return null; // we only parse APILOT_Event events
        JSONObject jsonEventDetails = jsonEvent.getJSONObject("APILOT_Event");
        // new event
        JSONObject simulationEvent = new JSONObject();
        JSONObject simulationDetails = new JSONObject();
        // ID and Timestamp of the events are preserved
        simulationDetails.put("AV_ID", jsonEventDetails.get("AV_ID"));
        simulationDetails.put("TimeStamp", jsonEventDetails.get("TimeStamp"));
        // transform events
        // fog
        String fog = jsonEventDetails.get("FogConditions").toString();
        if (fog.equals("Light Fog") || fog.equals("N/A")) {
            simulationDetails.put("Max Velocity", "50");
            simulationDetails.put("Lights", "Minimum");
        } else if (fog.equals("Medium Fog") || fog.equals("Heavy Fog")) {
            simulationDetails.put("Max Velocity", "40");
            simulationDetails.put("Lights", "Medium");
        }
        // rain
        String rain = jsonEventDetails.get("RainConditions").toString();
        if (rain.equals("Light Rain") || rain.equals("N/A")) {
            simulationDetails.put("Max Velocity", "50");
            simulationDetails.put("Lights", "Minimum");
        } else if (rain.equals("Medium Rain") || rain.equals("Heavy Rain")) {
            simulationDetails.put("Max Velocity", "40");
            simulationDetails.put("Lights", "Medium");
        }
        // lighting
        String lighting = jsonEventDetails.get("EnvironmentalLightning").toString();
        if (lighting.equals("Bad") || lighting.equals("Sufficient")) {
            simulationDetails.put("Max Velocity", "50");
            simulationDetails.put("Lights", "Medium");
        }
        // speed
        int speed = Integer.parseInt(jsonEventDetails.get("Speed").toString());
        if (speed > 60) {
            simulationDetails.put("Max Velocity", "60");
            simulationDetails.put("Velocity", "Decrease");
            simulationDetails.put("Warning", "Max velocity reached");
        }
        // battery level
        if (Integer.parseInt(jsonEventDetails.get("BatteryLevel").toString()) <= 15) {
            simulationDetails.put("Max Velocity", "40");
            simulationDetails.put("Battery saving", "Activate");
            simulationDetails.put("Alarm sound", "very soft");
            simulationDetails.put("Warning", "Battery Saving mode activated");
        }
        // tireness level
        if (Integer.parseInt(jsonEventDetails.get("DriverTirenessLevel").toString()) > 75) {
            simulationDetails.put("Max Velocity", "40");
            simulationDetails.put("Alarm sound", "soft");
            simulationDetails.put("Warning", "Go stop to take a coffee");
        }
        // traffic light
        String trafficLight = jsonEventDetails.get("TrafficLight").toString();
        if ( trafficLight.equals("Red")) {
            simulationDetails.put("Max Velocity", "0");
            simulationDetails.put("Velocity", "Stop");
        }
        else if (trafficLight.equals("Yellow")) {
            simulationDetails.put("Velocity", "Decrease");
        }
        // obstacle proximity
        int obstacleProx = Integer.parseInt(jsonEventDetails.get("ObstacleProximity").toString());
        if (obstacleProx <= 15) {
            simulationDetails.put("Max Velocity", "10");
            simulationDetails.put("Velocity", "Decrease");
            simulationDetails.put("Alarm sound", "medium");
            simulationDetails.put("Warning", "Collision warning");
        }
        if (obstacleProx <= 30 && speed > 50) {
            simulationDetails.put("Max Velocity", "15");
            simulationDetails.put("Velocity", "Decrease");
            simulationDetails.put("Alarm sound", "soft");
            simulationDetails.put("Warning", "Collision warning");
        }
        // obstacle proximity
        int pedestrianProx = Integer.parseInt(jsonEventDetails.get("PedestrianProximity").toString());
        if (pedestrianProx <= 15) {
            simulationDetails.put("Max Velocity", "0");
            simulationDetails.put("Velocity", "Stop");
            simulationDetails.put("Alarm sound", "strong");
            simulationDetails.put("Warning", "Collision warning");
        }
        if (pedestrianProx <= 30 && speed > 50) {
            simulationDetails.put("Max Velocity", "15");
            simulationDetails.put("Velocity", "Decrease");
            simulationDetails.put("Alarm sound", "medium");
            simulationDetails.put("Warning", "Collision warning");
        }
        simulationEvent.put("APILOT_action", simulationDetails);
        return simulationEvent;
    }

    private void sendMessage(JSONObject processedMessage , String topicTarget) {
        System.out.println("This is the message to send = " + processedMessage.toString());
        String seqkey = topicTarget + "_" + String.valueOf( ((Double) (Math.random() * 10)).intValue() );
        System.out.println("Sending new message to Kafka, to the topic = " + topicTarget + ", with key=" + seqkey);
        ProducerRecord<String, String> record = new ProducerRecord<>(topicTarget, seqkey, processedMessage.toString());
        producer.send(record);
        System.out.println("Sent...");
    }
}
