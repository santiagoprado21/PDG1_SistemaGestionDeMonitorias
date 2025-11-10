package com.pdg.sigma;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.pdg.sigma.notification.Notification;
import com.pdg.sigma.notification.NotificationType;
import com.pdg.sigma.repository.NotificationRepository;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void findUnreadByProfessorReturnsOnlyUnreadSortedDesc() {
        // oldest
        Notification n1 = new Notification("P1", NotificationType.PROGRESS_UPDATE, "m1", 10);
        n1.setCreatedAt(new Date(System.currentTimeMillis() - 10_000));
        // middle
        Notification n2 = new Notification("P1", NotificationType.COMPLETED, "m2", 11);
        n2.setCreatedAt(new Date(System.currentTimeMillis() - 5_000));
        // newest (but marked as read, should be excluded)
        Notification n3 = new Notification("P1", NotificationType.OVERDUE, "m3", 12);
        n3.setCreatedAt(new Date(System.currentTimeMillis()));
        n3.setReadFlag(true);
        // another professor unread (should be excluded)
        Notification n4 = new Notification("P2", NotificationType.COMPLETED, "m4", 13);
        n4.setCreatedAt(new Date(System.currentTimeMillis() - 3_000));

        notificationRepository.save(n1);
        notificationRepository.save(n2);
        notificationRepository.save(n3);
        notificationRepository.save(n4);

        var list = notificationRepository.findByProfessorIdAndReadFlagFalseOrderByCreatedAtDesc("P1");
        assertThat(list).hasSize(2);
        // ensure desc order by createdAt: n2 then n1
        assertThat(list.get(0).getMessage()).isEqualTo("m2");
        assertThat(list.get(1).getMessage()).isEqualTo("m1");
    }

    @Test
    void existsByProfessorAndActivityIdOnlyCountsUnread() {
        Notification n1 = new Notification("P3", NotificationType.OVERDUE, "m", 100);
        notificationRepository.save(n1);
        assertThat(notificationRepository.existsByProfessorIdAndActivityIdAndReadFlagFalse("P3", 100)).isTrue();

        // mark as read and verify it no longer counts
        n1.setReadFlag(true);
        notificationRepository.save(n1);
        assertThat(notificationRepository.existsByProfessorIdAndActivityIdAndReadFlagFalse("P3", 100)).isFalse();
    }
}
