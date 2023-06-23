package com.aajpm.altair.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ExposureParams;
import com.aajpm.altair.repository.ExposureParamsRepository;
import com.aajpm.altair.service.observatory.FilterWheelService;

import reactor.core.publisher.Mono;

@Service
@Transactional
public class ExposureParamsService extends BasicEntityCRUDService<ExposureParams> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ExposureParamsRepository exposureParamsRepository;

    @Override
    protected ExposureParamsRepository getManagedRepository() {
        return exposureParamsRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    @Autowired
    private FilterWheelService filterWheel;

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ExposureParamsService() {
        super();
    }

    @Override
    public ExposureParams create() {
        return new ExposureParams();
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public ExposureParams save(ExposureParams params) {
        Assert.notNull(params, "The entity cannot be null.");

        Assert.notNull(params.isLightFrame(), "useLightFrame cannot be null.");
        Assert.notNull(params.getExposureTime(), "Exposure time cannot be null.");
        Assert.isTrue(params.getExposureTime() >= 0, "Exposure time cannot be negative");
        Assert.notNull(params.getProgram(), "ExposureParams must be assigned to a program");
        
        // Check if params.getFilter() is present in observatory.getFilterWheel().getFilters()
        Assert.notNull(params.getFilter(), "ExposureParams must be assigned to a filter");
        
        Mono<Boolean> filterFound = filterWheel
                                    .getFilterNames()
                                    .map(filters ->  filters
                                        .stream()
                                        .anyMatch(name -> name
                                            .equalsIgnoreCase(params.getFilter()))
                                    );

        // TODO: Check if this line breaks reactive flow
        boolean isFound = filterFound.blockOptional(Duration.ofSeconds(3)).orElse(false);

        Assert.isTrue(isFound, "Filter not found in the filter wheel");

        // Return the entity
        return super.save(params);
    }

    @Override
    public ExposureParams update(ExposureParams params) {
        Assert.notNull(params, "The entity cannot be null.");

        Assert.notNull(params.isLightFrame(), "useLightFrame cannot be null.");
        Assert.notNull(params.getExposureTime(), "Exposure time cannot be null.");
        Assert.isTrue(params.getExposureTime() >= 0, "Exposure time cannot be negative");
        Assert.notNull(params.getProgram(), "ExposureParams must be assigned to a program");
        
        // Check if params.getFilter() is present in observatory.getFilterWheel().getFilters()
        Assert.notNull(params.getFilter(), "ExposureParams must be assigned to a filter");
        
        Mono<Boolean> filterFound = filterWheel
                                    .getFilterNames()
                                    .map(filters ->  filters
                                        .stream()
                                        .anyMatch(name -> name
                                            .equalsIgnoreCase(params.getFilter()))
                                    );

        // TODO: Check if this line breaks reactive flow
        boolean isFound = filterFound.blockOptional(Duration.ofSeconds(3)).orElse(false);

        Assert.isTrue(isFound, "Filter not found in the filter wheel");

        // Return the entity
        return super.update(params);
    }

    ////////////////////////////////// METHODS /////////////////////////////////
    //#region METHODS

    

    //#endregion
    
}
