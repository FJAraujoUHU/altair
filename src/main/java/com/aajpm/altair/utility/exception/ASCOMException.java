package com.aajpm.altair.utility.exception;

public class ASCOMException extends DeviceException {
    private int errorCode = 0x4FF;

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
            case 0x400:
                return "ASCOM Error: Method not implemented (0x400)";
            case 0x401:
                return "ASCOM Error: Invalid value (0x401)";
            case 0x402:
                return "ASCOM Error: Value not set (0x402)";
            case 0x407:
                return "ASCOM Error: Not connected (0x407)";
            case 0x408:
                return "ASCOM Error: Invalid while parked (0x408)";
            case 0x409:
                return "ASCOM Error: Invalid while slaved (0x409)";
            case 0x40A:
                return "ASCOM Error: Settings related error (0x40A)";
            case 0x40B:
                return "ASCOM Error: Invalid operation (0x40B)";
            case 0x40C:
                return "ASCOM Error: Action not implemented (0x40C)";
            case 0x40D:
                return "ASCOM Error: Item not present in the ASCOM cache (0x40D)";
            case 0x4FF:
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
