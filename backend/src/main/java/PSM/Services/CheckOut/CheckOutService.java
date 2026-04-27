package PSM.Services.CheckOut;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import PSM.Checkout.api.cart.CartItemDTO;
import PSM.Checkout.api.cart.CartItemSourceDTO;
import PSM.Checkout.api.cart.CartResponseDTO;
import PSM.Checkout.api.cart.CartService;
import PSM.Checkout.api.checkout.CheckoutConfirmRequestDTO;
import PSM.Checkout.api.checkout.CheckoutConfirmationDTO;
import PSM.Checkout.api.checkout.CheckoutOrderValidationDTO;
import PSM.Checkout.api.checkout.CheckoutSessionResponseDTO;
import PSM.Location.Stop;
import PSM.Location.api.stop.StopRepository;
import PSM.Services.Payment.PaymentAuthorizationRequestDTO;
import PSM.Services.Payment.PaymentAuthorizationResponseDTO;
import PSM.Services.Payment.PaymentServiceClient;
import PSM.Services.Payment.PaymentStrategy;
import PSM.Services.Payment.PaymentTransactionStatusDTO;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.ticket.TicketRepository;
import PSM.UserManagement.User;
import PSM.UserManagement.api.user.UserRepository;

@Service
public class CheckOutService {

	private static final String SESSION_PREFIX = "checkout:session:user:";

	private final CartService cartService;
	private final PaymentServiceClient paymentServiceClient;
	private final UserRepository userRepository;
	private final TicketRepository ticketRepository;
	private final StopRepository stopRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration sessionTtl;

	public CheckOutService(
			CartService cartService,
			PaymentServiceClient paymentServiceClient,
			UserRepository userRepository,
			TicketRepository ticketRepository,
			StopRepository stopRepository,
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			@Value("${checkout.session-ttl-minutes:15}") long sessionTtlMinutes) {
		this.cartService = cartService;
		this.paymentServiceClient = paymentServiceClient;
		this.userRepository = userRepository;
		this.ticketRepository = ticketRepository;
		this.stopRepository = stopRepository;
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
		stored.setItems(new ArrayList<>(cart.getItems()));
		stored.setItemIds(cart.getItems().stream().map(CartItemDTO::getId).toList());

		saveSession(userId, sessionId, stored);

		CheckoutSessionResponseDTO response = new CheckoutSessionResponseDTO();
		response.setSessionId(sessionId);
		response.setSubtotal(cart.getSubtotal());
		response.setTaxes(cart.getTaxes());
		response.setTotal(cart.getTotal());
		return response;
	}

	@Transactional
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

		User user = userRepository.findWithBalanceLockById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		BigDecimal totalAmount = BigDecimal.valueOf(session.getTotal()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal currentBalance = BigDecimal.valueOf(user.getBalance()).setScale(2, RoundingMode.HALF_UP);
		if (currentBalance.compareTo(totalAmount) < 0) {
			throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
					"Insufficient balance. Available=" + currentBalance + ", required=" + totalAmount);
		}

		String orderId = "order_" + System.currentTimeMillis();

		PaymentAuthorizationRequestDTO paymentRequest = new PaymentAuthorizationRequestDTO();
		paymentRequest.setOrderId(orderId);
		paymentRequest.setUserId(userId.toString());
		paymentRequest.setSessionId(sessionId);
		paymentRequest.setPaymentMethodId(request.getPaymentMethodId().trim());
		paymentRequest.setAmount(totalAmount);
		paymentRequest.setCurrency("EUR");

		PaymentAuthorizationResponseDTO paymentResponse;
		try {
			paymentResponse = paymentServiceClient.authorize(paymentRequest);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment service is unavailable", e);
		}

		if (!paymentResponse.isApproved()) {
			throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
					"Payment was declined: " + paymentResponse.getMessage());
		}

		List<CartItemDTO> purchasedItems = session.getItems();
		if (purchasedItems == null || purchasedItems.isEmpty()) {
			purchasedItems = cartService.getCart(userId).getItems().stream()
					.filter(item -> session.getItemIds().contains(item.getId()))
					.toList();
		}

		for (CartItemDTO item : purchasedItems) {
			if (!"ticket".equalsIgnoreCase(item.getKind())) {
				continue;
			}

			int quantity = Math.max(1, item.getQuantity());
			for (int i = 0; i < quantity; i++) {
				Ticket ticket = new Ticket();
				ticket.setCreatedAt(LocalDateTime.now());
				ticket.setPrice(BigDecimal.valueOf(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));

				CartItemSourceDTO source = item.getSource();
				if (source != null) {
					ticket.setFrom(resolveStop(source.getFromStopId()));
					ticket.setTo(resolveStop(source.getToStopId()));
				}
				ticket.setValidFrom(LocalDateTime.now());
				ticket.setValidUntil(LocalDateTime.now().plusWeeks(1));
				ticket.setUser(user);
				ticketRepository.save(ticket);
				try {
					String qrText = orderId + ":" + ticket.getId();
					ticket.generateQrCode(qrText, 300);
					ticketRepository.save(ticket);
				} catch (Exception e) {
					System.err.println("QR generation failed for ticket " + ticket.getId() + ": " + e.getMessage());
				}
				user.addTicket(ticket);
			}
		}

		BigDecimal newBalance = currentBalance.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
		user.setBalance(newBalance.floatValue());
		userRepository.save(user);

		cartService.clearCart(userId);
		redisTemplate.delete(getSessionKey(userId, sessionId));

		CheckoutConfirmationDTO confirmation = new CheckoutConfirmationDTO();
		confirmation.setOrderId(orderId);
		confirmation.setConfirmationNumber(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		confirmation.setItems(new ArrayList<>(session.getItemIds()));
		return confirmation;
	}

	public CheckoutOrderValidationDTO validateOrder(UUID userId, String orderId) {
		if (orderId == null || orderId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
		}

		PaymentTransactionStatusDTO payment;
		try {
			payment = paymentServiceClient.getTransaction(orderId.trim());
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment service is unavailable", e);
		}

		String status = payment.getStatus() == null ? "UNKNOWN" : payment.getStatus().toUpperCase();
		boolean valid = "AUTHORIZED".equals(status) || "CAPTURED".equals(status) || "REFUNDED".equals(status);

		CheckoutOrderValidationDTO response = new CheckoutOrderValidationDTO();
		response.setOrderId(payment.getOrderId() != null ? payment.getOrderId() : orderId.trim());
		response.setPaymentStatus(status);
		response.setValid(valid);
		response.setMessage(valid ? "Order payment is valid" : "Order payment is not valid");
		return response;
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

	private Stop resolveStop(String stopId) {
		if (stopId == null || stopId.isBlank()) {
			return null;
		}

		try {
			return stopRepository.findById(UUID.fromString(stopId)).orElse(null);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	static class StoredCheckoutSession {
		private String sessionId;
		private double subtotal;
		private double taxes;
		private double total;
		private List<String> itemIds = new ArrayList<>();
		private List<CartItemDTO> items = new ArrayList<>();

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

		public List<CartItemDTO> getItems() {
			return items;
		}

		public void setItems(List<CartItemDTO> items) {
			this.items = items;
		}
	}
}