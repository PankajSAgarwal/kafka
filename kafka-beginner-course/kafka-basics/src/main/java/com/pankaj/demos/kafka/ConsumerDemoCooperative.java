package com.pankaj.demos.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {
    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());
    public static void main(String[] args) {
        log.info("I am a kafka consumer");

        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-third-application";
        String topic = "demo_java";

        // create consumer config
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());

        //create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        // get a reference to current thread
        final Thread mainThread = Thread.currentThread();

        // add the shutdownhook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Detected a shutdown, let's exit by calling consumer.wakeup() ... ");
            consumer.wakeup();
            // join the main thread to allow execution of the code in the main thread
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try{
            // subscribe consumer to our topics
            consumer.subscribe(Arrays.asList(topic));
            //consumer.subscribe(Collections.singletonList(topic));

            // poll for new data
            while (true){
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Key: " + record.key() + ", Value: " + record.value());
                    log.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }

            }
        }catch (WakeupException e){
            log.info("Wake Up Exception!");
        }catch(Exception e){
            log.error("Unexpected Exception !!");
        } finally {
            consumer.close(); // this will also commit the offset if need be
            log.info("The consumer is now gracefully closed");

        }


    }
}
