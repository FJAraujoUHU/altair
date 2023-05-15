package com.aajpm.altair.service.observatory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
        this(client, 0, config);
    }

    public ASCOMCameraService(AlpacaClient client, int deviceNumber, CameraConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = deviceNumber;
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

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Integer> getCameraStatus() {
        return this.get("camerastate").map(JsonNode::asInt);
    }

    @Override
    public Mono<Double> getStatusCompletion() {
        return this.get("percentcompleted")
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
                                    .map(tuples -> ASCOMCameraService.readImageBytes(tuples.getT1(), tuples.getT2()));
                    }

                    // If the camera falls back to standard Alpaca JSON
                    if (typeStr.startsWith("application/json")) {
                        Mono<JsonNode> body = response.bodyToMono(JsonNode.class);
                        Mono<HeaderData> headerData = this.getHeaderData();

                        return Mono.zip(body, headerData)
                                    .map(tuples -> ASCOMCameraService.readImageArray(tuples.getT1(), tuples.getT2()));
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

    
    public void dumpImage(String name) {
        cameraClient.get()
        .uri("/imagearray")
        .accept(MediaType.parseMediaType("application/imagebytes"))
        .exchangeToMono(response -> {
            if (!response.statusCode().is2xxSuccessful()) {
                return Mono
                    .error(new DeviceException(
                        "Error when retrieving image from camera: " + response.statusCode().toString()));
            }

            MediaType contentType = response.headers().contentType().orElse(null);
            if (contentType == null)    // If no content type is returned
                return Mono
                    .error(new DeviceException(
                        "Error when retrieving image from camera: No content type returned"));

            String typeStr = contentType.toString();
            // If the camera supports the Alpaca ImageBytes format
            if (typeStr.startsWith("application/imagebytes")) {
                return response.bodyToMono(byte[].class);
            }

            // If the camera falls back to standard Alpaca JSON
            if (typeStr.startsWith("application/json")) {
                return response.bodyToMono(JsonNode.class);
            }

            // If the camera returns an unsupported content type
            return Mono
                .error(new DeviceException(
                    "Error when retrieving image from camera: Unsupported content type returned: " + typeStr));

        }).subscribe(response -> {
            String filename = name;

            try {
                // Create image store directory if it doesn't exist
                if (!Files.exists(config.getImageStorePath())) {
                    Files.createDirectories(config.getImageStorePath());
                }

                if (response instanceof byte[]) {
                    byte[] data = (byte[]) response;

                    // Cleans up the filename to remove any illegal characters
                    filename = filename.replaceAll("[^a-zA-Z0-9\\._\\-]", "_");

                    // Add .bin extension if not present   
                    if (!filename.toUpperCase().endsWith(".BIN")) {
                        filename += ".bin";
                    }

                    // Write image to file
                    Path path = config.getImageStorePath().resolve(filename);
                    Files.write(path, data);

                    logger.info("Image saved to {}", path);
                }
                else if (response instanceof JsonNode) {
                    JsonNode data = (JsonNode) response;
        
                    // Add .json extension if not present   
                    if (!filename.toUpperCase().endsWith(".JSON")) {
                        filename += ".json";
                    } 
                            
                    // Write image to file
                    Path path = config.getImageStorePath().resolve(filename);
    
                    ObjectMapper mapper = new ObjectMapper();
                    OutputStream out = Files.newOutputStream(path);
                    mapper.writeValue(out, data);
                    out.close(); 
    
                    logger.info("Image saved to {}", path); 
                } else throw new IOException("Unknown response type");
            } catch (IOException e) {
                logger.error("Error when saving image to file: {}", e.getMessage());
            } 
        });
    }

    @SuppressWarnings({"java:S128", "java:S1481", "536870973", "java:S3776"}) // Shut up, I know what I'm doing with the switch statement and null checks are unavoidable
    protected static ImageHDU readImageBytes(byte[] bytes, HeaderData headerData) throws DeviceException {
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
            Class<?> imageDataClass = imageElementType.getJavaClassWrapper();

            // Create and populate the image array
            if (rank == 2) {
                imageData = Array.newInstance(imageDataClass, dim2, dim1);
                Object[][] imageData2D = (Object[][]) imageData;

                IntStream.range(0, dim1).parallel().forEach(x ->
                    IntStream.range(0, dim2).forEach(y -> {
                        
                        int bytesIndex = dataStart + ((x * dim2 + y) * transmissionElementType.getByteCount());
                            
                        Object value = TypeTransformer
                            .toFits(bytes, bytesIndex, imageElementType, transmissionElementType, true);

                        imageData2D[y][x] = value;  // FITS images are stored with the axes flipped
                    })
                );
            } else {
                imageData = Array.newInstance(imageDataClass, dim3, dim2, dim1);
                Object[][][] imageData3D = (Object[][][]) imageData;

                IntStream.range(0, dim1).parallel().forEach(x ->
                    IntStream.range(0, dim2).forEach(y ->
                        IntStream.range(0, dim3).forEach(z -> {
                            int bytesIndex = dataStart + ((x * dim2 * dim3 + y * dim3 + z) * transmissionElementType.getByteCount());
                            Object value = TypeTransformer
                                .toFits(bytes, bytesIndex, imageElementType, transmissionElementType, true);

                            imageData3D[z][y][x] = value;
                        })
                    )
                );
            }
            
            Header header = new Header();
            header.setSimple(true);

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

            imageData = unwrap(imageData, rank, imageElementType);        
            return (ImageHDU) FitsFactory.hduFactory(header, ImageHDU.encapsulate(imageData));

        } catch (IOException e) {
            throw new DeviceException("Error when retrieving image from camera: Error when parsing image bytes", e);
        } catch (FitsException e) {
            throw new DeviceException("Error when retrieving image from camera: Image array could not be converted to a HDU", e);
        }
    }

    @SuppressWarnings({"java:S128", "java:S3776"}) // Shut up, I know what I'm doing with the switch statement and null checks are unavoidable
    protected static ImageHDU readImageArray(JsonNode json, HeaderData headerData) throws DeviceException {
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
        int dim1 = imageNode.size();
        int dim2 = imageNode.get(0).size();
        int dim3 = 0;
        Object imageData = null;

        if (dim1 != headerData.numX)
            throw new DeviceException("Error when retrieving image from camera: Image width mismatch");
        if (dim2 != headerData.numY)
            throw new DeviceException("Error when retrieving image from camera: Image height mismatch");

        if (rank == 2) {
            imageData = Array.newInstance(type.getJavaClassWrapper(), dim2, dim1);
            Object[][] imageData2D = (Object[][]) imageData;

            IntStream.range(0, dim1).parallel().forEach(x ->
                IntStream.range(0, dim2).forEach(y ->
                    imageData2D[y][x] = asObject(imageNode.get(x).get(y), type)
                )
            );

        } else {
            dim3 = imageNode.get(0).get(0).size();
            imageData = Array.newInstance(type.getJavaClassWrapper(), dim3, dim2, dim1);
            Object[][][] imageData3D = (Object[][][]) imageData;

            int dimThree = dim3; // must be final for lambda
            IntStream.range(0, dim1).parallel().forEach(x ->
                IntStream.range(0, dim2).forEach(y ->
                    IntStream.range(0, dimThree).forEach(z ->
                        imageData3D[z][y][x] = asObject(imageNode.get(y).get(x).get(z), type)
                    )
                )
            );
        }

        try {
            Header header = new Header();
            header.setSimple(true);

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

            imageData = unwrap(imageData, rank, type);      
            return (ImageHDU) FitsFactory.hduFactory(header, ImageHDU.encapsulate(imageData));

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
        this.execute("connected", params);
    }

    @Override
    public void disconnect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        this.execute("connected", params);
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

    /**
     * Unwraps a 2D/3D array of primitive wrappers into a 2D/3D array of the corresponding primitives.
     * @param array The array to unwrap
     * @param rank The rank of the array/number of dimensions, must be 2 or 3
     * @param dataType The data type of the array
     * @return The unwrapped array. Note that this is a new array, not a reference to the original.
     */
    private static Object unwrap(Object array, int rank, NumberVarType dataType) {
        if (rank != 2 && rank != 3)
            throw new IllegalArgumentException("Only 2D and 3D arrays are supported");
        if (rank == 2) {
            Object[][] array2D = (Object[][]) array;
            int width = array2D.length;
            int height = array2D[0].length;
            switch(dataType) {
                case BYTE:
                    byte[][] byteArray = new byte[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            byteArray[x][y] = ((Byte) array2D[x][y]).byteValue()
                        )
                    );
                    return byteArray;
                case INT16:
                    short[][] shortArray = new short[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            shortArray[x][y] = ((Short) array2D[x][y]).shortValue()
                        )
                    );
                    return shortArray;
                case UINT16:
                case INT32:
                    int[][] intArray = new int[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            intArray[x][y] = ((Integer) array2D[x][y]).intValue()
                        )
                    );
                    return intArray;
                case UINT32:
                case INT64:
                    long[][] longArray = new long[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            longArray[x][y] = ((Long) array2D[x][y]).longValue()
                        )
                    );
                    return longArray;
                case SINGLE:
                    float[][] floatArray = new float[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            floatArray[x][y] = ((Float) array2D[x][y]).floatValue()
                        )
                    );
                    return floatArray;
                case DOUBLE:
                    double[][] doubleArray = new double[width][height];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y ->
                            doubleArray[x][y] = ((Double) array2D[x][y]).doubleValue()
                        )
                    );
                    return doubleArray;
                case UINT64:
                    return array;   // BigInteger is not a primitive type
                default:
                    return null;
            }
        } else {
            Object[][][] array3D = (Object[][][]) array;
            int width = array3D.length;
            int height = array3D[0].length;
            int depth = array3D[0][0].length;
            
            switch(dataType) {
                case BYTE:
                    byte[][][] byteArray = new byte[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                byteArray[x][y][z] = ((Byte) array3D[x][y][z]).byteValue();
                            }
                        })
                    );
                    return byteArray;
                case INT16:
                    short[][][] shortArray = new short[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                shortArray[x][y][z] = ((Short) array3D[x][y][z]).shortValue();
                            }
                        })
                    );
                    return shortArray;
                case UINT16:
                case INT32:
                    int[][][] intArray = new int[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                intArray[x][y][z] = ((Integer) array3D[x][y][z]).intValue();
                            }
                        })
                    );
                    return intArray;
                case UINT32:
                case INT64:
                    long[][][] longArray = new long[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                longArray[x][y][z] = ((Long) array3D[x][y][z]).longValue();
                            }
                        })
                    );
                    return longArray;
                case SINGLE:
                    float[][][] floatArray = new float[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                floatArray[x][y][z] = ((Float) array3D[x][y][z]).floatValue();
                            }
                        })
                    );
                    return floatArray;
                case DOUBLE:
                    double[][][] doubleArray = new double[width][height][depth];
                    IntStream.range(0, width).parallel().forEach(x ->
                        IntStream.range(0, height).forEach(y -> {
                            for (int z = 0; z < depth; z++) {
                                doubleArray[x][y][z] = ((Double) array3D[x][y][z]).doubleValue();
                            }
                        })
                    );
                    return doubleArray;
                case UINT64:
                    return array;   // BigInteger is not a primitive type
                default:
                    return null;
            }
        }
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

    protected record HeaderData (
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
