package com.pdg.sigma;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.pdg.sigma.notification.NotificationPreference;
import com.pdg.sigma.repository.NotificationPreferenceRepository;

@DataJpaTest
@ActiveProfiles("test")
class NotificationPreferenceRepositoryTest {

    @Autowired
    private NotificationPreferenceRepository repository;

    @Test
    void findByProfessorIdEmptyWhenNotExists() {
        assertThat(repository.findByProfessorId("NOPE")).isEmpty();
    }

    @Test
    void saveAndUpdatePreferences() {
        NotificationPreference pref = new NotificationPreference("PROF_X");
        // defaults are true
        repository.save(pref);
        NotificationPreference found = repository.findByProfessorId("PROF_X").orElseThrow();
        assertThat(found.isEnableProgressUpdate()).isTrue();
        assertThat(found.isEnableCompleted()).isTrue();
        assertThat(found.isEnableOverdue()).isTrue();
        assertThat(found.isEnableSound()).isTrue();

        // update flags
        found.setEnableOverdue(false);
        found.setEnableSound(false);
        repository.save(found);

        NotificationPreference updated = repository.findByProfessorId("PROF_X").orElseThrow();
        assertThat(updated.isEnableOverdue()).isFalse();
        assertThat(updated.isEnableSound()).isFalse();
    }
}
