package com.pdg.sigma.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChatStorageServiceTest {

    @TempDir
    Path tempDir;

    private ChatStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ChatStorageService(tempDir.toString());
    }

    @Test
    void store_nullFile_returnsNull() throws IOException {
        assertNull(service.store(1L, null));
    }

    @Test
    void store_emptyFile_returnsNull() throws IOException {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertNull(service.store(1L, empty));
    }

    @Test
    void store_invalidPath_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "../bad.txt", "text/plain", "data".getBytes());
        assertThrows(IOException.class, () -> service.store(1L, file));
    }

    @Test
    void store_validFile_returnsStoredAttachment() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "PNG data".getBytes());
        ChatStorageService.StoredChatAttachment result = service.store(42L, file);

        assertNotNull(result);
        assertTrue(result.relativePath().contains("message-42/"));
        assertTrue(result.relativePath().endsWith("image.png"));
        assertEquals("image.png", result.originalFilename());
        assertTrue(result.absolutePath().toFile().exists());
    }

    @Test
    void store_fileContentWritten() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "world".getBytes());
        ChatStorageService.StoredChatAttachment result = service.store(1L, file);

        try (var lines = java.nio.file.Files.lines(result.absolutePath())) {
            assertEquals("world", lines.findFirst().orElse(""));
        }
    }

    @Test
    void loadAsResource_nullPath_throws() {
        assertThrows(IOException.class, () -> service.loadAsResource(null));
    }

    @Test
    void loadAsResource_blankPath_throws() {
        assertThrows(IOException.class, () -> service.loadAsResource("  "));
    }

    @Test
    void loadAsResource_notFound_throws() {
        assertThrows(IOException.class, () -> service.loadAsResource("nonexistent/file.txt"));
    }

    @Test
    void loadAsResource_validPath_returnsResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        ChatStorageService.StoredChatAttachment stored = service.store(1L, file);

        Resource resource = service.loadAsResource(stored.relativePath());
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }
}
