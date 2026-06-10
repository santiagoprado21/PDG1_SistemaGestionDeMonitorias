package com.pdg.sigma;

import com.pdg.sigma.domain.Category;
import com.pdg.sigma.repository.CategoryRepository;
import com.pdg.sigma.repository.CourseRepository;
import com.pdg.sigma.service.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category mockCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockCategory = new Category();
        mockCategory.setId(1);
        mockCategory.setName("Tutoría");
    }

    @Test
    @DisplayName("Debe listar todas las categorías")
    void testFindAll() {
        when(categoryRepository.findAll()).thenReturn(List.of(mockCategory));

        List<Category> result = categoryService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Tutoría", result.get(0).getName());
    }

    @Test
    @DisplayName("Debe guardar categoría correctamente")
    void testSave() throws Exception {
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);

        Category result = categoryService.save(mockCategory);

        assertNotNull(result);
        assertEquals("Tutoría", result.getName());
        verify(categoryRepository, times(1)).save(mockCategory);
    }

    @Test
    @DisplayName("Debe fallar al guardar categoría sin nombre")
    void testSaveWithoutName() {
        Category invalid = new Category();
        invalid.setName("");

        Exception exception = assertThrows(Exception.class, () -> {
            categoryService.save(invalid);
        });

        assertTrue(exception.getMessage().contains("es obligatorio"));
    }

    @Test
    @DisplayName("Debe actualizar categoría correctamente")
    void testUpdate() throws Exception {
        when(categoryRepository.existsById(1)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);

        Category result = categoryService.update(mockCategory);

        assertNotNull(result);
        verify(categoryRepository, times(1)).save(mockCategory);
    }

    @Test
    @DisplayName("Debe fallar al actualizar categoría sin ID")
    void testUpdateWithoutId() {
        Category invalid = new Category();
        invalid.setName("Test");

        Exception exception = assertThrows(Exception.class, () -> {
            categoryService.update(invalid);
        });

        assertTrue(exception.getMessage().contains("ID"));
    }

    @Test
    @DisplayName("Debe fallar al actualizar categoría inexistente")
    void testUpdateNonExistent() {
        when(categoryRepository.existsById(999)).thenReturn(false);

        Category cat = new Category();
        cat.setId(999);
        cat.setName("Test");

        Exception exception = assertThrows(Exception.class, () -> {
            categoryService.update(cat);
        });

        assertTrue(exception.getMessage().contains("No se encontró"));
    }

    @Test
    @DisplayName("Debe eliminar categoría correctamente")
    void testDelete() throws Exception {
        doNothing().when(categoryRepository).delete(any(Category.class));

        categoryService.delete(mockCategory);

        verify(categoryRepository, times(1)).delete(mockCategory);
    }

    @Test
    @DisplayName("Debe fallar al eliminar categoría nula")
    void testDeleteNull() {
        Exception exception = assertThrows(Exception.class, () -> {
            categoryService.delete(null);
        });

        assertTrue(exception.getMessage().contains("nula"));
    }

    @Test
    @DisplayName("Debe eliminar por ID correctamente")
    void testDeleteById() throws Exception {
        when(categoryRepository.existsById(1)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1);

        categoryService.deleteById(1);

        verify(categoryRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Debe fallar al eliminar por ID inexistente")
    void testDeleteByIdNonExistent() {
        when(categoryRepository.existsById(999)).thenReturn(false);

        Exception exception = assertThrows(Exception.class, () -> {
            categoryService.deleteById(999);
        });

        assertTrue(exception.getMessage().contains("No se encontró"));
    }

    @Test
    @DisplayName("Debe buscar categoría por ID")
    void testFindById() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));

        Optional<Category> result = categoryService.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Tutoría", result.get().getName());
    }

    @Test
    @DisplayName("Debe buscar categorías por curso")
    void testFindByCourseId() {
        when(categoryRepository.findByCourseId(1)).thenReturn(List.of(mockCategory));

        List<Category> result = categoryService.findByCourseId(1);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Debe contar categorías")
    void testCount() {
        when(categoryRepository.count()).thenReturn(5L);

        Long result = categoryService.count();

        assertEquals(5L, result);
    }
}
