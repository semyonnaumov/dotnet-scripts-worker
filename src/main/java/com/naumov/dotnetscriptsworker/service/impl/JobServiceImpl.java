package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxContainerProperties;
import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.kafka.JobMessagesProducer;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.JobService;
import com.naumov.dotnetscriptsworker.service.JobFilesService;
import com.naumov.dotnetscriptsworker.service.exception.JobServiceException;
import com.naumov.dotnetscriptsworker.sync.ContainerizedJob;
import com.naumov.dotnetscriptsworker.sync.ContainerizedJobAllocationException;
import com.naumov.dotnetscriptsworker.sync.ContainerizedJobsPool;
import com.naumov.dotnetscriptsworker.util.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger LOGGER = LogManager.getLogger(JobServiceImpl.class);
    private final ContainerService containerService;
    private final JobFilesService jobFilesService;
    private final JobMessagesProducer jobMessagesProducer;
    private final SandboxProperties sandboxProperties;
    private final SandboxContainerProperties sandboxContainerProperties;
    private final ContainerizedJobsPool containerizedJobsPool;

    @Autowired
    public JobServiceImpl(ContainerService containerService,
                          JobFilesService jobFilesService,
                          JobMessagesProducer jobMessagesProducer,
                          SandboxProperties sandboxProperties,
                          SandboxContainerProperties sandboxContainerProperties,
                          ContainerizedJobsPool containerizedJobsPool) {
        this.containerService = containerService;
        this.jobFilesService = jobFilesService;
        this.jobMessagesProducer = jobMessagesProducer;
        this.sandboxProperties = sandboxProperties;
        this.sandboxContainerProperties = sandboxContainerProperties;
        this.containerizedJobsPool = containerizedJobsPool;
    }

    @PostConstruct
    public void init() {
        try {
            String prefix = sandboxContainerProperties.getNamePrefix();
            LOGGER.info("Initialization: starting container environment cleanup - " +
                    "removing all containers with name prefix {}", prefix);

            List<String> containers = containerService.getAllContainersIdsWithNamePrefix(prefix);
            for (String containerId : containers) {
                containerService.removeContainer(containerId, false);
                LOGGER.info("Initialization: removed container {}", containerId);
            }
            LOGGER.info("Initialization: finished container environment cleanup");
        } catch (RuntimeException e) {
            LOGGER.error("Initialization: failed container environment cleanup", e);
            throw e;
        }
    }

    @Override
    public void runJob(JobTask jobTask) {
        UUID jobId = jobTask.getJobId();
        JobResults.JobResultsBuilder jobResultsBuilder = JobResults.builder().jobId(jobId);
        ContainerizedJob containerizedJob;
        try {
            containerizedJob = containerizedJobsPool.tryAllocate(jobId, sandboxProperties.getContainerOperationsTimeoutMs());
            if (containerizedJob.isRequestedMultipleTimes()) {
                // such job is already running - do nothing (container-environment-wide deduping)
                LOGGER.info("Skipped running job {}, it is a duplicate", jobId);
                return;
            }

        } catch (ContainerizedJobAllocationException e) {
            // unable to run this job right now - reject
            LOGGER.warn("Job {} was rejected - failed to allocate container slot from the pool", jobId, e);
            JobResults jobResults = jobResultsBuilder.status(JobResults.Status.REJECTED).build();

            jobMessagesProducer.sendJobFinishedMessageAsync(jobResults);
            return;
        }

        jobResultsBuilder.status(JobResults.Status.ACCEPTED);
        LOGGER.debug("Allocated container slot for {}", jobId);

        try {
            String jobTempDirPath = jobFilesService.prepareJobFiles(jobTask);

            String containerId = startJobContainer(jobId, jobTempDirPath);
            containerizedJob.setContainerId(containerId);
            jobMessagesProducer.sendJobStartedMessageAsync(jobId);

            JobResults.ScriptResults scriptResults = getJobContainerResults(jobId, containerId);
            JobResults jobResults = jobResultsBuilder.scriptResults(scriptResults).build();
            jobMessagesProducer.sendJobFinishedMessageAsync(jobResults);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job {}", jobId, e);
            throw new JobServiceException("Failed to run job " + jobId, e);
        } finally {
            jobFilesService.cleanupJobFiles(jobId);
            containerizedJobsPool.reclaim(containerizedJob);
        }
    }

    private String startJobContainer(UUID jobId, String jobTempDirPath) {
        String containerName = sandboxContainerProperties.getNamePrefix() + jobId;
        try {
            List<String> volumeBinds = List.of(getVolumeBindDescriptorRo(jobTempDirPath, sandboxProperties.getJobFilesContainerDir()));
            Optional<List<String>> entrypoint = sandboxContainerProperties.getOverrideEntrypoint()
                    ? Optional.of(List.of("dotnet-script", sandboxProperties.getJobFilesContainerDir() + "/" + sandboxProperties.getJobScriptFileName(), "-c", "release"))
                    : Optional.empty();

            String containerId = containerService.createContainer(
                    containerName,
                    sandboxContainerProperties.getImage(),
                    volumeBinds,
                    entrypoint
            );

            containerService.startContainer(containerId);
            LOGGER.info("Started job {} in container {}", jobId, containerId);

            return containerId;
        } catch (RuntimeException e) {
            LOGGER.info("Failed to create and start container {} for job {}", containerName, jobId);
            containerService.stopContainer(containerName, false);
            containerService.removeContainer(containerName, false);
            throw e;
        }
    }

    private String getVolumeBindDescriptorRo(String srcPath, String destPath) {
        return srcPath + ":" + destPath + ":ro";
    }

    private JobResults.ScriptResults getJobContainerResults(UUID jobId,
                                                            String containerId) {
        long jobTimeoutMs = sandboxProperties.getJobTimeoutMs();
        long containerOperationsTimeoutMs = sandboxProperties.getContainerOperationsTimeoutMs();

        try {
            JobResults.ScriptResults.JobCompletionStatus completionStatus;
            if (completedInTime(containerId, jobTimeoutMs)) {
                completionStatus = containerService.getExitCode(containerId) == 0
                        ? JobResults.ScriptResults.JobCompletionStatus.SUCCEEDED
                        : JobResults.ScriptResults.JobCompletionStatus.FAILED;
                LOGGER.info("Job {} finished in time with status {}, container {} stopped",
                        jobId, completionStatus, containerId);
            } else {
                LOGGER.info("Job {} exceeded time limit, container {} will be stopped", jobId, containerId);
                completionStatus = JobResults.ScriptResults.JobCompletionStatus.TIME_LIMIT_EXCEEDED;
                containerService.stopContainer(containerId, true);
            }

            JobResults.ScriptResults scriptResults = JobResults.ScriptResults.builder()
                    .finishedWith(completionStatus)
                    .stdout(containerService.getStdout(containerId, containerOperationsTimeoutMs))
                    .stderr(containerService.getStderr(containerId, containerOperationsTimeoutMs))
                    .build();

            LOGGER.info("Finished job {} in container {}", jobId, containerId);
            return scriptResults;
        } finally {
            containerService.removeContainer(containerId, true);
        }
    }

    private boolean completedInTime(String containerId, Long jobTimeoutMs) {
        Timer jobTimer = new Timer(jobTimeoutMs);
        jobTimer.start();
        while (!jobTimer.isFinished()) {
            if (!containerService.isRunning(containerId)) return true;
        }

        return false;
    }

    @PreDestroy
    public void shutdown() {
        try {
            List<ContainerizedJob> containerizedJobs = containerizedJobsPool.getContainerizedJobs();
            LOGGER.info("Shutdown: removing containers for containerized jobs: {}", containerizedJobs);

            for (ContainerizedJob containerizedJob : containerizedJobs) {
                String containerId = containerizedJob.getContainerId();
                if (containerId != null) {
                    containerService.stopContainer(containerId, false);
                    containerService.removeContainer(containerId, false);
                    LOGGER.info("Removed container {}", containerId);
                }
            }

            LOGGER.info("Shutdown: finished removing containers for containerized jobs");
        } catch (RuntimeException e) {
            LOGGER.error("Shutdown: failed to remove containers for containerized jobs", e);
            throw e;
        }
    }
}
