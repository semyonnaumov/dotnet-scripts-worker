package com.naumov.dotnetscriptsworker.config;

import com.naumov.dotnetscriptsworker.config.props.WorkerKafkaProperties;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.net.SocketTimeoutException;

@Configuration
public class KafkaConsumerConfiguration implements KafkaListenerConfigurer {
    private final WorkerKafkaProperties kafkaProperties;
    private final LocalValidatorFactoryBean validator;

    @Autowired
    public KafkaConsumerConfiguration(WorkerKafkaProperties kafkaProperties, LocalValidatorFactoryBean validator) {
        this.kafkaProperties = kafkaProperties;
        this.validator = validator;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public KafkaPropertyMapWrapper commonConsumerProperties() {
        var props = new KafkaPropertyMapWrapper();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getReconnectBackoffMs());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getReconnectBackoffMaxMs());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getAutoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.naumov.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return props;
    }

    @Bean
    public ConsumerFactory<String, JobTaskMessage> jobsConsumerFactory() {
        KafkaPropertyMapWrapper props = commonConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumerGroup());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, JobTaskMessage.class.getName());

        return new DefaultKafkaConsumerFactory<>(props.toMap());
    }

    @Bean
    public DefaultErrorHandler commonErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(100L, 1L));
        errorHandler.addRetryableExceptions(SocketTimeoutException.class);
        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobTaskMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, JobTaskMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(jobsConsumerFactory());
        factory.setConcurrency(kafkaProperties.getConsumerConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(commonErrorHandler());

        return factory;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar registrar) {
        registrar.setValidator(this.validator);
    }
}
