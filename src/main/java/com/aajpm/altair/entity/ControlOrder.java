package com.aajpm.altair.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.time.Duration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "control_orders")
@DiscriminatorValue("control")
public class ControlOrder extends Order	{

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public ControlOrder() {
        super();
        images = new HashSet<>();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Column(name = "requested_time", nullable = false)
    @NotNull
    private Instant requestedTime;

    @Column(name = "requested_duration", nullable = false)
    @NotNull
    private Duration requestedDuration;


    //#region Getters & Setters
    public Instant getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(Instant requestedTime) {
        this.requestedTime = requestedTime;
    }

    public Duration getRequestedDuration() {
        return requestedDuration;
    }

    public void setRequestedDuration(Duration requestedDuration) {
        this.requestedDuration = requestedDuration;
    }
    //#endregion

    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @OneToMany( mappedBy = "controlOrder",
                fetch = FetchType.EAGER,
                cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE,
                    CascadeType.DETACH,
                    CascadeType.REFRESH })
    private Collection<AstroImage> images;


    //#region Getters & Setters
    public Collection<AstroImage> getImages() {
        return images;
    }

    public void setImages(Collection<AstroImage> images) {
        this.images = images;
    }

    public void addImage(AstroImage image) {
        images.add(image);
    }

    public void removeImage(AstroImage image) {
        images.remove(image);
    }
    //#endregion

    //#endregion
    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods
    
    @Override
    public Instant[] getAvailableTime() {
        return new Instant[] {requestedTime, requestedTime.plus(requestedDuration)};
    }

    //#endregion

}
