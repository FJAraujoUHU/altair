package com.aajpm.altair.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.ExposureParams;
import com.aajpm.altair.entity.ImageAttribute;
import com.aajpm.altair.entity.Program;
import com.aajpm.altair.entity.AstroObject.AstroType;
import com.aajpm.altair.security.account.*;
import com.aajpm.altair.service.AstroObjectService;
import com.aajpm.altair.service.ExposureParamsService;
import com.aajpm.altair.service.ImageAttributeService;
import com.aajpm.altair.service.ProgramService;

import jakarta.transaction.Transactional;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent>{

    private boolean alreadySetup = false;

    @Autowired
    private AltairUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AstroObjectService astroObjects;

    @Autowired
    private ProgramService programs;

    @Autowired
    private ExposureParamsService exposureParams;

    @Autowired
    private ImageAttributeService imageAttrs;

    @Autowired
    private PasswordEncoder encoder;

    private AltairSecurityConfig securityConfig;

    //@Autowired
    public SetupDataLoader(AltairSecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }


    // Makes sure that the database is populated with the right roles and
    // creates a default Admin account.
    @Override
    @Transactional
    @SuppressWarnings("java:S3776") // Duh, it's a small database
    public void onApplicationEvent(ContextRefreshedEvent event) {
        
        // This might get called several times, so it makes sure to only run once.
        if (alreadySetup)
            return;

        // Creates the roles if they don't exist.
        Role adminRole = createRoleIfNotFound("ADMIN");
        Role basicUserRole = createRoleIfNotFound("BASIC_USER");
        Role advUserRole = createRoleIfNotFound("ADVANCED_USER");

        // Creates the default admin account if it doesn't exist.
        AltairUser adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            adminUser = new AltairUser();
            adminUser.setUsername("admin");
            adminUser.setEnabled(true);
            // Please, change this default password before rolling out to production.
            adminUser.setPassword(encoder.encode(securityConfig.getDefaultPassword()));
            adminUser.addRole(adminRole);
            adminUser.addRole(advUserRole);
            adminUser.addRole(basicUserRole);
            userRepository.save(adminUser);
        }

        createAstroObjectsIfNotFound();
        createProgramsIfNotFound();
        createImageAttrIfNotFound();


    }

    @Transactional
    protected Role createRoleIfNotFound(String name) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            roleRepository.save(role);
        }
        return role;
    }

    // TODO: Find baseFocus for each object
    @Transactional
    @SuppressWarnings("java:S3776") // Duh, it's a small database
    private void createAstroObjectsIfNotFound() {
        AstroObject moon = astroObjects.findByName("Moon");
        if (moon == null) {
            moon = astroObjects.create();
            moon.setName("Moon");
            // Description from Copilot, sourced from Wikipedia.
            moon.setDescription("The Moon is an astronomical body orbiting Earth as its only natural satellite. It is the fifth-largest satellite in the Solar System, and by far the largest among planetary satellites relative to the size of the planet that it orbits (its primary). The Moon is, after Jupiter's satellite Io, the second-densest satellite in the Solar System among those whose densities are known.");
            moon.setMagnitude(-12.74);
            moon.setType(AstroType.MOON);
            moon.setRa(null);
            moon.setDec(null);
            moon = astroObjects.save(moon);
        }
        AstroObject mercury = astroObjects.findByName("Mercury");
        if (mercury == null) {
            mercury = astroObjects.create();
            mercury.setName("Mercury");
            // Description from Copilot, sourced from Wikipedia.
            mercury.setDescription("Mercury is the smallest and closest planet to the Sun in the Solar System. Its orbit around the Sun takes 87.97 Earth days, the shortest of all the Sun's planets. Mercury is one of four terrestrial planets in the Solar System, and is a rocky body like Earth.");
            mercury.setMagnitude(-2.2);
            mercury.setType(AstroType.PLANET);
            mercury.setRa(null);
            mercury.setDec(null);
            mercury = astroObjects.save(mercury);
        }

        AstroObject venus = astroObjects.findByName("Venus");
        if (venus == null) {
            venus = astroObjects.create();
            venus.setName("Venus");
            // Description from Copilot, sourced from Wikipedia.
            venus.setDescription("Venus is the second planet from the Sun. It is named after the Roman goddess of love and beauty. As the brightest natural object in Earth's night sky after the Moon, Venus can cast shadows and can be, on rare occasion, visible to the naked eye in broad daylight.");
            venus.setMagnitude(-4.6);
            venus.setType(AstroType.PLANET);
            venus.setRa(null);
            venus.setDec(null);
            venus = astroObjects.save(venus);
        }

        AstroObject mars = astroObjects.findByName("Mars");
        if (mars == null) {
            mars = astroObjects.create();
            mars.setName("Mars");
            // Description from Copilot, sourced from Wikipedia.
            mars.setDescription("Mars is the fourth planet from the Sun and the second-smallest planet in the Solar System, being larger than only Mercury. In English, Mars carries the name of the Roman god of war and is often referred to as the 'Red Planet'.");
            mars.setMagnitude(-2.3);
            mars.setType(AstroType.PLANET);
            mars.setRa(null);
            mars.setDec(null);
            mars = astroObjects.save(mars);
        }

        AstroObject jupiter = astroObjects.findByName("Jupiter");
        if (jupiter == null) {
            jupiter = astroObjects.create();
            jupiter.setName("Jupiter");
            // Description from Copilot, sourced from Wikipedia.
            jupiter.setDescription("Jupiter is the fifth planet from the Sun and the largest in the Solar System. It is a gas giant with a mass one-thousandth that of the Sun, but two-and-a-half times that of all the other planets in the Solar System combined.");
            jupiter.setMagnitude(-2.7);
            jupiter.setType(AstroType.PLANET);
            jupiter.setRa(null);
            jupiter.setDec(null);
            jupiter = astroObjects.save(jupiter);
        }

        AstroObject saturn = astroObjects.findByName("Saturn");
        if (saturn == null) {
            saturn = astroObjects.create();
            saturn.setName("Saturn");
            // Description from Copilot, sourced from Wikipedia.
            saturn.setDescription("Saturn is the sixth planet from the Sun and the second-largest in the Solar System, after Jupiter. It is a gas giant with an average radius of about nine times that of Earth. It only has one-eighth the average density of Earth; however, with its larger volume, Saturn is over 95 times more massive.");
            saturn.setMagnitude(-0.4);
            saturn.setType(AstroType.PLANET);
            saturn.setRa(null);
            saturn.setDec(null);
            saturn = astroObjects.save(saturn);
        }

        AstroObject uranus = astroObjects.findByName("Uranus");
        if (uranus == null) {
            uranus = astroObjects.create();
            uranus.setName("Uranus");
            // Description from Copilot, sourced from Wikipedia.
            uranus.setDescription("Uranus is the seventh planet from the Sun. Its name is a reference to the Greek god of the sky, Uranus, who, according to Greek mythology, was the grandfather of Zeus (Jupiter) and father of Cronus (Saturn). It has the third-largest planetary radius and fourth-largest planetary mass in the Solar System.");
            uranus.setMagnitude(5.7);
            uranus.setType(AstroType.PLANET);
            uranus.setRa(null);
            uranus.setDec(null);
            uranus = astroObjects.save(uranus);
        }

        AstroObject neptune = astroObjects.findByName("Neptune");
        if (neptune == null) {
            neptune = astroObjects.create();
            neptune.setName("Neptune");
            // Description from Copilot, sourced from Wikipedia.
            neptune.setDescription("Neptune is the eighth and farthest known planet from the Sun in the Solar System. In the Solar System, it is the fourth-largest planet by diameter, the third-most-massive planet, and the densest giant planet. It is 17 times the mass of Earth, slightly more massive than its near-twin Uranus.");
            neptune.setMagnitude(7.9);
            neptune.setType(AstroType.PLANET);
            neptune.setRa(null);
            neptune.setDec(null);
            neptune = astroObjects.save(neptune);
        }

        AstroObject pluto = astroObjects.findByName("Pluto");
        if (pluto == null) {
            pluto = astroObjects.create();
            pluto.setName("Pluto");
            // Description from Copilot, sourced from Wikipedia.
            pluto.setDescription("Pluto is a dwarf planet in the Kuiper belt, a ring of bodies beyond Neptune. It was the first Kuiper belt object to be discovered and is the largest known plutoid (or ice dwarf).");
            pluto.setMagnitude(14.1);
            pluto.setType(AstroType.PLANET);    // Sue me :P
            pluto.setRa(null);
            pluto.setDec(null);
            pluto = astroObjects.save(pluto);
        }

        AstroObject io = astroObjects.findByName("Io");
        if (io == null) {
            io = astroObjects.create();
            io.setName("Io");
            // Description from Copilot, sourced from Wikipedia.
            io.setDescription("Io is the innermost of the four Galilean moons of the planet Jupiter. It is the fourth-largest moon, has the highest density of all the moons, and has the least amount of water of any known astronomical object in the Solar System.");
            io.setMagnitude(5.0);
            io.setType(AstroType.MOON);
            io.setRa(null);
            io.setDec(null);
            io = astroObjects.save(io);
        }

        AstroObject europa = astroObjects.findByName("Europa");
        if (europa == null) {
            europa = astroObjects.create();
            europa.setName("Europa");
            // Description from Copilot, sourced from Wikipedia.
            europa.setDescription("Europa, is the smallest of the four Galilean moons orbiting Jupiter, and the sixth-closest to the planet of all the 79 known moons of Jupiter. It is also the sixth-largest moon in the Solar System.");
            europa.setMagnitude(5.3);
            europa.setType(AstroType.MOON);
            europa.setRa(null);
            europa.setDec(null);
            europa = astroObjects.save(europa);
        }

        AstroObject ganymede = astroObjects.findByName("Ganymede");
        if (ganymede == null) {
            ganymede = astroObjects.create();
            ganymede.setName("Ganymede");
            // Description from Copilot, sourced from Wikipedia.
            ganymede.setDescription("Ganymede is the largest and most massive moon of Jupiter and in the Solar System. The ninth-largest object in the Solar System, it is the largest without a substantial atmosphere. It has a diameter of 5,268 km (3,273 mi) and is 8% larger than the planet Mercury, although only 45% as massive.");
            ganymede.setMagnitude(4.6);
            ganymede.setType(AstroType.MOON);
            ganymede.setRa(null);
            ganymede.setDec(null);
            ganymede = astroObjects.save(ganymede);
        }

        AstroObject callisto = astroObjects.findByName("Callisto");
        if (callisto == null) {
            callisto = astroObjects.create();
            callisto.setName("Callisto");
            // Description from Copilot, sourced from Wikipedia.
            callisto.setDescription("Callisto is the second-largest moon of Jupiter, after Ganymede. It is the third-largest moon in the Solar System after Ganymede and Saturn's largest moon Titan, and the largest object in the Solar System not to be properly differentiated.");
            callisto.setMagnitude(5.7);
            callisto.setType(AstroType.MOON);
            callisto.setRa(null);
            callisto.setDec(null);
            callisto = astroObjects.save(callisto);
        }

        AstroObject titan = astroObjects.findByName("Titan");
        if (titan == null) {
            titan = astroObjects.create();
            titan.setName("Titan");
            // Description from Copilot, sourced from Wikipedia.
            titan.setDescription("Titan is the largest moon of Saturn and the second-largest natural satellite in the Solar System. It is the only moon known to have a dense atmosphere, and the only known body in space, other than Earth, where clear evidence of stable bodies of surface liquid has been found.");
            titan.setMagnitude(8.3);
            titan.setType(AstroType.MOON);
            titan.setRa(null);
            titan.setDec(null);
            titan = astroObjects.save(titan);
        }


        AstroObject ceres = astroObjects.findByName("Ceres");
        if (ceres == null) {
            ceres = astroObjects.create();
            ceres.setName("Ceres");
            // Description from Copilot, sourced from Wikipedia.
            ceres.setDescription("Ceres is the largest object in the asteroid belt that lies between the orbits of Mars and Jupiter, slightly closer to Mars' orbit. Its diameter is approximately 945 kilometers (587 miles), making it the largest of the minor planets within the orbit of Neptune.");
            ceres.setMagnitude(6.3);
            ceres.setType(AstroType.SMALL_BODY);
            ceres.setRa(null);
            ceres.setDec(null);
            ceres = astroObjects.save(ceres);
        }

        AstroObject vesta = astroObjects.findByName("Vesta");
        if (vesta == null) {
            vesta = astroObjects.create();
            vesta.setName("Vesta");
            // Description from Copilot, sourced from Wikipedia.
            vesta.setDescription("Vesta, minor-planet designation 4 Vesta, is one of the largest objects in the asteroid belt, with a mean diameter of 525 kilometres (326 mi). It was discovered by the German astronomer Heinrich Wilhelm Olbers on 29 March 1807 and is named after Vesta, the virgin goddess of home and hearth from Roman mythology.");
            vesta.setMagnitude(5.2);
            vesta.setType(AstroType.SMALL_BODY);
            vesta.setRa(null);
            vesta.setDec(null);
            vesta = astroObjects.save(vesta);
        }


        AstroObject iss = astroObjects.findByName("International Space Station");
        if (iss == null) {
            iss = astroObjects.create();
            iss.setName("International Space Station");
            // Description from Copilot, sourced from Wikipedia.
            iss.setDescription("The International Space Station (ISS) is a space station, or a habitable artificial satellite, in low Earth orbit. Its first component was launched into orbit in 1998, with the first long-term residents arriving in November 2000.");
            iss.setMagnitude(-6.0);
            iss.setType(AstroType.SMALL_BODY);
            iss.setRa(null);
            iss.setDec(null);
            iss = astroObjects.save(iss);
        }

        // Adding the Horizons easter egg
        AstroObject tesla = astroObjects.findByName("Starman");
        if (tesla == null) {
            tesla = astroObjects.create();
            tesla.setName("Starman");
            // Description from Copilot, sourced from Wikipedia.
            tesla.setDescription("Starman is a mannequin dressed in a spacesuit, occupying the driver's seat of a 2008 Tesla Roadster that was launched into space on February 6, 2018, by SpaceX's Falcon Heavy rocket on a test flight.");
            tesla.setMagnitude(20.0);
            tesla.setType(AstroType.SMALL_BODY);
            tesla.setRa(null);
            tesla.setDec(null);
            tesla = astroObjects.save(tesla);
        }
    }

    @Transactional
    @SuppressWarnings("java:S3776") // Duh, it's a small database
    private void createProgramsIfNotFound() {
        Program fastMoon = programs.findByName("Moon - Luminance");
        if (fastMoon == null) {
            fastMoon = programs.create();
            fastMoon.setName("Moon - Luminance");
            fastMoon.setDescription("Fast program for capturing the moon in luminance.");
            fastMoon.setEnabled(true);
            AstroObject target = astroObjects.findByName("Moon");
            fastMoon.setTarget(target);

            // Create the Exposures
            ExposureParams params = exposureParams.create();
            params.setLightFrame(true);
            params.setExposureTime(30.0);
            params.setFilter("Luminance");
            params.setBinX(1);
            params.setBinY(1);
            params.setSubFrameX(null);
            params.setSubFrameY(null);
            params.setSubFrameWidth(null);
            params.setSubFrameHeight(null);

            params = fastMoon.addExposure(params);
            fastMoon = programs.save(fastMoon);
        }
    }
    
    @Transactional
    @SuppressWarnings("java:S3776") // Duh, it's a small database
    private void createImageAttrIfNotFound() {
        // Camera
        ImageAttribute expStart = imageAttrs.findByKeyword("DATE-OBS");
        if (expStart == null) {
            expStart = imageAttrs.create();
            expStart.setFitsKeyword("DATE-OBS");
            expStart.setName("Exposure Start Date");
            expStart.setUnit("UTC");
            expStart = imageAttrs.save(expStart);
        }

        ImageAttribute expTime = imageAttrs.findByKeyword("EXPTIME");
        if (expTime == null) {
            expTime = imageAttrs.create();
            expTime.setFitsKeyword("EXPTIME");
            expTime.setName("Exposure Time");
            expTime.setUnit("s");
            expTime = imageAttrs.save(expTime);
        }

        ImageAttribute width = imageAttrs.findByKeyword("NAXIS1");
        if (width == null) {
            width = imageAttrs.create();
            width.setFitsKeyword("NAXIS1");
            width.setName("Image Width");
            width.setUnit("px");
            width = imageAttrs.save(width);
        }

        ImageAttribute height = imageAttrs.findByKeyword("NAXIS2");
        if (height == null) {
            height = imageAttrs.create();
            height.setFitsKeyword("NAXIS2");
            height.setName("Image Height");
            height.setUnit("px");
            height = imageAttrs.save(height);
        }

        ImageAttribute gain = imageAttrs.findByKeyword("GAIN");
        if (gain == null) {
            gain = imageAttrs.create();
            gain.setFitsKeyword("GAIN");
            gain.setName("Gain");
            gain = imageAttrs.save(gain);
        }

        ImageAttribute sensorTemp = imageAttrs.findByKeyword("CCD-TEMP");
        if (sensorTemp == null) {
            sensorTemp = imageAttrs.create();
            sensorTemp.setFitsKeyword("CCD-TEMP");
            sensorTemp.setName("Sensor Temperature");
            sensorTemp.setUnit("°C");
            sensorTemp = imageAttrs.save(sensorTemp);
        }

        ImageAttribute sensorTargetTemp = imageAttrs.findByKeyword("SET-TEMP");
        if (sensorTargetTemp == null) {
            sensorTargetTemp = imageAttrs.create();
            sensorTargetTemp.setFitsKeyword("SET-TEMP");
            sensorTargetTemp.setName("Sensor Target Temperature");
            sensorTargetTemp.setUnit("°C");
            sensorTargetTemp = imageAttrs.save(sensorTargetTemp);
        }

        ImageAttribute xbinning = imageAttrs.findByKeyword("XBINNING");
        if (xbinning == null) {
            xbinning = imageAttrs.create();
            xbinning.setFitsKeyword("XBINNING");
            xbinning.setName("X Binning");
            xbinning.setUnit("px");
            xbinning = imageAttrs.save(xbinning);
        }

        ImageAttribute ybinning = imageAttrs.findByKeyword("YBINNING");
        if (ybinning == null) {
            ybinning = imageAttrs.create();
            ybinning.setFitsKeyword("YBINNING");
            ybinning.setName("Y Binning");
            ybinning.setUnit("px");
            ybinning = imageAttrs.save(ybinning);
        }

        ImageAttribute bayerPattern = imageAttrs.findByKeyword("BAYERPAT");
        if (bayerPattern == null) {
            bayerPattern = imageAttrs.create();
            bayerPattern.setFitsKeyword("BAYERPAT");
            bayerPattern.setName("Bayer pattern");
            bayerPattern = imageAttrs.save(bayerPattern);
        }

        ImageAttribute patternOffsetX = imageAttrs.findByKeyword("XBAYROFF");
        if (patternOffsetX == null) {
            patternOffsetX = imageAttrs.create();
            patternOffsetX.setFitsKeyword("XBAYROFF");
            patternOffsetX.setName("Bayer pattern offset X");
            patternOffsetX.setUnit("px");
            patternOffsetX = imageAttrs.save(patternOffsetX);
        }

        ImageAttribute patternOffsetY = imageAttrs.findByKeyword("YBAYROFF");
        if (patternOffsetY == null) {
            patternOffsetY = imageAttrs.create();
            patternOffsetY.setFitsKeyword("YBAYROFF");
            patternOffsetY.setName("Bayer pattern offset Y");
            patternOffsetY.setUnit("px");
            patternOffsetY = imageAttrs.save(patternOffsetY);
        }

        ImageAttribute imgType = imageAttrs.findByKeyword("IMAGETYP");
        if (imgType == null) {
            imgType = imageAttrs.create();
            imgType.setFitsKeyword("IMAGETYP");
            imgType.setName("Image Type");
            imgType = imageAttrs.save(imgType);
        }

        // Telescope
        ImageAttribute alt = imageAttrs.findByKeyword("OBJCTALT");
        if (alt == null) {
            alt = imageAttrs.create();
            alt.setFitsKeyword("OBJCTALT");
            alt.setName("Altitude");
            alt.setUnit("°");
            alt = imageAttrs.save(alt);
        }

        ImageAttribute az = imageAttrs.findByKeyword("OBJCTAZ");
        if (az == null) {
            az = imageAttrs.create();
            az.setFitsKeyword("OBJCTAZ");
            az.setName("Azimuth");
            az.setUnit("°");
            az = imageAttrs.save(az);
        }

        ImageAttribute tracking = imageAttrs.findByKeyword("TRACKING");
        if (tracking == null) {
            tracking = imageAttrs.create();
            tracking.setFitsKeyword("TRACKING");
            tracking.setName("Telescope tracking?");
            tracking = imageAttrs.save(tracking);
        }

        // Dome
        ImageAttribute domeAz = imageAttrs.findByKeyword("DMAZ");
        if (domeAz == null) {
            domeAz = imageAttrs.create();
            domeAz.setFitsKeyword("DMAZ");
            domeAz.setName("Dome azimuth");
            domeAz.setUnit("°");
            domeAz = imageAttrs.save(domeAz);
        }

        ImageAttribute domeAlt = imageAttrs.findByKeyword("DMOPEN");
        if (domeAlt == null) {
            domeAlt = imageAttrs.create();
            domeAlt.setFitsKeyword("DMOPEN");
            domeAlt.setName("Shutter open");
            domeAlt.setUnit("%");
            domeAlt = imageAttrs.save(domeAlt);
        }

        ImageAttribute domeSlaved = imageAttrs.findByKeyword("DMSLAVED");
        if (domeSlaved == null) {
            domeSlaved = imageAttrs.create();
            domeSlaved.setFitsKeyword("DMSLAVED");
            domeSlaved.setName("Dome slaved?");
            domeSlaved = imageAttrs.save(domeSlaved);
        }

        // Focuser
        ImageAttribute fcStep = imageAttrs.findByKeyword("FOCUSPOS");
        if (fcStep == null) {
            fcStep = imageAttrs.create();
            fcStep.setFitsKeyword("FOCUSPOS");
            fcStep.setName("Focus position");
            fcStep.setUnit("steps");
            fcStep = imageAttrs.save(fcStep);
        }

        ImageAttribute fcTemp = imageAttrs.findByKeyword("FOCUSTEM");
        if (fcTemp == null) {
            fcTemp = imageAttrs.create();
            fcTemp.setFitsKeyword("FOCUSTEM");
            fcTemp.setName("Focus temperature");
            fcTemp.setUnit("°C");
            fcTemp = imageAttrs.save(fcTemp);
        }

        ImageAttribute fcPos = imageAttrs.findByKeyword("FOCUSTCP");
        if (fcPos == null) {
            fcPos = imageAttrs.create();
            fcPos.setFitsKeyword("FOCUSTCP");
            fcPos.setName("Focus Temperature Compensation?");
            fcPos = imageAttrs.save(fcPos);
        }

        // Filter wheel
        ImageAttribute filter = imageAttrs.findByKeyword("FILTER");
        if (filter == null) {
            filter = imageAttrs.create();
            filter.setFitsKeyword("FILTER");
            filter.setName("Filter");
            filter = imageAttrs.save(filter);
        }

        ImageAttribute filterOff = imageAttrs.findByKeyword("FILTEROFF");
        if (filterOff == null) {
            filterOff = imageAttrs.create();
            filterOff.setFitsKeyword("FILTEROFF");
            filterOff.setName("Filter focus offset");
            filterOff.setUnit("steps");
            filterOff = imageAttrs.save(filterOff);
        }

        // Weather
        ImageAttribute wwSafe = imageAttrs.findByKeyword("WWISSAFE");
        if (wwSafe == null) {
            wwSafe = imageAttrs.create();
            wwSafe.setFitsKeyword("WWISSAFE");
            wwSafe.setName("Weather safe?");
            wwSafe = imageAttrs.save(wwSafe);
        }

        ImageAttribute cloudCover = imageAttrs.findByKeyword("WWCLOUDS");
        if (cloudCover == null) {
            cloudCover = imageAttrs.create();
            cloudCover.setFitsKeyword("WWCLOUDS");
            cloudCover.setName("Cloud cover (Basic)");
            cloudCover = imageAttrs.save(cloudCover);
        }

        ImageAttribute cloudCoverPct = imageAttrs.findByKeyword("AOCCLOUD");
        if (cloudCoverPct == null) {
            cloudCoverPct = imageAttrs.create();
            cloudCoverPct.setFitsKeyword("AOCCLOUD");
            cloudCoverPct.setName("Cloud cover");
            cloudCoverPct.setUnit("%");
            cloudCoverPct = imageAttrs.save(cloudCoverPct);
        }

        ImageAttribute humidity = imageAttrs.findByKeyword("WWHUMID");
        if (humidity == null) {
            humidity = imageAttrs.create();
            humidity.setFitsKeyword("WWHUMID");
            humidity.setName("Humidity (Basic)");
            humidity = imageAttrs.save(humidity);
        }

        ImageAttribute humidityPct = imageAttrs.findByKeyword("AOCHUM");
        if (humidityPct == null) {
            humidityPct = imageAttrs.create();
            humidityPct.setFitsKeyword("AOCHUM");
            humidityPct.setName("Humidity");
            humidityPct.setUnit("%");
            humidityPct = imageAttrs.save(humidityPct);
        }

        ImageAttribute pressure = imageAttrs.findByKeyword("WWBAROM");
        if (pressure == null) {
            pressure = imageAttrs.create();
            pressure.setFitsKeyword("WWBAROM");
            pressure.setName("Pressure (Basic)");
            pressure = imageAttrs.save(pressure);
        }

        ImageAttribute pressureHpa = imageAttrs.findByKeyword("AOCBAROM");
        if (pressureHpa == null) {
            pressureHpa = imageAttrs.create();
            pressureHpa.setFitsKeyword("AOCBAROM");
            pressureHpa.setName("Pressure");
            pressureHpa.setUnit("hPa");
            pressureHpa = imageAttrs.save(pressureHpa);
        }

        ImageAttribute skyTemp = imageAttrs.findByKeyword("WWSKYT");
        if (skyTemp == null) {
            skyTemp = imageAttrs.create();
            skyTemp.setFitsKeyword("WWSKYT");
            skyTemp.setName("Sky temperature (Basic)");
            skyTemp = imageAttrs.save(skyTemp);
        }

        ImageAttribute skyTempC = imageAttrs.findByKeyword("AOCSKYT");
        if (skyTempC == null) {
            skyTempC = imageAttrs.create();
            skyTempC.setFitsKeyword("AOCSKYT");
            skyTempC.setName("Sky temperature");
            skyTempC.setUnit("°C");
            skyTempC = imageAttrs.save(skyTempC);
        }

        ImageAttribute ambTemp = imageAttrs.findByKeyword("WWAMBT");
        if (ambTemp == null) {
            ambTemp = imageAttrs.create();
            ambTemp.setFitsKeyword("WWAMBT");
            ambTemp.setName("Ambient temperature (Basic)");
            ambTemp = imageAttrs.save(ambTemp);
        }

        ImageAttribute ambTempC = imageAttrs.findByKeyword("AOCAMBT");
        if (ambTempC == null) {
            ambTempC = imageAttrs.create();
            ambTempC.setFitsKeyword("AOCAMBT");
            ambTempC.setName("Ambient temperature");
            ambTempC.setUnit("°C");
            ambTempC = imageAttrs.save(ambTempC);
        }

        ImageAttribute rain = imageAttrs.findByKeyword("WWRAIN");
        if (rain == null) {
            rain = imageAttrs.create();
            rain.setFitsKeyword("WWRAIN");
            rain.setName("Rain (Basic)");
            rain = imageAttrs.save(rain);
        }

        ImageAttribute rainMm = imageAttrs.findByKeyword("AOCRAIN");
        if (rainMm == null) {
            rainMm = imageAttrs.create();
            rainMm.setFitsKeyword("AOCRAIN");
            rainMm.setName("Rain");
            rainMm.setUnit("mm/h");
            rainMm = imageAttrs.save(rainMm);
        }

        ImageAttribute windSpeed = imageAttrs.findByKeyword("WWWIND");
        if (windSpeed == null) {
            windSpeed = imageAttrs.create();
            windSpeed.setFitsKeyword("WWWIND");
            windSpeed.setName("Wind speed (Basic)");
            windSpeed = imageAttrs.save(windSpeed);
        }

        ImageAttribute windSpeedMS = imageAttrs.findByKeyword("AOCWIND");
        if (windSpeedMS == null) {
            windSpeedMS = imageAttrs.create();
            windSpeedMS.setFitsKeyword("AOCWIND");
            windSpeedMS.setName("Wind speed");
            windSpeedMS.setUnit("m/s");
            windSpeedMS = imageAttrs.save(windSpeedMS);
        }

        ImageAttribute windDir = imageAttrs.findByKeyword("WWWINDD");
        if (windDir == null) {
            windDir = imageAttrs.create();
            windDir.setFitsKeyword("WWWINDD");
            windDir.setName("Wind direction (Basic)");
            windDir = imageAttrs.save(windDir);
        }

        ImageAttribute windDirDeg = imageAttrs.findByKeyword("AOCWINDD");
        if (windDirDeg == null) {
            windDirDeg = imageAttrs.create();
            windDirDeg.setFitsKeyword("AOCWINDD");
            windDirDeg.setName("Wind direction");
            windDirDeg.setUnit("°");
            windDirDeg = imageAttrs.save(windDirDeg);
        }

        ImageAttribute windGust = imageAttrs.findByKeyword("WWWINDG");
        if (windGust == null) {
            windGust = imageAttrs.create();
            windGust.setFitsKeyword("WWWINDG");
            windGust.setName("Wind gust (Basic)");
            windGust = imageAttrs.save(windGust);
        }

        ImageAttribute windGustMS = imageAttrs.findByKeyword("AOCWINDG");
        if (windGustMS == null) {
            windGustMS = imageAttrs.create();
            windGustMS.setFitsKeyword("AOCWINDG");
            windGustMS.setName("Wind gust");
            windGustMS.setUnit("m/s");
            windGustMS = imageAttrs.save(windGustMS);
        }

        ImageAttribute skyQual = imageAttrs.findByKeyword("WWSKYQU");
        if (skyQual == null) {
            skyQual = imageAttrs.create();
            skyQual.setFitsKeyword("WWSKYQU");
            skyQual.setName("Sky quality (Basic)");
            skyQual = imageAttrs.save(skyQual);
        }

        ImageAttribute skyQualMag = imageAttrs.findByKeyword("AOCSKYQU");
        if (skyQualMag == null) {
            skyQualMag = imageAttrs.create();
            skyQualMag.setFitsKeyword("AOCSKYQU");
            skyQualMag.setName("Sky quality");
            skyQualMag.setUnit("mag/arcsec²");
            skyQualMag = imageAttrs.save(skyQualMag);
        }

        ImageAttribute skyBright = imageAttrs.findByKeyword("WWSKYBR");
        if (skyBright == null) {
            skyBright = imageAttrs.create();
            skyBright.setFitsKeyword("WWSKYBR");
            skyBright.setName("Sky brightness (Basic)");
            skyBright = imageAttrs.save(skyBright);
        }

        ImageAttribute skyBrightLux = imageAttrs.findByKeyword("AOCSKYBR");
        if (skyBrightLux == null) {
            skyBrightLux = imageAttrs.create();
            skyBrightLux.setFitsKeyword("AOCSKYBR");
            skyBrightLux.setName("Sky brightness");
            skyBrightLux.setUnit("lux");
            skyBrightLux = imageAttrs.save(skyBrightLux);
        }
    }
}
