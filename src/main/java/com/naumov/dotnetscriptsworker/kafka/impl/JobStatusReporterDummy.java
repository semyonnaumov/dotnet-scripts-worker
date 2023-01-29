package com.naumov.dotnetscriptsworker.kafka.impl;

import com.naumov.dotnetscriptsworker.kafka.JobStatusReporter;
import com.naumov.dotnetscriptsworker.model.JobResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("dev")
@Component
public class JobStatusReporterDummy implements JobStatusReporter {
    private static final Logger LOGGER = LogManager.getLogger(JobStatusReporterDummy.class);

    @Override
    public void reportJobStartedAsync(String jobId) {
        LOGGER.info("Reported job jobId={} started", jobId);
    }

    @Override
    public void reportJobFinishedAsync(JobResults jobResults) {
        LOGGER.info("Reported job jobId={} finished", jobResults.getJobId());
    }
}
