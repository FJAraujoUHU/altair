package com.aajpm.altair.entity.converter;

import java.time.Duration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a {@link Duration} to a {@link Long} representing the number of minutes.
 */
@Converter
public class DurationMinutesConverter implements AttributeConverter<Duration, Long> {

    @Override
    public Long convertToDatabaseColumn(Duration duration) {
        return duration != null ? duration.toMinutes() : null;
    }

    @Override
    public Duration convertToEntityAttribute(Long minutes) {
        return minutes != null ? Duration.ofMinutes(minutes) : null;
    }
    
}
