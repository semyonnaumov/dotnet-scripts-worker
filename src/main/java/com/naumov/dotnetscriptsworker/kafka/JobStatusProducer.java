package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.config.props.KafkaProperties;
import com.naumov.dotnetscriptsworker.dto.JobFinishedDto;
import com.naumov.dotnetscriptsworker.dto.JobStartedDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobFinishedToDtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobStatusProducer {
    private static final Logger LOGGER = LogManager.getLogger(JobStatusProducer.class);
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate;
    private final KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate;
    private final DtoMapper<JobResults, JobFinishedDto> jobFinishedDtoMapper;

    @Autowired
    public JobStatusProducer(KafkaProperties kafkaProperties,
                             KafkaTemplate<String, JobStartedDto> jobStartedKafkaTemplate,
                             KafkaTemplate<String, JobFinishedDto> jobFinishedKafkaTemplate,
                             JobFinishedToDtoMapper jobFinishedToDtoMapper) {
        this.kafkaProperties = kafkaProperties;
        this.jobStartedKafkaTemplate = jobStartedKafkaTemplate;
        this.jobFinishedKafkaTemplate = jobFinishedKafkaTemplate;
        this.jobFinishedDtoMapper = jobFinishedToDtoMapper;
    }

    public void reportJobStartedAsync(String jobId) {
        String runningTopic = kafkaProperties.getRunningTopicName();
        jobStartedKafkaTemplate.send(runningTopic, new JobStartedDto(jobId))
                .thenAccept(res -> {
                    LOGGER.info("Reported job {} started to topic {}", jobId, runningTopic);
                }).exceptionally(e -> {
                    LOGGER.error("Failed to report job {} started to topic {}", jobId, runningTopic, e);
                    return null;
                });
    }

    public void reportJobFinishedAsync(JobResults jobResults) {
        JobFinishedDto jobFinishedDto = jobFinishedDtoMapper.map(jobResults);
        String finishedTopic = kafkaProperties.getFinishedTopicName();
        jobFinishedKafkaTemplate.send(finishedTopic, jobFinishedDto)
                .thenAccept(res -> {
                    LOGGER.info("Reported job {} finished to topic {}", jobFinishedDto.getJobId(), finishedTopic);
                }).exceptionally(e -> {
                    LOGGER.error("Failed to report job {} finished to topic {}", jobFinishedDto.getJobId(), finishedTopic, e);
                    return null;
                });
    }

    @PreDestroy
    void shutdown() {
        // only needed if producer per thread enabled
        jobStartedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
        jobFinishedKafkaTemplate.getProducerFactory().closeThreadBoundProducer();
    }
}
