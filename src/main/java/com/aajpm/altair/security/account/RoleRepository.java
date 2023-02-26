package com.aajpm.altair.security.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Role findByName(String name);
}
