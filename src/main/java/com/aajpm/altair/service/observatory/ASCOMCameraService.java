package com.aajpm.altair.service.observatory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.time.Duration;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.aajpm.altair.config.ObservatoryConfig.CameraConfig;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import io.netty.channel.ChannelOption;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.Bitpix;

import com.aajpm.altair.utility.TypeTransformer;
import com.aajpm.altair.utility.TypeTransformer.NumberVarType;
import com.aajpm.altair.utility.exception.*;


import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;


public class ASCOMCameraService extends CameraService {

    AlpacaClient client;

    final int deviceNumber;

    private boolean isWarmingUp = false;
    private boolean isCoolingDown = false;
    private final Logger logger = LoggerFactory.getLogger(ASCOMCameraService.class.getName());

    private WebClient cameraClient;

    public ASCOMCameraService(AlpacaClient client, CameraConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = 0;
        this.cameraClient = WebClient.builder()
            .baseUrl(client.getBaseURL() + "/api/v1/camera/" + deviceNumber + "/")
            .clientConnector(
                new ReactorClientHttpConnector(
                    HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(5))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(config.getImageBufferSize()))
            .build();
    }

    public ASCOMCameraService(AlpacaClient client, int deviceNumber, CameraConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = deviceNumber;
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Integer> getStatus() {
        return this.get("camerastate").map(JsonNode::asInt);
    }

    @Override
    public Mono<Double> getStatusCompletion() {
        return this.get("perecentcompleted")
            .map(JsonNode::asDouble)
            .onErrorReturn(e -> { // If the current status doesn't have a completion percentage, return NaN
                if (e instanceof ASCOMException) {
                    return (((ASCOMException) e).getErrorCode() == ASCOMException.INVALID_OPERATION);
                } else {
                    return false;
                }
            }, Double.NaN);
    }

    //#region Temperature info

    @Override
    public Mono<Double> getTemperature() {
        return this.get("ccdtemperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getTemperatureTarget() {
        //yes, even if it's called "setccdtemperature", it returns the target temperature. ASCOM can be weird.
        return this.get("setccdtemperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getTemperatureAmbient() {
        return this.get("heatsinktemperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getCoolerPower() {
        return this.get("coolerpower").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Integer> getCoolerStatus() {
        if (this.isCoolingDown) return Mono.just(CameraService.COOLER_COOLDOWN);
        if (this.isWarmingUp) return Mono.just(CameraService.COOLER_WARMUP);

        Mono<Boolean> isCoolerOn = this.get("cooleron").map(JsonNode::asBoolean);
        Mono<Double> coolerPower = this.getCoolerPower();
        Mono<Double> currentTemp = this.getTemperature();
        Mono<Double> targetTemp = this.getTemperatureTarget();

        return Mono.zip(isCoolerOn, coolerPower, currentTemp, targetTemp).flatMap(tuples -> {
            if (Boolean.FALSE.equals(tuples.getT1())) { // If not ON
                return Mono.just(CameraService.COOLER_OFF);
            }

            if (tuples.getT2() > config.getCoolerSaturationThreshold()) {   // If the cooler is on and at full power
                return Mono.just(CameraService.COOLER_SATURATED);
            }

            if (Math.abs(tuples.getT3() - tuples.getT4()) < 1.1) { // If the cooler is on and at the target temperature
                return Mono.just(CameraService.COOLER_STABLE);
            }
            
            // If the cooler is on, but neither at full power or at the target temperature, it must be warming up or cooling down.
            return Mono.just(CameraService.COOLER_ACTIVE);
        }).onErrorReturn(CameraService.COOLER_ERROR);
    }


    //#endregion


    //#region Exposure parameters

    @Override
    public Mono<Integer> getSubframeWidth() {
        return this.get("numx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeHeight() {
        return this.get("numy").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeStartX() {
        return this.get("startx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeStartY() {
        return this.get("starty").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getBinningX() {
        return this.get("binx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getBinningY() {
        return this.get("biny").map(JsonNode::asInt);
    }

    //#endregion
    

    //#region Sensor info

    public Mono<String> getSensorType() {
        return this.get("sensortype").map(type -> {
            switch (type.asInt()) {
                case 0:
                    return "Monochrome";
                case 1:
                    return "Colour";
                case 2:
                    return "RGGB";
                case 3:
                    return "CMYG";
                case 4:
                    return "CMYG2";
                case 5:
                    return "LRGB";
                default:
                    return "Unknown";
            }
        });
    }

    public Mono<Tuple2<Integer, Integer>> getBayerOffset() {
        Mono<Integer> x = this.get("bayeroffsetx").map(JsonNode::asInt);
        Mono<Integer> y = this.get("bayeroffsety").map(JsonNode::asInt);
        return Mono.zip(x, y);
    }

    //#endregion
    

    //#region Image readout

    @Override
    public Mono<Boolean> isImageReady() {
        return this.get("imageready").map(JsonNode::asBoolean);
    }

    //TODO: test
    @Override
    public Mono<ImageHDU> getImage() {
        return cameraClient.get()
            .uri("/imagearray")
            .accept(MediaType.parseMediaType("application/imagebytes"))
            .exchangeToMono(response -> {
                if (response.statusCode().is2xxSuccessful()) {
                    MediaType contentType = response.headers().contentType().orElse(null);
                    if (contentType == null)
                        return Mono
                            .error(new DeviceException(
                                "Error when retrieving image from camera: No content type returned"));

                    String typeStr = contentType.toString();
                    // If the camera supports the Alpaca ImageBytes format
                    if (typeStr.startsWith("application/imagebytes")) {
                        Mono<byte[]> body = response.bodyToMono(byte[].class);
                        Mono<HeaderData> headerData = this.getHeaderData();

                        return Mono.zip(body, headerData)
                                    .map(tuples -> this.readImageBytes(tuples.getT1(), tuples.getT2()));
                    }

                    // If the camera falls back to standard Alpaca JSON
                    if (typeStr.startsWith("application/json")) {
                        Mono<JsonNode> body = response.bodyToMono(JsonNode.class);
                        Mono<HeaderData> headerData = this.getHeaderData();

                        return Mono.zip(body, headerData)
                                    .map(tuples -> this.readImageArray(tuples.getT1(), tuples.getT2()));
                    }

                    // If the camera returns an unsupported content type
                    return Mono
                        .error(new DeviceException(
                            "Error when retrieving image from camera: Unsupported content type returned: " + typeStr));

                } else {
                    return Mono
                        .error(new DeviceException(
                            "Error when retrieving image from camera: " + response.statusCode().toString()));
                }
            });
    }

    @SuppressWarnings({"java:S128", "java:S1481", "536870973", "java:S3776"}) // Shut up, I know what I'm doing with the switch statement and null checks are unavoidable
    private ImageHDU readImageBytes(byte[] bytes, HeaderData headerData) throws DeviceException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        try {
            int metadataVersion = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            int errorNumber = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            long clientTransactionID = TypeTransformer.convertUInt32LE(dis.readNBytes(4));
            long serverTransactionID = TypeTransformer.convertUInt32LE(dis.readNBytes(4));
            int dataStart = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            NumberVarType imageElementType = NumberVarType.fromValue(TypeTransformer.convertInt32LE(dis.readNBytes(4)));
            NumberVarType transmissionElementType = NumberVarType.fromValue(TypeTransformer.convertInt32LE(dis.readNBytes(4)));
            int rank = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            int dim1 = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            int dim2 = TypeTransformer.convertInt32LE(dis.readNBytes(4));
            int dim3 = TypeTransformer.convertInt32LE(dis.readNBytes(4));

            if (errorNumber != 0)
                throw new ASCOMException(errorNumber); // Could also parse the error message reading the blob as UTF-8
            if (imageElementType == NumberVarType.UNKNOWN || transmissionElementType == NumberVarType.UNKNOWN)
                throw new DeviceException("Error when retrieving image from camera: Unknown image element type");
            if (!(rank == 2 || rank == 3))
                throw new DeviceException("Error when retrieving image from camera: Unsupported image rank");

            dis.close();

            int nElems = rank == 2 ? dim1 * dim2 : dim1 * dim2 * dim3;
            int nBytes = nElems * transmissionElementType.getByteCount();  

            if (nBytes + dataStart > bytes.length)  // If there pixel count and the data size don't match
                throw new DeviceException("Error when retrieving image from camera: Image size mismatch");


            Object imageData;
            Class<?> imageDataClass = imageElementType.getJavaClass();

            // Create and populate the image array
            if (rank == 2) {
                imageData = Array.newInstance(imageDataClass, dim1, dim2);
                Object[][] imageData2D = (Object[][]) imageData;

                IntStream.range(0, nElems)
                    .parallel().forEach(i -> {
                        int bytesIndex = dataStart + i * transmissionElementType.getByteCount();
                        Object value = TypeTransformer
                            .toFits(bytes, bytesIndex, imageElementType, transmissionElementType, true);

                        int x = i % dim1;
                        int y = i / dim1;
                        imageData2D[x][y] = value;
                    });
            } else {
                imageData = Array.newInstance(imageDataClass, dim1, dim2, dim3);
                Object[][][] imageData3D = (Object[][][]) imageData;

                IntStream.range(0, nElems)
                    .parallel().forEach(i -> {
                        int bytesIndex = dataStart + i * transmissionElementType.getByteCount();
                        Object value = TypeTransformer
                            .toFits(bytes, bytesIndex, imageElementType, transmissionElementType, true);
 
                        int z = i % dim3;       // TODO: Check if this is correct bc it's 3 am and I made it up ngl
                        int y = (i / dim3) % dim2;
                        int x = i / (dim2 * dim3);
                        imageData3D[x][y][z] = value;
                    });
            }

            ImageHDU imageHDU = (ImageHDU) FitsFactory.hduFactory(imageData);
            Header header = imageHDU.getHeader();

            //Bitpix setting
            switch (imageElementType) {
                case BYTE:
                    header.setBitpix(Bitpix.BYTE);
                    break;
                case UINT16:
                    header.addValue("BZERO", 32768, "Offset data range to that of unsigned short");
                    // continue
                case INT16:
                    header.setBitpix(Bitpix.SHORT);
                    break;
                case UINT32:
                    header.addValue("BZERO", 2147483648L, "Offset data range to that of unsigned int");
                    // continue
                case INT32:
                    header.setBitpix(Bitpix.INTEGER);
                    break;
                case UINT64:    // As string because it's too big for a Number
                    header.addValue("BZERO", "9223372036854775808", "Offset data range to that of unsigned long");
                    // continue
                    case INT64:
                    header.setBitpix(Bitpix.LONG);
                    break;
                case SINGLE:
                    header.setBitpix(Bitpix.FLOAT);
                    break;
                case DOUBLE:
                    header.setBitpix(Bitpix.DOUBLE);
                    break;
                default:
            }
            header.addValue("BSCALE", 1, "Default scaling factor");
            header.setNaxes(rank);
            header.setNaxis(1, dim1);
            header.setNaxis(2, dim2);
            if (rank == 3 && dim3 > 0)
                header.setNaxis(3, dim3);

            if (headerData != null) {
                if (headerData.dateObs != null)
                    header.addValue("DATE-OBS", headerData.dateObs, "UTC start of exposure");
                if (headerData.expTime != null)
                    header.addValue("EXPTIME", headerData.expTime, "Exposure time (s)");
                if (headerData.gain != null)
                    header.addValue("GAIN", headerData.gain, "Gain setting");
                if (headerData.ccdTemp != null)
                    header.addValue("CCD-TEMP", headerData.ccdTemp, "CCD temperature (C)");
                if (headerData.setTemp != null)
                    header.addValue("SET-TEMP", headerData.setTemp, "CCD set temperature (C)");
                if (headerData.binX != null)
                    header.addValue("XBINNING", headerData.binX, "Binning factor in X axis");
                if (headerData.binY != null)
                    header.addValue("YBINNING", headerData.binY, "Binning factor in Y axis");
                if (headerData.bayerPat != null) {
                    header.addValue("BAYERPAT", headerData.bayerPat, "Bayer color filter array pattern");
                    if (headerData.bayerX != null) {
                        header.addValue("XBAYROFF", headerData.bayerX, "Bayer offset in X");
                        header.addValue("BAYOFFX", headerData.bayerX, "Bayer offset in X (for legacy software)");
                    }
                    if (headerData.bayerY != null) {
                        header.addValue("YBAYROFF", headerData.bayerY, "Bayer offset in Y");
                        header.addValue("BAYOFFY", headerData.bayerY, "Bayer offset in Y (for legacy software)");
                    }
                }

            }

            return imageHDU;

        } catch (IOException e) {
            throw new DeviceException("Error when retrieving image from camera: Error when parsing image bytes", e);
        } catch (FitsException e) {
            throw new DeviceException("Error when retrieving image from camera: Image array could not be converted to a HDU", e);
        }
    }

    @SuppressWarnings({"java:S128", "java:S3776"}) // Shut up, I know what I'm doing with the switch statement and null checks are unavoidable
    private ImageHDU readImageArray(JsonNode json, HeaderData headerData) throws DeviceException {
        int errorNumber = json.get("ErrorNumber").asInt();
        NumberVarType type = NumberVarType.fromValue(json.get("Type").asInt());
        int rank = json.get("Rank").asInt();

        if (errorNumber != 0) {
            String errorString = json.get("ErrorMessage").asText();
            throw new ASCOMException(errorNumber, errorString);
        }
        if (type == NumberVarType.UNKNOWN)
            throw new DeviceException("Error when retrieving image from camera: Unknown image element type");
        if (!(rank == 2 || rank == 3))
            throw new DeviceException("Error when retrieving image from camera: Unsupported image rank");
        if (!json.get("Value").isArray())
            throw new DeviceException("Error when retrieving image from camera: Image data is not an array");

        JsonNode imageNode = json.get("Value");
        int dim1 = imageNode.get(0).size();
        int dim2 = imageNode.size();
        int dim3 = 0;
        Object imageData = null;

        if (dim1 != headerData.numX)
            throw new DeviceException("Error when retrieving image from camera: Image width mismatch");
        if (dim2 != headerData.numY)
            throw new DeviceException("Error when retrieving image from camera: Image height mismatch");

        if (rank == 2) {
            imageData = Array.newInstance(type.getJavaClass(), dim1, dim2);
            Object[][] imageData2D = (Object[][]) imageData;

            IntStream.range(0, dim1).parallel().forEach(x ->
                IntStream.range(0, dim2).parallel().forEach(y ->
                    imageData2D[x][y] = asObject(imageNode.get(y).get(x), type)
                )
            );

        } else {
            dim3 = imageNode.get(0).get(0).size();
            imageData = Array.newInstance(type.getJavaClass(), dim1, dim2, dim3);
            Object[][][] imageData3D = (Object[][][]) imageData;

            int dimThree = dim3; // must be final for lambda
            IntStream.range(0, dim1).parallel().forEach(x ->
                IntStream.range(0, dim2).parallel().forEach(y ->
                    IntStream.range(0, dimThree).forEach(z ->
                        imageData3D[x][y][z] = asObject(imageNode.get(y).get(x).get(z), type)
                    )
                )
            );
        }

        try {
            ImageHDU imageHDU = (ImageHDU) FitsFactory.hduFactory(imageData);
            Header header = imageHDU.getHeader();

            // Bitpix setting
            switch (type) {
                case BYTE:
                    header.setBitpix(Bitpix.BYTE);
                    break;
                case UINT16:
                    header.addValue("BZERO", 32768, "Offset data range to that of unsigned short");
                    // continue
                case INT16:
                    header.setBitpix(Bitpix.SHORT);
                    break;
                case UINT32:
                    header.addValue("BZERO", 2147483648L, "Offset data range to that of unsigned int");
                    // continue
                case INT32:
                    header.setBitpix(Bitpix.INTEGER);
                    break;
                case UINT64: // As string because it's too big for a Number
                    header.addValue("BZERO", "9223372036854775808", "Offset data range to that of unsigned long");
                    // continue
                case INT64:
                    header.setBitpix(Bitpix.LONG);
                    break;
                case SINGLE:
                    header.setBitpix(Bitpix.FLOAT);
                    break;
                case DOUBLE:
                    header.setBitpix(Bitpix.DOUBLE);
                    break;
                default:
            }
            header.addValue("BSCALE", 1, "Default scaling factor");
            header.setNaxes(rank);
            header.setNaxis(1, dim1);
            header.setNaxis(2, dim2);
            if (rank == 3 && dim3 > 0)
                header.setNaxis(3, dim3);

            if (headerData != null) {
                if (headerData.dateObs != null)
                    header.addValue("DATE-OBS", headerData.dateObs, "UTC start of exposure");
                if (headerData.expTime != null)
                    header.addValue("EXPTIME", headerData.expTime, "Exposure time (s)");
                if (headerData.gain != null)
                    header.addValue("GAIN", headerData.gain, "Gain setting");
                if (headerData.ccdTemp != null)
                    header.addValue("CCD-TEMP", headerData.ccdTemp, "CCD temperature (C)");
                if (headerData.setTemp != null)
                    header.addValue("SET-TEMP", headerData.setTemp, "CCD set temperature (C)");
                if (headerData.binX != null)
                    header.addValue("XBINNING", headerData.binX, "Binning factor in X axis");
                if (headerData.binY != null)
                    header.addValue("YBINNING", headerData.binY, "Binning factor in Y axis");
                if (headerData.bayerPat != null) {
                    header.addValue("BAYERPAT", headerData.bayerPat, "Bayer color filter array pattern");
                    if (headerData.bayerX != null) {
                        header.addValue("XBAYROFF", headerData.bayerX, "Bayer offset in X");
                        header.addValue("BAYOFFX", headerData.bayerX, "Bayer offset in X (for legacy software)");
                    }
                    if (headerData.bayerY != null) {
                        header.addValue("YBAYROFF", headerData.bayerY, "Bayer offset in Y");
                        header.addValue("BAYOFFY", headerData.bayerY, "Bayer offset in Y (for legacy software)");
                    }
                }
            }

            return imageHDU;

        } catch (FitsException e) {
            throw new DeviceException("Error when retrieving image from camera: " + e.getMessage(), e);
        }
    }



    //#endregion


    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public void connect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(true));
        this.execute("connected", new LinkedMultiValueMap<>());
    }

    @Override
    public void disconnect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        this.execute("connected", new LinkedMultiValueMap<>());
    }


    //#region Cooler

    @Override
    public void setCooler(boolean enable) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("CoolerOn", String.valueOf(enable));
        this.execute("cooleron", params);
    }

    @Override
    public void setTargetTemp(double temperature) {
        if (isCoolingDown) isCoolingDown = false;
        if (isWarmingUp) isWarmingUp = false;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("SetCCDTemperature", String.valueOf(temperature));
        this.execute("setccdtemperature", params);
    }

    @Override
    public void cooldown(double target) {
        Thread t = new Thread(() -> {
            this.setCooler(true);
            this.isWarmingUp = false;
            this.isCoolingDown = true;
            double delta = this.config.getMaxCooldownRate();
            double threshold = this.config.getCoolerSaturationThreshold();
            double minRate = this.config.getMinCooldownRate();
            try {
                while (this.isCoolingDown) {
                    double startTemp = this.getTemperature().block();
                    // Set new target as current temp minus delta, or the target temp if that is closer.
                    double newTarget = (startTemp - target > delta) ? startTemp - delta : target;

                    this.setTargetTemp(newTarget);
                    Thread.sleep(60000);

                    double newTemp = this.getTemperature().block();
                    
                    // If it's saturated and barely cooled, stop cooling further.
                    if (startTemp - newTemp < minRate && this.getCoolerPower().block() > threshold) {
                        this.isCoolingDown = false;
                    }

                    // If it's cooled down enough, stop cooling further.
                    if (newTemp - target < 1.1) {
                        this.isCoolingDown = false;
                    }
                }
            } catch (InterruptedException e) {
                this.logger.error("Error while cooling down", e);
            }
        });
        t.start();
    }

    @Override
    public void warmup() {
        Double target;
        try {
            target = this.getTemperatureAmbient().block();
            if (target == null) {
                target = 25.0;  // Default to 25C if we can't get the ambient temperature.
            }
        } catch (Exception e) {
            target = 25.0;  
        }
        this.warmup(target);
    }

    @Override
    public void warmup(double target) {
        Thread t = new Thread(() -> {
            this.setCooler(true);
            this.isCoolingDown = false;
            this.isWarmingUp = true;
            double delta = this.config.getMaxWarmupRate();
            try {
                while (this.isWarmingUp) {
                    double startTemp = this.getTemperature().block();
                    // Set new target as current temp plus delta, or the target temp if that is closer.
                    double newTarget = (target - startTemp > delta) ? target - startTemp : target;

                    this.setTargetTemp(newTarget);
                    Thread.sleep(60000);

                    double newTemp = this.getTemperature().block();

                    // If it's warm enough, stop warming.
                    if (target - newTemp < 1.1) {
                        this.isWarmingUp = false;
                    }
                }
            } catch (InterruptedException e) {
                this.logger.error("Error while warming up", e);
            }
        });
        t.start();
    }
    

    //#endregion


    //#region Exposure

    @Override
    public void setSubframeStartX(int startX) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("StartX", String.valueOf(startX));
        this.execute("startx", params);
    }

    @Override
    public void setSubframeStartY(int startY) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("StartY", String.valueOf(startY));
        this.execute("starty", params);
    }

    @Override
    public void setSubframeWidth(int width) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("NumX", String.valueOf(width));
        this.execute("numx", params);
    }

    @Override
    public void setSubframeHeight(int height) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("NumY", String.valueOf(height));
        this.execute("numy", params);
    }

    private void setBinningX(int binx) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("BinX", String.valueOf(binx));
        this.execute("binx", params);
    }

