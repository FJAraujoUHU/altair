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
    ///////////////////////////////// VALUES //////////////////////////////////
    //#region Values

    public enum States {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private States state = States.PENDING;


    //#region Getters & Setters
    public Boolean isCompleted() {
        return state == States.COMPLETED;
    }

    public States getState() {
        return state;
    }

    public void setState(States status) {
        this.state = status;
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
