package com.aajpm.altair.entity;

import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@Table(name = "exposure_orders")
public class ExposureOrder extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    public ExposureOrder() {
        super();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Column(name = "completed", nullable = false)
    private Boolean completed;


    //#region Getters & Setters
    public Boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ProgramOrder program;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ExposureParams exposureParams;

    //#region Getters & Setters
    public ProgramOrder getProgram() {
        return program;
    }

    public void setProgram(ProgramOrder program) {
        this.program = program;
    }

    public ExposureParams getExposureParams() {
        return exposureParams;
    }

    public void setExposureParams(ExposureParams exposureParams) {
        this.exposureParams = exposureParams;
    }
    //#endregion

    //#endregion
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods



    //#endregion
    
}
