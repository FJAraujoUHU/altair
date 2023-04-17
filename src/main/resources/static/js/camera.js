let connected, temperature, cameraStatus, binning, subframe, cooler;
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

        // TODO : update controls
        if (connected) {
            $("#caConnect").text("Disconnect");
        } else {
            $("#caConnect").text("Connect");
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
});