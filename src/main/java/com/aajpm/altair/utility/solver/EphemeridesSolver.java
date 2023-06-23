package com.aajpm.altair.utility.solver;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import com.aajpm.altair.config.AstrometricsConfig;
import com.aajpm.altair.utility.Interval;

/**
 * An abstract class for calculating the coordinates and rise/set times of
 * celestial bodies.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Ephemeris">Ephemeris</a>
 */
public abstract class EphemeridesSolver {

    /**
     * The AstrometricsConfig to use as base for calculations.
     */
    protected AstrometricsConfig config;

    /**
     * The J2000 epoch, as an Instant.
     */
    protected static final Instant J2000_INSTANT = Instant.parse("2000-01-01T11:58:55.816Z");

    /**
     * Creates a new EphemeridesSolver.
     * 
     * @param config The AstrometricsConfig to use for calculations.
     */
    protected EphemeridesSolver(AstrometricsConfig config) {
        this.config = config;
    }

    /**
     * Gets the AstrometricsConfig used for calculations.
     * 
     * @return The AstrometricsConfig used for calculations.
     */
    public AstrometricsConfig getConfig() {
        return config;
    }

    /**
     * Calculates the current Altitude and Azimuth of a body in the Solar system.
     * 
     * @param body The name of the body to calculate the Altitude and Azimuth for.
     * 
     * @return A {@link Mono} containing the Altitude and Azimuth of the body, or
     *         a {@link Mono#error(Throwable)} if the body is not found.
     */
    public Mono<double[]> getAltAz(String body) {
        return getAltAz(body, Instant.now());
    }

    /**
     * Calculates the Altitude and Azimuth of a body in the Solar system at a
     * given time.
     * 
     * @param body The name of the body to calculate the Altitude and Azimuth for.
     * @param time The time to calculate the Altitude and Azimuth for.
     * 
     * @return A {@link Mono} containing the Altitude and Azimuth of the body, or
     *         a {@link Mono#error(Throwable)} if the body is not found.
     */
    public abstract Mono<double[]> getAltAz(String body, Instant time);
    
    /**
     * Calculates if the body is currently above the configured horizon line.
     * 
     * @param body The name of the body to check.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *         horizon line, {@code false} if it is below, or a
     *         {@link Mono#error(Throwable)} if the body is not found.
     */
    public Mono<Boolean> isVisible(String body) {
        return isVisible(body, Instant.now(), config.getHorizonLine());
    }

    /**
     * Calculates if the body is above the configured horizon line at a given time.
     * 
     * @param body The name of the body to check.
     * @param time The time at which to check if the body is above the horizon line.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *         horizon line, {@code false} if it is below, or a
     *         {@link Mono#error(Throwable)} if the body is not found.
     */
    public Mono<Boolean> isVisible(String body, Instant time) {
        return isVisible(body, time, config.getHorizonLine());
    }

    /**
     * Calculates if the body is above the designated altitude at a given time.
     * 
     * @param body The name of the body to check.
     * @param time The time at which to check if the body is above the horizon line.
     * @param targetAltitude The altitude to check if the body is above.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *         altitude, {@code false} if it is below, or a
     *         {@link Mono#error(Throwable)} if the body is not found.
     */
    public abstract Mono<Boolean> isVisible(String body, Instant time, double targetAltitude);

    /**
     * Calculates if a body at the given Right Ascension and Declination is
     * currently above the configured horizon line and visible i.e. not in daylight.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *        horizon line, {@code false} if it is below.
     */
    public Mono<Boolean> isVisible(double ra, double dec) {
        return isVisible(ra, dec, Instant.now(), config.getHorizonLine());
    }

    /**
     * Calculates if a body at the given Right Ascension and Declination is above
     * the configured horizon line and visible i.e. not in daylight at a given time.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param time The time at which to check if the body is above the horizon line.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *        horizon line, {@code false} if it is below.
     */
    public Mono<Boolean> isVisible(double ra, double dec, Instant time) {
        return isVisible(ra, dec, time, config.getHorizonLine());
    }

