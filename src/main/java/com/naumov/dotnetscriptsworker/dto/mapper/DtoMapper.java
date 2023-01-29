package com.naumov.dotnetscriptsworker.dto.mapper;

public interface DtoMapper<F, T> {

    T map(F entity);
}
