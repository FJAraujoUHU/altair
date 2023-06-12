package com.aajpm.altair.entity;

import java.io.Serializable;
import java.time.Instant;

import com.aajpm.altair.security.account.AltairUser;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "order_type")
public abstract class Order extends BasicEntity implements Serializable {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    protected Order() {
        super();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Column(name = "creation_time")
    private Instant creationTime;

    @Column(name = "is_completed")
    private Boolean completed;


    //#region Getters & Setters
    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant time) {
        this.creationTime = time;
    }

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

    @ManyToOne(fetch = FetchType.EAGER)
    private AltairUser user;


    //#region Getters & Setters
    public AltairUser getUser() {
        return user;
    }

    public void setUser(AltairUser user) {
        this.user = user;
    }
    //#endregion

    //#endregion
    //////////////////////////////// METHODS ///////////////////////////////////
    //#region Methods

    /**
     * Gets a time range this order is available to be run.
     * @return A Instant array of length 2, where the first element is the start time and the second element is the end time.
     */
    public abstract Instant[] getAvailableTime();

    //#endregion
    
}
