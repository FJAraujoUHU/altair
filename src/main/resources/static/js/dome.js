let connected, azimuth, shutter, shutterStatus, slaved, slewing, atHome, parked;

$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });
    // Set up monitoring
    const source = new EventSource("/altair/api/dome/stream");
    source.onmessage = function (event) {
        $("#dmConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);

        connected = data.connected;
        azimuth = data.azimuth;
        shutter = data.shutter;
        slaved = data.slaved;
        slewing = data.slewing;
        atHome = data.atHome;
        parked = data.parked;
        shutterStatus = data.shutterStatus.toUpperCase();

        // Dome
        $("#dmConnected").text(connected);
        $("#dmAzimuth").text(toDMS(azimuth));

        let statusStr;
        if (shutterStatus === "OPEN")
            statusStr = "Open at " + shutter + "\%";
        else
            statusStr = data.shutterStatus;
        $("#dmShutter").text(statusStr);

        $("#dmSlaved").text(slaved);
        $("#dmSlewing").text(slewing);
        $("#dmAtHome").text(atHome);
        $("#dmParked").text(parked);

        // Update control labels
        if (parked) {
            $("#dmPark").text("Unpark");
        } else {
            $("#dmPark").text("Park");
        }

        switch (shutterStatus) {
            case "OPEN":
                $("#dmShutterOpen").prop('disabled', !connected);
                $("#dmShutterOpen").text("Close shutter");
                break;
            case "CLOSED":
                $("#dmShutterOpen").prop('disabled', !connected);
                $("#dmShutterOpen").text("Open shutter");
                break;
            default:
                $("#dmShutterOpen").prop('disabled', true);
                break;
        }

        if (connected) {
            $("#dmConnect").text("Disconnect");
        } else {
            $("#dmConnect").text("Connect");
        }

        $("#dmSlave").prop('checked', slaved);

        // Disable controls if not connected
        $('#controlPanel').find('button:not(#dmConnect, #dmShutterOpen)').prop('disabled', !connected);
        $("#dmSlave").prop('disabled', !connected);
        $(".dmmoveshutter").prop('disabled', !connected || (shutterStatus !== "OPEN"));
        $("#dmSetTo").prop('disabled', !connected || (shutterStatus !== "OPEN"))
        $(".dmslew, .dmslewaz").prop('disabled', !connected || !couldSlew());
        $("#dmAbort").prop('disabled', !(slewing && connected));
    };

    // Set up controls
    // Connect
    $("#dmConnect").click(function () {
        let url = connected ? "/altair/api/dome/disconnect" : "/altair/api/dome/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Park
    $("#dmPark").click(function () {
        let url = parked ? "/altair/api/dome/unpark" : "/altair/api/dome/park";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Go home
    $("#dmGoHome").click(function () {
        $.ajax({
            url: "/altair/api/dome/findhome",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });


    // Abort slew
    $("#dmAbort").click(function () {
        $.ajax({
            url: "/altair/api/dome/abort",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Set slaving
    $("#dmSlave").change(function () {
        let value = $(this).prop('checked');
        $.ajax({
            url: "/altair/api/dome/slavedome",
            type: "POST",
            data: {
                enable: value
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });


    // SlewRelative
    $(".dmslew").click(function () {
        let clockwise;
        let rate = parseFloat($("#dmRate").val());
        clockwise = $(this).attr("id") === "dmSlewCW" ? true : false;

        rate = clockwise ? rate : -rate;

        $.ajax({
            url: "/altair/api/dome/slewrelative",
            type: "POST",
            data: {
                degrees: rate
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Move shutter
    $(".dmmoveshutter").click(function () {
        let open;
        let amount = parseFloat($("#dmRate").val());
        open = $(this).attr("id") === "dmSlewOpen" ? true : false;

        amount = open ? amount : -amount;

        $.ajax({
            url: "/altair/api/dome/moveshutter",
            type: "POST",
            data: {
                amount: amount
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Open shutter
    $("#dmShutterOpen").click(function () {
        let url = shutterStatus === "OPEN" ? "/altair/api/dome/closeshutter" : "/altair/api/dome/openshutter";

        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#dmShutterSlider").on("input", function () {
        let value = $(this).val();
        console.log("slider input: " + value);
    });

    $("#dmShutterSlider").on("change", function () {
        let value = $(this).val();
        console.log("slider change: " + value);
    });

    // Slew
    $("#dmSlewAbs").submit(function (event) {
        event.preventDefault();

        let degs = parseFloat($("#dmSlewDegs").val());
        let mins = parseFloat($("#dmSlewMins").val());
        let secs = parseFloat($("#dmSlewSecs").val());

        let az = toSexagesimal(degs, mins, secs);

        $.ajax({
            url: "/altair/api/dome/slew",
            type: "POST",
            data: {
                az: az
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Set shutter
    $("#dmSetTo").click(function () {
        let value = parseFloat($("#dmShutterSlider").val());

        $.ajax({
            url : "/altair/api/dome/setshutter",
            type: "POST",
            data: {
                amount: value
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });


    // Validate inputs
    $(".dmslewaz").on("input", validateDmSlew);
});

function toHMS(value) {
    let h = Math.floor(value);
    let m = Math.floor((value - h) * 60);
    let s = Math.floor(((value - h) * 60 - m) * 60);
    let date = new Date(0);
    date.setUTCHours(h, m, s);
    return date.toLocaleString('en-US', {
        hour12: false,
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
    return d + "° " + m + "\' " + s + "\"";
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
    return connected && !parked && !slewing && !slaved;
}

// Check if the inputs make sense
const twoDigits = /^\d{1,2}$/;
const threeDigits = /^\d{1,3}$/;

function validateDmSlew() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#dmSlewDegs").val().trim();
        mVal = $("#dmSlewMins").val().trim();
        sVal = $("#dmSlewSecs").val().trim();
    } catch (e) { return; }

    if (threeDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let degs = parseInt(dVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (degs > 360 || (degs == 360 && mins > 0) || (degs == 360 && mins == 0 && secs > 0)) {
            $("#dmSlewDegs").val("360");
            $("#dmSlewMins").val("0");
            $("#dmSlewSecs").val("0");
        }
        if (mins > 59) $("#dmSlewMins").val("59");
        if (secs > 59) $("#dmSlewSecs").val("59");
    } else {
        if (!threeDigits.test(dVal)) $("#dmSlewDegs").val("");
        if (!twoDigits.test(mVal)) $("#dmSlewMins").val("");
        if (!twoDigits.test(sVal)) $("#dmSlewSecs").val("");
    }
}

function checkTsAZ() {
    let dVal, mVal, sVal;
    try {
        dVal = $("#dmSlewDegs").val().trim();
        mVal = $("#dmSlewMins").val().trim();
        sVal = $("#dmSlewSecs").val().trim();
    } catch (e) { return false; }

    return (threeDigits.test(dVal) && twoDigits.test(mVal) && twoDigits.test(sVal));
}