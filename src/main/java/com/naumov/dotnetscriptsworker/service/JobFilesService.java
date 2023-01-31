package com.naumov.dotnetscriptsworker.service;

import com.naumov.dotnetscriptsworker.model.JobTask;

public interface JobFilesService {

    String prepareJobFiles(JobTask jobTask);

    void cleanupJobFiles(String jobId);
}
