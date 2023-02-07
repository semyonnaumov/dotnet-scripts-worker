package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobTaskFromDtoMapper;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JobTaskConsumer {
    private static final Logger LOGGER = LogManager.getLogger(JobTaskConsumer.class);
    private final KafkaProperties kafkaProperties;
    private final JobService jobService;
    private final JobTaskFromDtoMapper jobTaskFromDtoMapper;

    @Autowired
    public JobTaskConsumer(JobService jobService,
                           KafkaProperties kafkaProperties,
                           JobTaskFromDtoMapper jobTaskFromDtoMapper) {
        this.jobService = jobService;
        this.kafkaProperties = kafkaProperties;
        this.jobTaskFromDtoMapper = jobTaskFromDtoMapper;
    }


    // TODO add exception handling for broken messages (cannot deserialize/invalid)
    @KafkaListener(
            topics = "${worker.kafka.jobs-topic-name}",
            containerFactory = "kafkaListenerContainerFactory"
//            errorHandler = "kafkaListenerErrorHandler"
    )
    public void processJobTask(JobTaskMessage jobTaskMessage, Acknowledgment ack) {
        JobTask jobTask = jobTaskFromDtoMapper.map(jobTaskMessage);
        String messageId = UUID.randomUUID().toString();
        jobTask.setMessageId(messageId);
        ack.acknowledge();

        LOGGER.info("Received job task {} (message {})", jobTaskMessage.getJobId(), messageId);
        try {
            jobService.runJob(jobTask);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job for job task {} (message {})", jobTaskMessage.getJobId(), messageId);
            throw e;
        }
    }
}
