package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    List<User> findByRoleAndStatus(Role role, User.Status status);
}
