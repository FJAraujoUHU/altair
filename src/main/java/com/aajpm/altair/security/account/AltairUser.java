package com.aajpm.altair.security.account;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;



@Entity
@Table(name = "users")
public class AltairUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is mandatory")
    @Size(min = 3, max = 24, message = "Username must be between 3 and 24 characters")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Column(nullable = false, length = 60)
    private String password;

    // Spring Security
    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_login_attempt")
    private LocalDateTime lastLoginAttempt;

    @Column(name = "failed_login_attempts")
    @Min(value = 0, message = "Login attempts must be greater than or equal to 0")
    private int failedLoginAttempts;

    // Roles act as permissions since Spring hasn't ported RoleHierarchy yet and the
    // old method is deprecated. Change it if ever porting to Spring 6.X?
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>(2);


    // JPA requires a no-arg constructor
    public AltairUser() {}

    // Creates a new user with the given username and password, no roles and disabled
    public AltairUser(String username, String password) {
        this.username = username;
        this.password = password;
        this.enabled = false;
        this.failedLoginAttempts = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return failedLoginAttempts < 5;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return getEnabled();
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastLoginAttempt() {
        return lastLoginAttempt;
    }

    public void setLastLoginAttempt(LocalDateTime lastLoginAttempt) {
        this.lastLoginAttempt = lastLoginAttempt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles();
    }


    // generate equals() and hashCode() methods
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AltairUser that = (AltairUser) o;

        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;
        if (username != null ? !username.equals(that.username) : that.username != null)
            return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (enabled != that.enabled)
            return false;
        if (lastLoginAttempt != null ? !lastLoginAttempt.equals(that.lastLoginAttempt) : that.lastLoginAttempt != null)
            return false;
        if (failedLoginAttempts != that.failedLoginAttempts)
            return false;

        return roles != null ? roles.equals(that.roles) : that.roles == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (lastLoginAttempt != null ? lastLoginAttempt.hashCode() : 0);
        result = 31 * result + failedLoginAttempts;
        result = 31 * result + (roles != null ? roles.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String censoredPwd;
        StringBuilder sb = new StringBuilder(256);
        if (password != null) {
            for (int i = 0; i < password.length(); i++) {
                sb.append("*");
            }
        }
        censoredPwd = sb.toString();
        sb.setLength(0);
        sb.append("AltairUser{")
            .append("id=").append(id)
            .append(", username='").append(username).append('\'')
            .append(", password='").append(censoredPwd).append('\'')
            .append(", enabled=").append(enabled)
            .append(", lastLoginAttempt=").append(lastLoginAttempt)
            .append(", failedLoginAttempts=").append(failedLoginAttempts)
            .append(", roles=").append(roles)
            .append('}');
        
        return sb.toString();
    }
}
