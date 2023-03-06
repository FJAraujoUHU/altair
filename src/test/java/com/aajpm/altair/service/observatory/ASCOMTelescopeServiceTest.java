package com.aajpm.altair.service.observatory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.aajpm.altair.utility.webutils.AlpacaClient;

public class ASCOMTelescopeServiceTest {

    static AlpacaClient client;

    static int deviceNumber = 0;

    static String url = "http://localhost:32323/";

    static ASCOMTelescopeService service;

    @BeforeAll
    static void beforeClass() {
        client = new AlpacaClient(url, 5000, 60000);
        service = new ASCOMTelescopeService(client, deviceNumber);

        if (!service.isConnected()) {
            service.connect();
        }
        if (service.isTracking()) {
            service.setTracking(false);
        }
        if (!service.isParked()) {
            service.park();
        }
        service.disconnect();
    }

    @BeforeEach
    void beforeEach() {
        if (!service.isConnected()) {
            service.connect();
        }
    }

    @AfterAll
    static void afterClass() {
        if (service.isConnected()) {
            service.disconnect();
        }
    }

    @Test
    void testAbortSlew() throws Exception {
        service.connect();
        if (service.isParked()) {
            service.unpark();
        }
        service.slewToAltAzAsync(45, 90);
        Thread.sleep(1000);
        assertTrue(service.isSlewing());
        service.abortSlew();
        Thread.sleep(100);
        assertFalse(service.isSlewing());
    }

    @Test
    void testSlewToAltAz() throws Exception {
        int maxError = 5;  // max degrees of error between target and actual
        System.out.println("--- testSlewToAltAz -> error tolerance: " + maxError + " degrees ---");

        service.connect();
        if (service.isParked()) {
            service.unpark();
        }
        double altAz[] = service.getAltAz();

        double startAlt = altAz[0];
        double startAz = altAz[1];
        double targetAlt = Math.random() * 90;
        double targetAz = Math.random() * 360;
        System.out.println("\tStarting position: " + startAlt + "º, " + startAz + "º");
        System.out.println("\tTarget position: " + targetAlt + "º, " + targetAz + "º");
        System.out.println("\tSlewing...");

        service.slewToAltAz(targetAlt, targetAz);
        altAz = service.getAltAz();
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
    void testSlewToAltAzAsync() throws Exception {
        int waitTime = 1000;  // time to wait for slew to do something
        System.out.println("--- testSlewToAltAzAsync ---");

        service.connect();
        if (service.isParked()) {
            service.unpark();
        }
        double altAz[] = service.getAltAz();

        double startAlt = altAz[0];
        double startAz = altAz[1];
        double targetAlt = Math.random() * 90;
        double targetAz = Math.random() * 360;
        System.out.println("\tStarting position: " + startAlt + "º, " + startAz + "º");
        System.out.println("\tTarget position: " + targetAlt + "º, " + targetAz + "º");
        System.out.println("\tSlewing...");

        service.slewToAltAzAsync(targetAlt, targetAz);
        Thread.sleep(waitTime);
        
        altAz = service.getAltAz();
        double actualAlt = altAz[0];
        double actualAz = altAz[1];
        System.out.println("\tActual position: " + actualAlt + "º, " + actualAz + "º");
        assertNotEquals(startAlt, actualAlt);
        assertNotEquals(startAz, actualAz);
        System.out.println("--- testSlewToAltAz -> success ---");
    }

}
