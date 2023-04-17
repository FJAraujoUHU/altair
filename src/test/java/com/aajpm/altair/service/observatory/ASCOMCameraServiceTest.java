package com.aajpm.altair.service.observatory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;

import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.util.FitsOutputStream;

import com.aajpm.altair.service.observatory.ASCOMCameraService.HeaderData;

public class ASCOMCameraServiceTest {
    
    static AlpacaClient client;

    static int deviceNumber = 0;

    static String url = "http://localhost:32323/";
    
    
    @Test
    void testReadImageArray() throws Exception {
        int width = 256;
        int height = 128;
        JsonNode imageArray = imageArrayGenerator(width, height);
        HeaderData hd;
        hd = new HeaderData(
            "2023-01-13T12:23:45",
            30,
            -10.0,
            -10.0,
            0,
            null,
            null,
            null,
            null,
            null,
            width,
            height
        );

        ImageHDU hdu = ASCOMCameraService.readImageArray(imageArray, hd);
        Fits f = new Fits();
        f.addHDU(hdu);
        FitsOutputStream gzip = new FitsOutputStream(new GZIPOutputStream(new FileOutputStream("testReadImageArray.fits.gz")));
        f.write("testReadImageArray.fits");
        f.write(gzip);
        gzip.close();
        f.close();

        assertNotNull(hdu);
    }

    @Test
    void testReadImageBytes() throws Exception {
        int width = 256;
        int height = 128;
        byte[] imagebytes = imageBytesGenerator(width, height);
        HeaderData hd;
        hd = new HeaderData(
            "2023-01-13T12:23:45",
            30,
            -10.0,
            -10.0,
            0,
            null,
            null,
            null,
            null,
            null,
            width,
            height
        );

        ImageHDU hdu = ASCOMCameraService.readImageBytes(imagebytes, hd);
        Fits f = new Fits();
        f.addHDU(hdu);
        FitsOutputStream gzip = new FitsOutputStream(new GZIPOutputStream(new FileOutputStream("testReadImageBytes.fits.gz")));
        f.write("testReadImageBytes.fits");
        f.write(gzip);
        gzip.close();
        f.close();

        assertNotNull(hdu);
    }

    byte[] imageBytesGenerator(int width, int height) {
        byte[] imagebytes = new byte[44 + (width * height)];
        System.arraycopy(toInt32LEBytes(1), 0, imagebytes, 0, 4);   // MetadataVersion
        System.arraycopy(toInt32LEBytes(0), 0, imagebytes, 4, 4);   // ErrorNumber
        System.arraycopy(toUInt32LEBytes(0), 0, imagebytes, 8, 4);  // ClientTransactionID
        System.arraycopy(toUInt32LEBytes(0), 0, imagebytes, 12, 4); // ServerTransactionID
        System.arraycopy(toInt32LEBytes(44), 0, imagebytes, 16, 4); // DataStart
        System.arraycopy(toInt32LEBytes(2), 0, imagebytes, 20, 4);  // ImageElementType (int16)
        System.arraycopy(toInt32LEBytes(6), 0, imagebytes, 24, 4);  // TransmissionElementType (byte)
        System.arraycopy(toInt32LEBytes(2), 0, imagebytes, 28, 4);  // Rank
        System.arraycopy(toInt32LEBytes(width), 0, imagebytes, 32, 4);  // Dimension1/Width
        System.arraycopy(toInt32LEBytes(height), 0, imagebytes, 36, 4); // Dimension2/Height
        System.arraycopy(toInt32LEBytes(0), 0, imagebytes, 40, 4);  // Dimension3/Depth
        
        /*for (int i = 0; i < width * height; i++) {
            int value = (i % height) % 256;
            imagebytes[44 + i] = (byte) (value & 0xFF);
        }*/
        for (int i = 0; i < width * height; i++) {
            int value = i % 256;
            imagebytes[44 + i] = (byte) value;
        }
        
        return imagebytes;
    }

    JsonNode imageArrayGenerator(int width, int height) {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode json = mapper.createObjectNode();
        ObjectNode jsonNode = (ObjectNode) json;
        jsonNode.put("ClientTransactionID", 1);
        jsonNode.put("ServerTransactionID", 1);
        jsonNode.put("ErrorNumber", 0);
        jsonNode.put("ErrorMessage", "");
        jsonNode.put("Type", 2);
        jsonNode.put("Rank", 2);

        ArrayNode array = mapper.createArrayNode();
        for (int i = 0; i < width; i++) {
            ArrayNode columnNode = mapper.createArrayNode();
            for(int j = 0; j < height; j++) {
                int value = i % 256;
                columnNode.add(value);
            }
            array.add(columnNode);
        }

        jsonNode.set("Value", array);
        
        return json;
    }

    byte[] toInt32LEBytes(int number) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (number & 0xFF);
        bytes[1] = (byte) ((number >> 8) & 0xFF);
        bytes[2] = (byte) ((number >> 16) & 0xFF);
        bytes[3] = (byte) ((number >> 24) & 0xFF);
        return bytes;
    }

    byte[] toUInt32LEBytes(int number) {
        byte[] bytes = new byte[4];
        number = number & 0xFFFFFFFF;
        bytes[0] = (byte) (number & 0xFF);
        bytes[1] = (byte) ((number >> 8) & 0xFF);
        bytes[2] = (byte) ((number >> 16) & 0xFF);
        bytes[3] = (byte) ((number >> 24) & 0xFF);
        return bytes;
    }
}
