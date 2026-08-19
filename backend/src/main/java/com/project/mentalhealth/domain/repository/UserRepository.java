package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByEmailVerifiedTrue();

    /**
     * Admins who can still sign in. Used to refuse the last one being demoted or
     * disabled, which would lock everybody out of the admin area.
     */
    long countByRoleNameAndEnabledTrue(String roleName);
}
