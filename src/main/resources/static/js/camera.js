let connected, temperature, cameraStatus, binning, subframe, cooler, coolerConnected;
$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

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

        // TODO : dont use placeholder
        connected = true; // spoof simulator, must remove for production
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
        $("#caBinningSym").prop('disabled', !connected);
        $("#caExposureBtns").find('button').prop('disabled', !connected);
        $("#caExposureDLBtns").find('button').prop('disabled', !connected);
        $("#caCoolerEnable").prop('disabled', !connected);
        $("#caCoolerWarmup").prop('disabled', !connected);
        $("#caCoolerTempSetPane").find('button').prop('disabled', !connected);

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

    // TODO : Symmetric does nothing for now
    $("#caBinningSet").click(function () {
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
    });
    
    $("#caExposureStart").click(function () {
        let hours = $("#caExposureHours").val();
        let minutes = $("#caExposureMinutes").val();
        let seconds = $("#caExposureSeconds").val();
        let duration = (hours * 3600) + (minutes * 60) + seconds;
        let lightframe = $("#caExposureLight").is(":checked");

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
            url: "/altair/api/camera/coolwarmup",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
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