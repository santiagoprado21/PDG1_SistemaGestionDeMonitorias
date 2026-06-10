package com.pdg.sigma;

import com.pdg.sigma.controller.CategoryController;
import com.pdg.sigma.domain.Category;
import com.pdg.sigma.domain.Course;
import com.pdg.sigma.service.CategoryServiceImpl;
import com.pdg.sigma.service.CourseServiceImpl;
import com.pdg.sigma.util.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryServiceImpl categoryService;

    @MockBean
    private CourseServiceImpl courseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllCategories_returnsList() throws Exception {
        Category c = new Category();
        c.setId(1);
        when(categoryService.findAll()).thenReturn(List.of(c));

        mockMvc.perform(get("/category/getA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getCategoryById_found_returnsOk() throws Exception {
        Category c = new Category();
        c.setId(1);
        when(categoryService.findById(1)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/category/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCategoryById_notFound_returnsEmpty() throws Exception {
        when(categoryService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/category/{id}", 99))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void getCategoriesByCourse_returnsList() throws Exception {
        Category c = new Category();
        c.setId(1);
        when(categoryService.findByCourseId(1)).thenReturn(List.of(c));

        mockMvc.perform(get("/category/course/{courseId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createCategory_returnsCreated() throws Exception {
        Category c = new Category();
        c.setId(1);
        c.setName("Category 1");
        when(categoryService.save(any(Category.class))).thenReturn(c);

        mockMvc.perform(post("/category/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Category 1\",\"course\":{\"id\":1}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createCategory_noCourse_returns400() throws Exception {
        mockMvc.perform(post("/category/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Category 1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_returnsOk() throws Exception {
        Category c = new Category();
        c.setId(1);
        when(categoryService.update(any(Category.class))).thenReturn(c);

        mockMvc.perform(put("/category/update/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteCategory_returnsOk() throws Exception {
        doNothing().when(categoryService).deleteById(1);

        mockMvc.perform(delete("/category/delete/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().string("Categoría eliminada con éxito"));
    }
}
