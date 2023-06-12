package com.aajpm.altair.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

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

    @Column(name = "description")
    private String description;

    @Column(name = "rightAscension", nullable = true)
    private Double ra;

    @Column(name = "declination", nullable = true)
    private Double dec;

    @Column(name = "magnitude", nullable = true)
    private Double magnitude;

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



    //#endregion

}
