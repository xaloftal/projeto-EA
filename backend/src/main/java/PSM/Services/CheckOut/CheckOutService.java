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
import PSM.Checkout.api.checkout.CheckoutSessionResponseDTO;
import PSM.Location.Stop;
import PSM.Location.Zone;
import PSM.Location.api.stop.StopRepository;
import PSM.Location.api.zone.ZoneRepository;
import PSM.Services.Payment.PaymentAuthorizationRequestDTO;
import PSM.Services.Payment.PaymentAuthorizationResponseDTO;
import PSM.Services.Payment.PaymentServiceClient;
import PSM.Services.Payment.PaymentStrategy;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.card.CardRepository;
import PSM.Ticketing.api.ticket.TicketRepository;
import PSM.Ticketing.api.ticketpack.TicketPackService;
import PSM.UserManagement.User;
import PSM.UserManagement.api.user.UserRepository;

@Service
public class CheckOutService {

	private static final String SESSION_PREFIX = "checkout:session:user:";

	private final CartService cartService;
	private final PaymentServiceClient paymentServiceClient;
	private final TicketPackService ticketPackService;
	private final UserRepository userRepository;
	private final TicketRepository ticketRepository;
	private final StopRepository stopRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final Duration sessionTtl;
	private final ZoneRepository zoneRepository;
	private final CardRepository cardRepository;

	public CheckOutService(
			CartService cartService,
			PaymentServiceClient paymentServiceClient,
			UserRepository userRepository,
			TicketRepository ticketRepository,
			StopRepository stopRepository,
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			ZoneRepository zoneRepository,
			CardRepository cardRepository,
			TicketPackService ticketPackService,
			@Value("${checkout.session-ttl-minutes:15}") long sessionTtlMinutes) {
		this.cartService = cartService;
		this.paymentServiceClient = paymentServiceClient;
		this.userRepository = userRepository;
		this.ticketRepository = ticketRepository;
		this.stopRepository = stopRepository;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.zoneRepository = zoneRepository;
		this.cardRepository = cardRepository;
		this.ticketPackService = ticketPackService;
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
    stored.setDiscount(cart.getDiscount()); 
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

        // O totalAmount já reflete o valor líquido (deduzido de descontos) vindo do CartService
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

        // Lista temporária para monitorizar e capturar os bilhetes gerados nesta compra
        List<Title> ticketsCriadosParaPack = new ArrayList<>();

        for (CartItemDTO item : purchasedItems) {
            if ("card".equalsIgnoreCase(item.getKind())) {
                CartItemSourceDTO source = item.getSource();
                if (source == null || source.getCardId() == null)
                    continue;

                Zone zone = zoneRepository.findById(UUID.fromString(source.getCardId())).orElse(null);
                if (zone == null)
                    continue;

                Card card = new Card();
                card.setCreatedAt(LocalDateTime.now());
                card.setPrice(BigDecimal.valueOf(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                card.setValidFrom(LocalDateTime.now());
                card.setValidUntil(LocalDateTime.now().plusMonths(1));
                card.setUser(user);
                card.zone = zone;
                card.setStateName("ACTIVE");
                Card savedCard = cardRepository.save(card);
                user.setCard(savedCard);

            } else if ("ticket".equalsIgnoreCase(item.getKind())) {
                int quantity = Math.max(1, item.getQuantity());
                for (int i = 0; i < quantity; i++) {
                    Ticket ticket = new Ticket();
                    ticket.setCreatedAt(LocalDateTime.now());
                    ticket.setStateName("UNUSED");
                    ticket.setPrice(BigDecimal.valueOf(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));

                    CartItemSourceDTO source = item.getSource();
                    if (source != null) {
                        Stop fromStop = resolveStop(source.getFromStopId());
                        Stop toStop = resolveStop(source.getToStopId());
                        
                        if (fromStop != null && toStop != null) {
                            ticket.setFrom(fromStop);
                            ticket.setTo(toStop);
                        } else {
                            ticket.setFrom(fromStop);
                            ticket.setTo(toStop);
                        }
                    }
                    ticket.setValidFrom(LocalDateTime.now());
                    ticket.setValidUntil(LocalDateTime.now().plusWeeks(1));
                    ticket.setUser(user);
                    
                    // 1. Persiste o Ticket para gerar o ID primário (UUID)
                    Ticket savedTicket = ticketRepository.save(ticket);

                    try {
                        String qrText = orderId + ":" + savedTicket.getId();
                        savedTicket.generateQrCode(qrText, 300);
                        ticketRepository.save(savedTicket);
                    } catch (Exception e) {
                        System.err.println("QR generation failed for ticket " + savedTicket.getId() + ": " + e.getMessage());
                    }
                    
                    user.addTicket(savedTicket);
                    
                    // 2. Adiciona o bilhete guardado à lista do Pack (Ticket estende Title)
                    ticketsCriadosParaPack.add(savedTicket);
                }
            }
        }

        // =========================================================================
        // LOGICA DE MATERIALIZAÇÃO DO PACK
        // Se a sessão indica que houve desconto aplicado, cria o Pack e vincula as FKs
        // =========================================================================
        if (session.getDiscount() > 0 && !ticketsCriadosParaPack.isEmpty()) {
            ticketPackService.createAndBindPack(ticketsCriadosParaPack, session.getDiscount());
        }
        // =========================================================================

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
			String code = stopId.contains(":") ? stopId.substring(stopId.indexOf(":") + 1) : stopId;
			return stopRepository.findByStopCode(code)
					.orElseGet(() -> stopRepository.findFirstByNameIgnoreCase(code).orElse(null));
		}
	}

	static class StoredCheckoutSession {
		private String sessionId;
		private double subtotal;
		private double taxes;
		private double total;
		private double discount;
		private List<String> itemIds = new ArrayList<>();
		private List<CartItemDTO> items = new ArrayList<>();

		public String getSessionId() {
			return sessionId;
		}
		public double getDiscount() {
        return discount;
    	}

    	public void setDiscount(double discount) {
    	    this.discount = discount;
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