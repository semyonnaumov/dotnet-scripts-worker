package com.naumov.dotnetscriptsworker.dto.prod;

import lombok.Data;

@Data
public class JobStartedMessage {
    private final String jobId;
}
