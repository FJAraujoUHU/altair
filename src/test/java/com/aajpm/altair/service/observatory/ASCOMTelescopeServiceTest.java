package com.aajpm.altair.service.observatory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

import com.aajpm.altair.utility.webutils.AlpacaClient;

@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2925")
@Deprecated     // This test was written for the old ASCOM service definition, which has been overhauled and tests may no longer work as intended
public class ASCOMTelescopeServiceTest {

    static AlpacaClient client;

    static int deviceNumber = 0;

    static String url = "http://localhost:32323/";

    static ASCOMTelescopeService service;

    @BeforeAll
    static void beforeClass() {
        client = new AlpacaClient(url, 5000, 60000);
        service = new ASCOMTelescopeService(client, deviceNumber, 2000, 60000);

        if (!service.isConnected().block()) {
            service.connect();
        }
        if (service.isTracking().block()) {
            service.setTracking(false);
        }
        if (!service.isParked().block()) {
            service.park();
        }
        service.disconnect();
    }

    @BeforeEach
    void beforeEach() {
        if (!service.isConnected().block()) {
            service.connect();
        }
    }

    @AfterAll
    static void afterClass() {
        if (service.isConnected().block()) {
            service.disconnect();
        }
    }

    @Test
    @Order(1)
    void testSlewToAltAz() throws Exception {
        int maxError = 5;  // max degrees of error between target and actual
        System.out.println("--- testSlewToAltAz -> error tolerance: " + maxError + " degrees ---");

        service.connect();
        if (service.isParked().block()) {
            service.unpark();
        }
        double altAz[] = service.getAltAz().block();

        double startAlt = altAz[0];
        double startAz = altAz[1];
        double targetAlt = Math.random() * 90;
        double targetAz = Math.random() * 360;
        System.out.println("\tStarting position: " + startAlt + "º, " + startAz + "º");
        System.out.println("\tTarget position: " + targetAlt + "º, " + targetAz + "º");
        System.out.println("\tSlewing...");

        service.slewToAltAzAwait(targetAlt, targetAz);
        altAz = service.getAltAz().block();
        double actualAlt = altAz[0];
        double actualAz = altAz[1];
        System.out.println("\tActual position: " + actualAlt + "º, " + actualAz + "º");
        assertNotEquals(startAlt, targetAlt);
        assertNotEquals(startAz, targetAz);
        assertTrue(Math.abs(targetAlt - actualAlt) < maxError);
        assertTrue(Math.abs(targetAz - actualAz) < maxError);
        System.out.println("--- testSlewToAltAz -> success ---");
    }

    @Test
    @Order(2)
    void testSlewToAltAzAsync() throws Exception {
        int waitTime = 1000;  // time to wait for slew to do something
        System.out.println("--- testSlewToAltAzAsync ---");

        service.connect();
        if (service.isParked().block()) {
            service.unpark();
        }
        double altAz[] = service.getAltAz().block();

        double startAlt = altAz[0];
        double startAz = altAz[1];
        double targetAlt = Math.random() * 90;
        double targetAz = Math.random() * 360;
        System.out.println("\tStarting position: " + startAlt + "º, " + startAz + "º");
        System.out.println("\tTarget position: " + targetAlt + "º, " + targetAz + "º");
        System.out.println("\tSlewing...");

        service.slewToAltAz(targetAlt, targetAz);
        Thread.sleep(waitTime);
        
        altAz = service.getAltAz().block();
        double actualAlt = altAz[0];
        double actualAz = altAz[1];
        System.out.println("\tActual position: " + actualAlt + "º, " + actualAz + "º");
        assertNotEquals(startAlt, actualAlt);
        assertNotEquals(startAz, actualAz);
        System.out.println("--- testSlewToAltAz -> success ---");
    }

    @Test
    @Order(3)
    void testAbortSlew() throws Exception {
        service.connect();
        if (service.isParked().block()) {
            service.unpark();
        }
        service.slewToAltAz(45, 90);
        Thread.sleep(1000);
        assertTrue(service.isSlewing().block());
        service.abortSlew();
        Thread.sleep(100);
        assertFalse(service.isSlewing().block());
    }

}
