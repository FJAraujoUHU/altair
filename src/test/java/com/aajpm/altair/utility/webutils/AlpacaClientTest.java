package com.aajpm.altair.utility.webutils;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import reactor.core.publisher.Mono;

import org.junit.jupiter.api.Test;

public class AlpacaClientTest {
   
    static String url = "http://localhost:11111/";
    static AlpacaClient client = new AlpacaClient(url, 5000, 60000);
    
    @Test
    void testCameraPhoto() {
        Mono<BufferedImage> image = client.cameraPhoto();
        client.cameraPhoto().block();

    }
}
