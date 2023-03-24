package com.aajpm.altair.service.observatory;

public class CameraService {
    /////////////////////////////// CONSTANTS /////////////////////////////////
    //#region Constants

    public static final int STATUS_IDLE = 0;        // Available to start exposure
    public static final int STATUS_WAITING = 1;     // Exposure started but waiting
    public static final int STATUS_EXPOSING = 2;    // Expoure in progress
    public static final int STATUS_READING = 3;     // Reading from CCD
    public static final int STATUS_DOWNLOADING = 4; // Downloading to PC
    public static final int STATUS_ERROR = 5;       // Camera disabled due to error
    
    //#endregion
    
}
