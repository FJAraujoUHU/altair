package com.aajpm.altair.entity;

import java.util.Collection;
import java.util.HashSet;

import jakarta.persistence.*;

@Entity
@Table(name = "program_orders")
@DiscriminatorValue("program")
public class ProgramOrder extends Order {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public ProgramOrder() {
        super();
        exposureOrders = new HashSet<>();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes



    //#endregion
    //////////////////////////// RELATIONSHIPS ////////////////////////////////
    //#region Relationships

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Program program;

    @OneToMany(mappedBy = "program", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

    public void addExpousureOrder(ExposureOrder exposureOrder) {
        exposureOrders.add(exposureOrder);
    }

    public void removeExpousureOrder(ExposureOrder exposureOrder) {
        exposureOrders.remove(exposureOrder);
    }
    //#endregion

    //#endregion
    ////////////////////////////// METHODS ////////////////////////////////////
    //#region Methods

    // None
    
    //#endregion

}
