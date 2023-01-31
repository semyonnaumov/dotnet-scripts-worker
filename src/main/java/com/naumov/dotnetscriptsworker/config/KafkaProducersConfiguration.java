package com.naumov.dotnetscriptsworker.config;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducersConfiguration {
    private final KafkaProperties kafkaProperties;

    @Autowired
    public KafkaProducersConfiguration(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public Map<String, Object> commonKafkaProducerProperties() {
        var props = new HashMap<String, Object>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducerAcks());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getReconnectBackoffMs());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getReconnectBackoffMaxMs());

        return props;
    }

    @Bean
    public ProducerFactory<String, JobStartedDto> jobStartedMessagesProducerFactory() {
        var producerFactory = new DefaultKafkaProducerFactory<String, JobStartedDto>(commonKafkaProducerProperties());
        producerFactory.setProducerPerThread(true);

        return producerFactory;
    }

    @Bean
    public ProducerFactory<String, JobFinishedDto> jobFinishedMessagesProducerFactory() {
        var producerFactory = new DefaultKafkaProducerFactory<String, JobFinishedDto>(commonKafkaProducerProperties());
        producerFactory.setProducerPerThread(true);

        return producerFactory;
    }

    @Bean
    public KafkaTemplate<String, JobStartedDto> jobStartedMessagesKafkaTemplate() {
        return new KafkaTemplate<>(jobStartedMessagesProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, JobFinishedDto> jobFinishedMessagesKafkaTemplate() {
        return new KafkaTemplate<>(jobFinishedMessagesProducerFactory());
    }
}
