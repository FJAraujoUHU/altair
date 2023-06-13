package com.aajpm.altair.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aajpm.altair.entity.ProgramOrder;
import com.aajpm.altair.repository.ProgramOrderRepository;

@Service
@Transactional
public class ProgramOrderService extends BasicEntityCRUDService<ProgramOrder> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ProgramOrderRepository programOrderRepository;

    @Override
    protected ProgramOrderRepository getManagedRepository() {
        return programOrderRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ProgramOrderService() {
        super();
    }

    @Override
    public ProgramOrder create() {
        return new ProgramOrder();
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    // TODO: Check

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    // None

    //#endregion Methods
    
}
