package com.aajpm.altair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aajpm.altair.utility.solver.EphemeridesSolver;
import com.aajpm.altair.utility.solver.HorizonsEphemeridesSolver;

@Configuration
@ConfigurationProperties(prefix = "altair.astrometrics")
public class AstrometricsConfig {

    ///////////////////////////////// FIELDS //////////////////////////////////
    //#region Fields

    // Latitude of the observatory, in degrees, North positive
    private double siteLatitude = 0.0;

    // Longitude of the observatory, in degrees, East positive
    private double siteLongitude = 0.0;

    // Elevation of the observatory, in meters
    private double siteElevation = 0.0;

    // Horizon line, in degrees, below which the object is considered to be below the horizon
    private double horizonLine = 0.0;


    //#region Getters/Setters
    public double getSiteLatitude() {
        return siteLatitude;
    }

    public void setSiteLatitude(double siteLatitude) {
        this.siteLatitude = siteLatitude;
    }

    public double getSiteLongitude() {
        return siteLongitude;
    }

    public void setSiteLongitude(double siteLongitude) {
        this.siteLongitude = siteLongitude;
    }

    public double getSiteElevation() {
        return siteElevation;
    }

    public void setSiteElevation(double siteElevation) {
        this.siteElevation = siteElevation;
    }

    public double getHorizonLine() {
        return horizonLine;
    }

    public void setHorizonLine(double horizonLine) {
        this.horizonLine = horizonLine;
    }
    //#endregion

    //#endregion
    ////////////////////////////////// BEANS //////////////////////////////////

    @Bean
    public EphemeridesSolver ephemeridesSolver() {
        return new HorizonsEphemeridesSolver(this);
    }
    
}
