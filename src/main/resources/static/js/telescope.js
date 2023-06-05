let connected, parked, atHome, slewing, tracking, rightAscension, declination, azimuth, altitude, siderealTime;
let canFindHome, canPark, canUnpark, canSlewAwait, canSlew, canTrack;

$(document).ready(function () {
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Set up capabilities
    if (!capabilities) {
        canFindHome = false;
        canPark = false;
        canUnpark = false;
        canSlewAwait = false;
        canSlew = false;
        canTrack = false;
    } else {
        canFindHome = capabilities.canFindHome;
        canPark = capabilities.canPark;
        canUnpark = capabilities.canUnpark;
        canSlewAwait = capabilities.canSlewAwait;
        canSlew = capabilities.canSlew;
        canTrack = capabilities.canTrack;
    }
    

    // Set up monitoring
    const source = new EventSource("/altair/api/telescope/stream");
    source.onmessage = function (event) {
        $("#tsConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);

        connected = data.connected;
        parked = data.parked;
        atHome = data.atHome;
        slewing = data.slewing;
        tracking = data.tracking;
        rightAscension = data.rightAscension;
        declination = data.declination;
        azimuth = data.azimuth;
        altitude = data.altitude;
        siderealTime = data.siderealTime;

        // Telescope
        $("#tsConnected").text(connected);
        $("#tsSiderealTime").text(toHMS(siderealTime));
        $("#tsRightAscension").text(toHMS(rightAscension));
        $("#tsDeclination").text(toDMS(declination));
        $("#tsAzimuth").text(toDMS(azimuth));
        $("#tsAltitude").text(toDMS(altitude));
        $("#tsSlewing").text(canSlew ? slewing : "N/A");
        $("#tsTracking").text(canTrack ? tracking : "N/A");
        $("#tsAtHome").text(canFindHome ? atHome : "N/A");
        $("#tsParked").text(canPark ? parked : "N/A");

        // Update control labels
        if (parked) {
            $("#tsPark").text("Unpark");
        } else {
            $("#tsPark").text("Park");
        }

        if (connected) {
            $("#tsConnect").text("Disconnect");
        } else {
            $("#tsConnect").text("Connect");
        }

        if (tracking) {
            $("#tsTrack").prop('checked', true);
        } else {
            $("#tsTrack").prop('checked', false);
        }

        // Disable controls if not connected
        $('#controlPanel').find('button:not(#tsConnect)').prop('disabled', !connected);
        $("#tsTrack").prop('disabled', !connected || !canTrack);
        $("#tsSlewRaDecBtn").prop('disabled', !tracking || !couldSlew());
        $("#tsSlewAltAzBtn").prop('disabled', tracking || !couldSlew());
        $(".tsslew").prop('disabled', tracking || !couldSlew());
        $("#tsAbort").prop('disabled', !(slewing && connected && canSlew));
        $("#tsGoHome").prop('disabled', !(connected && canFindHome));
        $("#tsPark").prop('disabled', !(connected && canPark));
        
    };

    // Set up controls
    // Connect
    $("#tsConnect").click(function () {
        let url = connected ? "/altair/api/telescope/disconnect" : "/altair/api/telescope/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            },
            success: function (result, status, xhr) {
                location.reload(true);
            }
        });
    });

    // Park
    $("#tsPark").click(function () {
        let url = parked ? "/altair/api/telescope/unpark" : "/altair/api/telescope/park";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Go home
    $("#tsGoHome").click(function () {
        $.ajax({
            url: "/altair/api/telescope/findhome",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });


    // Abort slew
    $("#tsAbort").click(function () {
        $.ajax({
            url: "/altair/api/telescope/abortslew",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Set tracking
    $("#tsTrack").change(function () {
        let tracking = $(this).prop('checked');
        $.ajax({
            url: "/altair/api/telescope/settracking",
            type: "POST",
            data: {
                tracking: tracking
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // SlewRelative
    $(".tsslew").click(function () {
        let direction;
        let rate = parseFloat($("#tsRate").val());
        switch ($(this).attr("id")) {
            case "tsSlewN":
                direction = 0;
                break;
            case "tsSlewE":
                direction = 1;
                break;
            case "tsSlewS":
                direction = 2;
                break;
            case "tsSlewW":
                direction = 3;
                break;
        }

        $.ajax({
            url: "/altair/api/telescope/slewrelative",
            type: "POST",
            data: {
                direction: direction,
                degrees: rate
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Slew
    $("#tsSlewAbs").submit(function (event) {
        event.preventDefault();

        let mode = $("#tsSlewTabs a").filter(".active").attr('href');
        console.log(mode);

        if (mode === "#altAz") {
            let azdegs = parseFloat($("#tsSlewAZdegs").val());
            let azmins = parseFloat($("#tsSlewAZmins").val());
            let azsecs = parseFloat($("#tsSlewAZsecs").val());
            let az = toSexagesimal(azdegs, azmins, azsecs);

            let altdegs = parseFloat($("#tsSlewALTdegs").val());
            let altmins = parseFloat($("#tsSlewALTmins").val());
            let altsecs = parseFloat($("#tsSlewALTsecs").val());
            let alt = toSexagesimal(altdegs, altmins, altsecs);
            $.ajax({
                url: "/altair/api/telescope/slewtoaltaz",
                type: "POST",
                data: {
                    az: az,
                    alt: alt
                },
                error: function (xhr, status, error) {
                    console.log("Error: " + error);
                }
            });

        } else if (mode === "#raDec") {
            let rahours = parseFloat($("#tsSlewRAhours").val());
            let ramins = parseFloat($("#tsSlewRAmins").val());
            let rasecs = parseFloat($("#tsSlewRAsecs").val());
            let ra = toSexagesimal(rahours, ramins, rasecs);

            let decdegs = parseFloat($("#tsSlewDECdegs").val());
            let decmins = parseFloat($("#tsSlewDECmins").val());
            let decsecs = parseFloat($("#tsSlewDECsecs").val());
            let dec = toSexagesimal(decdegs, decmins, decsecs);
            $.ajax({
                url: "/altair/api/telescope/slewtocoords",
                type: "POST",
                data: {
                    ra: ra,
                    dec: dec
                },
                error: function (xhr, status, error) {
                    console.log("Error: " + error);
                }
            });
        }
    });

    // Validate inputs
    $(".tsslewra").on("input", validateTsSlewRA);
    $(".tsslewdec").on("input", validateTsSlewDEC);
    $(".tsslewaz").on("input", validateTsSlewAZ);
    $(".tsslewalt").on("input", validateTsSlewALT);
});

function toHMS(value) {
    let h = Math.floor(value);
    let m = Math.floor((value - h) * 60);
    let s = Math.floor(((value - h) * 60 - m) * 60);
    let date = new Date(0);
    date.setUTCHours(h, m, s);
    return date.toLocaleString('en-GB', {
        hour12: false,
        hourCycle: 'h23', // bug in INTL ECMA, 12:00 pm gets converted to 24:00 -> https://stackoverflow.com/a/68646518
        timeZone: 'UTC',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
    });
}

function toDMS(value) {
    let d = Math.floor(value);
    let m = Math.floor((value - d) * 60);
    let s = Math.floor(((value - d) * 60 - m) * 60);
    return d + "° " + m + "' " + s + "\"";
}

function isTrue(input) {
    if (typeof input == 'string') {
        return input.toLowerCase() == 'true';
    }
    return !!input;
}

function toSexagesimal(deg, min, seg) {
    return parseFloat(deg) + parseFloat(min / 60) + parseFloat(seg / 3600);
}

function couldSlew() {
    return connected && !parked && !slewing && canSlew;
}

// Check if the inputs make sense
const twoDigits = /^\d{1,2}$/;
const threeDigits = /^\d{1,3}$/;
const twoDigitsNeg = /^(-\d{0,2}|\d{1,2})$/;
const threeDigitsNeg = /^(-\d{0,3}|\d{1,3})$/;

function validateTsSlewRA() {
    let hVal, mVal, sVal;
    try {
        hVal = $("#tsSlewRAhours").val().trim();
        mVal = $("#tsSlewRAmins").val().trim();
        sVal = $("#tsSlewRAsecs").val().trim();
    } catch (e) { return; }

    if (twoDigits.test(hVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let hours = parseInt(hVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (hours > 23) {
            $("#tsSlewRAhours").val("23");
            $("#tsSlewRAmins").val("59");
            $("#tsSlewRAsecs").val("59");
        }
        if (mins > 59) {
            $("#tsSlewRAmins").val("59");
        }
        if (secs > 59) {
            $("#tsSlewRAsecs").val("59");
        }
    } else {
        if (!twoDigits.test(hVal)) {
            $("#tsSlewRAhours").val("");
        }
        if (!twoDigits.test(mVal)) {
            $("#tsSlewRAmins").val("");
        }
        if (!twoDigits.test(sVal)) {
            $("#tsSlewRAsecs").val("");
        }
    }
}

function validateTsSlewDEC() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewDECdegs").val().trim();
        mVal = $("#tsSlewDECmins").val().trim();
        sVal = $("#tsSlewDECsecs").val().trim();
    } catch (e) { return; }

    if (twoDigitsNeg.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let degs = parseInt(dVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (degs > 90 || (degs == 90 && mins > 0) || (degs == 90 && mins == 0 && secs > 0)) {
            $("#tsSlewDECdegs").val("90");
            $("#tsSlewDECmins").val("0");
            $("#tsSlewDECsecs").val("0");
        }
        if (degs < -90 || (degs == -90 && mins > 0) || (degs == -90 && mins == 0 && secs > 0)) {
            $("#tsSlewDECdegs").val("-90");
            $("#tsSlewDECmins").val("0");
            $("#tsSlewDECsecs").val("0");
        }
        if (mins > 59) $("#tsSlewDECmins").val("59");
        if (secs > 59) $("#tsSlewDECsecs").val("59");
    } else {
        if (!twoDigitsNeg.test(dVal)) $("#tsSlewDECdegs").val("");
        if (!twoDigits.test(mVal)) $("#tsSlewDECmins").val("");
        if (!twoDigits.test(sVal)) $("#tsSlewDECsecs").val("");
    }
}

function validateTsSlewALT() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewALTdegs").val().trim();
        mVal = $("#tsSlewALTmins").val().trim();
        sVal = $("#tsSlewALTsecs").val().trim();
    } catch (e) { return; }

    if (twoDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let degs = parseInt(dVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (degs > 90 || (degs == 90 && mins > 0) || (degs == 90 && mins == 0 && secs > 0)) {
            $("#tsSlewALTdegs").val("90");
            $("#tsSlewALTmins").val("0");
            $("#tsSlewALTsecs").val("0");
        }
        if (mins > 59) $("#tsSlewALTmins").val("59");
        if (secs > 59) $("#tsSlewALTsecs").val("59");
    } else {    
        if (!twoDigits.test(dVal)) $("#tsSlewALTdegs").val("");
        if (!twoDigits.test(mVal)) $("#tsSlewALTmins").val("");
        if (!twoDigits.test(sVal)) $("#tsSlewALTsecs").val("");
    }
}

function validateTsSlewAZ() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewAZdegs").val().trim();
        mVal = $("#tsSlewAZmins").val().trim();
        sVal = $("#tsSlewAZsecs").val().trim();
    } catch (e) { return; }

    if (threeDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let degs = parseInt(dVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (degs > 360 || (degs == 360 && mins > 0) || (degs == 360 && mins == 0 && secs > 0)) {
            $("#tsSlewAZdegs").val("360");
            $("#tsSlewAZmins").val("0");
            $("#tsSlewAZsecs").val("0");
        }
        if (mins > 59) $("#tsSlewAZmins").val("59");
        if (secs > 59) $("#tsSlewAZsecs").val("59");
    } else {
        if (!threeDigits.test(dVal)) $("#tsSlewAZdegs").val("");
        if (!twoDigits.test(mVal)) $("#tsSlewAZmins").val("");
        if (!twoDigits.test(sVal)) $("#tsSlewAZsecs").val("");
    }
}

function checkTsRA() {
    let hVal, mVal, sVal;
    try {
        hVal = $("#tsSlewRAhours").val().trim();
        mVal = $("#tsSlewRAmins").val().trim();
        sVal = $("#tsSlewRAsecs").val().trim();
    } catch (e) { return false; }

    return (twoDigits.test(hVal) && twoDigits.test(mVal) && twoDigits.test(sVal));
}

function checkTsDEC() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewDECdegs").val().trim();
        mVal = $("#tsSlewDECmins").val().trim();
        sVal = $("#tsSlewDECsecs").val().trim();
    } catch (e) { return false; }

    return (twoDigitsNeg.test(dVal) && (dVal !== "-") && twoDigits.test(mVal) && twoDigits.test(sVal));
}

function checkTsALT() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewALTdegs").val().trim();
        mVal = $("#tsSlewALTmins").val().trim();
        sVal = $("#tsSlewALTsecs").val().trim();
    } catch (e) { return false; }

    return (twoDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal));
}

function checkTsAZ() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#tsSlewAZdegs").val().trim();
        mVal = $("#tsSlewAZmins").val().trim();
        sVal = $("#tsSlewAZsecs").val().trim();
    } catch (e) { return false; }

    return (threeDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal));
}