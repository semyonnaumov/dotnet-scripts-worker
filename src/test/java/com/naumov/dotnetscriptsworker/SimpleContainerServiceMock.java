package com.naumov.dotnetscriptsworker;

import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.exception.ContainerServiceException;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SimpleContainerServiceMock implements ContainerService {
    private final List<Container> allContainers = new CopyOnWriteArrayList<>();

    @Override
    public List<String> getAllContainersIds() {
        return allContainers.stream()
                .map(Container::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllContainersIdsWithNamePrefix(String prefix) {
        return allContainers.stream()
                .filter(c -> c.getName().startsWith(prefix))
                .map(Container::getId)
                .collect(Collectors.toList());
    }

    @Override
    public String createContainer(String containerName,
                                  String imageName,
                                  List<String> volumeBindsDescriptors,
                                  Optional<List<String>> entrypoint) {
        Container newContainer = new Container(containerName);
        allContainers.add(newContainer);
        return newContainer.getId();
    }

    @Override
    public void startContainer(String containerId) {
        getOptionalContainer(containerId)
                .ifPresent(c -> c.setStatus(Container.Status.RUNNING));
    }

    @Override
    public boolean isRunning(String containerId) {
        return getOptionalContainer(containerId)
                .map(c -> c.getStatus() == Container.Status.RUNNING).orElse(false);
    }

    @Override
    public void stopContainer(String containerId, boolean mustExist) {
        getOptionalContainer(containerId)
                .ifPresent(c -> c.setStatus(Container.Status.STOPPED));
    }

    @Override
    public String getStdout(String containerId, long timeoutMs) {
        if (getOptionalContainer(containerId).isPresent()) {
            return "stdout";
        } else {
            throw new ContainerServiceException("");
        }
    }

    @Override
    public String getStderr(String containerId, long timeoutMs) {
        if (getOptionalContainer(containerId).isPresent()) {
            return "stdout";
        } else {
            throw new ContainerServiceException("");
        }
    }

    @Override
    public Long getExitCode(String containerId) {
        if (getOptionalContainer(containerId).isPresent()) {
            return 0L;
        } else {
            throw new ContainerServiceException("");
        }
    }

    @Override
    public void removeContainer(String containerId, boolean mustExist) {
        Optional<Container> optionalContainer = getOptionalContainer(containerId);
        if (optionalContainer.isEmpty()) {
            throw new ContainerServiceException("");
        }

        allContainers.remove(optionalContainer.get());
    }

    @NotNull
    private Optional<Container> getOptionalContainer(String containerId) {
        return allContainers.stream()
                .filter(c -> c.getId().equals(containerId))
                .findAny();
    }

    @Getter
    private static class Container {
        private final String id = UUID.randomUUID().toString();
        private final String name;
        @Setter
        private volatile Status status = Status.CREATED;

        private Container(String name) {
            this.name = name;
        }

        private enum Status {
            CREATED,
            RUNNING,
            STOPPED
        }
    }
}
