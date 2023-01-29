package com.naumov.dotnetscriptsworker.service;

import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;

public interface JobService {

    /**
     * Runs the job.
     *
     * @param job job task to run
     * @return job results
     */
    JobResults runJob(JobTask job);
}
