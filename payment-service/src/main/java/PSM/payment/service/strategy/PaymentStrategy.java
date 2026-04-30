package PSM.payment.service.strategy;

import PSM.payment.api.PaymentAuthorizationRequestDTO;
import PSM.payment.api.PaymentAuthorizationResponseDTO;

public interface PaymentStrategy {

    boolean supports(String paymentMethodId);

    PaymentAuthorizationResponseDTO authorize(PaymentAuthorizationRequestDTO request);
}
