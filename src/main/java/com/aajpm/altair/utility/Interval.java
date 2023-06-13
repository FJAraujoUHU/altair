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

    protected static final Interval EMPTY = new Interval(Instant.EPOCH, Duration.ZERO);

    /**
     * Represents a time interval. Immutable.
     * 
     * @param start The start of the interval.
     * 
     * @param duration The duration of the interval. If negative,
     *                 it will be compensated by moving the start
     *                 backwards by the same amount.
     */
    public Interval(Instant start, Duration duration) {
        if (start == null)
            throw new IllegalArgumentException("start cannot be null");
        if (duration == null)
            throw new IllegalArgumentException("duration cannot be null");

        if (duration.isNegative()) {
            Instant newStart = start.plus(duration);
            this.start = newStart;
            this.duration = duration.negated();
        } else {
            this.start = start;
            this.duration = duration;
        }
    }

    /**
     * Represents a time interval. Immutable.
     * <p> If end is before start, the start and end will be swapped.
     * 
     * @param start The start of the interval.
     * @param end The end of the interval.
     */
    public Interval(Instant start, Instant end) {
        this(start, Duration.between(start, end));
    }

    /**
     * Returns the start of the interval.
     * 
     * @return The start of the interval, as an Instant.
     * 
     * @throws IllegalStateException If the interval is empty.
     */
    public Instant getStart() throws IllegalStateException {
        if (this.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");

        return start;
    }

    /**
     * Returns the duration of the interval.
     * 
     * @return The duration of the interval, as a Duration.
     *         Will never be negative.
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Returns the duration of the interval, in days. 
     * <p> Acts as a shortcut for getDuration().toDays()
     * 
     * @return The duration of the interval, in days, as a long.
     *         Will never be negative.
     */
    public long getDurationDays() {
        return duration.toDays();
    }

    /**
     * Returns the duration of the interval, in hours. 
     * <p> Acts as a shortcut for getDuration().toHours()
     * 
     * @return The duration of the interval, in hours, as a long.
     *         Will never be negative.
     */
    public long getDurationHours() {
        return duration.toHours();
    }

    /**
     * Returns the duration of the interval, in minutes. 
     * <p> Acts as a shortcut for getDuration().toMinutes()
     * 
     * @return The duration of the interval, in minutes, as a long.
     *         Will never be negative.
     */
    public long getDurationMinutes() {
        return duration.toMinutes();
    }

    /**
     * Returns the duration of the interval, in seconds. 
     * <p> Acts as a shortcut for getDuration().getSeconds()
     * 
     * @return The duration of the interval, in seconds, as a long.
     *         Will never be negative.
     */
    public long getDurationSeconds() {
        return duration.getSeconds();
    }

    /**
     * Returns the duration of the interval, in milliseconds. 
     * <p> Acts as a shortcut for getDuration().toMillis()
     * 
     * @return The duration of the interval, in milliseconds, as a long.
     *         Will never be negative.
     */
    public long getDurationMillis() {
        return duration.toMillis();
    }

    /**
     * Returns the end of the interval.
     * 
     * @return The end of the interval, as an Instant.
     * 
     * @throws IllegalStateException If the interval is empty.
     */
    public Instant getEnd() throws IllegalStateException {
        if (this.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");

        return start.plus(duration);
    }


    /**
     * Returns whether the given instant is contained within the interval.
     * 
     * @param instant The instant to check.
     * 
     * @return Whether the given instant is contained within the interval.
     */
    public boolean contains(Instant instant) {
        if (this.isEmpty())
            return false;

        return instant.isAfter(start) && instant.isBefore(getEnd());
    }

    /**
     * Returns whether the given interval is contained within this interval.
     * 
     * @param interval The interval to check.
     * 
     * @return Whether the given interval is contained within this interval.
     */
    public boolean contains(Interval interval) {
        if (this.isEmpty() || interval.isEmpty())
            return false;

        return contains(interval.getStart()) && contains(interval.getEnd());
    }

    /**
     * Returns whether the given interval overlaps with this interval.
     * 
     * @param interval The interval to check
     * 
     * @return Whether the given interval overlaps with this interval.
     */
    public boolean hasOverlap(Interval interval) {
        if (this.isEmpty() || interval.isEmpty())
            return false;

        return contains(interval.getStart()) || contains(interval.getEnd()) || interval.contains(start);
    }

    /**
     * Returns the overlap between this interval and the given interval.
     * <p> If the intervals do not overlap, returns {@link Interval#empty()}.
     * 
     * @param other The overlaping interval.
     * 
     * @return The overlap between this interval and the given interval.
     *         If the intervals do not overlap, returns {@link Interval#empty()}.
     */
    public Interval overlap(Interval other) {
        if (!hasOverlap(other))
            return Interval.empty();

        Instant newStart = start.isAfter(other.getStart()) ? start : other.getStart();
        Instant newEnd = getEnd().isBefore(other.getEnd()) ? getEnd() : other.getEnd();

        return new Interval(newStart, newEnd);
    }

    /**
     * Returns whether the given interval is before this interval.
     * 
     * @param interval The interval to check.
     * 
     * @return Whether the given interval is before this interval.
     * 
     * @throws IllegalStateException If either interval is empty.
     */
    public boolean isBefore(Interval interval) throws IllegalStateException {
        if (this.isEmpty() || interval.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");

        return getEnd().isBefore(interval.getStart());
    }

    /**
     * Returns whether the given instant is before this interval.
     * 
     * @param instant The instant to check.
     * 
     * @return Whether the given instant is before this interval.
     * 
     * @throws IllegalStateException If the interval is empty.
     */
    public boolean isBefore(Instant instant) throws IllegalStateException {
        if (this.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");
        return getEnd().isBefore(instant);
    }

    /**
     * Returns whether the given interval is after this interval.
     * 
     * @param interval The interval to check.
     * 
     * @return Whether the given interval is after this interval.
     * 
     * @throws IllegalStateException If either interval is empty.
     */
    public boolean isAfter(Interval interval) throws IllegalStateException {
        if (this.isEmpty() || interval.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");
        return interval.isBefore(this);
    }

    /**
     * Returns whether the given instant is after this interval.
     * 
     * @param instant The instant to check.
     * 
     * @return Whether the given instant is after this interval.
     * 
     * @throws IllegalStateException If the interval is empty.
     */
    public boolean isAfter(Instant instant) throws IllegalStateException {
        if (this.isEmpty())
            throw new IllegalStateException("Trying to operate on an empty interval");
        return instant.isBefore(start);
    }

    /**
     * Checks if this interval is empty. An interval is empty if its start and
     * end are equal.
     * 
     * @return {@code True} if this interval is empty, {@code False} otherwise
     */
    public boolean isEmpty() {
        return (duration == null) || duration.isZero();
    }

    /**
     * Returns an empty interval.
     * 
     * @return An empty interval, with a start and end equal to the epoch.
     */
    public static Interval empty() {
        return Interval.EMPTY;
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
