let connected, temperature, cameraStatus, binning, binningSym, subframe, cooler, coolerConnected, lastCoolerStatus;
$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    lastCoolerStatus = null;
    binningSym = false;

    // Set up monitoring
    const source = new EventSource("/altair/api/camera/stream");
    source.onmessage = function (event) {
        $("#caConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);
        
        connected = data.connected;
        temperature = data.temperature;
        cameraStatus = data.status;
        if (parseFloat(data.statusCompletion)) {    // if statusCompletion is not falsy AKA not null or undefined
            cameraStatus += " (" + parseFloat(data.statusCompletion * 100).toFixed(2) + "%)";
        }
        binning = data.binning;
        subframe = data.sfWidth + "x" + data.sfHeight + " @(" + data.sfX + "," + data.sfY + ")";
        cooler = data.coolerStatus;
        if (parseFloat(data.coolerPower)) {
            cooler += " (" + parseFloat(data.coolerPower).toFixed(2) + "%)";
        }

        $("#caConnected").text(connected);
        $("#caTemperature").text(temperature);
        $("#caStatus").text(cameraStatus);
        $("#caBinning").text(binning);
        $("#caSubframe").text(subframe);
        $("#caCooler").text(cooler);
        $("#caCoolerPwr").text(data.coolerPower);


        if (connected) {
            $("#caConnect").text("Disconnect");
        } else {
            $("#caConnect").text("Connect");
        }
        if (data.coolerStatus.toUpperCase() !== "OFF") {
            coolerConnected = true;
            $("#caCoolerEnable").text("Turn off");
        } else {
            coolerConnected = false;
            $("#caCoolerEnable").text("Turn on");
        }
        
        $("#caSubframeButton").prop('disabled', !connected);
        $("#caBinningSym").prop('disabled', !connected);
        $("#caBinningSet").prop('disabled', !connected);
        $("#caExposureBtns").find('button').prop('disabled', !connected);
        $("#caExposureDLBtns").find('button').prop('disabled', !connected);
        $("#caCoolerEnable").prop('disabled', !connected);
        $("#caCoolerWarmup").prop('disabled', !connected);
        $("#caCoolerTempSetPane").find('button').prop('disabled', !connected);

        if (data.coolerStatus !== lastCoolerStatus) {
            $(".caCoolerStateIndicator").removeClass("text-bg-warning text-bg-success text-bg-danger text-bg-info text-bg-primary");
            $(".caCoolerStateIndicator").addClass("text-bg-dark");
            switch (data.coolerStatus.toUpperCase()) {
                case "OFF":
                    $("#caCoolerStateOff").removeClass("text-bg-dark");
                    $("#caCoolerStateOff").addClass("text-bg-warning");
                    break;
                case "COOLING DOWN":
                    $("#caCoolerStateCdn").removeClass("text-bg-dark");
                    $("#caCoolerStateCdn").addClass("text-bg-info");
                    break;
                case "WARMING UP":
                    $("#caCoolerStateWup").removeClass("text-bg-dark");
                    $("#caCoolerStateWup").addClass("text-bg-warning");
                    break;
                case "STABLE":
                    $("#caCoolerStateStb").removeClass("text-bg-dark");
                    $("#caCoolerStateStb").addClass("text-bg-success");
                    break;
                case "SATURATED":
                    $("#caCoolerStateSat").removeClass("text-bg-dark");
                    $("#caCoolerStateSat").addClass("text-bg-danger");
                    break;
                case "ACTIVE":
                    $("#caCoolerStateAct").removeClass("text-bg-dark");
                    $("#caCoolerStateAct").addClass("text-bg-primary");
                    break;
                default:
                    break;
            }
            lastCoolerStatus = data.coolerStatus;
        }

    }

    // Set up controls
    // Connect
    $("#caConnect").click(function () {
        let url = connected ? "/altair/api/camera/disconnect" : "/altair/api/camera/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caSubframeButton").click(function () {
        let startx = $("#caSubframeStartX").val();
        let starty = $("#caSubframeStartY").val();
        let width = $("#caSubframeWidth").val();
        let height = $("#caSubframeHeight").val();

        $.ajax({
            url: "/altair/api/camera/setsubframe",
            type: "POST",
            data: {
                startx: startx,
                starty: starty,
                width: width,
                height: height
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caBinningSym").change(function () {
        binningSym = $(this).is(':checked');
        $("#caBinningY").val($("#caBinningX").val());
        if (binningSym) {
            $("#caBinningY").prop('disabled', true);
            $(".hideOnSym").hide();
        } else {
            $("#caBinningY").prop('disabled', false);
            $(".hideOnSym").show();
        }
    });

    $("#caBinningSet").click(function () {
        if (binningSym) {
            let bin = $("#caBinningX").val();

            $.ajax({
                url: "/altair/api/camera/setbinning",
                type: "POST",
                data: {
                    binning: bin
                },
                error: function (xhr, status, error) {
                    console.log("Error: " + error);
                }
            });
        } else {
            let binx = $("#caBinningX").val();
            let biny = $("#caBinningY").val();

            $.ajax({
                url: "/altair/api/camera/setbinning",
                type: "POST",
                data: {
                    binx: binx,
                    biny: biny
                },
                error: function (xhr, status, error) {
                    console.log("Error: " + error);
                }
            });
        } 
    });

    $(".caExposureTime").on("input", validateExposureTime);
    
    $("#caExposureStart").click(function () {
        let hours = parseInt($("#caExposureHrs").val());
        if (hours === undefined || hours === null || hours < 0) hours = 0;
        let minutes = parseInt($("#caExposureMins").val());
        if (minutes === undefined || minutes === null || minutes < 0) minutes = 0;
        let seconds = parseInt($("#caExposureSecs").val());
        if (seconds === undefined || seconds === null || seconds < 0) seconds = 0;
        let duration = (hours * 3600) + (minutes * 60) + seconds;
        let lightframe = $("#caExposureLight").is(":checked");

        if (duration === undefined || duration === null || duration <= 0 ||  duration > 3600 * 12) {
            console.log("Invalid exposure duration");
            return;
        }

        $.ajax({
            url: "/altair/api/camera/startexposure",
            type: "POST",
            data: {
                duration: duration,
                lightframe: lightframe
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caExposureStop").click(function () {
        $.ajax({
            url: "/altair/api/camera/stopexposure",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caExposureAbort").click(function () {
        $.ajax({
            url: "/altair/api/camera/abortexposure",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // TODO : Prompt for filename and check if there are available first
    $("#caImgSave").click(function () {
        let filename = new Date().toISOString();

        $.ajax({
            url: "/altair/api/camera/saveimage",
            type: "POST",
            data: {
                filename: filename
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caImgDump").click(function () {
        let filename = new Date().toISOString();

        $.ajax({
            url: "/altair/api/camera/dumpimage",
            type: "POST",
            data: {
                filename: filename
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caCoolerEnable").click(function () {
        let enable = !coolerConnected;

        $.ajax({
            url: "/altair/api/camera/cooleron",
            type: "POST",
            data: {
                enable: enable
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caCoolerWarmup").click(function () {
        $.ajax({
            url: "/altair/api/camera/warmup",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    $("#caCoolerTarget").on("input", function () {
        let target = $("#caCoolerTarget").val();

        if (target === undefined || target === null || target < -30 || target > 30) {
            console.log("Invalid target temperature");
            return;
        }

        target = Math.round(target * 10) / 10;
        $("#caCoolerTgtDisp").text(target);

    });


    $("#caCoolerTargetEase").click(function () {
        let target = $("#caCoolerTarget").val();

        $.ajax({
            url: "/altair/api/camera/cooldown",
            type: "POST",
            data: {
                target: target
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });
    
    $("#caCoolerTargetSet").click(function () {
        let target = $("#caCoolerTarget").val();

        $.ajax({
            url: "/altair/api/camera/settargettemp",
            type: "POST",
            data: {
                target: target
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });



});

// Exposure time input validation
const twoDigits = /^\d{1,2}$/;
function validateExposureTime() {
    let hVal, mVal, sVal;
    try {
        hVal = $("#caExposureHrs").val().trim();
        mVal = $("#caExposureMins").val().trim();
        sVal = $("#caExposureSecs").val().trim();
    } catch (e) { return; }

    if (twoDigits.test(hVal) && twoDigits.test(mVal) && twoDigits.test(sVal)) {
        let hours = parseInt(hVal);
        let mins = parseInt(mVal);
        let secs = parseInt(sVal);
        if (hours > 23) {
            $("#caExposureHrs").val("23");
            $("#caExposureMins").val("59");
            $("#caExposureSecs").val("59");
        }
        if (mins > 59) {
            $("#caExposureMins").val("59");
        }
        if (secs > 59) {
            $("#caExposureSecs").val("59");
        }
    } else {
        if (!twoDigits.test(hVal)) {
            $("#caExposureHrs").val("");
        }
        if (!twoDigits.test(mVal)) {
            $("#caExposureMins").val("");
        }
        if (!twoDigits.test(sVal)) {
            $("#caExposureSecs").val("");
        }
    }
}
