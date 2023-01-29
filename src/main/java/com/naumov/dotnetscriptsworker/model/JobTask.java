package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

import java.util.Map;

@Data
public class JobTask {
    private final String jobId;
    private String jobScript;
    private Map<String, String> jobConfig;
}
