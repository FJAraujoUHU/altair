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
            shutter = "Open at " + data.dmShutter + "\%";
        else
            shutter = data.dmShutterStatus;
        $("#dmShutter").text(shutter);

        $("#dmSlaved").text(data.dmSlaved);
        $("#dmSlewing").text(data.dmSlewing);
        $("#dmAtHome").text(data.dmAtHome);
        $("#dmParked").text(data.dmParked);
    };
});

function toHMS(value) {
    let h = Math.floor(value);
    let m = Math.floor((value - h) * 60);
    let s = Math.floor(((value - h) * 60 - m) * 60);
    return h + ":" + m + ":" + s;
}

function toDMS(value) {
    let d = Math.floor(value);
    let m = Math.floor((value - d) * 60);
    let s = Math.floor(((value - d) * 60 - m) * 60);
    return d + "° " + m + "\' " + s + "\"";
}