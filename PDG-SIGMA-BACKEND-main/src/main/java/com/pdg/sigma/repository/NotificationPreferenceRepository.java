package com.pdg.sigma.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pdg.sigma.notification.NotificationPreference;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    Optional<NotificationPreference> findByProfessorId(String professorId);
}
