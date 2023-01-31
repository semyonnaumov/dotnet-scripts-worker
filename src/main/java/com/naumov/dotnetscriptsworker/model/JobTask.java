package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

@Data
public class JobTask {
    private final String jobId;
    private String messageId;
    private String jobScript;
    private JobConfig jobConfig;

    @Data
    public static class JobConfig {
        private String nugetConfig;
    }
}
