package com.aajpm.altair.security.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Role findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name = :authority OR r.name = CONCAT('ROLE_', :authority)")
    Role findByAuthority(String authority);

}
