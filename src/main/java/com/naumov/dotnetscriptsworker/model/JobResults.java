package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

@Data
public class JobResults {
    private final String jobId;
    private JobResults.Status finishedWith;
    private String stdout;
    private String stderr;

    /**
     * What end user sees
     */
    public enum Status {
        SUCCEEDED,
        FAILED,
        TIME_LIMIT_EXCEEDED
    }
}