    private void setBinningY(int biny) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("BinY", String.valueOf(biny));
        this.execute("biny", params);
    }

    @Override
    public void setBinning(int bin) {
        setBinning(bin, bin);
    }

    @Override
    public void setBinning(int binx, int biny) {
        setBinningX(binx);
        setBinningY(biny);
    }

    @Override
    public void startExposure(double duration, boolean useLightFrame) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Duration", String.valueOf(duration));
        params.add("Light", String.valueOf(useLightFrame));
        this.execute("startexposure", params);
    }

    @Override
    public void stopExposure() {
        this.execute("stopexposure", null);
    }

    @Override
    public void abortExposure() {
        this.execute("abortexposure", null);
    }

    //#endregion





    //#endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("camera", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("camera", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("camera", deviceNumber, action, params).subscribe();
    }


    private static Object asObject(JsonNode value, NumberVarType castTo) {
        Object ret = null;
        switch (castTo) {
            case BYTE:
                ret = (byte) (value.asInt() - Byte.MIN_VALUE);
                break;
            case DOUBLE:
                ret = value.asDouble();
                break;
            case INT16:
                ret = (short) value.asInt();
                break;
            case INT32:
                ret = value.asInt();
                break;
            case INT64:
                ret = value.asLong();
                break;
            case SINGLE:
                ret = (float) value.asDouble();
                break;
            case UINT16:
                ret = (short) (value.asInt() + Short.MIN_VALUE);
                break;
            case UINT32:
                ret = (int) (value.asLong() + Integer.MIN_VALUE);
                break;
            case UINT64:
                ret = value.bigIntegerValue().add(BigInteger.valueOf(Long.MIN_VALUE)).longValue();
                break;
            default:
                break;
        }
        return ret;
    }

    @SuppressWarnings("java:S3776")
    private Mono<HeaderData> getHeaderData() {
        Mono<String> dateObs = this.get("lastexposurestarttime").map(JsonNode::asText).onErrorReturn("");
        Mono<Integer> expTime = this.get("lastexposureduration").map(JsonNode::asInt).onErrorReturn(Integer.MIN_VALUE);
        Mono<Integer> gain = this.get("gain").map(JsonNode::asInt).onErrorReturn(Integer.MIN_VALUE);
        Mono<String> bayerPat = this.get("sensortype").map(type -> {
            switch (type.asInt()) {
                case 2:
                    return "RGGB";
                case 3:
                    return "CMYG";
                case 4:
                    return "CMYG2";
                case 5:
                    return "LRGB";
                default:
                    return "";
            }
        }).onErrorReturn("");

        Mono<Tuple2<Integer, Integer>> bayerXY = this.getBayerOffset().onErrorReturn(Tuples.of(Integer.MIN_VALUE, Integer.MIN_VALUE));
        Mono<Tuple2<Integer, Integer>> binXY = this.getBinning().onErrorReturn(Tuples.of(Integer.MIN_VALUE, Integer.MIN_VALUE));
        Mono<Tuple2<Integer, Integer>> numXY = Mono.zip(
            this.getSubframeWidth().onErrorReturn(Integer.MIN_VALUE),
            this.getSubframeHeight().onErrorReturn(Integer.MIN_VALUE)
        );
        
        Mono<Tuple2<Double, Double>> temps = Mono.zip(
            this.getTemperature().onErrorReturn(Double.NaN),
            this.getTemperatureTarget().onErrorReturn(Double.NaN)
        );

        return Mono.zip(dateObs, expTime, temps, gain, bayerPat, bayerXY, binXY, numXY).map(
            tuple -> new HeaderData(
                tuple.getT1().isEmpty() ?                           null : tuple.getT1(),
                tuple.getT2().equals(Integer.MIN_VALUE) ?           null : tuple.getT2(),
                tuple.getT3().getT1().equals(Double.NaN) ?          null : tuple.getT3().getT1(),
                tuple.getT3().getT2().equals(Double.NaN) ?          null : tuple.getT3().getT2(),
                tuple.getT4().equals(Integer.MIN_VALUE) ?           null : tuple.getT4(),
                tuple.getT5().isEmpty() ?                           null : tuple.getT5(),
                tuple.getT6().getT1().equals(Integer.MIN_VALUE) ?   null : tuple.getT6().getT1(),
                tuple.getT6().getT2().equals(Integer.MIN_VALUE) ?   null : tuple.getT6().getT2(),
                tuple.getT7().getT1().equals(Integer.MIN_VALUE) ?   null : tuple.getT7().getT1(),
                tuple.getT7().getT2().equals(Integer.MIN_VALUE) ?   null : tuple.getT7().getT2(),
                tuple.getT8().getT1().equals(Integer.MIN_VALUE) ?   null : tuple.getT8().getT1(),
                tuple.getT8().getT2().equals(Integer.MIN_VALUE) ?   null : tuple.getT8().getT2()
            )
        );
    }

    private record HeaderData (
        String dateObs,
        Integer expTime,
        Double ccdTemp,
        Double setTemp,
        Integer gain,
        String bayerPat,
        Integer bayerX,
        Integer bayerY,
        Integer binX,
        Integer binY,
        Integer numX,
        Integer numY
    ) {}



    //#endregion
}
