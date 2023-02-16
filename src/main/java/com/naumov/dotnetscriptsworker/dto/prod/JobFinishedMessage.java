package com.naumov.dotnetscriptsworker.dto.prod;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class JobFinishedMessage {
    private UUID jobId;
    private JobStatus status;
    private ScriptResults scriptResults;

    @Override
    public String toString() {
        return "JobFinishedMessage{" +
                "jobId=" + jobId +
                ", status=" + status +
                ", scriptResults=" + scriptResults +
                '}';
    }
}
