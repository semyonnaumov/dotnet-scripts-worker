package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.dto.KafkaDtoMapper;
import com.naumov.dotnetscriptsworker.dto.cons.JobTaskMessage;
import com.naumov.dotnetscriptsworker.service.JobService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JobMessagesConsumer {
    private static final Logger LOGGER = LogManager.getLogger(JobMessagesConsumer.class);
    private final JobService jobService;
    private final KafkaDtoMapper kafkaDtoMapper;
    private final Optional<Reporter<JobTaskMessage>> messageProcessedReporter;

    @Autowired
    public JobMessagesConsumer(JobService jobService,
                               KafkaDtoMapper kafkaDtoMapper,
                               Optional<Reporter<JobTaskMessage>> messageProcessedReporter) {
        this.jobService = jobService;
        this.kafkaDtoMapper = kafkaDtoMapper;
        this.messageProcessedReporter = messageProcessedReporter;
    }

    @KafkaListener(
            topics = "${worker.kafka.jobs-topic-name}",
            containerFactory = "kafkaListenerContainerFactory",
            errorHandler = "kafkaListenerPayloadValidationErrorHandler"
    )
    public void onJobTaskMessage(@Payload @Valid JobTaskMessage jobTaskMessage, Acknowledgment ack) {
        ack.acknowledge();
        UUID messageId = UUID.randomUUID();
        UUID jobId = jobTaskMessage.getJobId();
        LOGGER.info("Received job task {}, assigned message id {}", jobId, messageId);
        try {
            jobService.runJob(kafkaDtoMapper.fromJobTaskMessage(jobTaskMessage, messageId));
            LOGGER.info("Finished running job for job task {} with message id {}", jobId, messageId);
            onJobMessageProcessed(jobTaskMessage);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job for job task {} with message id {}", jobId, messageId);
            throw e;
        }
    }

    private void onJobMessageProcessed(JobTaskMessage jobMessage) {
        messageProcessedReporter.ifPresent(r -> r.report(jobMessage));
    }
}
