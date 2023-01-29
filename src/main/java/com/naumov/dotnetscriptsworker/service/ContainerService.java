package com.naumov.dotnetscriptsworker.service;

import java.util.List;

public interface ContainerService {

    List<String> listAllContainers();

    List<String> listRunningContainers();

    List<String> listStoppedContainers();

    String createContainer(String containerName, String tempJobScriptDirOnHost);

    void startContainer(String containerId);

    boolean isRunning(String containerId);

    void stopContainer(String containerId);

    void stopContainers(List<String> containerId);

    String getStdout(String containerId);

    String getStderr(String containerId);

    Long getExitCode(String containerId);

    void removeForcefullyContainer(String containerId);

    void removeForcefullyContainers(List<String> containerId);

    void stopAndRemoveAllContainers();
}
