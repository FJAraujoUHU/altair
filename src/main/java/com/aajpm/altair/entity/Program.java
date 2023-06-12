package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "programs")
public class Program extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public Program() {
        super();
        exposures = new HashSet<>();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes
    
    @Column(name = "name", unique = true, nullable = false)
    @NotBlank
    private String name;

    @Column(name = "description")
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
    private Collection<ExposureParams> exposures;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private AstroObject target;


    //#region Getters & Setters
    public Collection<ExposureParams> getExposures() {
        return exposures;
    }

    public void setExposures(Collection<ExposureParams> exposures) {
        this.exposures = exposures;
    }

    public void addExposure(ExposureParams exposure) {
        exposures.add(exposure);
    }

    public void removeExposure(ExposureParams exposure) {
        exposures.remove(exposure);
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
