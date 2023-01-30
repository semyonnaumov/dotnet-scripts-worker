package com.naumov.dotnetscriptsworker.dto;

import lombok.Data;

import java.util.Map;

@Data
public class JobTaskDto {
    private String jobId;
    private String script;
    private Map<String, String> jobConfig;
}
