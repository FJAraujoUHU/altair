let canCloud, canHumidity, canPressure, canTemperature, canRain, canWind, canSkyBrightness, canSkyQuality;
let wwConnected, isSafe, cloudCover, humidity, pressure, rainRate, skyBrightness, skyQuality, temperatureSky, temperatureAmbient, windDirection, windGust, windSpeed;
const CAP_NONE = 0;
const CAP_GEN = 1;
const CAP_SP = 2;

$(document).ready(function () {
    let token = $("meta[name='_csrf']").attr("content");
    let header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    // Set up capabilities
    if (!capabilities) {
        canCloud = CAP_NONE;
        canHumidity = CAP_NONE;
        canPressure = CAP_NONE;
        canTemperature = CAP_NONE;
        canRain = CAP_NONE;
        canWind = CAP_NONE;
        canSkyBrightness = CAP_NONE;
        canSkyQuality = CAP_NONE;
    } else {
        canCloud = capabilities.canCloud;
        canHumidity = capabilities.canHumidity;
        canPressure = capabilities.canPressure;
        canTemperature = capabilities.canTemperature;
        canRain = capabilities.canRain;
        canWind = capabilities.canWind;
        canSkyBrightness = capabilities.canSkyBrightness;
        canSkyQuality = capabilities.canSkyQuality;
    }

    // Set up monitoring
    const source = new EventSource("/altair/api/weatherwatch/stream");
    source.onmessage = function (event) {
        $("#wwConnect").prop('disabled', false);
        let data = JSON.parse(event.data);
        console.log(data);
        
        wwConnected = data.connected;
        isSafe = data.isSafe;
        cloudCover = data.cloudCover;
        humidity = data.humidity;
        pressure = data.pressure;
        rainRate = data.rainRate;
        skyBrightness = data.skyBrightness;
        skyQuality = data.skyQuality;
        temperatureSky = data.temperatureSky;
        temperatureAmbient = data.temperatureAmbient;
        windDirection = data.windDirection;
        windGust = data.windGust;
        windSpeed = data.windSpeed;



        $("#wwConnected").text(wwConnected);
        $("#wwIsSafe").text(isSafe);
        switch (canCloud) {
            case CAP_NONE:
                cloudCover = "N/A";
                break;
            case CAP_SP:
                cloudCover = parseFloat(cloudCover).toFixed(1) + " %";
                break;
        }
        $("#wwCloudCover").text(cloudCover);
        switch (canHumidity) {
            case CAP_NONE:
                humidity = "N/A";
                break;
            case CAP_SP:
                humidity = parseFloat(humidity).toFixed(1) + " %";
                break;
        }
        $("#wwHumidity").text(humidity);
        switch (canPressure) {
            case CAP_NONE:
                pressure = "N/A";
                break;
            case CAP_SP:
                pressure = parseFloat(pressure).toFixed(1) + " hPa";
                break;
        }
        $("#wwPressure").text(pressure);
        switch (canTemperature) {
            case CAP_NONE:
                temperatureSky = "N/A";
                temperatureAmbient = "N/A";
                break;
            case CAP_SP:
                temperatureSky = parseFloat(temperatureSky).toFixed(1) + " °C";
                temperatureAmbient = parseFloat(temperatureAmbient).toFixed(1) + " °C";
                break;
        }
        $("#wwTempSky").text(temperatureSky);
        $("#wwTempAmbient").text(temperatureAmbient);
        switch (canRain) {
            case CAP_NONE:
                rainRate = "N/A";
                break;
            case CAP_SP:
                rainRate = parseFloat(rainRate).toFixed(1) + " mm/h";
                break;
        }
        $("#wwRainRate").text(rainRate);
        switch (canWind) {
            case CAP_NONE:
                windDirection = "N/A";
                windGust = "N/A";
                windSpeed = "N/A";
                break;
            case CAP_SP:
                windDirection = (isNaN(windDirection)) ? "None" : parseFloat(windDirection).toFixed(1) + " °";
                windGust = parseFloat(windGust).toFixed(1) + " m/s";
                windSpeed = parseFloat(windSpeed).toFixed(1) + " m/s";
                break;
        }
        $("#wwWindDirection").text(windDirection);
        $("#wwWindGust").text(windGust);
        $("#wwWindSpeed").text(windSpeed);
        switch (canSkyBrightness) {
            case CAP_NONE:
                skyBrightness = "N/A";
                break;
            case CAP_SP:
                skyBrightness = parseFloat(skyBrightness).toFixed(1) + " lux";
                break;
        }
        $("#wwSkyBrightness").text(skyBrightness);
        switch (canSkyQuality) {
            case CAP_NONE:
                skyQuality = "N/A";
                break;
            case CAP_SP:
                skyQuality = parseFloat(skyQuality).toFixed(1) + " mag/arcsec²";
                break;
        }
        $("#wwSkyQuality").text(skyQuality);

        if (wwConnected) {
            $("#wwConnect").text("Disconnect");
        } else {
            $("#wwConnect").text("Connect");
        }

    }

    $("#wwConnect").click(function () {
        let url = wwConnected ? "/altair/api/weatherwatch/disconnect" : "/altair/api/weatherwatch/connect";
        $.ajax({
            url: url,
            type: "POST",
            error: function (xhr, status, error) {
                console.log("Error: " + error);
            },
            success: function (result, status, xhr) {
                location.reload(true); // reload page to update capabilities
            }
        });
        
    });




});