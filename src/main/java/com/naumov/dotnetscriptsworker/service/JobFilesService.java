package com.naumov.dotnetscriptsworker.service;

import java.nio.file.Path;

public interface JobFilesService {

    void prepareJobFiles(String jobId, String script);

    void cleanupJobFiles(String jobId);

    Path getTempJobScriptDirectoryPath(String jobId);

    Path getTempJobScriptFilePath(String jobId);
}
