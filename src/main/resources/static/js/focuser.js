let connected, position, temperature, tempComp, moving;
let canAbsolute, canTempComp, maxIncrement, maxStep, stepSize;

$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Set up capabilities
    if (!capabilities) {
        canAbsolute = false;
        canTempComp = false;
        maxIncrement = 1;
        maxStep = 1;
        stepSize = 0;
    } else {
        canAbsolute = capabilities.canAbsolute;
        canTempComp = capabilities.canTempComp;
        maxIncrement = parseInt(capabilities.maxIncrement, 10);
        maxStep = parseInt(capabilities.maxStep, 10);
        stepSize = parseInt(capabilities.stepSize, 10);
    }

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

        
        if (connected) {
            $("#fcConnect").text("Disconnect");
        } else {
            $("#fcConnect").text("Connect");
        }
        $("#fcTempCompToggle").prop('checked', tempComp);

        $("#fcInBtnGroup").find("button").prop('disabled', !connected || moving);
        $("#fcOutBtnGroup").find("button").prop('disabled', !connected || moving);
        $("#fcMoveToBtn").prop('disabled', !connected || moving || !(maxIncrement > 0));
        $("#fcMoveToTxt").prop('disabled', !connected || moving || !(maxIncrement > 0));
        $("#fcAbort").prop('disabled', !moving);
        $("#fcTempCompToggle").prop('disabled', !connected || moving || !canTempComp);

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
            },
            success: function (result, status, xhr) {
                location.reload(true);
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

    // Validate move to input
    $("#fcMoveToTxt").on("input", function () {
        let inputValue = $(this).val();

        // Remove any non-digit characters
        inputValue = inputValue.replace(/\D/g, "");

        if (!inputValue == "") {
            return;
        }

        // Convert the input value to a number
        let value = parseInt(inputValue);

        if (isNaN(value)) {
            $(this).val("");
            return;
        }

        // Clamp the value between 0 and valMax
        value = Math.max(0, Math.min(maxStep, value));

        // Update the input value with the clamped value
        $(this).val(value);
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