package com.aajpm.altair.entity;

import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@Table(name = "image_values")
public class ImageValue extends BasicEntity implements Serializable {
    
    ////////////////////////////// CONSTRUCTORS ///////////////////////////////	
    //#region Constructors

    

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Column(name = "value", length = 512)
    private String value;


    //#region Getters & Setters
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private AstroImage image;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ImageAttribute attribute;


    //#region Getters & Setters
    public AstroImage getImage() {
        return image;
    }

    public void setImage(AstroImage image) {
        this.image = image;
    }

    public ImageAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(ImageAttribute attribute) {
        this.attribute = attribute;
    }
    //#endregion

    //#endregion
    /////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods



    //#endregion
}
