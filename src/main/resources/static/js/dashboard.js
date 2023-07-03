$(document).ready(function () {
    // Set up CSRF token
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });
    
    const source = new EventSource("/altair/observatory/stream");
    source.onmessage = function (event) {
        let data = JSON.parse(event.data);
        console.log(data);

        // Telescope
        $("#tsConnected").text(data.telescope.connected);
        $("#tsSiderealTime").text(toHMS(data.telescope.siderealTime));
        $("#tsRightAscension").text(toHMS(data.telescope.rightAscension));
        $("#tsDeclination").text(toDMS(data.telescope.declination));
        $("#tsAzimuth").text(toDMS(data.telescope.azimuth));
        $("#tsAltitude").text(toDMS(data.telescope.altitude));
        $("#tsSlewing").text(data.telescope.slewing);
        $("#tsTracking").text(data.telescope.tracking);
        $("#tsAtHome").text(data.telescope.atHome);
        $("#tsParked").text(data.telescope.parked);
        
        // Dome
        $("#dmConnected").text(data.dome.connected);
        $("#dmAzimuth").text(toDMS(data.dome.azimuth));
        let shutter;
        if (data.dome.shutterStatus.toUpperCase() === "OPEN")
            shutter = "Open at " + data.dome.shutter + "%";
        else
            shutter = data.dome.shutterStatus;
        $("#dmShutter").text(shutter);
        $("#dmSlaved").text(data.dome.slaved);
        $("#dmSlewing").text(data.dome.slewing);
        $("#dmAtHome").text(data.dome.atHome);
        $("#dmParked").text(data.dome.parked);

        // Focuser
        $("#fcConnected").text(data.focuser.connected);
        $("#fcPosition").text(data.focuser.position);
        $("#fcTemperature").text(data.focuser.temperature);
        $("#fcTempComp").text(data.focuser.tempComp);
        $("#fcMoving").text(data.focuser.moving);

        // Camera
        let caStatus = data.camera.status;
        if (parseFloat(data.camera.statusCompletion)) {    // if statusCompletion is not falsy AKA not null or undefined
            caStatus += " (" + parseFloat(data.camera.statusCompletion * 100).toFixed(2) + "%)";
        }
        let caSubframe = data.camera.sfWidth + "x" + data.camera.sfHeight + " @(" + data.camera.sfX + "," + data.camera.sfY + ")";
        let caCooler = data.camera.coolerStatus;
        if (parseFloat(data.camera.coolerPower)) {
            caCooler += " (" + parseFloat(data.camera.coolerPower).toFixed(2) + "%)";
        }
        $("#caConnected").text(data.camera.connected);
        $("#caTemperature").text(data.camera.temperature);
        $("#caStatus").text(caStatus);
        $("#caBinning").text(data.camera.binning);
        $("#caSubframe").text(caSubframe);
        $("#caCooler").text(caCooler);

        // Filter Wheel
        $("#fwCurrentOff").text(data.filterWheel.curOffset);
        $("#fwCurrentName").text(data.filterWheel.curName);

        // WeatherWatch
        $("#wwConnected").text(data.weatherWatch.connected);
        $("#wwIsSafe").text(data.weatherWatch.isSafe);
        $("#wwCloudCover").text(data.weatherWatch.cloudCover);
        $("#wwHumidity").text(data.weatherWatch.humidity);
        $("#wwPressure").text(data.weatherWatch.pressure);
        $("#wwTempSky").text(data.weatherWatch.temperatureSky);
        $("#wwTempAmbient").text(data.weatherWatch.temperatureAmbient);
        $("#wwRainRate").text(data.weatherWatch.rainRate);
        $("#wwWindDirection").text(data.weatherWatch.windDirection);
        $("#wwWindGust").text(data.weatherWatch.windGust);
        $("#wwWindSpeed").text(data.weatherWatch.windSpeed);
        $("#wwSkyBrightness").text(data.weatherWatch.skyBrightness);
        $("#wwSkyQuality").text(data.weatherWatch.skyQuality);

    };

    const sourceGov = new EventSource("/altair/api/governor/stream");
    sourceGov.onmessage = function (event) {
        let gvData = JSON.parse(event.data);
        console.log(gvData);

        $("#gvState").text(gvData.state);
        $("#gvSafe").text(gvData.isSafe);
        $("#gvSafeOvr").text(gvData.isSafeOverride);
        $("#gvCurrOrder").text(gvData.currentOrder);
        $("#gvCurrUser").text(gvData.currentUser);
        $("#gvRemainingTime").text(gvData.currentOrderRemainingTime);
        $("#gvNextNight").text(gvData.nextNight);
        $("#gvSlaving").text(gvData.slaving);

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