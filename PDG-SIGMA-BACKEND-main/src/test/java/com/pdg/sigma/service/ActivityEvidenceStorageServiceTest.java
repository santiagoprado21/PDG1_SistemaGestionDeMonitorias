package com.pdg.sigma.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ActivityEvidenceStorageServiceTest {

    @TempDir
    Path tempDir;

    private ActivityEvidenceStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ActivityEvidenceStorageService(tempDir.toString());
    }

    @Test
    void store_nullFile_returnsNull() throws IOException {
        assertNull(service.store(1, null));
    }

    @Test
    void store_emptyFile_returnsNull() throws IOException {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertNull(service.store(1, empty));
    }

    @Test
    void store_invalidPath_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "../malicious.txt", "text/plain", "data".getBytes());
        assertThrows(IOException.class, () -> service.store(1, file));
    }

    @Test
    void store_validFile_returnsStoredEvidence() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "PDF data".getBytes());
        ActivityEvidenceStorageService.StoredEvidence result = service.store(42, file);

        assertNotNull(result);
        assertTrue(result.relativePath().contains("activity-42/"));
        assertTrue(result.relativePath().endsWith("report.pdf"));
        assertEquals("report.pdf", result.originalFilename());
        assertTrue(result.absolutePath().toFile().exists());
    }

    @Test
    void store_fileExistsInDirectory() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "content".getBytes());
        ActivityEvidenceStorageService.StoredEvidence result = service.store(1, file);

        assertTrue(result.absolutePath().toFile().exists());
        try (var lines = java.nio.file.Files.lines(result.absolutePath())) {
            assertEquals("content", lines.findFirst().orElse(""));
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
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        ActivityEvidenceStorageService.StoredEvidence stored = service.store(1, file);

        Resource resource = service.loadAsResource(stored.relativePath());
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }
}
