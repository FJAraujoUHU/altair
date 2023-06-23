package com.aajpm.altair.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.BasicEntity;

/**
 * A service that provides basic CRUD operations for a given entity.
 * 
 * @param <T> The type of entity to provide CRUD operations for,
 *           must extend {@link BasicEntity}.
 * 
 * @see BasicEntity
 */
@Transactional
public abstract class BasicEntityCRUDService<T extends BasicEntity> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    protected abstract JpaRepository<T, Long> getManagedRepository();

    //////////////////////////////// BASIC CRUD ///////////////////////////////

    /**
     * Creates a new instance of the entity. This method is used just to create
     * a new instance of the entity, and should not be used to save the entity.
     * 
     * @return A new instance of the entity. This instance has not been saved.
     */
    public abstract T create();

    /**
     * Finds all entities. If no entities exist, it will return an empty
     * {@link List}.
     * 
     * @return A {@link List} of all entities.
     */
    public List<T> findAll() {
        List<T> entities = getManagedRepository().findAll();

        // To check for transaction integrity
        Assert.notNull(entities, "The query for all entities returned null.");

        return entities;
    }

    /**
     * Finds the entity with the given id. If the entity does not exist, it will
     * throw an {@link IllegalArgumentException}.
     * 
     * @param id The id of the entity to be queried.
     * 
     * @return The entity with the given id.
     */
    public T findById(long id) {
        Assert.isTrue(id != 0, "The id of the query [" + id + "] is not valid.");
        T entity = getManagedRepository().findById(id).orElse(null);

        // To check for transaction integrity
        Assert.notNull(entity, "The query for order with id " + id + " returned null.");

        return entity;
    }

    /**
     * Checks if an entity with the given id exists.
     * 
     * @param id The id of the entity to be queried.
     * 
     * @return {@code true} if the entity exists, {@code false} otherwise.
     */
    public boolean existsById(long id) {
        Assert.isTrue(id != 0, "The id of the query [" + id + "] is not valid.");
        return getManagedRepository().existsById(id);
    }

    /**
     * Saves the entity. If the entity has already been saved, it will throw an
     * {@link IllegalArgumentException}, must use {@link #update(BasicEntity)}
     * instead.
     * 
     * <p> Probably should be overriden to add extra checks to the entity before
     * saving it.
     * 
     * @param entity The entity to be saved.
     * 
     * @return The saved entity.
     */
    public T save(T entity) {
        Assert.notNull(entity, "The entity to save cannot be null.");
        Assert.isTrue(entity.getId() == 0, "This object has already been saved.");

        return getManagedRepository().save(entity);
    }

    /**
     * Updates the entity. If the entity has not been saved yet, it will throw an
     * {@link IllegalArgumentException}, must use {@link #save(BasicEntity)}
     * instead.
     * 
     * <p> Probably should be overriden to add extra checks to the entity before
     * saving it.
     * 
     * @param entity The entity to be updated.
     * 
     * @return The updated entity.
     */
    public T update(T entity) {
        Assert.notNull(entity, "The entity to update cannot be null.");
        Assert.isTrue(entity.getId() != 0, "This object has not been saved yet.");
        Assert.isTrue(getManagedRepository().existsById(entity.getId()), "This object does not exist.");

        return getManagedRepository().save(entity);
    }

    /**
     * Saves or updates the entity. If the entity has not been saved yet, it will
     * save it, otherwise it will update it.
     * 
     * @param entity The entity to be saved or updated.
     * 
     * @return The saved or updated entity.
     */
    public T saveOrUpdate(T entity) {
        Assert.notNull(entity, "The entity to save or update cannot be null.");

        if (entity.getId() == 0) {
            return save(entity);
        } else {
            return update(entity);
        }
    }

    /**
     * Deletes the entity. If the entity has not been saved yet, it will throw an
     * {@link IllegalArgumentException}.
     * 
     * @param entity The entity to be deleted.
     */
    public void delete(T entity) {
        Assert.notNull(entity, "The entity to delete cannot be null.");
        Assert.isTrue(entity.getId() != 0, "This object has not been saved yet.");
        Assert.isTrue(getManagedRepository().existsById(entity.getId()), "This object does not exist.");

        getManagedRepository().delete(entity);
    }
    
}
