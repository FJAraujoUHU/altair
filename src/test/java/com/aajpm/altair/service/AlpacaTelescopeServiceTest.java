package com.aajpm.altair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;


public class AlpacaTelescopeServiceTest {
    
    AlpacaTelescopeService service = new AlpacaTelescopeService("http://localhost:32323");
    
    @Test
    void testMakeGetReq() {
        System.out.println("----------------------testMakeGetReq");
        JsonNode result = service.makeGetReq("/management/v1/configureddevices");
        System.out.println("--- Response ---");
        System.out.println(result.toPrettyString());
        assertEquals(0, result.findValue("ErrorNumber").asInt());
    }

    @Test
    void testMakePutReq() {
        System.out.println("----------------------testMakePutReq");

        boolean before = service.makeGetReq("/api/v1/telescope/0/connected")
                            .findValue("Value").asBoolean();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Connected", before ? "false" : "true");

        JsonNode result = service.makePutReq("/api/v1/telescope/0/connected", formData);
        System.out.println("--- Response ---");
        System.out.println(result.toPrettyString());

        boolean after = service.makeGetReq("/api/v1/telescope/0/connected")
                            .findValue("Value").asBoolean();

        MultiValueMap<String, String> formData2 = new LinkedMultiValueMap<>();
        formData2.add("Connected", String.valueOf(before));
        service.makePutReq("/api/v1/telescope/0/connected", formData2);             

        boolean after2 = service.makeGetReq("/api/v1/telescope/0/connected")
                            .findValue("Value").asBoolean();


        assertNotEquals(before, after);
        assertEquals(before, after2);
    }
}
