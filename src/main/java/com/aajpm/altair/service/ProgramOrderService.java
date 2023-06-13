package com.aajpm.altair.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.ProgramOrder;
import com.aajpm.altair.repository.ProgramOrderRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;

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

    @Autowired
    private AstroObjectService astroObjectService;

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ProgramOrderService() {
        super();
    }

    @Override
    public ProgramOrder create() {
        return new ProgramOrder();
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public ProgramOrder save(ProgramOrder order) {
        Assert.notNull(order, "The order cannot be null.");

        Assert.notNull(order.getProgram(), "Program cannot be null.");
        Assert.notEmpty(order.getExposureOrders(), "Must contain at least one exposure order.");

        return super.save(order);
    }

    @Override
    public ProgramOrder update(ProgramOrder order) {
        Assert.notNull(order, "The order cannot be null.");

        Assert.notNull(order.getProgram(), "Program cannot be null.");
        // No need to check for empty exposure orders, since some might have been removed.

        return super.update(order);
    }

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Finds all {@link ProgramOrder} made by the given user.
     * 
     * @param user The user to find orders for.
     * 
     * @return A {@link Collection} of orders made by the given user.
     */
    public Collection<ProgramOrder> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");

        return programOrderRepository.findByUserId(user.getId());
    }

    /**
     * Finds all {@link ProgramOrder} made by the current user.
     * 
     * @return A {@link Collection} of orders made by the current user.
     */
    public Collection<ProgramOrder> findByCurrentUser() {
        AltairUser currUser = AltairUserService.getCurrentUser();

        Assert.notNull(currUser, "There is no current user.");

        Collection<ProgramOrder> orders = findByUser(currUser);

        Assert.notNull(orders, "The query for all orders by user [" + currUser.getId() + "] returned null.");

        return orders;
    }

    /**
     * Finds all {@link ProgramOrder} that have not been completed.
     * 
     * @return A {@link List} of orders that have not been completed,
     *         ordered by creation time.
     */
    public List<ProgramOrder> findNotCompleted() {
        return programOrderRepository.findByCompletedFalseOrderByCreationTimeAsc();
    }

    /* TODO
    public List<ProgramOrder> findInRange(Instant startTime, Instant endTime) {
        List<ProgramOrder> pendingOrders = programOrderRepository.findByCompletedFalseOrderByCreationTimeAsc();

        pendingOrders.stream()
                    .filter(order -> {
                        AstroObject target = order.getProgram().getTarget();

                        astroObjectService.isVisible(target, null)

                        
                    })
        
    }*/

    //#endregion Methods
    
}
