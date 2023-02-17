package com.naumov.dotnetscriptsworker.dto.cons;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class JobTaskMessage {
    @NotNull
    private UUID jobId;
    @NotBlank
    private String script;
    @Valid
    private JobConfig jobConfig;

    @Override
    public String toString() {
        return "JobTaskMessage{" +
                "jobId=" + jobId +
                ", script='" + script + '\'' +
                ", jobConfig=" + jobConfig +
                '}';
    }
}
