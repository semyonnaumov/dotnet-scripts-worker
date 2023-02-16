package com.naumov.dotnetscriptsworker.dto.prod;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class JobStartedMessage {
    @NotNull
    private UUID jobId;

    @Override
    public String toString() {
        return "JobStartedMessage{" +
                "jobId=" + jobId +
                '}';
    }
}
