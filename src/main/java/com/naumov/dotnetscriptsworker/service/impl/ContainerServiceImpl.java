package com.naumov.dotnetscriptsworker.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.exception.ContainerServiceException;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ContainerServiceImpl implements ContainerService {
    private static final Logger LOGGER = LogManager.getLogger(ContainerServiceImpl.class);
    private final DockerClient dockerClient;
    private final SandboxProperties sandboxProperties;

    @Autowired
    public ContainerServiceImpl(DockerClient dockerClient, SandboxProperties sandboxProperties) {
        this.dockerClient = dockerClient;
        this.sandboxProperties = sandboxProperties;
    }

    @Override
    public List<String> listAllContainers() {
        return listContainers(List.of(
                ContainerStatus.CREATED,
                ContainerStatus.RUNNING,
                ContainerStatus.PAUSED,
                ContainerStatus.RESTARTING,
                ContainerStatus.REMOVING,
                ContainerStatus.EXITED,
                ContainerStatus.DEAD
        ));
    }

    @Override
    public List<String> listRunningContainers() {
        return listContainers(List.of(
                ContainerStatus.RUNNING
        ));
    }

    @Override
    public List<String> listStoppedContainers() {
        return listContainers(List.of(
                ContainerStatus.EXITED,
                ContainerStatus.DEAD
        ));
    }

    private List<String> listContainers(List<ContainerStatus> statuses) {
        List<String> stringStatuses = statuses.stream()
                .map(ContainerStatus::getValue)
                .toList();
        try {
            List<Container> containerList = dockerClient.listContainersCmd()
                    .withStatusFilter(stringStatuses)
                    .exec();

            return containerList.stream()
                    .map(Container::getId)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            LOGGER.error("Failed to list containers with statuses={}", stringStatuses, e);
            throw new ContainerServiceException("Failed to list containers with statuses=" + stringStatuses, e);
        }
    }

    @Override
    public String createContainer(String containerName, String tempJobScriptDirOnHost) {
        try {
            LOGGER.info("Creating container with name={}", containerName);
            String volumeDescriptor = tempJobScriptDirOnHost +
                    ":" +
                    sandboxProperties.getScriptFileInContainerDir() +
                    ":ro";

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(Bind.parse(volumeDescriptor));

            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(sandboxProperties.getSandboxImage())
                    .withCmd("--bind_ip_all")
                    .withName(containerName)
                    .withEnv("MONGO_LATEST_VERSION=3.6")
                    .withHostConfig(hostConfig)
                    .exec();

            String containerId = createContainerResponse.getId();
            LOGGER.info("Container with with name={} created: id={}", containerName, containerId);

            return containerId;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to create container with name={}", containerName, e);
            throw new ContainerServiceException("Failed to create container with name= " + containerName, e);
        }
    }

    @Override
    public void startContainer(String containerId) {
        try {
            LOGGER.info("Starting container with id={}", containerId);
            dockerClient.startContainerCmd(containerId).exec();
            LOGGER.info("Container with with id={} started", containerId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start container with id={}", containerId, e);
            throw new ContainerServiceException("Failed to start container with id=" + containerId, e);
        }
    }

    @Override
    public boolean isRunning(String containerId) {
        try {
            InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
            return Boolean.TRUE.equals(inspectContainerResponse.getState().getRunning());

        } catch (RuntimeException e) {
            LOGGER.error("Failed to check whether container with id={} is running", containerId, e);
            throw new ContainerServiceException("Failed to check whether container with id=" + containerId + " is running", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            LOGGER.info("Stopping container with id={}", containerId);
            dockerClient.stopContainerCmd(containerId).exec();
            LOGGER.info("Container with with id={} stopped", containerId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to stop container with id={}", containerId, e);
            throw new ContainerServiceException("Failed to stop container with id=" + containerId, e);
        }
    }

    @Override
    public void stopContainers(List<String> containerIds) {
        if (containerIds != null) {
            for (String containerId : containerIds) {
                this.stopContainer(containerId);
            }
        }
    }

    @Override
    public String getStdout(String containerId) {
        return getLogs(containerId, true);
    }

    @Override
    public String getStderr(String containerId) {
        return getLogs(containerId, false);
    }

    private String getLogs(String containerId, boolean isStdout) {
        try {
            List<String> logLines = new ArrayList<>();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(isStdout)
                    .withStdErr(!isStdout)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame item) {
                            logLines.add(item.toString());
                        }
                    })
                    .awaitCompletion(sandboxProperties.getJobTimeoutMs(), TimeUnit.MILLISECONDS);

            return String.join(System.lineSeparator(), logLines);
        } catch (RuntimeException | InterruptedException e) {
            String logsType = isStdout ? "STDOUT" : "STDERR";
            LOGGER.error("Failed to get logs ({}) for container with id={}", logsType, containerId, e);
            throw new ContainerServiceException("Failed to get logs ({" + logsType + "}) for container with id=" + containerId, e);
        }
    }

    @Override
    public Long getExitCode(String containerId) {
        try {
            InspectContainerResponse rs = dockerClient.inspectContainerCmd(containerId).exec();
            return rs.getState().getExitCodeLong();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to get container exit code with id={}", containerId, e);
            throw new ContainerServiceException("Failed to get container exit code with id=" + containerId, e);
        }
    }

    @Override
    public void removeForcefullyContainer(String containerId) {
        try {
            LOGGER.info("Removing container with id={}", containerId);
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();

            LOGGER.info("Container with with id={} removed", containerId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to remove container with id={}", containerId, e);
            throw new ContainerServiceException("Failed to remove container with id=" + containerId, e);
        }
    }

    @Override
    public void removeForcefullyContainers(List<String> containerIds) {
        if (containerIds != null) {
            for (String containerId : containerIds) {
                this.removeForcefullyContainer(containerId);
            }
        }
    }

    @Override
    public void stopAndRemoveAllContainers() {
        List<String> runningContainerIds = this.listRunningContainers();
        List<String> allContainerIds = this.listAllContainers();
        this.stopContainers(runningContainerIds);
        this.removeForcefullyContainers(allContainerIds);
    }

    @PreDestroy
    public void shutdown() {
        try {
            dockerClient.close();
        } catch (IOException e) {
            throw new ContainerServiceException("Failed to close " + dockerClient.getClass().getSimpleName(), e);
        }
    }

    private enum ContainerStatus {
        CREATED("created"),
        RUNNING("running"),
        PAUSED("paused"),
        RESTARTING("restarting"),
        REMOVING("removing"),
        EXITED("exited"),
        DEAD("dead");

        private final String value;

        ContainerStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
