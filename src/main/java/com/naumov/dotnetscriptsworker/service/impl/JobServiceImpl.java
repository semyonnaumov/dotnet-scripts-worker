package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.kafka.JobStatusReporter;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.ContainerService;
import com.naumov.dotnetscriptsworker.service.JobService;
import com.naumov.dotnetscriptsworker.service.ScriptFilesService;
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
    private static final Long IS_RUNNING_REQUEST_INTERVAL_MS = 100L;
    private final ContainerService containerService;
    private final ScriptFilesService scriptFilesService;
//    private final JobStatusReporter jobStatusReporter;
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
                          ScriptFilesService scriptFilesService,
//                          JobStatusReporter jobStatusReporter,
                          SandboxProperties sandboxProperties) {
        this.containerService = containerService;
        this.scriptFilesService = scriptFilesService;
//        this.jobStatusReporter = jobStatusReporter;
        this.sandboxProperties = sandboxProperties;
    }

    @Override
    public JobResults runJob(JobTask jobTask) {
        // Поднять контейнер, отвалиться по таймауту если не смогли
        // 1. создать временную папку с файлом скрипта на диске (внутри контейнера джавы)
        // 2. у нас есть ссылка на нужный тип docker image для дотнет раннера
        // 3. запустить контейнер из него, смонтировав как том эту временную папку ридонли
        //    docker run --name sr-<job_id> -v такое-то:туда-то:ro
        // 4. При старте контейнер должен прочитать эту папку
        // 5. Опрашивать контейнер
        // 6. после смерти контейнера прочитать его логи и вернуть пользователю:
        // docker logs <container_id>

        String jobId = jobTask.getJobId();
        JobResults jobResults = new JobResults(jobId);
        String containerName = sandboxProperties.getSandboxContainerPrefix() + jobId;
        try {
            LOGGER.debug("Starting job for jobId={}", jobId);
            scriptFilesService.createScriptFiles(jobId, jobTask.getJobScript());

            LOGGER.debug("Created script files for jobId={}", jobId);
            String tempDirPath = scriptFilesService.getTempJobScriptDirectoryPath(jobId);
            String containerId = containerService.createContainer(containerName, tempDirPath);

            LOGGER.debug("Starting container containerId={} for job jobId={}", containerId, jobId);
            containerService.startContainer(containerId);

            JobResults.Status completionStatus;
            if (completedInTime(containerId, sandboxProperties.getJobTimeoutMs())) {
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
            jobResults.setStdout(containerService.getStdout(containerId));
            jobResults.setStderr(containerService.getStderr(containerId));
        } catch (RuntimeException e) {
            LOGGER.error("Failed to run script job jobId={}", jobId, e);
            throw new JobServiceException("Failed to run script job jobId=" + jobId, e);
        } finally {
            containerService.removeForcefullyContainer(containerName);
            scriptFilesService.removeScriptFiles(jobId);
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
