package com.naumov.dotnetscriptsworker.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
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

    @Autowired
    public ContainerServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
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
            LOGGER.error("Failed to list containers with statuses {}", stringStatuses, e);
            throw new ContainerServiceException("Failed to list containers with statuses " + stringStatuses, e);
        }
    }

    @Override
    public String createContainer(String containerName,
                                  String sandboxImageName,
                                  String volumeSrcPath,
                                  String volumeDestPath,
                                  String scriptFileName) {
        try {
            String volumeBindDescriptor = getVolumeBindDescriptor(volumeSrcPath, volumeDestPath);
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(Bind.parse(volumeBindDescriptor));

            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(sandboxImageName)
                    .withEntrypoint("dotnet-script", volumeDestPath + "/" + scriptFileName, "-c", "release")
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .exec();

            String containerId = createContainerResponse.getId();
            LOGGER.info("Created container {}, (name={}, image={}, volume={})",
                    containerId, containerName, sandboxImageName, volumeBindDescriptor);

            return containerId;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to create container with name {}", containerName, e);
            throw new ContainerServiceException("Failed to create container with name " + containerName, e);
        }
    }

    private String getVolumeBindDescriptor(String volumeSrcPath, String volumeDestPath) {
        return volumeSrcPath + ":" + volumeDestPath + ":ro";
    }

    @Override
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            LOGGER.info("Started container {}", containerId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to start container {}", containerId, e);
            throw new ContainerServiceException("Failed to start container " + containerId, e);
        }
    }

    @Override
    public boolean isRunning(String containerId) {
        try {
            InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId)
                    .exec();
            return Boolean.TRUE.equals(inspectContainerResponse.getState().getRunning());
        } catch (RuntimeException e) {
            LOGGER.error("Failed to check whether container {} is running", containerId, e);
            throw new ContainerServiceException("Failed to check whether container " + containerId + " is running", e);
        }
    }

    @Override
    public void stopContainer(String containerId, boolean mustExist) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            LOGGER.info("Stopped container {}", containerId);
        } catch (NotModifiedException ignored) {
            // already stopped
        } catch (RuntimeException e) {
            if (e instanceof NotFoundException && !mustExist) return;

            LOGGER.error("Failed to stop container {}", containerId, e);
            throw new ContainerServiceException("Failed to stop container " + containerId, e);
        }
    }

    @Override
    public String getStdout(String containerId, long timeoutMs) {
        return getLogs(containerId, true, timeoutMs);
    }

    @Override
    public String getStderr(String containerId, long timeoutMs) {
        return getLogs(containerId, false, timeoutMs);
    }

    private String getLogs(String containerId, boolean isStdout, long timeoutMs) {
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
                    .awaitCompletion(timeoutMs, TimeUnit.MILLISECONDS);

            LOGGER.info("Received logs for container {}", containerId);
            return String.join(System.lineSeparator(), logLines);

        } catch (RuntimeException | InterruptedException e) {
            String logsType = isStdout ? "STDOUT" : "STDERR";
            LOGGER.error("Failed to receive logs ({}) for container {}", logsType, containerId, e);
            throw new ContainerServiceException("Failed to receive logs ({" + logsType + "}) for container " + containerId, e);
        }
    }

    @Override
    public Long getExitCode(String containerId) {
        try {
            InspectContainerResponse rs = dockerClient.inspectContainerCmd(containerId).exec();
            LOGGER.info("Received exit code for container {}", containerId);
            return rs.getState().getExitCodeLong();
        } catch (RuntimeException e) {
            LOGGER.error("Failed to receive exit code for container {}", containerId, e);
            throw new ContainerServiceException("Failed to receive exit code for container {}" + containerId, e);
        }
    }

    @Override
    public void removeContainer(String containerId, boolean mustExist) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();

            LOGGER.info("Removed container {}", containerId);
        } catch (RuntimeException e) {
            if (e instanceof NotFoundException && !mustExist) return;

            LOGGER.error("Failed to remove container {}", containerId, e);
            throw new ContainerServiceException("Failed to remove container " + containerId, e);
        }
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
