package com.aajpm.altair.utility;

import java.math.BigInteger;

public class TypeTransformer {
    public enum NumberVarType {
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
                case BYTE:
                    return 1;
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

        public Class<?> getJavaClass() {
            switch (this) {
                case BYTE:
                    return byte.class;
                case INT16:
                    return short.class;
                case UINT16:
                case INT32:
                    return int.class;
                case UINT32:
                case INT64:
                    return long.class;
                case UINT64:
                    return BigInteger.class;
                case SINGLE:
                    return float.class;
                case DOUBLE:
                    return long.class;
                default:
                    return null;
            }
        }

        public boolean isSigned() {
            return !(this == UINT16 || this == UINT32 || this == UINT64);
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

    public static short convertInt16(byte[] bytes) {
        return (short) ((bytes[1] & 0xFF) | ((bytes[0] & 0xFF) << 8));
    }

    public static int convertUInt16(byte[] bytes) {
        return convertInt16(bytes);
    }

    public static int convertInt32(byte[] bytes) {
        return (bytes[3] & 0xFF) | ((bytes[2] & 0xFF) << 8) | ((bytes[1] & 0xFF) << 16) | ((bytes[0] & 0xFF) << 24);
    }

    public static long convertUInt32(byte[] bytes) {
        return (bytes[3] & 0xFF) | ((long)(bytes[2] & 0xFF) << 8) | (((long)bytes[1] & 0xFF) << 16) | ((long)(bytes[0] & 0xFF) << 24);
    }

    public static long convertInt64(byte[] bytes) {
        return ((long)(bytes[0] & 0xFF) << 56) | ((long)(bytes[1] & 0xFF) << 48) | ((long)(bytes[2] & 0xFF) << 40) | ((long)(bytes[3] & 0xFF) << 32) | ((long)(bytes[4] & 0xFF) << 24) | ((long)(bytes[5] & 0xFF) << 16) | ((long)(bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
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

    public static short convertInt16LE(byte[] bytes) {
        return (short) ((bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8));
    }

    public static int convertUInt16LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
    }

    public static int convertInt32LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
    }

    public static long convertUInt32LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((long)(bytes[1] & 0xFF) << 8) | (((long)bytes[2] & 0xFF) << 16) | ((long)(bytes[3] & 0xFF) << 24);
    }

    public static long convertInt64LE(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((long)(bytes[1] & 0xFF) << 8) | (((long)bytes[2] & 0xFF) << 16) | ((long)(bytes[3] & 0xFF) << 24) | ((long)(bytes[4] & 0xFF) << 32) | ((long)(bytes[5] & 0xFF) << 40) | ((long)(bytes[6] & 0xFF) << 48) | ((long)(bytes[7] & 0xFF) << 56);
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


    /////////////////////////////// TRANSFORMERS /////////////////////////////////
    //#region Transformers

    // TODO : Test if this works and pray it does bc it's hella spaghetti
    @SuppressWarnings("java:S3776") // ik ik it's spaghetti but it fast
    public static Object toFits(byte[] imgBytes, int index, NumberVarType imgType, NumberVarType transType, boolean isLittleEndian)
    {
        byte[] buffer = new byte[imgType.getByteCount()];
        System.arraycopy(imgBytes, index, buffer, 0, transType.getByteCount());
        Object ret = null;

        if (imgType != transType) { // Conversion needed
            switch (imgType) {
                case DOUBLE:    // Must have been demoted to float
                    ret = isLittleEndian ? convertSingleLE(buffer) : convertSingle(buffer);
                    ret = ((Float)ret).doubleValue(); // Promote to real class
                    break;
                case INT16:    // Must have been demoted to byte
                    ret = (short) (buffer[0] & 0xFF);
                    break;
                case UINT16:    // Must have been demoted to byte
                    ret = (short) ((buffer[0] & 0xFF)  - Short.MIN_VALUE); // Offset to use BZERO
                    break;                
                case INT32:
                    ret = promoteToInt32(buffer, transType, isLittleEndian);
                    break;
                case UINT32:
                    ret = promoteToUInt32(buffer, transType, isLittleEndian);
                    break;
                case INT64:
                    ret = promoteToInt64(buffer, transType, isLittleEndian);
                    break;
                case UINT64:
                    ret = promoteToUInt64(buffer, transType, isLittleEndian);
                    break;
                default:
            }
            if (ret != null)    // If conversion was successful, return. Else, use fallback
                return ret;
        }

        switch (transType) {
            case INT16:
                ret = isLittleEndian ? convertInt16LE(buffer) : convertInt16(buffer);
                break;
            case INT32:
                ret = isLittleEndian ? convertInt32LE(buffer) : convertInt32(buffer);
                break;
            case DOUBLE:
                ret = isLittleEndian ? convertDoubleLE(buffer) : convertDouble(buffer);
                break;
            case SINGLE:
                ret = isLittleEndian ? convertSingleLE(buffer) : convertSingle(buffer);
                break;
            case UINT64:
                long uint64 = isLittleEndian ? convertInt64LE(buffer) : convertInt64(buffer);
                ret = uint64 < 0 ? uint64 + Long.MIN_VALUE : uint64 - Long.MIN_VALUE; // Offset to mantain resolution
                break;
            case BYTE:
                ret = buffer[0];
                break;
            case INT64:
                ret = isLittleEndian ? convertInt64LE(buffer) : convertInt64(buffer);
                break;
            case UINT16:
                short uint16 = isLittleEndian ? convertInt16LE(buffer) : convertInt16(buffer);
                ret = uint16 >= 0 ? uint16 + Short.MIN_VALUE : uint16 - Short.MIN_VALUE; // Offset to mantain resolution
                break;
            case UINT32:
                int uint32 = isLittleEndian ? convertInt32LE(buffer) : convertInt32(buffer);
                ret = uint32 < 0 ? uint32 + Integer.MIN_VALUE : uint32 - Integer.MIN_VALUE; // Offset to mantain resolution
                break;
            default:
                throw new UnsupportedOperationException("Unknown conversion between " + transType + " and " + imgType);
        }
        return ret;        
    }

    



    //////////////////////////////// HELPERS //////////////////////////////////
    //#region Helpers

    private static Integer promoteToInt32(byte[] buffer, NumberVarType transType, boolean isLittleEndian) {
        switch(transType) {
            case INT16:
                short int16 = isLittleEndian ? convertInt16LE(buffer) : convertInt16(buffer);
                return ((Short)int16).intValue();
            case UINT16:
                return isLittleEndian ? convertUInt16LE(buffer) : convertUInt16(buffer);
            case BYTE:
                return buffer[0] & 0xFF;
            default:
                return null;
        }
    }

    private static Integer promoteToUInt32(byte[] buffer, NumberVarType transType, boolean isLittleEndian) {
        Integer ret = promoteToInt32(buffer, transType, isLittleEndian);
        if (ret != null)
            ret -= Integer.MIN_VALUE; // Offset to use BZERO
        return ret;
    }

    private static Long promoteToInt64(byte[] buffer, NumberVarType transType, boolean isLittleEndian) {
        switch(transType) { 
            case BYTE:
            case INT16:
            case UINT16:
                return (long) promoteToInt32(buffer, transType, isLittleEndian);
            case INT32:
                return (long) (isLittleEndian ? convertInt32LE(buffer) : convertInt32(buffer));
            case UINT32:
                return isLittleEndian ? convertUInt32LE(buffer) : convertUInt32(buffer);
            case SINGLE:
                Float f = isLittleEndian ? convertSingleLE(buffer) : convertSingle(buffer);
                return f.longValue();
            default:
                return null;
        }
    }

    private static Long promoteToUInt64(byte[] buffer, NumberVarType transType, boolean isLittleEndian) {
        Long ret = promoteToInt64(buffer, transType, isLittleEndian);
        if (ret != null)
            ret -= Long.MIN_VALUE; // Offset to use BZERO
        return ret;
    }

    //#endregion


    //#endregion
}
