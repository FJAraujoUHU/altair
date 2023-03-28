package com.aajpm.altair.utility.exception;

public class ASCOMException extends DeviceException {
    //////////////////////////////// CONSTANTS ////////////////////////////////
    // Known ASCOM error codes
    public static final int METHOD_NOT_IMPLEMENTED = 0x400; // 1024
    public static final int INVALID_VALUE = 0x401;          // 1025
    public static final int VALUE_NOT_SET = 0x402;          // 1026
    public static final int NOT_CONNECTED = 0x407;          // 1031
    public static final int INVALID_WHILE_PARKED = 0x408;   // 1032
    public static final int INVALID_WHILE_SLAVED = 0x409;   // 1033
    public static final int SETTINGS_ERROR = 0x40A;         // 1034
    public static final int INVALID_OPERATION = 0x40B;      // 1035
    public static final int ACTION_NOT_IMPLEMENTED = 0x40C; // 1036
    public static final int ITEM_NOT_PRESENT = 0x40D;       // 1037
    public static final int UNSPECIFIED_ERROR = 0x4FF;      // 1279

    ///////////////////////////////////////////////////////////////////////////

    
    private final int errorCode;

    /**
     * Generates a message based on the error code
     * See https://ascom-standards.org/Help/Developer/html/T_ASCOM_ErrorCodes.htm and
     * https://github.com/ASCOMInitiative/ASCOMRemote/raw/master/Documentation/ASCOM%20Alpaca%20API%20Reference.pdf
     * @param errorCode The error code, as defined in the ASCOM Alpaca Standard
     * @return A message describing the error
     */
    private static String messageGenerator(int errorCode) {
        if (errorCode >= 0x500) {
            return "ASCOM Driver Error 0x" + Integer.toHexString(errorCode);
        }
        switch(errorCode) {
            case METHOD_NOT_IMPLEMENTED:
                return "ASCOM Error: Method not implemented (0x400)";
            case INVALID_VALUE:
                return "ASCOM Error: Invalid value (0x401)";
            case VALUE_NOT_SET:
                return "ASCOM Error: Value not set (0x402)";
            case NOT_CONNECTED:
                return "ASCOM Error: Not connected (0x407)";
            case INVALID_WHILE_PARKED:
                return "ASCOM Error: Invalid while parked (0x408)";
            case INVALID_WHILE_SLAVED:
                return "ASCOM Error: Invalid while slaved (0x409)";
            case SETTINGS_ERROR:
                return "ASCOM Error: Settings related error (0x40A)";
            case INVALID_OPERATION:
                return "ASCOM Error: Invalid operation (0x40B)";
            case ACTION_NOT_IMPLEMENTED:
                return "ASCOM Error: Action not implemented (0x40C)";
            case ITEM_NOT_PRESENT:
                return "ASCOM Error: Item not present in the ASCOM cache (0x40D)";
            case UNSPECIFIED_ERROR:
                return "ASCOM Error: Unspecified error (0x4FF)";
            default:
                return "ASCOM Error: Unknown error (0x" + Integer.toHexString(errorCode) + ")";
        }
    }

    public ASCOMException(int errorCode) {
        super(messageGenerator(errorCode));
        this.errorCode = errorCode; 
    }

    public ASCOMException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ASCOMException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ASCOMException(int errorCode, Throwable cause) {
        super(messageGenerator(errorCode), cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return messageGenerator(errorCode);
    }
}
