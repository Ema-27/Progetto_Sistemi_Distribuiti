package com.example.progetto_sistemidistribuiti.repository;

import com.example.progetto_sistemidistribuiti.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByEmail(String email);
}
