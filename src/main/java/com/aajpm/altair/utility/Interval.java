package com.aajpm.altair.utility;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * Represents a time interval. Immutable.
 */
public class Interval implements Serializable {

    private final Instant start;
    private final Duration duration;

    /**
     * Represents a time interval. Immutable.
     * @param start The start of the interval
     * @param duration The duration of the interval
     */
    public Interval(Instant start, Duration duration) {
        this.start = start;
        this.duration = duration;
    }

    /**
     * Represents a time interval. Immutable.
     * @param start The start of the interval
     * @param end The end of the interval
     */
    public Interval(Instant start, Instant end) {
        this.start = start;
        this.duration = Duration.between(start, end);
    }

    /**
     * Returns the start of the interval
     * @return The start of the interval, as an Instant
     */
    public Instant getStart() {
        return start;
    }

    /**
     * Returns the duration of the interval
     * @return The duration of the interval, as a Duration
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Returns the duration of the interval, in days. 
     * <p> Acts as a shortcut for getDuration().toDays()
     * 
     * @return The duration of the interval, in days, as a long
     */
    public long getDurationDays() {
        return duration.toDays();
    }

    /**
     * Returns the duration of the interval, in hours. 
     * <p> Acts as a shortcut for getDuration().toHours()
     * 
     * @return The duration of the interval, in hours, as a long
     */
    public long getDurationHours() {
        return duration.toHours();
    }

    /**
     * Returns the duration of the interval, in minutes. 
     * <p> Acts as a shortcut for getDuration().toMinutes()
     * 
     * @return The duration of the interval, in minutes, as a long
     */
    public long getDurationMinutes() {
        return duration.toMinutes();
    }

    /**
     * Returns the duration of the interval, in seconds. 
     * <p> Acts as a shortcut for getDuration().getSeconds()
     * 
     * @return The duration of the interval, in seconds, as a long
     */
    public long getDurationSeconds() {
        return duration.getSeconds();
    }

    /**
     * Returns the duration of the interval, in milliseconds. 
     * <p> Acts as a shortcut for getDuration().toMillis()
     * 
     * @return The duration of the interval, in milliseconds, as a long
     */
    public long getDurationMillis() {
        return duration.toMillis();
    }

    /**
     * Returns the end of the interval
     * @return The end of the interval, as an Instant
     */
    public Instant getEnd() {
        return start.plus(duration);
    }


    /**
     * Returns whether the given instant is contained within the interval
     * @param instant The instant to check
     * @return Whether the given instant is contained within the interval
     */
    public boolean contains(Instant instant) {
        return instant.isAfter(start) && instant.isBefore(getEnd());
    }

    /**
     * Returns whether the given interval is contained within this interval
     * @param interval The interval to check
     * @return Whether the given interval is contained within this interval
     */
    public boolean contains(Interval interval) {
        return contains(interval.getStart()) && contains(interval.getEnd());
    }

    /**
     * Returns whether the given interval overlaps with this interval
     * @param interval The interval to check
     * @return Whether the given interval overlaps with this interval
     */
    public boolean overlaps(Interval interval) {
        return contains(interval.getStart()) || contains(interval.getEnd()) || interval.contains(start);
    }

    /**
     * Returns whether the given interval is before this interval
     * @param interval The interval to check
     * @return Whether the given interval is before this interval
     */
    public boolean isBefore(Interval interval) {
        return getEnd().isBefore(interval.getStart());
    }

    /**
     * Returns whether the given instant is before this interval
     * @param instant The instant to check
     * @return Whether the given instant is before this interval
     */
    public boolean isBefore(Instant instant) {
        return getEnd().isBefore(instant);
    }

    /**
     * Returns whether the given interval is after this interval
     * @param interval The interval to check
     * @return Whether the given interval is after this interval
     */
    public boolean isAfter(Interval interval) {
        return interval.isBefore(this);
    }

    /**
     * Returns whether the given instant is after this interval
     * @param instant The instant to check
     * @return Whether the given instant is after this interval
     */
    public boolean isAfter(Instant instant) {
        return instant.isBefore(start);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((duration == null) ? 0 : duration.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Interval other = (Interval) obj;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        if (duration == null) {
            if (other.duration != null)
                return false;
        } else if (!duration.equals(other.duration))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + start + " -> " + this.getEnd() + "]";
    }
    
    
}
