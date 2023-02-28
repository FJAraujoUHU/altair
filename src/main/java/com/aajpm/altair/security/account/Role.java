package com.aajpm.altair.security.account;

import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;

    private static final String ROLE_PREFIX = "ROLE_";

    // JPA requires a no-arg constructor
    protected Role() {
    }

    public Role(String name) {
        this.name = name.startsWith(ROLE_PREFIX) ? name.substring(ROLE_PREFIX.length()) : name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.startsWith(ROLE_PREFIX) ? name.substring(ROLE_PREFIX.length()) : name;
    }

    //GrantedAuthority interface requires this method
    @Override
    public String getAuthority() {
        return name.startsWith(ROLE_PREFIX) ? name : ROLE_PREFIX + name;
    }

}
