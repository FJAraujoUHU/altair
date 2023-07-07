package com.aajpm.altair.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "program_orders")
@DiscriminatorValue("program")
public class ProgramOrder extends Order {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public ProgramOrder() {
        super();
        exposureOrders = new ArrayList<>();
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
    private List<ExposureOrder> exposureOrders;


    //#region Getters & Setters
    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public List<ExposureOrder> getExposureOrders() {
        return exposureOrders;
    }

    public void setExposureOrders(List<ExposureOrder> exposureOrders) {
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
