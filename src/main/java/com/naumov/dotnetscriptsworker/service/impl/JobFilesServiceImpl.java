package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.service.JobFilesService;
import com.naumov.dotnetscriptsworker.service.exception.ScriptFilesServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// done?
@Service
public class JobFilesServiceImpl implements JobFilesService {
    private static final Logger LOGGER = LogManager.getLogger(JobFilesServiceImpl.class);
    private final SandboxProperties sandboxProperties;

    @Autowired
    public JobFilesServiceImpl(SandboxProperties sandboxProperties) {
        this.sandboxProperties = sandboxProperties;
    }

    @Override
    public void prepareJobFiles(String jobId, String jobScript) {
        Path tempDirPath = getTempJobScriptDirectoryPath(jobId);
        Path scriptFilePath = getTempJobScriptFilePath(jobId);
        try {
            Files.createDirectories(tempDirPath);
            Files.createFile(scriptFilePath);
            Files.writeString(scriptFilePath, jobScript);
            LOGGER.debug("Created temp file at path={} for job jobId={}", scriptFilePath, jobId);
        } catch (IOException e) {
            LOGGER.error("Failed to prepare script files path={}", scriptFilePath, e);
            throw new ScriptFilesServiceException("Failed to prepare script files path=" + scriptFilePath, e);
        }
    }

    @Override
    public void cleanupJobFiles(String jobId) {
        Path tempDirectoryPath = getTempJobScriptDirectoryPath(jobId);
        try (Stream<Path> pathStream = Files.walk(tempDirectoryPath)) {
            List<Path> pathsToDelete = pathStream.sorted(Comparator.reverseOrder()).toList();
            for (Path pathToDelete : pathsToDelete) {
                Files.deleteIfExists(pathToDelete);
            }
            LOGGER.debug("Removed temp directory at path={} for job jobId={}", tempDirectoryPath, jobId);
        } catch (IOException e) {
            LOGGER.error("Failed to cleanup script files path={}", tempDirectoryPath, e);
            throw new ScriptFilesServiceException("Failed to cleanup script files path=" + tempDirectoryPath, e);
        }
    }

    @Override
    public Path getTempJobScriptDirectoryPath(String jobId) {
        return Paths.get(sandboxProperties.getScriptFilesOnHostDir(), jobId)
                .toAbsolutePath();
    }

    @Override
    public Path getTempJobScriptFilePath(String jobId) {
        return Paths.get(sandboxProperties.getScriptFilesOnHostDir(), jobId, sandboxProperties.getScriptFileName())
                .toAbsolutePath();
    }
}
