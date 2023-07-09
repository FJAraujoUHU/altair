package com.aajpm.altair.service.observatory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import com.aajpm.altair.config.ObservatoryConfig.FocuserConfig;
import com.aajpm.altair.service.observatory.FocuserService.FocuserCapabilities;
import com.aajpm.altair.utility.webutils.AlpacaClient;

@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2925")
@Deprecated
public class ASCOMFocuserServiceTest {

    static AlpacaClient client;
    static FocuserConfig config;

    static int deviceNumber = 0;

    static String url = "http://localhost:32323/";

    //static ASCOMFocuserService prepService;

    @BeforeAll
    static void beforeClass() {
        client = new AlpacaClient(url, 5000, 120000);
        config = new FocuserConfig();
        config.setBacklashSteps(0);
        config.setPositionTolerance(5);
    }

    @BeforeEach
    void before() throws Exception {
        Thread.sleep(1000);
    }

    @Test
    @Order(1)
    void testConnect() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testConnect");
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber,  config, 1000, 60000);
        
        service.connect().block(Duration.ofSeconds(10));

        boolean isConnected = service.isConnected().block(Duration.ofSeconds(10));

        assertTrue(isConnected);
    }

    @Test
    @Order(2)
    void testDisconnect() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testConnect");
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber, config, 1000, 60000);
        
        service.connect().block(Duration.ofSeconds(10));

        assertTrue(service.isConnected().block(Duration.ofSeconds(10)), "Service should be connected before disconnecting");

        service.disconnect().block(Duration.ofSeconds(10));

        assertFalse(service.isConnected().block(Duration.ofSeconds(10)), "Service should be disconnected after disconnecting");

    }

    @Test
    @Order(3)
    void testMove() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testMove");
        int timeoutMs = 120000;
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber, config, 1000, timeoutMs);
        int error = 5;
        
        
        service.connect().block(Duration.ofSeconds(10));

        assertTrue(service.isConnected().block(Duration.ofSeconds(10)), "Service should be connected before testing this");



        FocuserCapabilities caps = service.getCapabilities().block(Duration.ofSeconds(10));
        int maxStep = caps.maxStep();
        int minStep = 0;

        int newPosition = minStep;
        service.move(newPosition).subscribe();

        // check every 500ms for 10 seconds
        long timer = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timer) {
            Thread.sleep(1000);
            if (!service.isMoving().block()) {
                break;
            }
        }

        assertTrue(System.currentTimeMillis() < timer, "Move should have completed in under " + timeoutMs + " ms");
        int realPosition = service.getPosition().block();
        assertTrue(Math.abs(realPosition - newPosition) < error, "Move should have been within 5 steps of the target");

        newPosition = maxStep;
        service.move(newPosition).subscribe();

        // check every 500ms for 10 seconds
        timer = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timer) {
            Thread.sleep(500);
            if (!service.isMoving().block()) {
                break;
            }
        }

        assertTrue(System.currentTimeMillis() < timer, "Move should have completed in under " + timeoutMs + " ms");
        realPosition = service.getPosition().block();
        assertTrue(Math.abs(realPosition - newPosition) < error, "Move should have been within 5 steps of the target");
    }

    @Test
    @Order(4)
    void testMoveAwait() throws Exception {
        System.out.println("/////////////////////////////////////////////////////////////testMove");
        int timeoutMs = 120000;
        ASCOMFocuserService service = new ASCOMFocuserService(client, deviceNumber, config, 1000, timeoutMs);
        int error = 5;
        
        service.connect().block(Duration.ofSeconds(10));

        assertTrue(service.isConnected().block(Duration.ofSeconds(10)), "Service should be connected before testing this");


        FocuserCapabilities caps = service.getCapabilities().block(Duration.ofSeconds(10));
        int maxStep = caps.maxStep();
        int minStep = 0;

        int newPosition = minStep;
        service.moveAwait(newPosition).block(Duration.ofMillis(timeoutMs));

        int realPosition = service.getPosition().block();
        assertTrue(Math.abs(realPosition - newPosition) < error, "Move should have been within 5 steps of the target");

        newPosition = maxStep;
        service.moveAwait(newPosition).block(Duration.ofMillis(timeoutMs));

        realPosition = service.getPosition().block();
        assertTrue(Math.abs(realPosition - newPosition) < error, "Move should have been within 5 steps of the target");
    }

}
