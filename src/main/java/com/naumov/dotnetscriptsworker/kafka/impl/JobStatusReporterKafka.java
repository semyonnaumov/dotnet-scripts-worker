package com.naumov.dotnetscriptsworker.kafka.impl;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobFinishedDtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// almost finished?
@Profile("!dev")
@Component
public class JobStatusReporterKafka {
    private static final Logger LOG = LogManager.getLogger(JobStatusReporterKafka.class);

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate;
    private final KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate;
    private final DtoMapper<JobResults, JobFinishedDto> jobFinishedDtoMapper;


    @Autowired
    public JobStatusReporterKafka(KafkaProperties kafkaProperties,
                                  KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate,
                                  KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate,
                                  JobFinishedDtoMapper jobFinishedDtoMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jobStartedKafkaTemplate = jobStartedKafkaTemplate;
        this.jobFinishedKafkaTemplate = jobFinishedKafkaTemplate;
        this.jobFinishedDtoMapper = jobFinishedDtoMapper;
    }

    public void reportJobStartedAsync(String jobId) {
        String runningTopic = kafkaProperties.getRunningTopic();
        jobStartedKafkaTemplate.send(runningTopic, new JobStartedDto(jobId))
                .thenAccept(res -> {
                    LOG.info("Reported job {} started to topic {}", jobId, runningTopic);
                }).exceptionally(e -> {
                    LOG.error("Failed to report job {} started to topic {}", jobId, runningTopic, e);
                    return null;
                });
    }

    public void reportJobFinishedAsync(JobResults jobResults) {
        JobFinishedDto jobFinishedDto = jobFinishedDtoMapper.map(jobResults);
        String finishedTopic = kafkaProperties.getRunningTopic();
        jobFinishedKafkaTemplate.send(finishedTopic, jobFinishedDto)
                .thenAccept(res -> {
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
