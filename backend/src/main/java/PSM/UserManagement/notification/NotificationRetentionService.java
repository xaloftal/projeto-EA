package PSM.UserManagement.notification;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PSM.UserManagement.api.user.UserNotificationRepository;

@Service
public class NotificationRetentionService {

	private final UserNotificationRepository userNotificationRepository;
	private final NotificationCacheService notificationCacheService;
	private final int retentionDays;

	public NotificationRetentionService(
			UserNotificationRepository userNotificationRepository,
			NotificationCacheService notificationCacheService,
			@Value("${notifications.retention.days:60}") int retentionDays) {
		this.userNotificationRepository = userNotificationRepository;
		this.notificationCacheService = notificationCacheService;
		this.retentionDays = retentionDays;
	}

	@Scheduled(cron = "${notifications.retention.cron:0 0 3 * * *}")
	@Transactional
	public void cleanupExpiredNotifications() {
		if (retentionDays <= 0) {
			return;
		}

		LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
		long deleted = userNotificationRepository.deleteByCreatedAtBefore(cutoff);
		if (deleted > 0) {
			notificationCacheService.evictAll();
		}
	}
}
