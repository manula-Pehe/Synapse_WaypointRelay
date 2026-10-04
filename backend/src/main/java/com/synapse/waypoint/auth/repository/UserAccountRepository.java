package com.synapse.waypoint.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.common.security.Role;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    Optional<UserAccount> findByEmailIgnoreCaseAndActiveTrue(String email);

    Optional<UserAccount> findByStaffIdIgnoreCaseAndActiveTrue(String staffId);

    List<UserAccount> findByRoleAndActiveTrue(Role role);

    List<UserAccount> findByRoleAndDepotIgnoreCaseAndActiveTrue(Role role, String depot);
}
