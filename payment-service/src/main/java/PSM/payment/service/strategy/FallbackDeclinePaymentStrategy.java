package PSM.payment.service.strategy;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import PSM.payment.api.PaymentAuthorizationRequestDTO;
import PSM.payment.api.PaymentAuthorizationResponseDTO;

@Component
public class FallbackDeclinePaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(String paymentMethodId) {
        return true;
    }

    @Override
    public PaymentAuthorizationResponseDTO authorize(PaymentAuthorizationRequestDTO request) {
        PaymentAuthorizationResponseDTO response = new PaymentAuthorizationResponseDTO();
        response.setApproved(false);
        response.setStatus("DECLINED");
        response.setMessage("Unsupported payment method");
        response.setTransactionId("txn_" + UUID.randomUUID());
        response.setProcessedAt(Instant.now());
        return response;
    }
}
