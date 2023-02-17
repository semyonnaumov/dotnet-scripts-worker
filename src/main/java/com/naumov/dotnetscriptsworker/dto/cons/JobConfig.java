package com.naumov.dotnetscriptsworker.dto.cons;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class JobConfig {
    private String nugetConfigXml;

    @Override
    public String toString() {
        return "JobConfig{" +
                "nugetConfigXml='" + nugetConfigXml + '\'' +
                '}';
    }
}
