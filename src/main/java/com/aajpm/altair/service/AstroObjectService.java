package com.aajpm.altair.service;

import java.time.Instant;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.AstroObject.AstroType;
import com.aajpm.altair.repository.AstroObjectRepository;
import com.aajpm.altair.utility.Interval;
import com.aajpm.altair.utility.solver.EphemeridesSolver;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class AstroObjectService extends BasicEntityCRUDService<AstroObject> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private AstroObjectRepository astroObjectRepository;

    @Override
    protected AstroObjectRepository getManagedRepository() {
        return astroObjectRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    @Autowired
    private EphemeridesSolver solver;

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public AstroObjectService() {
        super();
    }

    @Override
    public AstroObject create() {
        return new AstroObject();
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public AstroObject save(AstroObject object) {
        Assert.notNull(object, "The entity cannot be null.");

        Assert.hasText(object.getName(), "The name of the entity cannot be null.");
        Assert.notNull(object.getType(), "The type of the entity cannot be null.");

        if (shouldHaveRaDec(object)) {
            Assert.notNull(object.getRa(), "The right ascension of the entity cannot be null.");
            Assert.notNull(object.getDec(), "The declination of the entity cannot be null.");
        }

        return super.save(object);
    }

    @Override
    public AstroObject update(AstroObject object) {
        Assert.notNull(object, "The entity cannot be null.");

        Assert.hasText(object.getName(), "The name of the entity cannot be null.");
        Assert.notNull(object.getType(), "The type of the entity cannot be null.");

        if (shouldHaveRaDec(object)) {
            Assert.notNull(object.getRa(), "The right ascension of the entity cannot be null.");
            Assert.notNull(object.getDec(), "The declination of the entity cannot be null.");
        }

        return super.update(object);
    }

    /**
     * Checks if the given {@link AstroObject} should have a right ascension
     * and a declination.
     * <p>
     * If the type of the object is not a small body, a moon, a planet, or Sol,
     * then it should have a right ascension and a declination.
     * 
     * @param object The {@link AstroObject} to be checked.
     * 
     * @return {@code true} if the given {@link AstroObject} should have a
     *         right ascension and a declination.
     */
    private boolean shouldHaveRaDec(AstroObject object) {
        AstroType type = object.getType();
        boolean isSmallBody = type == AstroType.SMALL_BODY;
        boolean isMoon = type == AstroType.MOON;
        boolean isPlanet = type == AstroType.PLANET;

        return isSmallBody || isMoon || isPlanet || object.isSol();
    }


    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Finds the {@link AstroObject} with the given name.
     * 
     * @param name The name of the object to be queried.
     * 
     * @return The {@link AstroObject} with the given name.
     */
    public AstroObject findByName(String name) {
        AstroObject object = astroObjectRepository.findByName(name);

        // To check for transaction integrity
        Assert.notNull(object, "The query for astro object with name " + name + " returned null.");

        return object;
    }

    /**
     * Returns all {@link AstroObject} of the given {@link AstroType}.
     * 
     * @param type The type of the objects to be queried.
     * 
     * @return A {@link Collection} of {@link AstroObject} of the given type.
     */
    public Collection<AstroObject> findByType(AstroType type) {
        Collection<AstroObject> objects = astroObjectRepository.findByType(type.name());

        Assert.notNull(objects, "The query for astro objects with type " + type + " returned null.");

        return objects;
    }

    /**
     * Returns the rise and set times of the given object in the given interval.
     * 
     * @param object The object to be queried.
     * @param searchInterval The interval in which to search for the rise and set times.
     * 
     * @return A {@link Mono} containing the closest rise and set times of the
     *         object, or if it is already visible, the current time and the
     *         setting time, or if it doesn't set, it returns {@code searchInterval},
     *         or a {@link Mono#error(Throwable)} if the body is not found or
     *         is out of bounds.
     */
    public Mono<Interval> getRiseSetTime(AstroObject object, Interval searchInterval) {
        Assert.notNull(object, "The object to be queried cannot be null.");
        Assert.hasText(object.getName(), "The name of the object to be queried cannot be null.");
        Assert.notNull(object.getType(), "The type of the object to be queried cannot be null.");

        Assert.isTrue(!object.isEarth(), "The Earth does not have a rise and set time.");

        if (object.isSol()) {
            return solver.getRiseSetTime("SOL", searchInterval, 0.0);
        }

        switch (object.getType()) {
            case SMALL_BODY:    // For solar system objects
            case MOON:
            case PLANET:
                return solver.getRiseSetTime(object.getName(), searchInterval);
            default:            // For outer space objects
                Assert.notNull(object.getRa(), "The object " + object.getName() + " does not have a valid Right Ascension.");
                Assert.notNull(object.getDec(), "The object " + object.getName() + " does not have a valid Declination.");
                return solver.getRiseSetTime(object.getRa(), object.getDec(), searchInterval);
        }
    }

    /**
     * Checks if the given {@link AstroObject} is visible at the given time.
     * 
     * @param object The object to be queried.
     * @param time The time at which to check the visibility.
     * 
     * @return A {@link Mono} containing {@code true} if the object is visible
     *         at the given time, {@code false} otherwise, or a
     *         {@link Mono#error(Throwable)} if the body is not found or is out
     *         of bounds.
     */
    public Mono<Boolean> isVisible(AstroObject object, Instant time) {
        Assert.notNull(object, "The object to be queried cannot be null.");
        Assert.hasText(object.getName(), "The name of the object to be queried cannot be null.");
        Assert.notNull(object.getType(), "The type of the object to be queried cannot be null.");

        if (object.isEarth()) { // Why are you even checking this?
            return Mono.just(true);
        }

        if (object.isSol()) {
            return solver.isVisible("SOL", time, 0.0);
        }

        switch (object.getType()) {
            case SMALL_BODY:    // For solar system objects
            case MOON:
            case PLANET:
                return solver.isVisible(object.getName(), time);
            default:            // For outer space objects
                Assert.notNull(object.getRa(), "The object " + object.getName() + " does not have a valid Right Ascension.");
                Assert.notNull(object.getDec(), "The object " + object.getName() + " does not have a valid Declination.");
                return solver.isVisible(object.getRa(), object.getDec(), time);
        }
    }

    /**
     * Checks if the given {@link AstroObject} is visible during the given
     * interval.
     * 
     * @param object The object to be queried.
     * @param interval The interval during which to check the visibility.
     * 
     * @return A {@link Mono} containing {@code true} if the object is visible
     *         during the given interval, {@code false} otherwise, or a
     *         {@link Mono#error(Throwable)} if the body is not found or is out
     *         of bounds.
     */
    public Mono<Boolean> isVisible(AstroObject object, Interval interval) {
        Assert.notNull(object, "The object to be queried cannot be null.");
        
        Mono<Boolean> isDaylight = solver
                                .getRiseSetTime("Sol", interval, 0.0)
                                .map(interval::equals);
        
        if (object.isSol())
            return isDaylight;

        if (object.isEarth()) // Why are you even checking this?
            return Mono.just(true);
        
        return getRiseSetTime(object, interval).flatMap(times -> {
            // To be visible in an interval, getRiseSetTime must return the interval itself
            if (!times.equals(interval)) {
                return Mono.just(false);
            }

            // Now check if it is daylight
            return isDaylight.map(daylight -> !daylight);
        });
    }
    
    /**
     * Returns all {@link AstroObject} that are visible during the given
     * interval.
     * 
     * @param interval The interval during which to check the visibility.
     * 
     * @return A {@link Flux} containing all {@link AstroObject} that are
     *         visible during the given interval.
     */
    public Flux<AstroObject> getVisibleObjects(Interval interval) {
        return Flux.fromIterable(astroObjectRepository.findAll())
                .filterWhen(object -> isVisible(object, interval));
    }


    //#endregion
}
