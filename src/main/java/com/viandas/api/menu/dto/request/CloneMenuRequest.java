package com.viandas.api.menu.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CloneMenuRequest(
        @NotNull LocalDate date,
        LocalTime orderClosesAt
) {
}
