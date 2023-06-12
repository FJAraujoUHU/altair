package com.aajpm.altair.entity;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BasicEntity {

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    protected BasicEntity() {
        super();
    }

    //#endregion
    /////////////////////////////// ATTRIBUTES ////////////////////////////////
    //#region Attributes

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)    // To ensure uniqueness
    private Long id;

    @Version
    private Long version;

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    //#endregion
    //////////////////////////////// METHODS ///////////////////////////////////
    //#region Methods

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof Long)    // If comparing to a Long, suppose it's an id
            return id.equals(obj);
        if (!this.getClass().isInstance(obj))
            return false;

        BasicEntity other = (BasicEntity) obj;
        return id.equals(other.id);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + ", version=" + this.version +  "]";
    }

    //#endregion
    
}
