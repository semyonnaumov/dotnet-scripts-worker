package com.naumov.dotnetscriptsworker.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties("worker.docker-client")
@Component
public class DockerClientProperties {
    private String dockerHost;
    private Boolean dockerTlsVerify;
    private String dockerCertPath;
    private String registryUser;
    private String registryPassword;
    private String registryEmail;
    private String registryUrl;
}