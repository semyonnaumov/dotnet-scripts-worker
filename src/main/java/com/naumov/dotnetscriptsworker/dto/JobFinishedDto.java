package com.naumov.dotnetscriptsworker.dto;

import lombok.Data;

@Data
public class JobFinishedDto {
    private final String jobId;
    private JobFinishedDto.Status status;
    private JobFinishedDto.ScriptResults scriptResults;

    public enum Status {
        ACCEPTED,
        REJECTED
    }

    @Data
    public static class ScriptResults {
        private Status finishedWith;
        private String stdout;
        private String stderr;

        public enum Status {
            SUCCEEDED,
            FAILED,
            TIME_LIMIT_EXCEEDED
        }
    }
}
