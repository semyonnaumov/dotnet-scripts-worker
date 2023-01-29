package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobFinishedDtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobStartedDtoMapper;
import com.naumov.dotnetscriptsworker.model.Job;
import com.naumov.dotnetscriptsworker.model.JobResults;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// almost finished?
@Profile("!dev")
@Component
public class JobStatusReporter {
    private static final Logger LOG = LogManager.getLogger(JobStatusReporter.class);

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate;
    private final KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate;
    private final DtoMapper<Job, JobStartedDto> jobStartedDtoMapper;
    private final DtoMapper<JobResults, JobFinishedDto> jobFinishedDtoMapper;


    @Autowired
    public JobStatusReporter(KafkaProperties kafkaProperties,
                             KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate,
                             KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate,
                             JobStartedDtoMapper jobStartedDtoMapper,
                             JobFinishedDtoMapper jobFinishedDtoMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jobStartedKafkaTemplate = jobStartedKafkaTemplate;
        this.jobFinishedKafkaTemplate = jobFinishedKafkaTemplate;
        this.jobStartedDtoMapper = jobStartedDtoMapper;
        this.jobFinishedDtoMapper = jobFinishedDtoMapper;
    }

    public void reportJobStartedAsync(Job jobTask) {
        JobStartedDto jobStartedDto = jobStartedDtoMapper.map(jobTask);
        String runningTopic = kafkaProperties.getRunningTopic();
        jobStartedKafkaTemplate.send(runningTopic, jobStartedDto)
                .thenAccept(res -> {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    LOG.info("Reported job {} started to topic {}", jobStartedDto.getJobId(), runningTopic);
                }).exceptionally(e -> {
                    LOG.error("Failed to report job {} started to topic {}", jobStartedDto.getJobId(), runningTopic, e);
                    return null;
                });
    }

    public void reportJobFinishedAsync(JobResults jobResults) {
        JobFinishedDto jobFinishedDto = jobFinishedDtoMapper.map(jobResults);
        String finishedTopic = kafkaProperties.getRunningTopic();
        jobFinishedKafkaTemplate.send(finishedTopic, jobFinishedDto)
                .thenAccept(res -> {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    LOG.info("Reported job {} finished to topic {}", jobFinishedDto.getJobId(), finishedTopic);
                }).exceptionally(e -> {
                    LOG.error("Failed to report job {} finished to topic {}", jobFinishedDto.getJobId(), finishedTopic, e);
                    return null;
                });
    }

    @PreDestroy
    void shutdown() {
        jobStartedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
        jobFinishedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
    }
}
