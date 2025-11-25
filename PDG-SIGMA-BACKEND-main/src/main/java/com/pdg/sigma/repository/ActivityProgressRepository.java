package com.pdg.sigma.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdg.sigma.domain.ActivityProgress;

@Repository
public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, Long> {

    List<ActivityProgress> findByActivityIdOrderByCreatedAtDesc(Integer activityId);

}
