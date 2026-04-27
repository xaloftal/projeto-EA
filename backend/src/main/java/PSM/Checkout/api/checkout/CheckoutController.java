package PSM.Checkout.api.checkout;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import PSM.Services.CheckOut.CheckOutService;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckOutService checkoutService;

    public CheckoutController(CheckOutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/session")
    public CheckoutSessionResponseDTO createSession() {
        return checkoutService.createSession(getCurrentUserId());
    }

    @PostMapping("/confirm")
    public CheckoutConfirmationDTO confirm(@RequestBody CheckoutConfirmRequestDTO request) {
        return checkoutService.confirm(getCurrentUserId(), request);
    }

    @GetMapping("/orders/{orderId}/validation")
    public CheckoutOrderValidationDTO validateOrder(@PathVariable String orderId) {
        return checkoutService.validateOrder(getCurrentUserId(), orderId);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userIdString)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        try {
            return UUID.fromString(userIdString);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user", e);
        }
    }
}
