package com.aajpm.altair.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "image_attributes")
public class ImageAttribute extends BasicEntity implements Serializable {
    
    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    public ImageAttribute() {
        super();
        values = new HashSet<>();
    }
    
    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @NotBlank
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    // If this attribute is used as a FITS keyword, this is the keyword name.
    // Set to null if not used as a FITS keyword. Used when creating an AstroImage.
    @Column(name = "fits_keyword", unique = true, nullable = true)
    private String fitsKeyword;


    //#region Getters & Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFitsKeyword() {
        return fitsKeyword;
    }

    public void setFitsKeyword(String fitsKeyword) {
        this.fitsKeyword = fitsKeyword;
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
