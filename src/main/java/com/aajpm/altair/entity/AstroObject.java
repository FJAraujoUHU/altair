package com.aajpm.altair.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "objects")
public class AstroObject extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    public AstroObject() {
        super();
    }

    //#endregion
    ///////////////////////////////// VALUES //////////////////////////////////
    //#region Values

    public enum AstroType {
        SMALL_BODY,
        MOON,
        PLANET,
        STAR,
        CONSTELLATION,
        CLUSTER,
        NEBULA,
        GALAXY,
        OTHER
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "rightAscension", nullable = true)
    private Double ra;

    @Column(name = "declination", nullable = true)
    private Double dec;

    @Column(name = "magnitude", nullable = true)
    private Double magnitude;

    @PositiveOrZero
    @Column(name = "baseFocus", nullable = true)
    private Integer baseFocus;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AstroType type;

    //#region Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public Double getRa() {
        return ra;
    }

    public void setRa(Double ra) {
        this.ra = ra;
    }

    public Double getDec() {
        return dec;
    }

    public void setDec(Double dec) {
        this.dec = dec;
    }

    public Double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(Double magnitude) {
        this.magnitude = magnitude;
    }

    public Integer getBaseFocus() {
        return baseFocus;
    }

    public void setBaseFocus(int baseFocus) {
        this.baseFocus = baseFocus;
    }

    public AstroType getType() {
        return type;
    }

    public void setType(AstroType type) {
        this.type = type;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships



    //#region Getters & Setters

    //#endregion

    //#endregion
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods

    public boolean isSol() {
        boolean isStar = this.type != null && this.type.equals(AstroType.STAR);
        boolean isSun = this.name != null && (this.name.equalsIgnoreCase("Sun") || this.name.equalsIgnoreCase("Sol"));

        return isStar && isSun;
    }

    public boolean isLuna() {
        boolean isMoon = this.type != null && this.type.equals(AstroType.MOON);
        boolean isLuna = this.name != null && (this.name.equalsIgnoreCase("Moon") || this.name.equalsIgnoreCase("Luna"));

        return isMoon || isLuna;
    }

    public boolean isEarth() {
        boolean isPlanet = this.type != null && this.type.equals(AstroType.PLANET);
        boolean isEarth = this.name != null && (this.name.equalsIgnoreCase("Earth") || this.name.equalsIgnoreCase("Terra"));

        return isPlanet && isEarth;
    }


    /**
     * Checks if this {@link AstroObject} should have a right ascension
     * and a declination.
     * <p>
     * If the type of the object is not a small body, a moon, a planet, or Sol,
     * then it should have a right ascension and a declination.
     * 
     * @return {@code true} if it should have a right ascension and a declination.
     */
    public boolean shouldHaveRaDec() {
        boolean isSmallBody = type == AstroType.SMALL_BODY;
        boolean isMoon = type == AstroType.MOON;
        boolean isPlanet = type == AstroType.PLANET;

        return !(isSmallBody || isMoon || isPlanet || isSol());
    }


    //#endregion

}
