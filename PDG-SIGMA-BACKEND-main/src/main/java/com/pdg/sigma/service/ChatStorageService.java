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
public class ChatStorageService {

    private final Path storageRoot;
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public ChatStorageService(@Value("${sigma.chat.attachments-path:uploads/chat}") String storagePath) throws IOException {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(this.storageRoot);
    }

    public StoredChatAttachment store(Long messageId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new IOException("Nombre de archivo inválido: " + originalName);
        }

        String filename = FILE_STAMP.format(LocalDateTime.now()) + "_" + originalName;
        Path messageDir = storageRoot.resolve("message-" + messageId);
        Files.createDirectories(messageDir);

        Path target = messageDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = storageRoot.relativize(target).toString().replace('\\', '/');
        return new StoredChatAttachment(relativePath, originalName, target);
    }

    public Resource loadAsResource(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("Ruta de archivo vacía");
        }
        Path filePath = storageRoot.resolve(relativePath).normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("No se encontró el archivo solicitado");
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("No se puede leer el archivo");
        }
        return resource;
    }

    public record StoredChatAttachment(String relativePath, String originalFilename, Path absolutePath) {}
}
