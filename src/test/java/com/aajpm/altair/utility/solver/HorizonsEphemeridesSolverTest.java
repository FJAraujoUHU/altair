package com.aajpm.altair.utility.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class HorizonsEphemeridesSolverTest {
    
    @Test
    void testGetAltAz() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

        double[] expectedAltAz = { 2.589722257, 121.561196473 };
        double[] altAz = solver.getAltAz("Sun", Instant.parse("2012-12-31T08:00:00Z"))
        .block();

        boolean isAltCorrect = Math.abs(expectedAltAz[0] - altAz[0]) < 0.1;
        boolean isAzCorrect = Math.abs(expectedAltAz[1] - altAz[1]) < 0.1;
        assertTrue(isAzCorrect && isAltCorrect);

    }

    @Test
    void testGetRiseSetTimes() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

        Instant[] expectedRiseSetTimes = { Instant.parse("2012-12-31T07:40:00Z"),Instant.parse("2012-12-31T17:23:00Z") };
        Instant[] riseSetTimes = solver.getRiseSetTimes("Sun", Instant.parse("2012-12-31T00:00:00Z"), Duration.ofDays(1), 0.0).block();

        boolean isRiseCorrect = expectedRiseSetTimes[0].equals(riseSetTimes[0]);
        boolean isSetCorrect = expectedRiseSetTimes[1].equals(riseSetTimes[1]);
        assertTrue(isRiseCorrect && isSetCorrect);

    }

    @Test
    void testIsVisible() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

        boolean expected = true;
        boolean isVisible = solver.isVisible("Sun", Instant.parse("2012-12-31T12:00:00Z")).block();

        assertEquals(expected, isVisible);

    }

    @Test
    void testLST() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

        double expected = 10 + 3/60.0 + 16/3600.0;
        double lst = solver.getLST(Instant.parse("2023-06-09T17:19:43Z"), true).block();

        assertTrue(Math.abs(expected - lst) < 0.01);
    }

    @Test
    void testRaDecToAltAz() {
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

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
        EphemeridesSolver solver = new HorizonsEphemeridesSolver(37.2597, -6.9325, 130);

        int minuteMargin = 10;

        Instant[] expectedRiseSetTimes = { Instant.parse("2023-06-09T00:00:00Z"),Instant.parse("2023-06-09T10:30:00Z") };
        Instant[] riseSetTimes = solver.getRiseSetTimes(18.61666, 38.78333, 37.2597, -6.9325, Instant.parse("2023-06-09T00:00:00Z"), Duration.ofDays(1), 0.0).block();

        System.out.println(riseSetTimes[0] + " " + riseSetTimes[1]);

        boolean isRiseCorrect = Duration.between(expectedRiseSetTimes[0], riseSetTimes[0]).abs().toMinutes() < minuteMargin;
        boolean isSetCorrect =  Duration.between(expectedRiseSetTimes[1], riseSetTimes[1]).abs().toMinutes() < minuteMargin;
        assertTrue(isRiseCorrect && isSetCorrect);
    }
}
