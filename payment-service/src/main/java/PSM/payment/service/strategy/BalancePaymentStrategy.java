package PSM.payment.service.strategy;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import PSM.payment.api.PaymentAuthorizationRequestDTO;
import PSM.payment.api.PaymentAuthorizationResponseDTO;

@Component
public class BalancePaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(String paymentMethodId) {
        if (paymentMethodId == null) {
            return false;
        }

        String normalized = paymentMethodId.toLowerCase();
        return normalized.startsWith("balance-");
    }

    @Override
    public PaymentAuthorizationResponseDTO authorize(PaymentAuthorizationRequestDTO request) {
        PaymentAuthorizationResponseDTO response = new PaymentAuthorizationResponseDTO();
        response.setApproved(true);
        response.setStatus("AUTHORIZED");
        response.setMessage("Balance payment authorized");
        response.setTransactionId("txn_" + UUID.randomUUID());
        response.setProcessedAt(Instant.now());
        return response;
    }
}
