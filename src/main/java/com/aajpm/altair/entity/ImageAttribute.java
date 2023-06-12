package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.Collection;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "image_attributes")
public class ImageAttribute extends BasicEntity implements Serializable {
    
    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @NotBlank
    @Column(name = "name", unique = true, nullable = false)
    private String name;


    //#region Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @OneToMany(mappedBy = "attribute", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Collection<ImageValue> values;


    //#region Getters & Setters
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
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods



    //#endregion
}
