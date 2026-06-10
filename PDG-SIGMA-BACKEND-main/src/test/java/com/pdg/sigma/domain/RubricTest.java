package com.pdg.sigma.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class RubricTest {

    @Test
    void constructor_setsFields() {
        Rubric r = new Rubric("name", "desc", 100, "[{\"criterion\":\"test\",\"points\":10}]", null);
        assertEquals("name", r.getName());
        assertEquals("desc", r.getDescription());
        assertEquals(100, r.getTotalPoints());
    }

    @Test
    void getCriteriaList_parsesJson() {
        Rubric r = new Rubric("n", "d", 10, "[{\"criterion\":\"c1\",\"points\":5}]", null);
        List<Rubric.RubricCriterion> list = r.getCriteriaList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("c1", list.get(0).getCriterion());
        assertEquals(5, list.get(0).getPoints());
    }

    @Test
    void getCriteriaList_returnsNull_whenCriteriaNull() {
        Rubric r = new Rubric("n", "d", 10, null, null);
        assertNull(r.getCriteriaList());
    }

    @Test
    void setCriteriaList_serializesToJson() {
        Rubric r = new Rubric();
        r.setCriteriaList(List.of(new Rubric.RubricCriterion("crit", 5, "desc")));
        assertNotNull(r.getCriteria());
        assertTrue(r.getCriteria().contains("crit"));
    }

    @Test
    void getCriteriaList_handlesInvalidJson() {
        Rubric r = new Rubric("n", "d", 10, "{invalid}", null);
        List<Rubric.RubricCriterion> list = r.getCriteriaList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void setCriteriaList_overridesExistingCriteria() {
        Rubric r = new Rubric("n", "d", 10, "[]", null);
        r.setCriteriaList(List.of(new Rubric.RubricCriterion("x", 1, "y")));
        assertTrue(r.getCriteria().contains("\"criterion\":\"x\""));
    }
}
