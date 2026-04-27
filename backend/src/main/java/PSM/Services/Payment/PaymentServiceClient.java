package PSM.Services.Payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class PaymentServiceClient {

    private final RestClient restClient;
    private final String authorizePath;

    public PaymentServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${payment.service.base-url:http://localhost:8081}") String paymentServiceBaseUrl,
            @Value("${payment.service.authorize-path:/api/payments/authorize}") String authorizePath) {
        this.restClient = restClientBuilder.baseUrl(paymentServiceBaseUrl).build();
        this.authorizePath = authorizePath;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "authorizeFallback")
    public PaymentAuthorizationResponseDTO authorize(PaymentAuthorizationRequestDTO request) {
        PaymentAuthorizationResponseDTO response = restClient.post()
                .uri(authorizePath)
                .body(request)
                .retrieve()
                .body(PaymentAuthorizationResponseDTO.class);

        if (response == null) {
            throw new IllegalStateException("Payment service returned an empty response");
        }

        return response;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "getTransactionFallback")
    public PaymentTransactionStatusDTO getTransaction(String orderId) {
        PaymentTransactionStatusDTO response = restClient.get()
                .uri("/api/payments/{orderId}", orderId)
                .retrieve()
                .body(PaymentTransactionStatusDTO.class);

        if (response == null) {
            throw new IllegalStateException("Payment service returned an empty response");
        }

        return response;
    }

    @SuppressWarnings("unused")
    private PaymentAuthorizationResponseDTO authorizeFallback(PaymentAuthorizationRequestDTO request, Throwable cause) {
        throw new PaymentServiceUnavailableException("Payment service is unavailable", cause);
    }

    @SuppressWarnings("unused")
    private PaymentTransactionStatusDTO getTransactionFallback(String orderId, Throwable cause) {
        throw new PaymentServiceUnavailableException("Payment service is unavailable", cause);
    }
}
