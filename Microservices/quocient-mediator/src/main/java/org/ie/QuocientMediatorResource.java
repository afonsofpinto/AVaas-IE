package org.ie;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import org.json.JSONObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Path("/get-events")
public class QuocientMediatorResource {
    Producer<String, String> producer;
    KafkaConsumer<String, String> consumer;
    AdminClient adminClient;
    Set<String> subscribedTopics = new HashSet<>();
    Map<String, JSONObject> events = new HashMap<>();

    @Inject
    public QuocientMediatorResource() {
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

        consumer = new KafkaConsumer<>(properties);
        producer = new KafkaProducer<>(properties);
        adminClient = AdminClient.create(properties);
    }

    @POST
    public void produceToTopic() {

    }

    @GET
    public String consumeEvent() {
        Pattern topicPattern = Pattern.compile("APILOT");
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
                    saveEvent(record.value());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return getEvent();
    }

    private String getEvent() {
        try {
            List<Map.Entry<String, JSONObject>> entryList = new ArrayList<>(events.entrySet());
            Random random = new Random();
            int randomIndex = random.nextInt(entryList.size());
            Map.Entry<String, JSONObject> randomEntry = entryList.get(randomIndex);
            events.remove(randomEntry.getKey());
            return randomEntry.getValue().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    private void saveEvent(String event) {
        JSONObject jsonEvent = new JSONObject(event);
        JSONObject jsonEventDetails;
        if (jsonEvent.has("APILOT_Event")) {
            jsonEventDetails = jsonEvent.getJSONObject("APILOT_Event");
        } else jsonEventDetails = jsonEvent.getJSONObject("APILOT_action");

        String timestamp = jsonEventDetails.get("TimeStamp").toString();

        if (!events.containsKey(timestamp)) { // store event
            events.put(timestamp, jsonEventDetails);
        }
        else { // update stored event
            JSONObject storedEvent = events.get(timestamp);
            for (String key : jsonEventDetails.keySet()) {
                storedEvent.put(key, jsonEventDetails.get(key));
            }
            events.put(timestamp, storedEvent);
        }
    }
}
