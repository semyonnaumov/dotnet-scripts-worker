package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.kafka.JobStatusReporter;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.JobService;
import com.naumov.dotnetscriptsworker.service.JobFilesService;
import com.naumov.dotnetscriptsworker.service.exception.JobServiceException;
import com.naumov.dotnetscriptsworker.util.Timer;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobServiceImpl implements JobService {
    private static final Logger LOGGER = LogManager.getLogger(JobServiceImpl.class);
    private final ContainerService containerService;
    private final JobFilesService jobFilesService;
    private final JobStatusReporter jobStatusReporter;
    private final SandboxProperties sandboxProperties;
    private volatile Integer maxContainers = 0;
    private final AtomicInteger availableContainers = new AtomicInteger(0);

    @PostConstruct
    public void initContainerCounter() {
        maxContainers = sandboxProperties.getMaxConcurrentSandboxes();
        availableContainers.set(maxContainers);
    }

    @Autowired
    public JobServiceImpl(ContainerService containerService,
                          JobFilesService jobFilesService,
                          JobStatusReporter jobStatusReporter,
                          SandboxProperties sandboxProperties) {
        this.containerService = containerService;
        this.jobFilesService = jobFilesService;
        this.jobStatusReporter = jobStatusReporter;
        this.sandboxProperties = sandboxProperties;
    }

    @Override
    public JobResults runJob(JobTask jobTask) {
        String jobId = jobTask.getJobId();
        LOGGER.debug("Starting job for jobId={}", jobId);

        JobResults jobResults;
        try {
            jobFilesService.prepareJobFiles(jobId, jobTask.getJobScript());
            jobResults = runJobInContainer(jobId);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run job jobId={}", jobId, e);
            throw new JobServiceException("Failed to run job jobId=" + jobId, e);
        } finally {
            jobFilesService.cleanupJobFiles(jobId);
        }

        return jobResults;
    }

    private JobResults runJobInContainer(String jobId) {
        JobResults jobResults = new JobResults(jobId);
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

            LOGGER.debug("Starting container containerId={} for job jobId={}", containerId, jobId);
            containerService.startContainer(containerId);
            jobStatusReporter.reportJobStartedAsync(jobId);

            JobResults.Status completionStatus;
            Long jobTimeoutMs = sandboxProperties.getJobTimeoutMs();
            if (completedInTime(containerId, jobTimeoutMs)) {
                completionStatus = containerService.getExitCode(containerId) == 0
                        ? JobResults.Status.SUCCEEDED
                        : JobResults.Status.FAILED;
            } else {
                LOGGER.warn("Job jobId={}, running in container containerId={} " +
                        "has exceeded time limit and will be stopped", jobId, containerId);
                completionStatus = JobResults.Status.TIME_LIMIT_EXCEEDED;
                containerService.stopContainer(containerId);
            }

            jobResults.setFinishedWith(completionStatus);
            jobResults.setStdout(containerService.getStdout(containerId, jobTimeoutMs));
            jobResults.setStderr(containerService.getStderr(containerId, jobTimeoutMs));
            jobStatusReporter.reportJobFinishedAsync(jobResults);
        } finally {
            containerService.removeForcefullyContainer(containerName);
        }

        return jobResults;
    }

    private boolean completedInTime(String containerId, Long jobTimeoutMs) {
        Timer jobTimer = new Timer(jobTimeoutMs);
        jobTimer.start();
        while (!jobTimer.isFinished()) {
            if (!containerService.isRunning(containerId)) return true;
        }

        return false;
    }

    // TODO methods left for concurrency

    private boolean tryAcquireContainerSlot(long timeoutMs) {
        Thread currentThread = Thread.currentThread();

        long start = System.currentTimeMillis();
        long end = start + timeoutMs;
        while (System.currentTimeMillis() < end && !currentThread.isInterrupted()) {
            int expected = availableContainers.get();
            if (expected > 0) {
                if (availableContainers.compareAndSet(expected, expected - 1)) return true;
            }
        }

        return false;
    }

    private void releaseContainerSlot() {
        availableContainers.incrementAndGet();
    }
}
