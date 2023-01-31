package com.naumov.dotnetscriptsworker.dto;

import lombok.Data;

@Data
public class JobTaskDto {
    private String jobId;
    private String script;
    private JobConfigDto jobConfig;

    @Data
    public static class JobConfigDto {
        private String nugetConfig;
    }
}
