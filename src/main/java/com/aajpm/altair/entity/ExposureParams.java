package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.Collection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "exposure_params")
public class ExposureParams extends BasicEntity implements Serializable {
    
    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @NotNull
    @Column(name = "use_light_frame", nullable = false)
    private Boolean useLightFrame;

    @Positive
    @NotNull
    @Column(name = "use_dark_frame", nullable = false)
    private Double exposureTime;

    @NotNull
    @Column(name = "filter", nullable = false)
    private String filter;

    //#region Getters & Setters
    public Boolean getUseLightFrame() {
        return useLightFrame;
    }

    public void setUseLightFrame(Boolean useLightFrame) {
        this.useLightFrame = useLightFrame;
    }

    public Double getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(Double exposureTime) {
        this.exposureTime = exposureTime;
    }

    public String getFilter(){
        return filter;
    }

    public void setFilter(String filter){
        this.filter = filter;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Program program;

    @OneToMany(mappedBy = "exposureParams", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<ExposureOrder> exposureOrders;


    //#region Getters & Setters
    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Collection<ExposureOrder> getExposureOrders() {
        return exposureOrders;
    }

    public void setExposureOrders(Collection<ExposureOrder> exposureOrders) {
        this.exposureOrders = exposureOrders;
    }

    public void addExposureOrder(ExposureOrder exposureOrder) {
        this.exposureOrders.add(exposureOrder);
    }

    public void removeExposureOrder(ExposureOrder exposureOrder) {
        this.exposureOrders.remove(exposureOrder);
    }
    //#endregion

    //#endregion
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods



    //#endregion
}
