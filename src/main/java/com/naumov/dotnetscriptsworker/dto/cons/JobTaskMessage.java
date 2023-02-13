package com.naumov.dotnetscriptsworker.dto.cons;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public final class JobTaskMessage {
    @NotNull
    private UUID jobId;
    @NotBlank
    private String script;
    @Valid
    private JobConfig jobConfig;

    @Getter
    @Setter
    @Builder
    public static final class JobConfig {
        private String nugetConfigXml;
    }
}
