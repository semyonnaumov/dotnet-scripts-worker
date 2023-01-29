package com.naumov.dotnetscriptsworker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.naumov.dotnetscriptsworker.config.props.DockerClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerClientConfiguration {
    private final DockerClientProperties props;

    @Autowired
    public DockerClientConfiguration(DockerClientProperties props) {
        this.props = props;
    }

    @Bean
    public DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(props.getDockerHost())
                .withDockerTlsVerify(props.getDockerTlsVerify())
                .withDockerCertPath(props.getDockerCertPath())
                .withRegistryUsername(props.getRegistryUser())
                .withRegistryPassword(props.getRegistryPassword())
                .withRegistryEmail(props.getRegistryEmail())
                .withRegistryUrl(props.getRegistryUrl())
                .build();
    }

    // TODO move connection params to properties
    @Bean
    public DockerHttpClient dockerHttpClient() {
        DockerClientConfig config = dockerClientConfig();
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
    }

    @Bean
    public DockerClient dockerClient() {
        return DockerClientImpl.getInstance(dockerClientConfig(), dockerHttpClient());
    }
}
