package com.naumov.dotnetscriptsworker.kafka;

import com.naumov.dotnetscriptsworker.model.JobResults;

public interface JobStatusReporter {

    void reportJobStartedAsync(String jobId);

    void reportJobFinishedAsync(JobResults jobResults);
}
