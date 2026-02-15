package com.pdg.sigma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.notification.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc(String professorId);
    boolean existsByProfessorIdAndActivityIdAndReadFlagFalse(String professorId, Integer activityId);
}
