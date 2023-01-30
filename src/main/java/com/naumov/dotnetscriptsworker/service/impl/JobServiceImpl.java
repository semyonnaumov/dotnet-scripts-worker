package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.kafka.JobStatusReporter;
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
    private final JobStatusReporter jobStatusReporter;
    private final SandboxProperties sandboxProperties;
    private final ContainerizedJobsPool containerizedJobsPool;

    @Autowired
    public JobServiceImpl(ContainerService containerService,
                          JobFilesService jobFilesService,
                          JobStatusReporter jobStatusReporter,
                          SandboxProperties sandboxProperties,
                          ContainerizedJobsPool containerizedJobsPool) {
        this.containerService = containerService;
        this.jobFilesService = jobFilesService;
        this.jobStatusReporter = jobStatusReporter;
        this.sandboxProperties = sandboxProperties;
        this.containerizedJobsPool = containerizedJobsPool;
    }

    @Override
    public JobResults runJob(JobTask jobTask) {
        String jobId = jobTask.getJobId();
        JobResults jobResults = new JobResults(jobId);

        ContainerizedJob containerizedJob;
        try {
            containerizedJob = containerizedJobsPool.tryAllocate(jobId, sandboxProperties.getContainerOperationsTimeoutMs());
            if (containerizedJob.isRequestedMultipleTimes()) {
                // such job is already running - do nothing (docker-wide deduping)
                LOGGER.info("Skipped running job {}, it is a duplicate", jobId);
                jobResults.setFinishedWith(JobResults.Status.FAILED);
                jobStatusReporter.reportJobFinishedAsync(jobResults);
                return jobResults; // TODO delete after testing web endpoint deletion
            }
        } catch (ContainerizedJobAllocationException e) {
            // unable to run this job right now - reject
            LOGGER.warn("Job {} was rejected", jobId, e);
            jobResults.setFinishedWith(JobResults.Status.REJECTED);
            jobStatusReporter.reportJobFinishedAsync(jobResults);
            return jobResults; // TODO delete after testing web endpoint deletion
        }

        LOGGER.debug("Allocated container slot for {}", jobId);

        try {
            jobFilesService.prepareJobFiles(jobId, jobTask.getJobScript());

            String containerId = startJobContainer(jobId);
            containerizedJob.setContainerId(containerId);
            jobStatusReporter.reportJobStartedAsync(jobId);

            jobResults = getJobContainerResults(jobId, containerId);
            jobStatusReporter.reportJobFinishedAsync(jobResults);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job {}", jobId, e);
            throw new JobServiceException("Failed to run job " + jobId, e);
        } finally {
            jobFilesService.cleanupJobFiles(jobId);
            containerizedJobsPool.reclaim(containerizedJob);
        }

        return jobResults;
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

    private JobResults getJobContainerResults(String jobId,
                                              String containerId) {
        long jobTimeoutMs = sandboxProperties.getJobTimeoutMs();
        long containerOperationsTimeoutMs = sandboxProperties.getContainerOperationsTimeoutMs();

        try {
            JobResults jobResults = new JobResults(jobId);
            JobResults.Status completionStatus;
            if (completedInTime(containerId, jobTimeoutMs)) {
                completionStatus = containerService.getExitCode(containerId) == 0
                        ? JobResults.Status.SUCCEEDED
                        : JobResults.Status.FAILED;
            } else {
                LOGGER.info("Job {} exceeded time limit, container {} will be stopped", jobId, containerId);
                completionStatus = JobResults.Status.TIME_LIMIT_EXCEEDED;
                containerService.stopContainer(containerId, true);
            }

            jobResults.setFinishedWith(completionStatus);
            jobResults.setStdout(containerService.getStdout(containerId, containerOperationsTimeoutMs));
            jobResults.setStderr(containerService.getStderr(containerId, containerOperationsTimeoutMs));

            LOGGER.info("Finished job {} in container {}", jobId, containerId);

            return jobResults;
        } catch (RuntimeException e) {
            containerService.removeContainer(containerId, true);
            throw e;
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
        List<ContainerizedJob> containerizedJobs = containerizedJobsPool.getContainerizedJobs();
        LOGGER.info("{} shutdown. Shutting down and removing containers for containerized jobs: {}",
                JobServiceImpl.class.getSimpleName(), containerizedJobs);

        for (ContainerizedJob containerizedJob : containerizedJobs) {
            String containerId = containerizedJob.getContainerId();
            if (containerId != null) {
                containerService.stopContainer(containerId, false);
                containerService.removeContainer(containerId, false);
            }
        }
    }
}
