package com.naumov.dotnetscriptsworker.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("worker.sandbox")
@Component
public class SandboxProperties {
    private String os;
    private String platform;
    private String workerType;
    private Integer maxConcurrentSandboxes;
    private Long sandboxInitTimeoutMs;
    private Long jobTimeoutMs;
    private String sandboxImage;
    private String sandboxContainerPrefix;
    private String scriptFileName;
    private String scriptFilesOnHostDir;
    private String scriptFileInContainerDir;
}