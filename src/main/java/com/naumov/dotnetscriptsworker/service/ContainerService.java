package com.naumov.dotnetscriptsworker.service;

import java.util.List;
import java.util.Optional;

/**
 * Service used to manipulate containers with job tasks.
 */
public interface ContainerService {

    /**
     * Lists all existing container ids.
     *
     * @return list of container ids
     */
    List<String> getAllContainersIds();

    /**
     * Lists all existing container ids with the specified name prefix.
     *
     * @param prefix container name prefix
     * @return list of container ids
     */
    List<String> getAllContainersIdsWithNamePrefix(String prefix);

    /**
     * Pulls image from remote registry.
     *
     * @param imageName  image name
     * @param imageTag   image tag
     * @param timeoutSec timeout for pulling
     * @throws InterruptedException is thrown when thread is interrupted while pulling
     */
    void pullImage(String imageName, String imageTag, int timeoutSec) throws InterruptedException;

    /**
     * Creates a job task container.
     *
     * @param containerName          name to set to container
     * @param imageName              image to create the container from
     * @param volumeBindsDescriptors list of volumes to bind e.g.: ["/from:/to:ro", "/from:/somewhere-else"]
     * @param entrypoint             optional entrypoint to override the container's, e.g.: ["/bin/sh", "echo", "hello"]
     * @return created container id
     */
    String createContainer(String containerName,
                           String imageName,
                           List<String> volumeBindsDescriptors,
                           Optional<List<String>> entrypoint);

    /**
     * Starts the given container.
     *
     * @param containerId container to start
     */
    void startContainer(String containerId);

    /**
     * Checks whether the given container is running.
     *
     * @param containerId container to check
     */
    boolean isRunning(String containerId);

    /**
     * Stops the container. When {@code mustExist} is set to true and the container doesn't
     * exist throws an error, returns successfully otherwise.
     *
     * @param containerId container to stop
     * @param mustExist   fail when not exists flag
     */
    void stopContainer(String containerId, boolean mustExist);

    /**
     * Retrieves the STDOUT of the container in the blocking manner.
     *
     * @param containerId container to retrieve STDOUT from
     * @param timeoutSec  timeout to wait for while trying
     * @return STDOUT string
     */
    String getStdout(String containerId, int timeoutSec);

    /**
     * Retrieves the STDERR of the container in the blocking manner.
     *
     * @param containerId container to retrieve STDERR from
     * @param timeoutSec  timeout to wait for while trying
     * @return STDERR string
     */
    String getStderr(String containerId, int timeoutSec);

    /**
     * Retrieves the exit code of the container.
     *
     * @param containerId container to retrieve the exit code from
     * @return exit code
     */
    Long getExitCode(String containerId);

    /**
     * Removes the container. When {@code mustExist} is set to true and the container doesn't
     * exist throws an error, returns successfully otherwise.
     *
     * @param containerId container to remove
     * @param mustExist   fail when not exists flag
     */
    void removeContainer(String containerId, boolean mustExist);
}
