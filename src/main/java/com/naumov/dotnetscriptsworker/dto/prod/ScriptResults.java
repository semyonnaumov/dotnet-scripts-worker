package com.naumov.dotnetscriptsworker.dto.prod;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ScriptResults {
    private JobCompletionStatus finishedWith;
    private String stdout;
    private String stderr;

    @Override
    public String toString() {
        return "ScriptResults{" +
                "finishedWith=" + finishedWith +
                ", stdout='" + stdout + '\'' +
                ", stderr='" + stderr + '\'' +
                '}';
    }
}
