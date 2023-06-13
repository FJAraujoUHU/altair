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
    private long id;

    @Version
    private long version;

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    //#endregion
    //////////////////////////////// METHODS ///////////////////////////////////
    //#region Methods

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + ", version=" + this.version +  "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof Long)
            return id == (Long) obj;
        if (getClass() != obj.getClass())
            return false;

        BasicEntity other = (BasicEntity) obj;
        return id == other.id;
    }

    //#endregion
    
}
