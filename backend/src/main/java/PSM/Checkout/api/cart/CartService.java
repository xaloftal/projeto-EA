package PSM.Checkout.api.cart;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CartService {

    private static final String CART_PREFIX = "cart:user:";
    private static final double TAX_RATE = 0.10;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cartTtl;

    public CartService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${cart.ttl-minutes:1440}") long cartTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cartTtl = Duration.ofMinutes(Math.max(1, cartTtlMinutes));
    }

    public CartResponseDTO getCart(UUID userId) {
        List<CartItemDTO> items = loadItems(userId);
        return toResponse(items);
    }

    public CartResponseDTO upsertItem(UUID userId, CartItemDTO request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart item is required");
        }

        CartItemDTO normalized = normalize(request);
        List<CartItemDTO> items = loadItems(userId);

        int existingIndex = indexOfItem(items, normalized.getId());
        if (existingIndex >= 0) {
            items.set(existingIndex, normalized);
        } else {
            items.add(normalized);
        }

        saveItems(userId, items);
        return toResponse(items);
    }

    public CartResponseDTO removeItem(UUID userId, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item id is required");
        }

        List<CartItemDTO> items = loadItems(userId);
        items.removeIf(item -> itemId.equals(item.getId()));
        saveItems(userId, items);
        return toResponse(items);
    }

    public CartResponseDTO clearCart(UUID userId) {
        redisTemplate.delete(getCartKey(userId));
        return toResponse(List.of());
    }

    private CartItemDTO normalize(CartItemDTO request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item id is required");
        }

        if (request.getKind() == null || request.getKind().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item kind is required");
        }

        String kind = request.getKind().trim().toLowerCase(Locale.ROOT);
        if (!"card".equals(kind) && !"ticket".equals(kind)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item kind must be card or ticket");
        }

        int quantity = Math.max(1, request.getQuantity());
        double unitPrice = Math.max(0, request.getUnitPrice());

        CartItemDTO normalized = new CartItemDTO();
        normalized.setId(request.getId().trim());
        normalized.setKind(kind);
        normalized.setTitle(request.getTitle());
        normalized.setDescription(request.getDescription());
        normalized.setQuantity(quantity);
        normalized.setUnitPrice(unitPrice);
        normalized.setTotalPrice(unitPrice * quantity);
        normalized.setSource(request.getSource());

        return normalized;
    }

    private CartResponseDTO toResponse(List<CartItemDTO> items) {
        double subtotal = items.stream().mapToDouble(CartItemDTO::getTotalPrice).sum();
        double taxes = subtotal * TAX_RATE;

        CartResponseDTO response = new CartResponseDTO();
        response.setItems(items);
        response.setSubtotal(subtotal);
        response.setTaxes(taxes);
        response.setTotal(subtotal + taxes);

        return response;
    }

    private List<CartItemDTO> loadItems(UUID userId) {
        String json = redisTemplate.opsForValue().get(getCartKey(userId));
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<CartItemDTO>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read cart from Redis", e);
        }
    }

    private void saveItems(UUID userId, List<CartItemDTO> items) {
        if (items.isEmpty()) {
            redisTemplate.delete(getCartKey(userId));
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(getCartKey(userId), json, cartTtl);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save cart to Redis", e);
        }
    }

    private int indexOfItem(List<CartItemDTO> items, String itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (itemId.equals(items.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private String getCartKey(UUID userId) {
        return CART_PREFIX + userId;
    }
}
