package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.JobTaskDto;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Profile("!dev")
@Component
public class JobTasksListener {
    private static final Logger LOGGER = LogManager.getLogger(JobTasksListener.class);

    private final JobService jobService;
    private final KafkaProperties kafkaProperties;

    @Autowired
    public JobTasksListener(JobService jobService, KafkaProperties kafkaProperties1) {
        this.jobService = jobService;
        this.kafkaProperties = kafkaProperties1;
    }

    // called on one instance from multiple threads of listeners' container - make sure to make it thread-safe
    @KafkaListener(
            topics = "${worker.kafka.jobs-topic}",
            errorHandler = "kafkaListenerErrorHandler"  // bean name here,

    )
    public void processJobTask(JobTaskDto jobTaskDto, Acknowledgment ack) {
        LOGGER.info("Received job jobId={}", jobTaskDto.getJobId());
//        ack.acknowledge();
//        LOG.info("Received job task {} from {}", jobTaskDto.getJobId(), kafkaProperties.getJobsTopic());
//
//        // TODO process
//        try {
//            JobResults jobResults = jobService.runJob(jobTaskDto, 10000L);
//        } catch (JobRunnerException e) {
//            LOG.error("Error during running job {}", jobTaskDto.getJobId(), e);
//        }
    }
}
