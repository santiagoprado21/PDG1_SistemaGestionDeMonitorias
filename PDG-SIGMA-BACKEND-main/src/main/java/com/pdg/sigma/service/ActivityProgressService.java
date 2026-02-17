package com.pdg.sigma.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.pdg.sigma.domain.ActivityProgress;
import com.pdg.sigma.dto.ActivityProgressRequestDTO;

public interface ActivityProgressService {

    ActivityProgress registerProgress(Integer activityId, ActivityProgressRequestDTO payload, MultipartFile evidence) throws Exception;

    List<ActivityProgress> findByActivity(Integer activityId);

    ActivityProgress findById(Long progressId) throws Exception;
}
