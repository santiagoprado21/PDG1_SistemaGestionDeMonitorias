package com.pdg.sigma.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActivityEvidenceStorageService {

    private final Path storageRoot;
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public ActivityEvidenceStorageService(@Value("${sigma.activities.evidence-path:uploads/activities}") String storagePath) throws IOException {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(this.storageRoot);
    }

    public StoredEvidence store(Integer activityId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new IOException("Nombre de archivo inválido: " + originalName);
        }

        String filename = FILE_STAMP.format(LocalDateTime.now()) + "_" + originalName;
        Path activityDir = storageRoot.resolve("activity-" + activityId);
        Files.createDirectories(activityDir);

        Path target = activityDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = storageRoot.relativize(target).toString().replace('\\', '/');
        return new StoredEvidence(relativePath, originalName, target); // keep full path for later
    }

    public Resource loadAsResource(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("Ruta de evidencia vacía");
        }
        Path filePath = storageRoot.resolve(relativePath).normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("No se encontró la evidencia solicitada");
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("No se puede leer la evidencia");
        }
        return resource;
    }

    public record StoredEvidence(String relativePath, String originalFilename, Path absolutePath) { }
}
