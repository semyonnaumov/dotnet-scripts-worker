package com.naumov.dotnetscriptsworker.dto.cons;

import lombok.Data;

@Data
public class JobTaskMessage {
    private String jobId;
    private String script;
    private JobConfig jobConfig;

    @Data
    public static class JobConfig {
        private String nugetConfigXml;
    }
}
