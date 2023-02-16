package com.naumov.dotnetscriptsworker.config.props;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("worker.sandbox")
@Component
public class SandboxProperties {
    @NotBlank
    private String runnerType;
    @Min(1)
    @Max(60)
    private int containerOperationsTimeoutSec;
    @Min(1)
    @Max(600)
    private int jobTimeoutSec;
    @Min(10)
    @Max(120)
    private int imagePullTimeoutSec;
    @NotBlank
    private String jobFilesHostDir;
    @NotBlank
    private String jobFilesContainerDir;
    @NotBlank
    private String jobScriptFileName;
    @NotNull
    @Min(1)
    private Integer maxContainers;
}