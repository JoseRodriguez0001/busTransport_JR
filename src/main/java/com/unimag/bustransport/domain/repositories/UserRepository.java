package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    List<User> findByRoleAndStatus(Role role, User.Status status);
    List<User> findActiveUsersByRole(@Param("role") Role role);
}
