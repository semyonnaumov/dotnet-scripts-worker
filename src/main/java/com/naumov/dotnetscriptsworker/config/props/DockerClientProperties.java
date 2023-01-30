package com.naumov.dotnetscriptsworker.config.props;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("worker.docker-client")
@Component
public class DockerClientProperties {
    @NotBlank
    private String dockerHost;
    private Boolean dockerTlsVerify = false;
    private String dockerCertPath;
    private String registryUser;
    private String registryPassword;
    private String registryEmail;
    private String registryUrl;
}