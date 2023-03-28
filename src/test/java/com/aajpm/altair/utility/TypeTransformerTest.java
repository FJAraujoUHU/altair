package com.aajpm.altair.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class TypeTransformerTest {

    @Test
    void testConvertByte() {
        byte[] bytes = {(byte) 0xFF};
        int expect = 255;
        int actual = TypeTransformer.convertByte(bytes[0]);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertDouble() {
        byte[] bytes = {(byte) 0x3F, (byte) 0xF3, (byte) 0xC0, (byte) 0xCA, (byte) 0x42, (byte) 0xC9, (byte) 0x59, (byte) 0xF1};
        double expect = 1.2345678910111213;
        double actual = TypeTransformer.convertDouble(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertDoubleLE() {
        byte[] bytes = {(byte) 0x3F, (byte) 0xF3, (byte) 0xC0, (byte) 0xCA, (byte) 0x42, (byte) 0xC9, (byte) 0x59, (byte) 0xF1};
        byte[] setyb = new byte[8];
        for (int i = 0; i < 8; i++) {
            setyb[i] = bytes[7 - i];
        }
        double expect = 1.2345678910111213;
        double actual = TypeTransformer.convertDoubleLE(setyb);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertInt16() {
        byte[] bytes = {(byte) 0x01, (byte) 0x23};
        int expect = 291;
        int actual = TypeTransformer.convertInt16(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertInt16LE() {
        byte[] bytes = {(byte) 0x23, (byte) 0x01};
        int expect = 291;
        int actual = TypeTransformer.convertInt16LE(bytes);

        assertEquals(expect, actual);
        
    }

    @Test
    void testConvertInt32() {
        byte[] bytes = {(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67};
        int expect = 19088743;
        int actual = TypeTransformer.convertInt32(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertInt32LE() {
        byte[] bytes = {(byte) 0x67, (byte) 0x45, (byte) 0x23, (byte) 0x01};
        int expect = 19088743;
        int actual = TypeTransformer.convertInt32LE(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertInt64() {
        byte[] bytes = {(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        long expect = 81985529216486895L;
        long actual = TypeTransformer.convertInt64(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertInt64LE() {
        byte[] bytes = {(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        byte[] setyb = new byte[8];
        for (int i = 0; i < 8; i++) {
            setyb[i] = bytes[7 - i];
        }
        long expect = 81985529216486895L;
        long actual = TypeTransformer.convertInt64LE(setyb);

        assertEquals(expect, actual);
        
    }

    @Test
    void testConvertSingle() {
        byte[] bytes = {(byte) 0x3F, (byte) 0x9E, (byte) 0x06, (byte) 0x51};
        float expect = 1.2345678f;
        float actual = TypeTransformer.convertSingle(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertSingleLE() {
        byte[] bytes = {(byte) 0x3F, (byte) 0x9E, (byte) 0x06, (byte) 0x51};
        byte[] setyb = {bytes[3], bytes[2], bytes[1], bytes[0]};
        float expect = 1.2345678f;
        float actual = TypeTransformer.convertSingleLE(setyb);

        assertEquals(expect, actual);
    }

    

    @Test
    void testConvertUInt16() {
        byte[] bytes = {(byte) 0xFA, (byte) 0xCC};
        int expect = 64204;
        int actual = TypeTransformer.convertUInt16(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertUInt16LE() {
        byte[] bytes = {(byte) 0xCC, (byte) 0xFA};
        int expect = 64204;
        int actual = TypeTransformer.convertUInt16LE(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertUInt32() {
        byte[] bytes = {(byte) 0xFA, (byte) 0xCC, (byte) 0xFA, (byte) 0xCC};
        long expect = 4207737548L;
        long actual = TypeTransformer.convertUInt32(bytes);

        assertEquals(expect, actual);
    }

    @Test
    void testConvertUInt32LE() {
        byte[] bytes = {(byte) 0xCC, (byte) 0xFA, (byte) 0xCC, (byte) 0xFA};
        long expect = 4207737548L;
        long actual = TypeTransformer.convertUInt32LE(bytes);

        assertEquals(expect, actual);
        
    }

    @Test
    void testConvertUInt64() {
        byte[] bytes = {(byte) 0xFE, (byte) 0xDC, (byte) 0xBA, (byte) 0x98, (byte) 0x76, (byte) 0x54, (byte) 0x32, (byte) 0x10};
        BigInteger expect = new BigInteger("18364758544493064720");
        BigInteger actual = TypeTransformer.convertUInt64(bytes);

        assertEquals(expect, actual);
        
    }

    @Test
    void testConvertUInt64LE() {
        byte[] bytes = {(byte) 0x10, (byte) 0x32, (byte) 0x54, (byte) 0x76, (byte) 0x98, (byte) 0xBA, (byte) 0xDC, (byte) 0xFE};
        BigInteger expect = new BigInteger("18364758544493064720");
        BigInteger actual = TypeTransformer.convertUInt64LE(bytes);

        assertEquals(expect, actual);
    }


}
