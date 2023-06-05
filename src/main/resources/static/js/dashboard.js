$(document).ready(function () {
    const source = new EventSource("/altair/observatory/stream");
    source.onmessage = function (event) {
        let data = JSON.parse(event.data);
        console.log(data);

        // Telescope
        $("#tsConnected").text(data.tsConnected);
        $("#tsSiderealTime").text(toHMS(data.tsSiderealTime));
        $("#tsRightAscension").text(toHMS(data.tsRightAscension));
        $("#tsDeclination").text(toDMS(data.tsDeclination));
        $("#tsAzimuth").text(toDMS(data.tsAzimuth));
        $("#tsAltitude").text(toDMS(data.tsAltitude));
        $("#tsSlewing").text(data.tsSlewing);
        $("#tsTracking").text(data.tsTracking);
        $("#tsAtHome").text(data.tsAtHome);
        $("#tsParked").text(data.tsParked);
        
        // Dome
        $("#dmConnected").text(data.dmConnected);
        $("#dmAzimuth").text(toDMS(data.dmAzimuth));
        let shutter;
        if (data.dmShutterStatus.toUpperCase() === "OPEN")
            shutter = "Open at " + data.dmShutter + "%";
        else
            shutter = data.dmShutterStatus;
        $("#dmShutter").text(shutter);
        $("#dmSlaved").text(data.dmSlaved);
        $("#dmSlewing").text(data.dmSlewing);
        $("#dmAtHome").text(data.dmAtHome);
        $("#dmParked").text(data.dmParked);

        // Focuser
        $("#fcConnected").text(data.fcConnected);
        $("#fcPosition").text(data.fcPosition);
        $("#fcTemperature").text(data.fcTemperature);
        $("#fcTempComp").text(data.fcTempComp);
        $("#fcMoving").text(data.fcMoving);

        // Camera
        let caStatus = data.caStatus;
        if (parseFloat(data.caStatusCompletion)) {    // if statusCompletion is not falsy AKA not null or undefined
            caStatus += " (" + parseFloat(data.caStatusCompletion * 100).toFixed(2) + "%)";
        }
        let caSubframe = data.caSfWidth + "x" + data.caSfHeight + " @(" + data.caSfX + "," + data.caSfY + ")";
        let caCooler = data.caCoolerStatus;
        if (parseFloat(data.caCoolerPower)) {
            caCooler += " (" + parseFloat(data.caCoolerPower).toFixed(2) + "%)";
        }
        $("#caConnected").text(data.caConnected);
        $("#caTemperature").text(data.caTemperature);
        $("#caStatus").text(caStatus);
        $("#caBinning").text(data.caBinning);
        $("#caSubframe").text(caSubframe);
        $("#caCooler").text(caCooler);
    };
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