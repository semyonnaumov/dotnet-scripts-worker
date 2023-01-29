package com.naumov.dotnetscriptsworker.model;

import lombok.Data;

@Data
public class ContainerResults {
    private String containerId;
    private Integer exitCode;
    private String logs;
}
