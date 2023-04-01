package com.pankaj.demos.kafka.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.client.*;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;


public class OpenSearchConsumer {
    public static RestHighLevelClient createOpenSearchClient(){
        String connString = "http://localhost:9200";
        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connURI = URI.create(connString);
        String userInfo = connURI.getUserInfo();

        if (userInfo == null) {
            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(
                                    connURI.getHost(),
                                    connURI.getPort(),
                                    connURI.getScheme())));
        } else {
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(
                            new HttpHost(connURI.getHost(), connURI.getPort(), connURI.getScheme()))
                    .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
        }
        return restHighLevelClient;
    }
    private static KafkaConsumer<String, String> createKafkaConsumer() {
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "consumer-opensearch-demo";

        // create consumer config
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");

        //create consumer
        return new KafkaConsumer<String, String>(properties);
    }
    public static void main(String[] args) throws IOException {
        Logger log = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

        // first create opensearch client
        RestHighLevelClient openSearchClient = createOpenSearchClient();
        // create kafka client
        KafkaConsumer<String, String> consumer = createKafkaConsumer();
        // we need to create the index on OpenSearch if it doesn't already exist
        try(openSearchClient;consumer){
            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);
            if(!indexExists){
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("Wikimedia Index has been created.");
            }else{
                log.info("Wikimedia index already exists");
            }

        }





        // main code logic

        // close things

    }


}
