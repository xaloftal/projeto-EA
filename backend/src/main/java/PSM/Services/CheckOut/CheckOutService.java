package PSM.Services.CheckOut;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import PSM.Checkout.api.cart.CartItemDTO;
import PSM.Checkout.api.cart.CartResponseDTO;
import PSM.Checkout.api.cart.CartService;
import PSM.Checkout.api.checkout.CheckoutConfirmRequestDTO;
import PSM.Checkout.api.checkout.CheckoutConfirmationDTO;
import PSM.Checkout.api.checkout.CheckoutSessionResponseDTO;
import PSM.Services.Payment.PaymentStrategy;
import PSM.Ticketing.Title;
import PSM.UserManagement.User;

@Service
public class CheckOutService {

	private static final String SESSION_PREFIX = "checkout:session:user:";

	private final CartService cartService;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration sessionTtl;

	public CheckOutService(
			CartService cartService,
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			@Value("${checkout.session-ttl-minutes:15}") long sessionTtlMinutes) {
		this.cartService = cartService;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.sessionTtl = Duration.ofMinutes(Math.max(1, sessionTtlMinutes));
	}

	public CheckoutSessionResponseDTO createSession(UUID userId) {
		CartResponseDTO cart = cartService.getCart(userId);
		if (cart.getItems() == null || cart.getItems().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
		}

		String sessionId = UUID.randomUUID().toString();

		StoredCheckoutSession stored = new StoredCheckoutSession();
		stored.setSessionId(sessionId);
		stored.setSubtotal(cart.getSubtotal());
		stored.setTaxes(cart.getTaxes());
		stored.setTotal(cart.getTotal());
		stored.setItemIds(cart.getItems().stream().map(CartItemDTO::getId).toList());

		saveSession(userId, sessionId, stored);

		CheckoutSessionResponseDTO response = new CheckoutSessionResponseDTO();
		response.setSessionId(sessionId);
		response.setSubtotal(cart.getSubtotal());
		response.setTaxes(cart.getTaxes());
		response.setTotal(cart.getTotal());
		return response;
	}

	public CheckoutConfirmationDTO confirm(UUID userId, CheckoutConfirmRequestDTO request) {
		if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
		}

		if (request.getPaymentMethodId() == null || request.getPaymentMethodId().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentMethodId is required");
		}

		String sessionId = request.getSessionId().trim();
		StoredCheckoutSession session = loadSession(userId, sessionId);
		if (session == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout session not found or expired");
		}

		cartService.clearCart(userId);
		redisTemplate.delete(getSessionKey(userId, sessionId));

		CheckoutConfirmationDTO confirmation = new CheckoutConfirmationDTO();
		confirmation.setOrderId("order_" + System.currentTimeMillis());
		confirmation.setConfirmationNumber(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		confirmation.setItems(new ArrayList<>(session.getItemIds()));
		return confirmation;
	}

	public boolean checkout(User _user, List<Title> _titles, PaymentStrategy _payment) {
		throw new UnsupportedOperationException();
	}

	public void calculateTotal(List<Title> _titles) {
		throw new UnsupportedOperationException();
	}

	private void saveSession(UUID userId, String sessionId, StoredCheckoutSession session) {
		try {
			String json = objectMapper.writeValueAsString(session);
			redisTemplate.opsForValue().set(getSessionKey(userId, sessionId), json, sessionTtl);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save checkout session", e);
		}
	}

	private StoredCheckoutSession loadSession(UUID userId, String sessionId) {
		String json = redisTemplate.opsForValue().get(getSessionKey(userId, sessionId));
		if (json == null || json.isBlank()) {
			return null;
		}

		try {
			return objectMapper.readValue(json, StoredCheckoutSession.class);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to read checkout session", e);
		}
	}

	private String getSessionKey(UUID userId, String sessionId) {
		return SESSION_PREFIX + userId + ":" + sessionId;
	}

	static class StoredCheckoutSession {
		private String sessionId;
		private double subtotal;
		private double taxes;
		private double total;
		private List<String> itemIds = new ArrayList<>();

		public String getSessionId() {
			return sessionId;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		public double getSubtotal() {
			return subtotal;
		}

		public void setSubtotal(double subtotal) {
			this.subtotal = subtotal;
		}

		public double getTaxes() {
			return taxes;
		}

		public void setTaxes(double taxes) {
			this.taxes = taxes;
		}

		public double getTotal() {
			return total;
		}

		public void setTotal(double total) {
			this.total = total;
		}

		public List<String> getItemIds() {
			return itemIds;
		}

		public void setItemIds(List<String> itemIds) {
			this.itemIds = itemIds;
		}
	}
}