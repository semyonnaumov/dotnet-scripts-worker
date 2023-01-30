package com.naumov.dotnetscriptsworker.config.props;

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
    private String workerType;
    @NotNull
    @Min(1)
    private Integer maxConcurrentSandboxes;
    @NotNull
    @Min(1)
    private Long containerOperationsTimeoutMs;
    @NotNull
    @Min(1)
    private Long jobTimeoutMs;
    @NotBlank
    private String sandboxImage;
    @NotBlank
    private String sandboxContainerPrefix;
    @NotBlank
    private String scriptFileName;
    @NotBlank
    private String scriptFilesOnHostDir;
    @NotBlank
    private String scriptFileInContainerDir;
}