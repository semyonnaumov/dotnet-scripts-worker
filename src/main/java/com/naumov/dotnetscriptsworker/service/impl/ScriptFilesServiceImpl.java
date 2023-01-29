package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.service.ScriptFilesService;
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
import java.util.stream.Stream;

@Service
public class ScriptFilesServiceImpl implements ScriptFilesService {
    private static final Logger LOGGER = LogManager.getLogger(ScriptFilesServiceImpl.class);
    private static final String DIR_SEPARATOR = "/";
    private final SandboxProperties sandboxProperties;

    @Autowired
    public ScriptFilesServiceImpl(SandboxProperties sandboxProperties) {
        this.sandboxProperties = sandboxProperties;
    }

    @Override
    public void createScriptFiles(String jobId, String script) {
        Path dirPath = Paths.get(getTempJobScriptDirectoryPath(jobId));
        Path filePath = Paths.get(getTempJobScriptFilePath(jobId));
        try {
            Files.createDirectories(dirPath);
            Files.createFile(filePath);
            Files.writeString(filePath, script);
        } catch (IOException e) {
            LOGGER.error("Failed to create script file path={}", filePath, e);
            throw new ScriptFilesServiceException("Failed to create script file path=" + filePath, e);
        }
    }

    @Override
    public void removeScriptFiles(String jobId) {
        Path path = Paths.get(getTempJobScriptDirectoryPath(jobId));

        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (IOException e) {
            LOGGER.error("Failed to delete script directory path={}", path, e);
            throw new ScriptFilesServiceException("Failed to delete script directory path=" + path, e);
        }
    }

    @Override
    public String getTempJobScriptDirectoryPath(String jobId) {
        return sandboxProperties.getScriptFilesOnHostDir() + DIR_SEPARATOR + jobId;
    }

    @Override
    public String getTempJobScriptFilePath(String jobId) {
        return getTempJobScriptDirectoryPath(jobId) + DIR_SEPARATOR + sandboxProperties.getScriptFileName();
    }
}
