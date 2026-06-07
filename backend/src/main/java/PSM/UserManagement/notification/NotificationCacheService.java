package PSM.UserManagement.notification;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import PSM.UserManagement.UserNotification;

@Service
public class NotificationCacheService {

	private static final String CACHE_KEY_PREFIX = "user:notifications:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration ttl;

	public NotificationCacheService(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			@Value("${notifications.cache.ttl-minutes:5}") long ttlMinutes) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.ttl = Duration.ofMinutes(Math.max(1, ttlMinutes));
	}

	public Optional<Set<UserNotification>> get(UUID userId) {
		try {
			String cached = redisTemplate.opsForValue().get(getKey(userId));
			if (cached == null) {
				return Optional.empty();
			}

			Set<UserNotification> notifications = objectMapper.readValue(cached,
					new TypeReference<Set<UserNotification>>() {
					});
			return Optional.of(notifications);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public void put(UUID userId, Set<UserNotification> notifications) {
		try {
			String payload = objectMapper.writeValueAsString(notifications);
			redisTemplate.opsForValue().set(getKey(userId), payload, ttl);
		} catch (Exception e) {
			// Cache write failures should not impact main flow.
		}
	}

	public void evict(UUID userId) {
		redisTemplate.delete(getKey(userId));
	}

	public void evictAll() {
		Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
		if (keys == null || keys.isEmpty()) {
			return;
		}

		redisTemplate.delete(keys);
	}

	private String getKey(UUID userId) {
		return CACHE_KEY_PREFIX + userId;
	}
}
