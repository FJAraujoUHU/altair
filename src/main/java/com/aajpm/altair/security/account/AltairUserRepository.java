package com.aajpm.altair.security.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AltairUserRepository extends JpaRepository<AltairUser, Long> {

    AltairUser findByUsername(String username);

    // Find users with failed login attempts whose last login attempt was more than the specified timeout ago
    @Query("SELECT u FROM AltairUser u WHERE u.failedLoginAttempts > 0 AND u.lastLoginAttempt < :timeout")
    List<AltairUser> findTimeoutDoneUsers(@Param("timeout") LocalDateTime timeout);

    // Find users with a certain role
    @Query("SELECT u FROM AltairUser u JOIN u.roles r WHERE r = :role")
    List<AltairUser> findUsersWithRole(@Param("role") Role role);
    @Query("SELECT u FROM AltairUser u JOIN u.roles r WHERE r.name = :role")
    List<AltairUser> findUsersWithRole(@Param("role") String role);
}
