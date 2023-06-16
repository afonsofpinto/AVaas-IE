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

public class APILOTMediator {
     Producer<String, String> producer;
     KafkaConsumer<String, String> consumer;
     AdminClient adminClient;

     Set<String> subscribedTopics = new HashSet<>();

     public APILOTMediator() {
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

     public void filterEvents() throws IOException {
        Pattern topicPattern = Pattern.compile("AV.*");


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
                            parseEvent(record.value(), record.topic());
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
        String topicToSend = topic.replaceFirst("AV", "APILOT");
        sendMessage(processedEvent, topicToSend);
    }

    private JSONObject processEvent(String event) {
        JSONObject jsonEvent = new JSONObject(event);
        JSONObject jsonEventDetails = jsonEvent.getJSONObject("AV_Event");
        // Remove unused fields
        jsonEventDetails.remove("AverageConsumptionLevel");
        jsonEventDetails.remove("Location");
        jsonEventDetails.remove("TractionWheelsLevel");
        // Update the event type property
        jsonEvent.put("APILOT_Event", jsonEventDetails);
        jsonEvent.remove("AV_Event");
        return jsonEvent;
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
