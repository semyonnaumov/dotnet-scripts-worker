package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

/**
 * Running job data
 */
@Data
public class Job {
    private final String jobId;
    private String containerId;
    // TODO
}
