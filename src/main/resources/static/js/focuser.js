let connected, position, temperature, tempComp, moving;

$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Set up monitoring
    const source = new EventSource("/altair/api/focuser/stream");
    source.onmessage = function (event) {
        $("#fcConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);
        
        connected = data.connected;
        position = data.position;
        temperature = data.temperature;
        tempComp = data.tempComp;
        moving = data.moving;

        // Focuser
        $("#fcConnected").text(connected);
        $("#fcPosition").text(position);
        $("#fcPositionTxt").text(position);
        $("#fcTemperature").text(temperature);
        $("#fcTempComp").text(tempComp);
        $("#fcMoving").text(moving);

        // TODO : update controls
        if (connected) {
            $("#fcConnect").text("Disconnect");
        } else {
            $("#fcConnect").text("Connect");
        }
        $("#fcTempCompToggle").prop('checked', tempComp);

        let enable = connected && !moving;
        $("#fcInBtnGroup").find("button").prop('disabled', !enable);
        $("#fcOutBtnGroup").find("button").prop('disabled', !enable);
        $("#fcMoveToBtn").prop('disabled', !enable);
        $("#fcAbort").prop('disabled', enable);
        $("#fcTempCompToggle").prop('disabled', !connected);

    }

    // Set up controls
    // Connect
    $("#fcConnect").click(function () {
        let url = connected ? "/altair/api/focuser/disconnect" : "/altair/api/focuser/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Move in
    $("#fcInBtnGroup").find("button").click(function () {
        let step = -parseInt($(this).data("steps"));
        $.ajax({
            url: "/altair/api/focuser/moverelative",
            type: "POST",
            data: {
                steps: step
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Move out
    $("#fcOutBtnGroup").find("button").click(function () {
        let step = $(this).data("steps");
        $.ajax({
            url: "/altair/api/focuser/moverelative",
            type: "POST",
            data: {
                steps: step
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Move to
    $("#fcMoveToBtn").click(function () {
        let position = parseInt($("#fcMoveToTxt").val());
        if (isNaN(position)) {
            $("#fcMoveToTxt").addClass("is-invalid");
            console.error("Must enter a valid position");
            return;
        } else {
            $("#fcMoveToTxt").removeClass("is-invalid");
        }
        $.ajax({
            url: "/altair/api/focuser/move",
            type: "POST",
            data: {
                position: position
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Abort
    $("#fcAbort").click(function () {
        $.ajax({
            url: "/altair/api/focuser/abort",
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });

    // Temperature compensation
    $("#fcTempCompToggle").change(function () {
        $.ajax({
            url: "/altair/api/focuser/tempcomp",
            type: "POST",
            data: {
                enable: $(this).prop("checked")
            },
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            }
        });
    });
});