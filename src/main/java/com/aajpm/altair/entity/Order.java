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

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @Column(name = "completed", nullable = false)
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

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
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

    

    //#endregion
    
}
