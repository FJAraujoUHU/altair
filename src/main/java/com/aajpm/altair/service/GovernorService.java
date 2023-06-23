package com.aajpm.altair.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aajpm.altair.utility.solver.EphemeridesSolver;

import reactor.core.publisher.Mono;

@Service
public class GovernorService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    

    //#endregion
    ///////////////////// SUPPORTING SERVICES & COMPONENTS ////////////////////
    //#region Supporting Services & Components

    @Autowired
    private ObservatoryService observatoryService;

    @Autowired
    private EphemeridesSolver ephemeridesSolver;

    //#endregion
    ///////////////////////////// CONSTRUCTORS ////////////////////////////////
    //#region Constructors

    public GovernorService() {
        super();
    }

    //#endregion
    //////////////////////////////// GETTERS //////////////////////////////////
    //#region Getters

    protected Mono<Boolean> isDaylight() {
        return ephemeridesSolver.isVisible("Sun");
    }

    /*protected Mono<Boolean> isSafe() {
        ObservatoryService.State observatoryState = observatoryService.getState();
        boolean observatorySafe = 
                (observatoryState != ObservatoryService.State.DISABLED) &&
                (observatoryState != ObservatoryService.State.ERROR);

        Mono<Boolean> weatherSafe = observatoryService.getWeatherWatch()
                                    .isSafe().onErrorReturn(false);
        Mono<Boolean> daylightSafe = isDaylight()
                                    .map(daylight -> !daylight).onErrorReturn(false);

        return Mono.zip(weatherSafe, daylightSafe)
                    .map(tuple -> observatorySafe && tuple.getT1() && tuple.getT2())
                    .onErrorReturn(false);
    }*/



    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions



    //#endregion

    
}
