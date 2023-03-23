package com.aajpm.altair.service.observatory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

import com.aajpm.altair.utility.webutils.AlpacaClient;

@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2925")
public class ASCOMFocuserServiceTest {

    static AlpacaClient client;

    static int deviceNumber = 0;

    static String url = "http://localhost:32323/";

    //static ASCOMFocuserService prepService;

    @BeforeAll
    static void beforeClass() {
        client = new AlpacaClient(url, 5000, 60000);
    }

    @BeforeEach
    void before() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    @Order(1)
    void testConnect() throws Exception {
        
        System.out.println("/////////////////////////////////////////////////////////////testConnect");
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber);
        service.connect();
        Thread.sleep(1000);

        boolean isConnected = service.isConnected().block();

        assertNotNull(service.absolute);
        assertTrue(isConnected);
    }

    @Test
    @Order(2)
    void testDisconnect() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testConnect");
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber);
        service.connect();
        Thread.sleep(1000);

        assertTrue(service.isConnected().block(), "Service should be connected before disconnecting");

        service.disconnect();
        Thread.sleep(1000);

        assertFalse(service.isConnected().block(), "Service should be disconnected after disconnecting");

    }

    @Test
    @Order(3)
    void testMove() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testMove");
        int movement = 100;
        int error = 5;

        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber);
        service.connect();
        Thread.sleep(1000);

        assertTrue(service.isConnected().block(), "Service should be connected before testing this");

        int startingPosition = service.getPosition().block();
        int newPosition = startingPosition + movement;
        service.move(newPosition);

        // check every 500ms for 10 seconds
        long timer = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < timer) {
            Thread.sleep(500);
            if (!service.isMoving().block()) {
                break;
            }
        }

        assertTrue(System.currentTimeMillis() < timer, "Move should have completed in under 10 seconds");
        int realPosition = service.getPosition().block();
        assertTrue(Math.abs(realPosition - newPosition) < error, "Move should have been within 5 steps of the target");
    }

}
