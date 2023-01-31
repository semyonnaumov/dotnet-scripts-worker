package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.kafka.JobStatusProducer;
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

@Service
public class JobServiceImpl implements JobService {
    private static final Logger LOGGER = LogManager.getLogger(JobServiceImpl.class);
    private final ContainerService containerService;
    private final JobFilesService jobFilesService;
    private final JobStatusProducer jobStatusProducer;
    private final SandboxProperties sandboxProperties;
    private final ContainerizedJobsPool containerizedJobsPool;

    @Autowired
    public JobServiceImpl(ContainerService containerService,
                          JobFilesService jobFilesService,
                          JobStatusProducer jobStatusProducer,
                          SandboxProperties sandboxProperties,
                          ContainerizedJobsPool containerizedJobsPool) {
        this.containerService = containerService;
        this.jobFilesService = jobFilesService;
        this.jobStatusProducer = jobStatusProducer;
        this.sandboxProperties = sandboxProperties;
        this.containerizedJobsPool = containerizedJobsPool;
    }

    @PostConstruct
    public void init() {
        try {
            LOGGER.info("{} initialization: starting container environment cleanup",
                    JobServiceImpl.class.getSimpleName());
            String prefix = sandboxProperties.getSandboxContainerPrefix();
            List<String> containers = containerService.getAllContainersIdsWithNamePrefix(prefix);
            for (String containerId : containers) {
                containerService.removeContainer(containerId, false);
            }
            LOGGER.info("{} initialization: finished container environment cleanup",
                    JobServiceImpl.class.getSimpleName());
        } catch (RuntimeException e) {
            LOGGER.error("{} initialization: failed container environment cleanup",
                    JobServiceImpl.class.getSimpleName(), e);
            throw e;
        }
    }

    @Override
    public void runJob(JobTask jobTask) {
        String jobId = jobTask.getJobId();
        JobResults jobResults = new JobResults(jobId);

        ContainerizedJob containerizedJob;
        try {
            containerizedJob = containerizedJobsPool.tryAllocate(jobId, sandboxProperties.getContainerOperationsTimeoutMs());
            if (containerizedJob.isRequestedMultipleTimes()) {
                // such job is already running - do nothing (docker-wide deduping)
                LOGGER.info("Skipped running job {}, it is a duplicate", jobId);
                return;
            }
        } catch (ContainerizedJobAllocationException e) {
            // unable to run this job right now - reject
            LOGGER.warn("Job {} was rejected", jobId, e);
            jobResults.setStatus(JobResults.Status.REJECTED);
            jobStatusProducer.reportJobFinishedAsync(jobResults);
            return;
        }

        jobResults.setStatus(JobResults.Status.ACCEPTED);
        LOGGER.debug("Allocated container slot for {}", jobId);

        try {
            jobFilesService.prepareJobFiles(jobId, jobTask.getJobScript());

            String containerId = startJobContainer(jobId);
            containerizedJob.setContainerId(containerId);
            jobStatusProducer.reportJobStartedAsync(jobId);

            JobResults.ScriptResults scriptResults = getJobContainerResults(jobId, containerId);
            jobResults.setScriptResults(scriptResults);
            jobStatusProducer.reportJobFinishedAsync(jobResults);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job {}", jobId, e);
            throw new JobServiceException("Failed to run job " + jobId, e);
        } finally {
            jobFilesService.cleanupJobFiles(jobId);
            containerizedJobsPool.reclaim(containerizedJob);
        }
    }

    private String startJobContainer(String jobId) {
        String containerName = sandboxProperties.getSandboxContainerPrefix() + jobId;
        try {
            String tempDirPath = jobFilesService.getTempJobScriptDirectoryPath(jobId).toString();
            String containerId = containerService.createContainer(
                    containerName,
                    sandboxProperties.getSandboxImage(),
                    tempDirPath,
                    sandboxProperties.getScriptFileInContainerDir(),
                    sandboxProperties.getScriptFileName()
            );

            containerService.startContainer(containerId);
            LOGGER.info("Started job {} in container {}", jobId, containerId);

            return containerId;
        } catch (RuntimeException e) {
            containerService.stopContainer(containerName, false);
            containerService.removeContainer(containerName, false);
            throw e;
        }
    }

    private JobResults.ScriptResults getJobContainerResults(String jobId,
                                                            String containerId) {
        long jobTimeoutMs = sandboxProperties.getJobTimeoutMs();
        long containerOperationsTimeoutMs = sandboxProperties.getContainerOperationsTimeoutMs();

        try {
            JobResults.ScriptResults scriptResults = new JobResults.ScriptResults();
            JobResults.ScriptResults.Status completionStatus;
            if (completedInTime(containerId, jobTimeoutMs)) {
                completionStatus = containerService.getExitCode(containerId) == 0
                        ? JobResults.ScriptResults.Status.SUCCEEDED
                        : JobResults.ScriptResults.Status.FAILED;
                LOGGER.info("Job {} finished in time, container {} stopped", jobId, containerId);
            } else {
                LOGGER.info("Job {} exceeded time limit, container {} will be stopped", jobId, containerId);
                completionStatus = JobResults.ScriptResults.Status.TIME_LIMIT_EXCEEDED;
                containerService.stopContainer(containerId, true);
            }

            scriptResults.setFinishedWith(completionStatus);
            scriptResults.setStdout(containerService.getStdout(containerId, containerOperationsTimeoutMs));
            scriptResults.setStderr(containerService.getStderr(containerId, containerOperationsTimeoutMs));

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
            LOGGER.info("{} shutdown: removing containers for containerized jobs: {}",
                    JobServiceImpl.class.getSimpleName(), containerizedJobs);

            for (ContainerizedJob containerizedJob : containerizedJobs) {
                String containerId = containerizedJob.getContainerId();
                if (containerId != null) {
                    containerService.stopContainer(containerId, false);
                    containerService.removeContainer(containerId, false);
                }
            }

            LOGGER.info("{} shutdown: finished removing containers for containerized jobs",
                    JobServiceImpl.class.getSimpleName());
        } catch (RuntimeException e) {
            LOGGER.error("{} shutdown: failed to remove containers for containerized jobs",
                    JobServiceImpl.class.getSimpleName(), e);
            throw e;
        }
    }
}
