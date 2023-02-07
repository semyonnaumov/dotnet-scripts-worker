package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

@Data
public class JobResults {
    private final String jobId;
    private Status status;
    private ScriptResults scriptResults;

    public enum Status {
        ACCEPTED,
        REJECTED
    }

    @Data
    public static class ScriptResults {
        private JobCompletionStatus finishedWith;
        private String stdout;
        private String stderr;

        public enum JobCompletionStatus {
            SUCCEEDED,
            FAILED,
            TIME_LIMIT_EXCEEDED
        }
    }
}
