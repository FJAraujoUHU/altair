package com.aajpm.altair.utility.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aajpm.altair.config.AstrometricsConfig;
import com.aajpm.altair.utility.Interval;

public class HorizonsEphemeridesSolverTest {

    private AstrometricsConfig config = new AstrometricsConfig();

    HorizonsEphemeridesSolverTest() {
        super();
        config.setSiteLatitude(37.2597);
        config.setSiteLongitude(-6.9325);
        config.setSiteElevation(130.0);
        config.setHorizonLine(0.0);
    }
    
    @Test
    void testGetAltAz() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        double[] expectedAltAz = { 2.589722257, 121.561196473 };
        double[] altAz = solver.getAltAz("Sun", Instant.parse("2012-12-31T08:00:00Z"))
        .block();

        boolean isAltCorrect = Math.abs(expectedAltAz[0] - altAz[0]) < 0.1;
        boolean isAzCorrect = Math.abs(expectedAltAz[1] - altAz[1]) < 0.1;
        assertTrue(isAzCorrect && isAltCorrect);

    }

    @Test
    void testGetRiseSetTimes() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        Interval expectedRiseSetTimes = new Interval(Instant.parse("2012-12-31T07:40:00Z"),Instant.parse("2012-12-31T17:23:00Z"));
        Interval riseSetTimes = solver.getRiseSetTime("Sun", new Interval(Instant.parse("2012-12-31T00:00:00Z"), Duration.ofDays(1)), 0.0).block();

        assertEquals(expectedRiseSetTimes, riseSetTimes);

    }

    @Test
    void testIsVisible() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        boolean expected = true;
        boolean isVisible = solver.isVisible("Sun", Instant.parse("2012-12-31T12:00:00Z")).block();

        assertEquals(expected, isVisible);

    }

    @Test
    void testLST() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        double expected = 10 + 3/60.0 + 16/3600.0;
        double lst = solver.getLST(Instant.parse("2023-06-09T17:19:43Z"), true).block();

        assertTrue(Math.abs(expected - lst) < 0.01);
    }

    @Test
    void testRaDecToAltAz() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        double ra = 18.61555;
        double dec = 38.783;
        Instant time = Instant.parse("2023-06-09T00:00:00Z");
        double[] expectedAltAz = { 67.0035, 77.1148 };
        
        double[] altAz = solver.raDecToAltAz(ra, dec, 37.2597, -6.9325, time).block();
        System.out.println(altAz[0] + " " + altAz[1]);

        assertEquals(expectedAltAz[0], altAz[0], 0.5);
        assertEquals(expectedAltAz[1], altAz[1], 0.5);
    }

    @Test
    void testGetRiseSetTimesSpace() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(config);

        int minuteMargin = 10;

        double ra = 18.61666;
        double dec = 38.78333;

        Interval expectedRiseSetTimes = new Interval(Instant.parse("2023-06-09T00:00:00Z"),Instant.parse("2023-06-09T10:30:00Z"));
        Interval riseSetTimes = solver.getRiseSetTime(ra, dec, 37.2597, -6.9325, new Interval(Instant.parse("2023-06-09T00:00:00Z"), Duration.ofDays(1)), 0.0).block();

        System.out.println(riseSetTimes.toString());

        boolean isRiseCorrect = Duration.between(expectedRiseSetTimes.getStart(), riseSetTimes.getStart()).abs().toMinutes() < minuteMargin;
        boolean isSetCorrect =  Duration.between(expectedRiseSetTimes.getEnd(), riseSetTimes.getEnd()).abs().toMinutes() < minuteMargin;
        assertTrue(isRiseCorrect && isSetCorrect);
    }
}
