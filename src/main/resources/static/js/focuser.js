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
        $("#fcTemperature").text(temperature);
        $("#fcTempComp").text(tempComp);
        $("#fcMoving").text(moving);

        // TODO : update controls

    }
});