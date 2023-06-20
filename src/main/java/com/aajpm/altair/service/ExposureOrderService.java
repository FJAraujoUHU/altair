package com.aajpm.altair.service;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ExposureOrder;
import com.aajpm.altair.entity.ExposureParams;
import com.aajpm.altair.entity.ProgramOrder;
import com.aajpm.altair.repository.ExposureOrderRepository;
import com.aajpm.altair.security.account.AltairUser;

@Service
@Transactional
public class ExposureOrderService extends BasicEntityCRUDService<ExposureOrder> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ExposureOrderRepository exposureOrderRepository;

    @Override
    protected ExposureOrderRepository getManagedRepository() {
        return exposureOrderRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ExposureOrderService() {
        super();
    }

    @Override
    public ExposureOrder create() {
        return new ExposureOrder();
    }

    public ExposureOrder create(ProgramOrder program, ExposureParams params) {
        ExposureOrder order = create();
        order.setProgram(program);
        order.setExposureParams(params);
        return order;
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public ExposureOrder save(ExposureOrder order) {
        Assert.notNull(order, "The order cannot be null.");
        Assert.notNull(order.getProgram(), "The ProgramOrder cannot be null.");
        Assert.notNull(order.getExposureParams(), "The ExposureParams cannot be null.");

        return super.save(order);
    }

    @Override
    public ExposureOrder update(ExposureOrder order) {
        Assert.notNull(order, "The order cannot be null.");
        Assert.notNull(order.getProgram(), "The ProgramOrder cannot be null.");
        Assert.notNull(order.getExposureParams(), "The ExposureParams cannot be null.");

        return super.update(order);
    }


    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    public Collection<ExposureOrder> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");

        return exposureOrderRepository.findByProgramUserId(user.getId());
    }

    public Collection<ExposureOrder> findNotCompleted() {
        return exposureOrderRepository.findByCompletedFalse();
    }

    //#endregion
    
}
