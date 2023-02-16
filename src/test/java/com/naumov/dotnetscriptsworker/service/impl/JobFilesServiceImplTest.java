package com.naumov.dotnetscriptsworker.service.impl;

import com.naumov.dotnetscriptsworker.AbstractIntegrationTest;
import com.naumov.dotnetscriptsworker.config.props.SandboxProperties;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.JobFilesService;
import com.naumov.dotnetscriptsworker.service.exception.ScriptFilesServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JobFilesServiceImplTest {
    private static final String JOB_SCRIPT_FILE_NAME = "script.txt";
    private static SandboxProperties sandboxProperties;
    private static JobFilesService jobFilesService;
    private static Path tempDirPath;

    @BeforeAll
    public static void init() throws IOException {
        URL tempfilesUrl = JobFilesServiceImplTest.class.getClassLoader().getResource("tempfiles");
        if (tempfilesUrl == null) throw new IllegalStateException("Folder 'tempfiles' must be on the test classpath");

        sandboxProperties = new SandboxProperties();
        sandboxProperties.setJobScriptFileName(JOB_SCRIPT_FILE_NAME);
        sandboxProperties.setJobFilesHostDir(tempfilesUrl.getPath());
        jobFilesService = new JobFilesServiceImpl(sandboxProperties);
        tempDirPath = Paths.get(sandboxProperties.getJobFilesHostDir());

        assertTrue(Files.isDirectory(tempDirPath));
        if (!isDirectoryEmpty(tempDirPath)) {
            clearDirectory(tempDirPath);
        }
    }

    @BeforeEach
    void setup() throws IOException {
        assertTrue(isDirectoryEmpty(tempDirPath));
    }

    @AfterEach
    void tearDown() throws IOException {
        clearDirectory(tempDirPath);
    }

    private static void clearDirectory(Path path) throws IOException {
        try (Stream<Path> pathStream = Files.walk(path)) {
            List<Path> pathsToDelete = pathStream.sorted(Comparator.reverseOrder()).toList();
            for (Path pathToDelete : pathsToDelete) {
                if (!path.equals(pathToDelete)) {
                    Files.deleteIfExists(pathToDelete);
                }
            }
        }
    }

    public static boolean isDirectoryEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }

        return false;
    }

    @Test
    void prepareJobFiles() throws IOException {
        UUID jobId = UUID.randomUUID();
        String nugetConfig = "<config />";
        String script = "script";

        JobTask jobTask = JobTask.builder()
                .jobId(jobId)
                .jobConfig(JobTask.JobConfig.builder().nugetConfigXml(nugetConfig).build())
                .jobScript(script)
                .build();

        jobFilesService.prepareJobFiles(jobTask);

        Path jobFilesTempDir = Paths.get(tempDirPath.toString(), jobId.toString());
        File jobFilesTempDirFile = jobFilesTempDir.toFile();
        assertTrue(jobFilesTempDirFile.exists());
        assertTrue(jobFilesTempDirFile.isDirectory());

        Path nugetConfigPath = Paths.get(tempDirPath.toString(), jobId.toString(), "NuGet.Config");
        File nugetConfigFile = nugetConfigPath.toFile();
        assertTrue(nugetConfigFile.exists());
        assertTrue(nugetConfigFile.isFile());
        assertEquals(nugetConfig, Files.readString(nugetConfigPath));

        Path scriptPath = Paths.get(tempDirPath.toString(), jobId.toString(), sandboxProperties.getJobScriptFileName());
        File scriptFile = scriptPath.toFile();
        assertTrue(scriptFile.exists());
        assertTrue(scriptFile.isFile());
        assertEquals(script, Files.readString(scriptPath));
    }

    @Test
    void prepareJobFilesNoNuget() throws IOException {
        UUID jobId = UUID.randomUUID();
        String script = "script";

        JobTask jobTask = JobTask.builder()
                .jobId(jobId)
                .jobScript(script)
                .build();

        jobFilesService.prepareJobFiles(jobTask);

        Path jobFilesTempDir = Paths.get(tempDirPath.toString(), jobId.toString());
        File jobFilesTempDirFile = jobFilesTempDir.toFile();
        assertTrue(jobFilesTempDirFile.exists());
        assertTrue(jobFilesTempDirFile.isDirectory());

        Path nugetConfigPath = Paths.get(tempDirPath.toString(), jobId.toString(), "NuGet.Config");
        File nugetConfigFile = nugetConfigPath.toFile();
        assertFalse(nugetConfigFile.exists());

        Path scriptPath = Paths.get(tempDirPath.toString(), jobId.toString(), sandboxProperties.getJobScriptFileName());
        File scriptFile = scriptPath.toFile();
        assertTrue(scriptFile.exists());
        assertTrue(scriptFile.isFile());
        assertEquals(script, Files.readString(scriptPath));
    }

    @Test
    void prepareJobFilesNoScript() throws IOException {
        UUID jobId = UUID.randomUUID();

        JobTask jobTask = JobTask.builder()
                .jobId(jobId)
                .build();

        assertThrows(NullPointerException.class, () -> jobFilesService.prepareJobFiles(jobTask));
        assertTrue(isDirectoryEmpty(tempDirPath));
    }

    @Test
    void prepareJobFilesNoId() throws IOException {
        JobTask jobTask = JobTask.builder().build();

        assertThrows(NullPointerException.class, () -> jobFilesService.prepareJobFiles(jobTask));
        assertTrue(isDirectoryEmpty(tempDirPath));
    }

    @Test
    void cleanupJobFilesJobExists() throws IOException {
        UUID jobId = UUID.randomUUID();
        String nugetConfig = "<config />";
        String script = "script";

        JobTask jobTask = JobTask.builder()
                .jobId(jobId)
                .jobConfig(JobTask.JobConfig.builder().nugetConfigXml(nugetConfig).build())
                .jobScript(script)
                .build();

        jobFilesService.prepareJobFiles(jobTask);
        Path jobDirPath = Paths.get(tempDirPath.toString(), jobId.toString());
        assertFalse(isDirectoryEmpty(jobDirPath));

        jobFilesService.cleanupJobFiles(jobId);
        assertTrue(isDirectoryEmpty(tempDirPath));
    }

    @Test
    void cleanupJobFilesJobNotExists() throws IOException {
        UUID jobId = UUID.randomUUID();
        String nugetConfig = "<config />";
        String script = "script";

        JobTask jobTask = JobTask.builder()
                .jobId(jobId)
                .jobConfig(JobTask.JobConfig.builder().nugetConfigXml(nugetConfig).build())
                .jobScript(script)
                .build();

        jobFilesService.prepareJobFiles(jobTask);
        Path jobDirPath = Paths.get(tempDirPath.toString(), jobId.toString());
        assertFalse(isDirectoryEmpty(jobDirPath));

        assertThrows(ScriptFilesServiceException.class, () -> jobFilesService.cleanupJobFiles(UUID.randomUUID()));
        assertFalse(isDirectoryEmpty(jobDirPath));
    }

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        URL tempfilesUrl = AbstractIntegrationTest.class.getClassLoader().getResource("tempfiles");
        if (tempfilesUrl == null) throw new IllegalStateException("Folder 'tempfiles' must be on the test classpath");

        registry.add("worker.sandbox.job-files-host-dir", tempfilesUrl::getPath);
    }
}