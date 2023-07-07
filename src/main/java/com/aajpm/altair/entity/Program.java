package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "programs")
public class Program extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public Program() {
        super();
        exposures = new ArrayList<>();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes
    
    @Column(name = "name", unique = true, nullable = false)
    @NotBlank
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;


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

    public Boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(Boolean enabled){
        this.enabled = enabled;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @OneToMany(mappedBy = "program", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ExposureParams> exposures;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private AstroObject target;


    //#region Getters & Setters
    public List<ExposureParams> getExposures() {
        return exposures;
    }

    public void setExposures(List<ExposureParams> exposures) {
        this.exposures = exposures;
    }

    public ExposureParams addExposure(ExposureParams exposure) {
        exposure.setProgram(this);
        exposures.add(exposure);
        return exposure;
    }

    public ExposureParams removeExposure(ExposureParams exposure) {
        exposure.setProgram(null);
        exposures.remove(exposure);
        return exposure;
    }

    public AstroObject getTarget() {
        return target;
    }

    public void setTarget(AstroObject target) {
        this.target = target;
    }
    //#endregion

    //#endregion
    /////////////////////////////// METHODS ///////////////////////////////////
    //#region Methods



    //#endregion

}
