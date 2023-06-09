package com.aajpm.altair.utility.solver;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

public abstract class EphemeridesSolver {

    protected double latitude;  // Positive for North, negative for South
    protected double longitude; // Positive for East, negative for West
    protected double elevation; // In meters

    protected static final Instant J2000_INSTANT = Instant.parse("2000-01-01T11:58:55.816Z");

    /**
     * Creates a new EphemeridesSolver.
     * @param latitude The latitude of the observatory in degrees, positive for North, negative for South.
     * @param longitude The longitude of the observatory in degrees, positive for East, negative for West.
     * @param elevation The elevation of the observatory in meters.
     */
    protected EphemeridesSolver(double latitude, double longitude, double elevation) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
    }

    /**
     * Calculates the current Altitude and Azimuth of a body in the Solar system.
     * @param body The name of the body to calculate the Altitude and Azimuth for.
     * @return A Mono containing the Altitude and Azimuth of the body, on a Mono<Error> if the body is not found
     */
    public Mono<double[]> getAltAz(String body) {
        return getAltAz(body, Instant.now());
    }

    /**
     * Calculates the Altitude and Azimuth of a body in the Solar system at a given time.
     * @param body The name of the body to calculate the Altitude and Azimuth for.
     * @param time The time to calculate the Altitude and Azimuth for.
     * @return A Mono containing the Altitude and Azimuth of the body, on a Mono<Error> if the body is not found
     */
    public abstract Mono<double[]> getAltAz(String body, Instant time);
    
    /**
     * Calculates if the body is above the horizon line at the current time.
     * @param body The name of the body to check.
     * @return A Mono containing true if the body is above the horizon line, false if it is below, or a Mono<Error> if the body is not found.
     */
    public Mono<Boolean> isVisible(String body) {
        return isVisible(body, Instant.now(), 0.0);
    }

    /**
     * Calculates if the body is above the horizon line at a given time.
     * @param body The name of the body to check.
     * @param time The time at which to check if the body is above the horizon line.
     * @return A Mono containing true if the body is above the horizon line, false if it is below, or a Mono<Error> if the body is not found.
     */
    public Mono<Boolean> isVisible(String body, Instant time) {
        return isVisible(body, time, 0.0);
    }

    /**
     * Calculates if the body is above the designated altitude at the current time.
     * @param body The name of the body to check.
     * @param altitude The altitude to check if the body is above.
     * @return A Mono containing true if the body is above the horizon line, false if it is below, or a Mono<Error> if the body is not found.
     */
    public Mono<Boolean> isVisible(String body, double altitude) {
        return isVisible(body, Instant.now(), altitude);
    }

    /**
     * Calculates if the body is above the designated altitude at a given time.
     * @param body The name of the body to check.
     * @param time The time at which to check if the body is above the horizon line.
     * @param altitude The altitude to check if the body is above.
     * @return A Mono containing true if the body is above the horizon line, false if it is below, or a Mono<Error> if the body is not found.
     */
    public abstract Mono<Boolean> isVisible(String body, Instant time, double altitude);


    /**
     * Calculates the closest rise and set times of a body in the Solar system to the current time.
     * @param body The name of the body to calculate the rise and set times for.
     * @return A Mono containing the closest rise and set times of the body, or if it is already visible, the current time and the setting time, or a Mono<Error> if the body is not found or is out of bounds.
     */
    public Mono<Instant[]> getRiseSetTimes(String body) {
        return getRiseSetTimes(body, Instant.now(), Duration.ofHours(12), 0.0);
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system to the current time.
     * @param body The name of the body to calculate the rise and set times for.
     * @param howFar The duration to search for the rise and set times.
     * @return A Mono containing the closest rise and set times of the body, or if it is already visible, the current time and the setting time, or a Mono<Error> if the body is not found or is out of bounds.
     */
    public Mono<Instant[]> getRiseSetTimes(String body, Duration howFar) {
        return getRiseSetTimes(body, Instant.now(), howFar, 0.0);
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system to the specified time.
     * @param body The name of the body to calculate the rise and set times for.
     * @param baseTime The base time to calculate the rise and set times for.
     * @param howFar The duration to search for the rise and set times.
     * @return A Mono containing the closest rise and set times of the body, or if it is already visible, the current time and the setting time, or a Mono<Error> if the body is not found or is out of bounds.
     */
    public Mono<Instant[]> getRiseSetTimes(String body, Instant baseTime, Duration howFar) {
        return getRiseSetTimes(body, baseTime, howFar, 0.0);
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system to the current time.
     * @param body The name of the body to calculate the rise and set times for.
     * @param howFar The duration to search for the rise and set times.
     * @param altitude The altitude to consider as set or rise.
     * @return A Mono containing the closest rise and set times of the body, or if it is already visible, the current time and the setting time, or a Mono<Error> if the body is not found or is out of bounds.
     */
    public Mono<Instant[]> getRiseSetTimes(String body, Duration howFar, double altitude) {
        return getRiseSetTimes(body, Instant.now(), howFar, altitude);
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system to the specified time.
     * @param body The name of the body to calculate the rise and set times for.
     * @param baseTime The base time to calculate the rise and set times for.
     * @param howFar The duration to search for the rise and set times.
     * @param altitude The altitude to consider as set or rise.
     * @return A Mono containing the closest rise and set times of the body, or if it is already visible, the current time and the setting time, or a Mono<Error> if the body is not found or is out of bounds.
     */
    public abstract Mono<Instant[]> getRiseSetTimes(String body, Instant baseTime, Duration howFar, double altitude);

    /**
     * Calculates the time since the J2000 epoch.
     * @return The time since the J2000 epoch.
     */
    public static Duration getJ2000Time() {
        return Duration.between(J2000_INSTANT, Instant.now());
    }

    /**
     * Calculates the time since the J2000 epoch.
     * @param time The time to calculate the time since the J2000 epoch for.
     * @return The time since the J2000 epoch.
     */
    public static Duration getJ2000Time(Instant time) {
        return Duration.between(J2000_INSTANT, time);
    }

    public Mono<Double> getLST(Instant time, boolean useHoursInstead) {
        return getLST(time, this.longitude, useHoursInstead);
    }

    /**
     * Calculates the Local Sidereal Time (LST) for the given time and longitude.
     * @param longitude The longitude of the location to calculate the LST for.
     * @param useHoursInstead Whether to return the LST in decimal hours (true) or decimal degrees (false).
     * @param time The time to calculate the LST for.
     * @return The LST for the given time.
     */
    public abstract Mono<Double> getLST(Instant time, double longitude, boolean useHoursInstead);

    /**
     * Basic conversion from Right Ascension and Declination to Altitude and Azimuth, about 2% error.
     * @param ra The Right Ascension of the object.
     * @param dec The Declination of the object.
     * @param latitude The latitude of the location to calculate the Altitude and Azimuth for.
     * @param longitude The longitude of the location to calculate the Altitude and Azimuth for.
     * @param time The time to calculate the Altitude and Azimuth for.
     * @return An array containing the Altitude and Azimuth of the object.
     */
    public abstract Mono<double[]> raDecToAltAz(double ra, double dec, double latitude, double longitude, Instant time);

    /**
     * Calculates the rise and set times of a body OUTSIDE the Solar system.
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param latitude The latitude of the location to calculate the rise and set times for, in decimal degrees north positive.
     * @param longitude The longitude of the location to calculate the rise and set times for, in decimal degrees east positive.
     * @param baseTime The base time to calculate the rise and set times for.
     * @param howFar The duration to search for the rise and set times.
     * @param altitude The altitude to consider as set or rise, in decimal degrees.
     * @return A Mono containing the rise and set times of the body, or if it is already visible, the current time and the setting time.
     */
    public abstract Mono<Instant[]> getRiseSetTimes(double ra, double dec, double latitude, double longitude, Instant baseTime, Duration howFar, double targetAltitude);

    public static double degToRad(double deg) {
        return deg * Math.PI / 180.0;
    }

    public static double radToDeg(double rad) {
        return rad * 180.0 / Math.PI;
    }

}
