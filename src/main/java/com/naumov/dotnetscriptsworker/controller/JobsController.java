package com.naumov.dotnetscriptsworker.controller;

import com.naumov.dotnetscriptsworker.dto.JobTaskDto;
import com.naumov.dotnetscriptsworker.dto.mapper.DtoMapper;
import com.naumov.dotnetscriptsworker.dto.mapper.impl.JobTaskFromDtoMapper;
import com.naumov.dotnetscriptsworker.model.JobResults;
import com.naumov.dotnetscriptsworker.model.JobTask;
import com.naumov.dotnetscriptsworker.service.JobService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RequestMapping("/jobs")
@RestController
public class JobsController {
    private static final Logger LOGGER = LogManager.getLogger(JobsController.class);
    private final JobService jobService;
    private final DtoMapper<JobTaskDto, JobTask> jobTaskFromDtoMapper;

    @Autowired
    public JobsController(JobService jobService, JobTaskFromDtoMapper jobTaskFromDtoMapper) {
        this.jobService = jobService;
        this.jobTaskFromDtoMapper = jobTaskFromDtoMapper;
    }

    @PostMapping
    public JobResults runJob(@RequestBody JobTaskDto jobTaskDto) {
        LOGGER.info("Received job task jobId={}", jobTaskDto.getJobId());

        return jobService.runJob(jobTaskFromDtoMapper.map(jobTaskDto));
    }
}
