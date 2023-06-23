package com.aajpm.altair.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aajpm.altair.config.AstrometricsConfig;
import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.Program;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.*;
import com.aajpm.altair.utility.solver.EphemeridesSolver;

import nom.tam.fits.Fits;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.FitsOutputStream;
import reactor.core.publisher.Mono;

import com.aajpm.altair.service.observatory.*;
import com.aajpm.altair.service.observatory.CameraService.CameraCapabilities;
import com.aajpm.altair.service.observatory.CameraService.CameraStatus;
import com.aajpm.altair.service.observatory.DomeService.DomeCapabilities;
import com.aajpm.altair.service.observatory.DomeService.DomeStatus;
import com.aajpm.altair.service.observatory.FilterWheelService.FilterWheelStatus;
import com.aajpm.altair.service.observatory.FocuserService.FocuserStatus;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeCapabilities;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeStatus;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchCapabilities;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchStatus;


@Service
public class ObservatoryService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected final ObservatoryConfig config;

    protected Program currentJob;

    protected AltairUser currentUser;

    //#endregion
    /////////////////////////// SUPPORTING SERVICES ////////////////////////////
    //#region Supporting services

    @Autowired
    private EphemeridesSolver ephemeridesSolver;

    @Autowired
    private TelescopeService telescope;

    @Autowired
    private DomeService dome;

    @Autowired
    private FocuserService focuser;

    @Autowired
    private FilterWheelService filterWheel;

    @Autowired
    private CameraService camera;

    @Autowired
    private WeatherWatchService weatherWatch;

    //#endregion
    ////////////////////////////// CONSTRUCTOR /////////////////////////////////
    //#region Constructor

    public ObservatoryService(ObservatoryConfig config) {
        this.config = config;
    }

    //#endregion
    //////////////////////////////// GETTERS //////////////////////////////////
    //#region Getters

    /**
     * Gets the current configuration of the observatory.
     * @return The current configuration of the observatory.
     */
    public ObservatoryConfig getConfig() { return config; }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Void> connectAll() {
        return Mono.when(
            telescope.connect(),
            dome.connect(),
            focuser.connect(),
            filterWheel.connect(),
            camera.connect(),
            weatherWatch.connect()
        );
    }

    /**
     * Disconnects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Void> disconnectAll() {
        return Mono.when(
            telescope.disconnect(),
            dome.disconnect(),
            focuser.disconnect(),
            filterWheel.disconnect(),
            camera.disconnect(),
            weatherWatch.disconnect()
        );
    }
    
    /**
     * Starts the observatory and sets it up ready for use asynchronously.
     * It should unpark the telescope and dome, set them at their home positions,
     * and start chilling the camera.
     * 
     * Note: This method will not open the dome.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
     */
    public Mono<Void> start() throws DeviceUnavailableException {
        Mono<TelescopeCapabilities> telescopeCapabilities = telescope.getCapabilities();
        Mono<DomeCapabilities> domeCapabilities = dome.getCapabilities();
        Mono<CameraCapabilities> cameraCapabilities = camera.getCapabilities();

        return connectAll()
                .then(Mono
                    .zip(telescopeCapabilities, domeCapabilities, cameraCapabilities)
                    .flatMap(tuple ->
                        Mono.zip(
                            startTelescope(tuple.getT1()),
                            startDome(tuple.getT2()),
                            startCamera(tuple.getT3()))
                        .then()
                    )
                );
    }


    private Mono<Void> startTelescope(TelescopeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canUnpark())
            ret = telescope.unpark();

        if (capabilities.canTrack())
            ret = ret.then(telescope.setTracking(false));

        if (capabilities.canFindHome())
            ret = ret.then(telescope.findHome());        

        return ret;
    }

    private Mono<Void> startDome(DomeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canUnpark())
            ret = dome.unpark();

        if (capabilities.canSlave())
            ret = ret.then(dome.setSlaved(false));

        if (capabilities.canFindHome())
            ret = ret.then(dome.findHome());

        if (capabilities.canSlave())
            ret = ret.then(dome.setSlaved(true));
        
        return ret;
    }

    private Mono<Void> startCamera(CameraCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canSetCoolerTemp())
            ret = camera.setCooler(true).then(camera.cooldown());

        return ret;
    }

    /**
     * Starts the observatory and sets it up ready for use.
     * It should unpark the telescope and dome, set them at their home positions,
     * open the shutter and start chilling the camera.
     * 
     * This {@link Mono} will not complete until the observatory is ready for use.
     * 
     * Note: This method will not open the dome. 
     * 
     * @return A {@link Mono} that will complete when the observatory is ready for use.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
    */
    public Mono<Void> startAwait() throws DeviceUnavailableException {
        Mono<TelescopeCapabilities> telescopeCapabilities = telescope.getCapabilities();
        Mono<DomeCapabilities> domeCapabilities = dome.getCapabilities();
        Mono<CameraCapabilities> cameraCapabilities = camera.getCapabilities();


        return connectAll()
                .then(Mono
                    .zip(telescopeCapabilities, domeCapabilities, cameraCapabilities)
                    .flatMap(tuple ->
                        Mono.zip(
                            startTelescopeAwait(tuple.getT1()),
                            startDomeAwait(tuple.getT2()),
                            startCamera(tuple.getT3()))
                        .then()
                    )
                );
    }


    private Mono<Void> startTelescopeAwait(TelescopeCapabilities capabilities) {

        Mono<Void> ret = Mono.empty();

        if (capabilities.canUnpark())
            ret = telescope.unparkAwait();

        if (capabilities.canTrack())
            ret = ret.then(telescope.setTracking(false));

        if (capabilities.canFindHome())
            ret = ret.then(telescope.findHomeAwait());        

        return ret;
    }

    private Mono<Void> startDomeAwait(DomeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canUnpark())
            ret = dome.unparkAwait();

        if (capabilities.canSlave())
            ret = ret.then(dome.setSlaved(false));

        if (capabilities.canFindHome())
            ret = ret.then(dome.findHomeAwait());

        if (capabilities.canSlave())
            ret = ret.then(dome.setSlaved(true));

        return ret;
    }

    /**
     * Stops the observatory and puts it into a safe state.
     * It should close the shutter, park the telescope and dome, and turn off
     * the camera cooler.
     * 
     * Note: This method will not disconnect the devices.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
     */
    public Mono<Void> stop() throws DeviceUnavailableException {
        Mono<TelescopeCapabilities> telescopeCapabilities = telescope.getCapabilities();
        Mono<DomeCapabilities> domeCapabilities = dome.getCapabilities();
        Mono<CameraCapabilities> cameraCapabilities = camera.getCapabilities();

        return connectAll()
                .then(Mono
                    .zip(telescopeCapabilities, domeCapabilities, cameraCapabilities)
                    .flatMap(tuple ->
                        Mono.zip(
                            stopTelescope(tuple.getT1()),
                            stopDome(tuple.getT2()),
                            stopCamera(tuple.getT3()))
                        .then()
                    )
                );
    }


    private Mono<Void> stopTelescope(TelescopeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canTrack())
            ret = telescope.setTracking(false);

        if (capabilities.canSlew())
            ret = ret.then(telescope.abortSlew());

        if (capabilities.canPark())
            ret = ret.then(telescope.park());    

        return ret;
    }

    private Mono<Void> stopDome(DomeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canSlave())
            ret = dome.setSlaved(false);
        
        ret = ret.then(dome.halt());

        if (capabilities.canShutter())
            ret = ret.then(dome.closeShutter());

        if (capabilities.canPark())
            ret = ret.then(dome.park());

        return ret;
    }

    private Mono<Void> stopCamera(CameraCapabilities capabilities) {
        return capabilities.canSetCoolerTemp() ? camera.warmup() : Mono.empty();
    }

    /**
     * Stops the observatory and puts it into a safe state.
     * It should close the shutter, park the telescope and dome, warmup the
     * camera and turn off the camera cooler.
     * 
     * Note: This method will not disconnect the devices.
     * 
     * @return A {@link Mono} that will complete when the observatory is in a safe state.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible. 
     */
    public Mono<Void> stopAwait() throws DeviceUnavailableException {
        Mono<TelescopeCapabilities> telescopeCapabilities = telescope.getCapabilities();
        Mono<DomeCapabilities> domeCapabilities = dome.getCapabilities();
        Mono<CameraCapabilities> cameraCapabilities = camera.getCapabilities();

        return connectAll()
                .then(Mono
                    .zip(telescopeCapabilities, domeCapabilities, cameraCapabilities)
                    .flatMap(tuple ->
                        Mono.zip(
                            stopTelescopeAwait(tuple.getT1()),
                            stopDomeAwait(tuple.getT2()),
                            stopCameraAwait(tuple.getT3()))
                        .then()
                    )
                );
    }


    private Mono<Void> stopTelescopeAwait(TelescopeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canTrack())
            ret = telescope.setTracking(false);

        if (capabilities.canSlew())
            ret = ret.then(telescope.abortSlew());

        if (capabilities.canPark())
            ret = ret.then(telescope.parkAwait());    

        return ret;
    }

    private Mono<Void> stopDomeAwait(DomeCapabilities capabilities) {
        Mono<Void> ret = Mono.empty();

        if (capabilities.canSlave())
            ret = dome.setSlaved(false);

        ret = ret.then(dome.halt());

        if (capabilities.canShutter() && capabilities.canPark()) {
            ret = ret.then(Mono.zip(dome.closeShutterAwait(),dome.parkAwait()).then());
        } else {
            if (capabilities.canShutter())
                ret = ret.then(dome.closeShutterAwait());

            if (capabilities.canPark())
                ret = ret.then(dome.parkAwait());
        }
        
        return ret;
    }

    private Mono<Void> stopCameraAwait(CameraCapabilities capabilities) {
        if (!capabilities.canSetCoolerTemp()) Mono.empty();

        return camera.warmupAwait()
                .then(camera.setCooler(false));
    }



    //#region Image processing

    /**
     * Gets the latest image from the camera, adding all available metadata
     * from the other devices from the observatory to its header.
     * 
     * @param target If the image is of a target, the target object to add to
     *               the header.
     * 
     * @return A {@link Mono} that will emit the image as a {@link ImageHDU}
     *         when it is available.
     */
    public Mono<ImageHDU> getImage(AstroObject target) {
        Mono<ImageHDU> image = camera.getImage();
        Mono<ObservatoryStatus> statuses = getStatus();
        Mono<WeatherWatchCapabilities> weatherWatchCapabilities = weatherWatch.getCapabilities();
        
        Mono<String> telescopeName = telescope.getCapabilities().map(TelescopeCapabilities::name).onErrorReturn("Unknown");

        AstrometricsConfig metrics = ephemeridesSolver.getConfig();
        double siteLatitude = metrics.getSiteLatitude();
        double siteLongitude = metrics.getSiteLongitude();
        double siteElevation = metrics.getSiteElevation();

        return Mono.zip(image, statuses, telescopeName, weatherWatchCapabilities).map(tuple -> {
            ImageHDU img = tuple.getT1();
            TelescopeStatus telescopeStatus = tuple.getT2().telescope();
            DomeStatus domeStatus = tuple.getT2().dome();
            FocuserStatus focuserStatus = tuple.getT2().focuser();
            FilterWheelStatus filterWheelStatus = tuple.getT2().filterWheel();
            WeatherWatchStatus weatherStatus = tuple.getT2().weatherWatch();
            WeatherWatchCapabilities weatherCapabilities = tuple.getT4();

            String telescopeNameStr = tuple.getT3();

            if (target != null) {
                addValueIfValid(img, "OBJECT", target.getName(), "Name of the object being imaged");
                if (target.shouldHaveRaDec()) {
                    addValueIfValid(img, "OBJCTRA", target.getRa(), "Right Ascension of the object, in decimal hours");
                    addValueIfValid(img, "OBJCTDEC", target.getDec(), "Declination of the object, in decimal degrees");
                }
            }

            addValueIfValid(img, "TELESCOP", telescopeNameStr, "Name of the telescope");
            addValueIfValid(img, "LATITUDE", siteLatitude, "Latitude of the observatory, in decimal degrees");
            addValueIfValid(img, "LONGITUD", siteLongitude, "Longitude of the observatory, in decimal degrees");
            addValueIfValid(img, "ELEVATIO", siteElevation, "Elevation of the observatory, in meters");

            addTelescopeValues(img, telescopeStatus, (target == null || !target.shouldHaveRaDec()));
            addDomeValues(img, domeStatus);
            addFocuserValues(img, focuserStatus);
            addFilterWheelValues(img, filterWheelStatus);
            addWeatherWatchValues(img, weatherStatus, weatherCapabilities);

           return img; 
        });    
    }

    /**
     * Saves the latest exposure made by the camera to the image store.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage() {
        return getImage(null).flatMap(image -> {
            try {
                return Mono.just(saveImage(image));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the latest exposure made by the camera to the image store.
     * 
     * @param target The target object to add to the image's metadata. If null,
     *               no target information will be added.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target) {
        return getImage(target).flatMap(image -> {
            try {
                return Mono.just(saveImage(image));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the given image to the image store.
     * 
     * @param image The image to save.
     * 
     * @return The path to the saved image.
     * 
     * @throws IOException If there was a problem saving the image.
     */
    public Path saveImage(ImageHDU image) throws IOException {
        return saveImage(image, null, false);
    }

    /**
     * Saves the latest exposure made by the camera to the image store, using
     * the given filename.
     * 
     * @param target The target object to add to the image's metadata. If null,
     *               no target information will be added.
     * @param filename The filename to save the image as. If null, the filename
     *                 will be generated from the image's metadata.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target, String filename) {
        return getImage(target).flatMap(image -> {
            try {
                return Mono.just(saveImage(image, filename, false));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the given image to the image store, using the given filename.
     * If the filename is null, the filename will be generated from the image's
     * metadata.
     * 
     * @param image The image to save.
     * @param filename The filename to save the image as. If null, the filename
     *                 will be generated from the image's metadata.
     * @param useCompression If true, the image will be compressed using gzip and
     *                       saved with a .gz extension.
     * 
     * @return The path to the saved image.
     * @throws IOException If there was an error saving the image.
     */
    @SuppressWarnings({"java:S3776", "java:S6541"})
    public Path saveImage(ImageHDU image, String filename, boolean useCompression) throws IOException {
        Path imageStore = config.getCamera().getImageStorePath();

        // Create necessary directories
        if (Files.notExists(imageStore)) {
            Files.createDirectories(imageStore);
        }

        if (filename == null) {
            String obsDate = image.getTrimmedString("DATE-OBS");
            if (obsDate == null) {
                obsDate = Instant.now().toString();
            }

            String objName = image.getTrimmedString("OBJECT");
            if (objName == null) {
                objName = "Unknown";
            }

            String filter = image.getTrimmedString("FILTER");
            if (filter == null) {
                filter = "";
            }

            String imgType = image.getTrimmedString("IMAGETYP");
            if (imgType == null) {
                imgType = "";
            } else {
                switch (imgType) {
                    case "Light Frame":
                        imgType = "Light";
                        break;
                    case "Dark Frame":
                        imgType = "Dark";
                        break;
                    case "Bias Frame":
                        imgType = "Bias";
                        break;
                    case "Flat Frame":
                        imgType = "Flat";
                        break;
                    case "Tricolor Image":
                        imgType = "RGB";
                        break;
                    default:
                        imgType = "";
                        break;
                }
            }

            String extension = useCompression ? ".fit.gz" : ".fit";

            filename = String.format("%s_%s_%s_%s%s", obsDate, objName, filter, imgType, extension);
        } else {
            // Add extension if necessary
            if (!(filename.toUpperCase().endsWith(".FIT") || filename.toUpperCase().endsWith(".FITS"))) {
                filename += useCompression ? ".fit.gz" : ".fit";
            }
        }

        filename = filename.replaceAll("[^a-zA-Z0-9\\._\\-]", "_");

        Path filepath = imageStore.resolve(filename);

        try (
            Fits fits = new Fits();
            FitsOutputStream out = useCompression ?
                new FitsOutputStream(new GZIPOutputStream(Files.newOutputStream(filepath))) :
                new FitsOutputStream(Files.newOutputStream(filepath))
        ) {
            fits.addHDU(image);
            fits.write(out);

            return filepath;

        } catch (Exception e) {
            throw new IOException("Error saving image", e);
        }
    }


    //#region header add value helpers
    protected void addValueIfValid(ImageHDU img, String key, String value, String comment) {
        if (value != null) {
            try {
                img.addValue(key, value, comment);
            } catch (HeaderCardException e) {
                // Ignore
            }
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, boolean value, String comment) {
        try {
            img.addValue(key, value, comment);
        } catch (HeaderCardException e) {
            // Ignore
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, int value, String comment) {
        try {
            img.addValue(key, value, comment);
        } catch (HeaderCardException e) {
            // Ignore
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, double value, String comment) {
        if (!Double.isNaN(value)) {
            try {
                img.addValue(key, value, comment);
            } catch (HeaderCardException e) {
                // Ignore
            }
        }
    }
    //#endregion

    //#region header add device values
    protected void addTelescopeValues(ImageHDU img, TelescopeStatus status, boolean addRaDec) {
        if (addRaDec) {
            addValueIfValid(img, "OBJCTRA", status.rightAscension(), "Right Ascension of the object, in decimal hours");
            addValueIfValid(img, "OBJCTDEC", status.declination(), "Declination of the object, in decimal degrees");
        }
        addValueIfValid(img, "OBJCTALT", status.altitude(), "Altitude of the object, in decimal degrees");
        addValueIfValid(img, "OBJCTAZ", status.azimuth(), "Azimuth of the object, in decimal degrees");
        addValueIfValid(img, "TRACKING", status.tracking(), "True if the telescope is tracking, false otherwise");
    }

    protected void addDomeValues(ImageHDU img, DomeStatus status) {
        addValueIfValid(img, "DMAZ", status.azimuth(), "Azimuth of the dome, in decimal degrees");
        addValueIfValid(img, "DMOPEN", status.shutter(), "Percentage of the dome shutter that is open");
        addValueIfValid(img, "DMSLAVED", status.slaved(), "True if the dome is slaved to the telescope, false otherwise");
    }

    protected void addFocuserValues(ImageHDU img, FocuserStatus status) {
        addValueIfValid(img, "FOCUSPOS", status.position(), "Position of the focuser, in steps");
        addValueIfValid(img, "FOCUSTEM", status.temperature(), "Temperature of the focuser, in degrees Celsius");
        addValueIfValid(img, "FOCUSTCP", status.tempComp(), "Temperature compensation of the focuser");
    }

    protected void addFilterWheelValues(ImageHDU img, FilterWheelStatus status) {
        addValueIfValid(img, "FILTER", status.curName(), "Name of selected filter");
        addValueIfValid(img, "FILTEROFF", status.curOffset(), "Focus offset of selected filter");
    }

    @SuppressWarnings("java:S3776")
    protected void addWeatherWatchValues(ImageHDU img, WeatherWatchStatus status, WeatherWatchCapabilities capabilities) {
        final int asString = WeatherWatchService.CAPABILITIES_GENERAL;
        final int asValue = WeatherWatchService.CAPABILITIES_SPECIFIC;

        addValueIfValid(img, "WWISSAFE",status.isSafe(), "True if the weather is safe, false otherwise");
        
        if (capabilities.canCloud() == asString)
            addValueIfValid(img, "WWCLOUDS", status.cloudCover(), "Cloud cover, either \"Clear\", \"Cloudy\" or \"Overcast\"");
        else if (capabilities.canCloud() == asValue)
            addValueIfValid(img, "AOCCLOUD", status.cloudCover(), "ASCOM Observatory Conditions - Cloud coverage in percent");

        if (capabilities.canHumidity() == asString)
            addValueIfValid(img, "WWHUMID", status.humidity(), "Humidity, either \"Dry\", \"Normal\" or \"Humid\"");
        else if (capabilities.canHumidity() == asValue)
            addValueIfValid(img, "AOCHUM", status.humidity(), "ASCOM Observatory Conditions - Humidity in percent");
        
        if (capabilities.canPressure() == asString)
            addValueIfValid(img, "WWBAROM", status.pressure(), "Pressure, either \"Low\", \"Normal\" or \"High\"");
        else if (capabilities.canPressure() == asValue)
            addValueIfValid(img, "AOCBAROM", status.pressure(), "ASCOM Observatory Conditions - Pressure in hPa");

        if (capabilities.canTemperature() == asString) {
            addValueIfValid(img, "WWSKYT", status.temperatureSky(), "Sky temperature (usually mesured using IR), either \"Hot\", \"Cold\" or  \"Normal\"");
            addValueIfValid(img, "WWAMBT", status.temperatureAmbient(), "Ambient temperature, either \"Hot\", \"Cold\" or \"Normal\"");          
        } else if (capabilities.canTemperature() == asValue) {
            addValueIfValid(img, "AOCSKYT", status.temperatureSky(), "ASCOM Observatory Conditions - Sky temperature in degrees C");
            addValueIfValid(img, "AOCTAMBT", status.temperatureAmbient(), "ASCOM Observatory Conditions - Ambient temperature in degrees C");
        }

        if (capabilities.canRain() == asString)
            addValueIfValid(img, "WWRAIN", status.rainRate(), "Rain, either \"Dry\", \"Wet\" or \"Rain\"");
        else if (capabilities.canRain() == asValue)
            addValueIfValid(img, "AOCRRAIN", status.rainRate(), "ASCOM Observatory Conditions - Rain rate in mm/hour");

        if (capabilities.canWind() == asString) {
            addValueIfValid(img, "WWWIND", status.windSpeed(), "Wind speed, either \"Calm\", \"Windy\" or \"Very windy\"");
            addValueIfValid(img, "WWWINDG", status.windGust(), "Wind gust");
            addValueIfValid(img, "WWWINDD", status.windDirection(), "Wind direction, using cardinal directions, or \"None\"");
        } else if (capabilities.canWind() == asValue) {
            addValueIfValid(img, "AOCWIND", status.windSpeed(), "ASCOM Observatory Conditions - Wind speed in m/s");
            addValueIfValid(img, "AOCWINDG", status.windGust(), "ASCOM Observatory Conditions - Wind gust in m/s");
            addValueIfValid(img, "AOCWINDD", status.windDirection(), "ASCOM Observatory Conditions - Wind direction in degrees");
        }

        if (capabilities.canSkyQuality() == asString)
            addValueIfValid(img, "WWSKYQU", status.skyQuality(), "Sky quality, either \"Good\", \"Normal\" or \"Poor\"");
        else if (capabilities.canSkyQuality() == asValue)
            addValueIfValid(img, "AOCSKYQU", status.skyQuality(), "ASCOM Observatory Conditions - Sky quality in magnitudes per square arcsecond");

        if (capabilities.canSkyBrightness() == asString)
            addValueIfValid(img, "WWSKYBR", status.skyBrightness(), "Sky brightness, either \"Dark\", \"Bright\" or \"Very bright\"");
        else if (capabilities.canSkyBrightness() == asValue)
            addValueIfValid(img, "AOCSKYBR", status.skyBrightness(), "ASCOM Observatory Conditions - Sky brightness in Lux");        
    }
    //#endregion

    //#endregion

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Gets the current status of the observatory.
     * @return The current status of the observatory.
     * @throws DeviceException If there was an error getting the status.
     */
    public Mono<ObservatoryStatus> getStatus() throws DeviceException {
        return Mono.zip(
            telescope.getStatus(),
            dome.getStatus(),
            focuser.getStatus(),
            camera.getStatus(),
            filterWheel.getStatus(),
            weatherWatch.getStatus()
        ).map(tuple -> new ObservatoryStatus(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), tuple.getT5(), tuple.getT6())); 
    }

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    // TODO: Move to Governor
    /**
     * Represents the status of the observatory.
     */
    // public enum State {
    //     /** An admin has disabled Altair. Same as OFF, but the scheduler should not start. */
    //     DISABLED,
    //     /** Telescope is off and device services are disconnected (should allow other apps to connect). */
    //     OFF,
    //     /** Telescope is parked and ready to be used. */
    //     PARKED,
    //     /** Telescope is managed by Altair, but not in use. */
    //     IDLE,
    //     /** Telescope is performing a {@link ProgramOrder}. */
    //     RUNNING_PROGRAM,
    //     /** Telescope is being used manually by an advanced user. */
    //     MANUAL,
    //     /** Telescope is being used manually by an admin. */
    //     ADMIN,
    //     /** Telescope is slewing or performing a job that shouldn't be interrupted. */
    //     BUSY,
    //     /** Telescope is in an error state and must be reset by an admin or physically fixed. */
    //     ERROR
    // }

    public record ObservatoryStatus(
        TelescopeStatus telescope,
        DomeStatus dome,
        FocuserStatus focuser,
        CameraStatus camera,
        FilterWheelStatus filterWheel,
        WeatherWatchStatus weatherWatch
    ) {}

    //#endregion

}
