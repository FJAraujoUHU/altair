package com.aajpm.altair.utility;

import java.math.BigInteger;

public class TypeTransformer {
    enum NumberVarType {
        // 0 to 3 are used by the Alpaca standard
        UNKNOWN(0), 
        INT16(1),   // gets converted to int
        INT32(2),   // gets converted to int
        DOUBLE(3),  // gets converted to double
        // 4 to 9 are an extension
        SINGLE(4),  // gets converted to float
        UINT64(5),  // gets converted to BigInteger
        BYTE(6),    // gets converted to int
        INT64(7),   // gets converted to long
        UINT16(8),  // gets converted to int
        UINT32(9);  // gets converted to long


        private final int value;

        private NumberVarType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public int getByteCount() {
            switch (this) {
                case INT16:
                case UINT16:
                    return 2;
                case INT32:
                case UINT32:
                case SINGLE:
                    return 4;
                case INT64:
                case UINT64:
                case DOUBLE:
                    return 8;
                default:
                    return 0;
            }
        }

        public static NumberVarType fromValue(int value) {
            for (NumberVarType type : NumberVarType.values()) {
                if (type.getValue() == value)
                    return type;
            }
            throw new IllegalArgumentException("Invalid image type value: " + value);
        }
    }

    /////////////////////////////// BYTE PARSERS /////////////////////////////////
    //#region Byte Parsers

    public static int convertByte(byte b) {
        return b & 0xFF;
    }
    

    public static int convertInt16(byte[] bytes) {
        return (bytes[1] & 0xFF) | ((bytes[0] & 0xFF) << 8);
    }

    public static int convertUInt16(byte[] bytes) {
        return convertInt16(bytes);
    }

    public static int convertInt32(byte[] bytes) {
        return (bytes[3] & 0xFF) | ((bytes[2] & 0xFF) << 8) | ((bytes[1] & 0xFF) << 16) | ((bytes[0] & 0xFF) << 24);
    }

    public static long convertUInt32(byte[] bytes) {
        return ((bytes[3] & 0xFF)) | ((long)(bytes[2] & 0xFF) << 8) | (((long)bytes[1] & 0xFF) << 16) | ((long)(bytes[0] & 0xFF) << 24);
    }

    public static long convertInt64(byte[] bytes) {
        return ((long)(bytes[0] & 0xFF) << 56) | ((long)(bytes[1] & 0xFF) << 48) | ((long)(bytes[2] & 0xFF) << 40) | ((long)(bytes[3] & 0xFF) << 32) | ((long)(bytes[4] & 0xFF) << 24) | ((long)(bytes[5] & 0xFF) << 16) | ((long)(bytes[6] & 0xFF) << 8) | ((bytes[7] & 0xFF));
    }

    public static BigInteger convertUInt64(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    public static double convertDouble(byte[] bytes) {
        return Double.longBitsToDouble(convertInt64(bytes));
    }  

    public static float convertSingle(byte[] bytes) {
        return Float.intBitsToFloat(convertInt32(bytes));
    }


    // Little Endian

    public static int convertInt16LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
    }

