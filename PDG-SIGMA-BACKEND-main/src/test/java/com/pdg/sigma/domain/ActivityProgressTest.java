package com.pdg.sigma.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ActivityProgressTest {

    @Test
    void prePersist_setsCreatedAt_whenNull() {
        ActivityProgress ap = new ActivityProgress();
        assertNull(ap.getCreatedAt());
        ap.prePersist();
        assertNotNull(ap.getCreatedAt());
    }

    @Test
    void prePersist_doesNotOverrideCreatedAt() {
        ActivityProgress ap = new ActivityProgress();
        java.util.Date d = new java.util.Date();
        ap.setCreatedAt(d);
        ap.prePersist();
        assertEquals(d, ap.getCreatedAt());
    }
}