    /**
     * Calculates if a body at the given Right Ascension and Declination is above
     * the designated altitude and visible i.e. not in daylight at a given time.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param time The time at which to check if the body is above the horizon line.
     * @param targetAltitude The altitude to check if the body is above.
     * 
     * @return A {@link Mono} containing {@code true} if the body is above the
     *         altitude, {@code false} if it is below.
     */
    public Mono<Boolean> isVisible(double ra, double dec, Instant time, double targetAltitude) {
        Mono<Boolean> isSunOut = isVisible("Sun", time, 0.0);
        Mono<Boolean> isVisible = raDecToAltAz(ra, dec, config.getSiteLatitude(), config.getSiteLongitude(), time)
                                 .map(altAz -> altAz[0] > targetAltitude);

        return Mono.zip(isSunOut, isVisible)
                   .map(tuple -> !tuple.getT1() && tuple.getT2());
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system
     * to the specified time.
     * 
     * @param body The name of the body to calculate the rise and set times for.
     * @param searchInterval The interval to search for the rise and set times.
     * 
     * @return <ul>
     *         <li>If the body is not visible within the search interval, an
     *             empty {@link Interval} is returned.</li>
     *         <li>If the body is visible within the search interval, an
     *             {@link Interval} containing the closest rise and set times
     *             is returned. The result will be clamped to stay within the
     *             {@code searchInterval}</li>
     *         <li>If the body is not found or is out of bounds, a
     *             {@link Mono#error(Throwable)} is returned.</li>
     *         </ul>
     */
    public Mono<Interval> getRiseSetTime(String body, Interval searchInterval) {
        return getRiseSetTime(body, searchInterval, config.getHorizonLine());
    }

    /**
     * Calculates the closest rise and set times of a body in the Solar system
     * to the specified time.
     * 
     * @param body The name of the body to calculate the rise and set times for.
     * @param searchInterval The interval to search for the rise and set times.
     * @param targetAltitude The altitude to consider as set or rise.
     * 
     * @return <ul>
     *         <li>If the body is not visible within the search interval, an
     *             empty {@link Interval} is returned.</li>
     *         <li>If the body is visible within the search interval, an
     *             {@link Interval} containing the closest rise and set times
     *             is returned. The result will be clamped to stay within the
     *             {@code searchInterval}</li>
     *         <li>If the body is not found or is out of bounds, a
     *             {@link Mono#error(Throwable)} is returned.</li>
     *         </ul>
     */
    public abstract Mono<Interval> getRiseSetTime(String body, Interval searchInterval, double targetAltitude);

    /**
     * Calculates the closest rise and set times of a body OUTSIDE the Solar
     * system to the current time.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param searchInterval The interval to search for the rise and set times.
     * 
     * @return <ul>
     *         <li>If the body is not visible within the search interval, an
     *             empty {@link Interval} is returned.</li>
     *         <li>If the body is visible within the search interval, an
     *             {@link Interval} containing the closest rise and set times
     *             is returned. The result will be clamped to stay within the
     *             {@code searchInterval}</li>
     *         <li>If the body is not found or is out of bounds, a
     *             {@link Mono#error(Throwable)} is returned.</li>
     *         </ul>
     */
    public Mono<Interval> getRiseSetTime(double ra, double dec, Interval searchInterval) {
        return getRiseSetTime(ra, dec, config.getSiteLatitude(), config.getSiteLongitude(), searchInterval, config.getHorizonLine());
    }

    /**
     * Calculates the rise and set times of a body OUTSIDE the Solar system.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param searchInterval The interval to search for the rise and set times.
     * @param targetAltitude The altitude to consider as set or rise, in decimal degrees.
     * 
     * @return <ul>
     *         <li>If the body is not visible within the search interval, an
     *             empty {@link Interval} is returned.</li>
     *         <li>If the body is visible within the search interval, an
     *             {@link Interval} containing the closest rise and set times
     *             is returned. The result will be clamped to stay within the
     *             {@code searchInterval}</li>
     *         <li>If the body is not found or is out of bounds, a
     *             {@link Mono#error(Throwable)} is returned.</li>
     *         </ul>
     */
    public Mono<Interval> getRiseSetTime(double ra, double dec, Interval searchInterval, double targetAltitude) {
        return getRiseSetTime(ra, dec, config.getSiteLatitude(), config.getSiteLongitude(), searchInterval, targetAltitude);
    }

    /**
     * Calculates the rise and set times of a body OUTSIDE the Solar system.
     * 
     * @param ra The Right Ascension of the body, in decimal hours.
     * @param dec The Declination of the body, in decimal degrees.
     * @param latitude The latitude of the location to calculate the rise and set times for, in decimal degrees north positive.
     * @param longitude The longitude of the location to calculate the rise and set times for, in decimal degrees east positive.
     * @param searchInterval The interval to search for the rise and set times.
     * @param targetAltitude The altitude to consider as set or rise, in decimal degrees.
     * 
     * @return <ul>
     *         <li>If the body is not visible within the search interval, an
     *             empty {@link Interval} is returned.</li>
     *         <li>If the body is visible within the search interval, an
     *             {@link Interval} containing the closest rise and set times
     *             is returned. The result will be clamped to stay within the
     *             {@code searchInterval}</li>
     *         <li>If the body is not found or is out of bounds, a
     *             {@link Mono#error(Throwable)} is returned.</li>
     *         </ul>
     */
    public abstract Mono<Interval> getRiseSetTime(double ra, double dec, double latitude, double longitude, Interval searchInterval, double targetAltitude);

    

    /**
     * Calculates the time since the J2000 epoch.
     * 
     * @return A {@link Duration} containing the time elapsed
     *         since the J2000 epoch.
     */
    public static Duration getJ2000Time() {
        return Duration.between(J2000_INSTANT, Instant.now());
    }

    /**
     * Calculates the time since the J2000 epoch.
     * 
     * @param time The time to calculate the time since the J2000 epoch for.
     * 
     * @return A {@link Duration} containing the time elapsed
     *         since the J2000 epoch.
     */
    public static Duration getJ2000Time(Instant time) {
        return Duration.between(J2000_INSTANT, time);
    }

    /**
     * Calculates the Local Sidereal Time (LST) for the current time and
     * configured longitude.
     * 
     * @param useHoursInstead {@code true} to return the LST in decimal hours,
     *                        {@code false} to return it in decimal degrees.
     *
     * @return The LST for the current time, clamped to a day's duration.
     */
    public Mono<Double> getLST(boolean useHoursInstead) {
        return getLST(Instant.now(), useHoursInstead);
    }

    /**
     * Calculates the Local Sidereal Time (LST) for the given time and
     * configured longitude.
     * 
     * @param time The time to calculate the LST for.
     * @param useHoursInstead {@code true} to return the LST in decimal hours,
     *                        {@code false} to return it in decimal degrees.
     *
     * @return The LST for the current time, clamped to a day's duration.
     */
    public Mono<Double> getLST(Instant time, boolean useHoursInstead) {
        return getLST(time, config.getSiteLongitude(), useHoursInstead);
    }

    /**
     * Calculates the Local Sidereal Time (LST) for the given time and
     * longitude.
     * 
     * @param time            The time to calculate the LST for.
     * @param longitude       The longitude to calculate the LST for, in decimal
     *                        degrees east positive.
     * @param useHoursInstead {@code true} to return the LST in decimal hours,
     *                        {@code false} to return it in decimal degrees.
     *
     * @return The LST for the current time, clamped to a day's duration.
     */
    public abstract Mono<Double> getLST(Instant time, double longitude, boolean useHoursInstead);

    /**
     * Basic conversion from Right Ascension and Declination to Altitude and
     * Azimuth. Might be inaccurate for some objects.
     * 
     * @param ra The Right Ascension of the object.
     * @param dec The Declination of the object.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param time The time to calculate for.
     * 
     * @return An array containing the Altitude and Azimuth of the object.
     */
    public abstract Mono<double[]> raDecToAltAz(double ra, double dec, double latitude, double longitude, Instant time);

    /**
     * An estimation of the precision of the
     * {@link #raDecToAltAz(double, double, double, double, Instant)} function.
     * 
     * @return The precision of the calculation, as a percentage (0.0 - 100.0)
     */
    public abstract double getRaDecToAltAzPrecision();

    /**
     * Simple conversion from decimal degrees to radians.
     * Shortcut for {@code deg * Math.PI / 180.0}.
     * 
     * @param deg The value in decimal degrees.
     * 
     * @return The value in radians.
     */
    public static double degToRad(double deg) {
        return deg * Math.PI / 180.0;
    }

    /**
     * Simple conversion from radians to decimal degrees.
     * Shortcut for {@code rad * 180.0 / Math.PI}.
     * 
     * @param rad The value in radians.
     * 
     * @return The value in decimal degrees.
     */
    public static double radToDeg(double rad) {
        return rad * 180.0 / Math.PI;
    }

}
