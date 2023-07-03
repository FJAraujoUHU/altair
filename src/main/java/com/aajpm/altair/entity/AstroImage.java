package com.aajpm.altair.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class AstroImage extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    public AstroImage() {
        super();
        values = new HashSet<>();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Column(name = "file_name", unique = true, nullable = false)
    private String fileName;

    @Column(name = "date", nullable = false)
    private Instant creationDate;


    //#region Getters & Setters
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant date) {
        this.creationDate = date;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private ControlOrder controlOrder;

    @OneToOne(fetch = FetchType.EAGER, optional = true, mappedBy = "image")
    private ExposureOrder exposureOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private AstroObject target;

    @OneToMany(mappedBy = "image", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<ImageValue> values;

    //#region Getters & Setters
    public ControlOrder getControlOrder() {
        return controlOrder;
    }

    public void setControlOrder(ControlOrder controlOrder) {
        this.controlOrder = controlOrder;
    }

    public ExposureOrder getExposureOrder() {
        return exposureOrder;
    }

    public AstroObject getTarget() {
        return target;
    }

    public void setTarget(AstroObject target) {
        this.target = target;
    }

    public void setExposureOrder(ExposureOrder exposureOrder) {
        this.exposureOrder = exposureOrder;
    }

    public Collection<ImageValue> getValues() {
        return values;
    }

    public void setValues(Collection<ImageValue> values) {
        this.values = values;
    }

    public void addValue(ImageValue value) {
        this.values.add(value);
    }

    public void removeValue(ImageValue value) {
        this.values.remove(value);
    }
    //#endregion

    //#endregion
    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    // None

    //#endregion
}