    public static int convertUInt16LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
    }

    public static int convertInt32LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
    }

    public static long convertUInt32LE(byte[] bytes) {
        return ((bytes[0] & 0xFF)) | ((long)(bytes[1] & 0xFF) << 8) | (((long)bytes[2] & 0xFF) << 16) | ((long)(bytes[3] & 0xFF) << 24);
    }

    public static long convertInt64LE(byte[] bytes) {
        return ((bytes[0] & 0xFF)) | ((long)(bytes[1] & 0xFF) << 8) | (((long)bytes[2] & 0xFF) << 16) | ((long)(bytes[3] & 0xFF) << 24) | ((long)(bytes[4] & 0xFF) << 32) | ((long)(bytes[5] & 0xFF) << 40) | ((long)(bytes[6] & 0xFF) << 48) | ((long)(bytes[7] & 0xFF) << 56);
    }

    public static BigInteger convertUInt64LE(byte[] bytes) {
        byte[] bytesBE = new byte[8];
        bytesBE[0] = bytes[7];
        bytesBE[1] = bytes[6];
        bytesBE[2] = bytes[5];
        bytesBE[3] = bytes[4];
        bytesBE[4] = bytes[3];
        bytesBE[5] = bytes[2];
        bytesBE[6] = bytes[1];
        bytesBE[7] = bytes[0];
        return convertUInt64(bytesBE);  
    }

    public static float convertSingleLE(byte[] bytes) {
        return Float.intBitsToFloat(convertInt32LE(bytes));
    }

    public static double convertDoubleLE(byte[] bytes) {
        return Double.longBitsToDouble(convertInt64LE(bytes));
    }

    //#endregion


    /////////////////////////////// CONVERTERS /////////////////////////////////
    //#region Converters
    public static Object convertToNumber(NumberVarType type, byte[] bytes) {
        switch (type) {
            case INT16:
                return convertInt16(bytes);
            case INT32:
                return convertInt32(bytes);
            case DOUBLE:
                return convertDouble(bytes);
            case SINGLE:
                return convertSingle(bytes);
            case UINT64:
                return convertUInt64(bytes);
            case BYTE:
                return convertByte(bytes[0]);
            case INT64:
                return convertInt64(bytes);
            case UINT16:
                return convertUInt16(bytes);
            case UINT32:
                return convertUInt32(bytes);
            default:
                throw new IllegalArgumentException("Invalid number type: " + type);
        }
    }

    public static Object convertToNumber(NumberVarType type, byte[] bytes, boolean isLittleEndian) {
        return isLittleEndian ? convertToNumberLE(type, bytes) : convertToNumber(type, bytes);
    }

    private static Object convertToNumberLE(NumberVarType type, byte[] bytes) {
        switch (type) {
            case INT16:
                return convertInt16LE(bytes);
            case INT32:
                return convertInt32LE(bytes);
            case DOUBLE:
                return convertDoubleLE(bytes);
            case SINGLE:
                return convertSingleLE(bytes);
            case UINT64:
                return convertUInt64LE(bytes);
            case BYTE:
                return convertByte(bytes[0]);
            case INT64:
                return convertInt64LE(bytes);
            case UINT16:
                return convertUInt16LE(bytes);
            case UINT32:
                return convertUInt32LE(bytes);
            default:
                throw new IllegalArgumentException("Invalid number type: " + type);
        }
    }

    public static int convertToInt(NumberVarType type, byte[] bytes, boolean isLittleEndian) {
        switch (type) {
            case INT16:
            case UINT16:
            case INT32:
            case BYTE:
                return (int) convertToNumber(type, bytes, isLittleEndian);
            default:
                throw new IllegalArgumentException("Invalid number type: " + type);
        }
    }

    public static int convertToInt(NumberVarType type, byte[] bytes) {
        return convertToInt(type, bytes, false);
    }

    public static long convertToLong(NumberVarType type, byte[] bytes, boolean isLittleEndian) {
        switch (type) {
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
            case BYTE:
            case INT64:
            case UINT64:
                return (long) convertToNumber(type, bytes, isLittleEndian);
            default:
                throw new IllegalArgumentException("Invalid number type: " + type);
        }
    }

    public static long convertToLong(NumberVarType type, byte[] bytes) {
        return convertToLong(type, bytes, false);
    }

    public static double convertToDouble(NumberVarType type, byte[] bytes, boolean isLittleEndian) {
        switch (type) {
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
            case BYTE:
            case SINGLE:
            case DOUBLE:
                return (double) convertToNumber(type, bytes, isLittleEndian);
            default:
                throw new IllegalArgumentException("Invalid number type: " + type);
        }
    }

    public static double convertToDouble(NumberVarType type, byte[] bytes) {
        return convertToDouble(type, bytes, false);
    }

    //#endregion

    /*public static Object inflate(byte[] bytes, NumberVarType from, NumberVarType to, boolean isOriginLittleEndian) {
        
        if (from == to)



    }*/


    
}
