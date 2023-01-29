package com.naumov.dotnetscriptsworker.service;

public interface ScriptFilesService {

    void createScriptFiles(String jobId, String script);

    void removeScriptFiles(String jobId);

    String getTempJobScriptDirectoryPath(String jobId);

    String getTempJobScriptFilePath(String jobId);
}
