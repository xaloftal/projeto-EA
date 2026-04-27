package PSM.payment.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.payment.service.PaymentProcessorService;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessorService paymentProcessorService;

    public PaymentController(PaymentProcessorService paymentProcessorService) {
        this.paymentProcessorService = paymentProcessorService;
    }

    @PostMapping("/authorize")
    public PaymentAuthorizationResponseDTO authorize(@Valid @RequestBody PaymentAuthorizationRequestDTO request) {
        return paymentProcessorService.authorize(request);
    }

    @PostMapping("/{orderId}/capture")
    public PaymentTransactionResponseDTO capture(
            @PathVariable String orderId,
            @RequestBody(required = false) @Valid CapturePaymentRequestDTO request) {
        return paymentProcessorService.capture(orderId, request);
    }

    @PostMapping("/{orderId}/refund")
    public PaymentTransactionResponseDTO refund(
            @PathVariable String orderId,
            @RequestBody(required = false) @Valid RefundPaymentRequestDTO request) {
        return paymentProcessorService.refund(orderId, request);
    }

    @GetMapping("/{orderId}")
    public PaymentTransactionResponseDTO getByOrderId(@PathVariable String orderId) {
        return paymentProcessorService.getTransaction(orderId);
    }
}
