package PSM.payment.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import PSM.payment.api.CapturePaymentRequestDTO;
import PSM.payment.api.PaymentAuthorizationRequestDTO;
import PSM.payment.api.PaymentAuthorizationResponseDTO;
import PSM.payment.api.PaymentTransactionResponseDTO;
import PSM.payment.api.RefundPaymentRequestDTO;
import PSM.payment.domain.PaymentEventType;
import PSM.payment.domain.PaymentStatus;
import PSM.payment.domain.PaymentTransaction;
import PSM.payment.domain.PaymentTransactionEvent;
import PSM.payment.repository.PaymentTransactionRepository;
import PSM.payment.service.strategy.FallbackDeclinePaymentStrategy;
import PSM.payment.service.strategy.PaymentStrategy;

@Service
public class PaymentProcessorService {

    private final List<PaymentStrategy> strategies;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentProcessorService(
            List<PaymentStrategy> strategies,
            PaymentTransactionRepository paymentTransactionRepository) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparing(strategy -> strategy instanceof FallbackDeclinePaymentStrategy))
                .toList();
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentAuthorizationResponseDTO authorize(PaymentAuthorizationRequestDTO request) {
		PaymentTransaction existing = paymentTransactionRepository.findByOrderId(request.getOrderId()).orElse(null);
		if (existing != null) {
			if (existing.getStatus() == PaymentStatus.AUTHORIZED || existing.getStatus() == PaymentStatus.CAPTURED) {
				return toAuthorizationResponse(existing, true, "Payment already authorized");
			}
			if (existing.getStatus() == PaymentStatus.DECLINED) {
				return toAuthorizationResponse(existing, false,
						existing.getFailureReason() != null ? existing.getFailureReason() : "Payment already declined");
			}
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Order already has payment in status " + existing.getStatus());
		}

		PaymentTransaction transaction = new PaymentTransaction();
		transaction.setOrderId(request.getOrderId());
		transaction.setUserId(request.getUserId());
		transaction.setAmount(request.getAmount());
		transaction.setCurrency(request.getCurrency().toUpperCase());
		transaction.setMethod(request.getPaymentMethodId());
		transaction.setStatus(PaymentStatus.PENDING);
		transaction.setRefundedAmount(BigDecimal.ZERO);

        PaymentStrategy selected = strategies.stream()
                .filter(strategy -> strategy.supports(request.getPaymentMethodId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No payment strategy configured"));

        PaymentAuthorizationResponseDTO result = selected.authorize(request);
        transaction.setProviderTransactionId(result.getTransactionId());

        if (result.isApproved()) {
            transaction.transitionTo(PaymentStatus.AUTHORIZED);
            transaction.addEvent(buildEvent(PaymentEventType.AUTHORIZED, request.getAmount(), "Payment authorized"));
        } else {
            transaction.transitionTo(PaymentStatus.DECLINED);
            transaction.setFailureReason(result.getMessage());
            transaction.addEvent(buildEvent(PaymentEventType.FAILED, request.getAmount(), result.getMessage()));
        }

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        return toAuthorizationResponse(saved, result.isApproved(), result.getMessage());
    }

    @Transactional
    public PaymentTransactionResponseDTO capture(String orderId, CapturePaymentRequestDTO request) {
        PaymentTransaction transaction = getByOrderId(orderId);

        if (transaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only AUTHORIZED payments can be captured. Current status: " + transaction.getStatus());
        }

        BigDecimal captureAmount = request != null && request.getAmount() != null
                ? request.getAmount()
                : transaction.getAmount();

        if (captureAmount.compareTo(transaction.getAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capture amount cannot exceed authorized amount");
        }

        transaction.transitionTo(PaymentStatus.CAPTURED);
        transaction.addEvent(buildEvent(PaymentEventType.CAPTURED, captureAmount, "Payment captured"));

        return toTransactionResponse(paymentTransactionRepository.save(transaction));
    }

    @Transactional
    public PaymentTransactionResponseDTO refund(String orderId, RefundPaymentRequestDTO request) {
        PaymentTransaction transaction = getByOrderId(orderId);

        if (transaction.getStatus() != PaymentStatus.CAPTURED && transaction.getStatus() != PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only CAPTURED payments can be refunded. Current status: " + transaction.getStatus());
        }

        BigDecimal refunded = transaction.getRefundedAmount() == null ? BigDecimal.ZERO : transaction.getRefundedAmount();
        BigDecimal remaining = transaction.getAmount().subtract(refunded);

        BigDecimal refundAmount = request != null && request.getAmount() != null ? request.getAmount() : remaining;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount must be greater than zero");
        }
        if (refundAmount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount exceeds remaining captured amount");
        }

        BigDecimal newRefundedAmount = refunded.add(refundAmount);
        transaction.setRefundedAmount(newRefundedAmount);
        transaction.addEvent(buildEvent(PaymentEventType.REFUNDED, refundAmount, "Payment refunded"));

        if (newRefundedAmount.compareTo(transaction.getAmount()) == 0 && transaction.getStatus() == PaymentStatus.CAPTURED) {
            transaction.transitionTo(PaymentStatus.REFUNDED);
        }

        return toTransactionResponse(paymentTransactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public PaymentTransactionResponseDTO getTransaction(String orderId) {
        return toTransactionResponse(getByOrderId(orderId));
    }

    private PaymentTransaction getByOrderId(String orderId) {
        return paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment transaction not found for orderId=" + orderId));
    }

    private PaymentTransactionEvent buildEvent(PaymentEventType type, BigDecimal amount, String message) {
        PaymentTransactionEvent event = new PaymentTransactionEvent();
        event.setEventType(type);
        event.setAmount(amount);
        event.setMessage(message);
        return event;
    }

    private PaymentAuthorizationResponseDTO toAuthorizationResponse(
            PaymentTransaction transaction,
            boolean approved,
            String message) {
        PaymentAuthorizationResponseDTO response = new PaymentAuthorizationResponseDTO();
        response.setApproved(approved);
        response.setTransactionId(transaction.getProviderTransactionId());
        response.setStatus(transaction.getStatus().name());
        response.setMessage(message);
        response.setProcessedAt(transaction.getUpdatedAt());
        return response;
    }

    private PaymentTransactionResponseDTO toTransactionResponse(PaymentTransaction transaction) {
        PaymentTransactionResponseDTO response = new PaymentTransactionResponseDTO();
        response.setOrderId(transaction.getOrderId());
        response.setStatus(transaction.getStatus().name());
        response.setAmount(transaction.getAmount());
        response.setRefundedAmount(transaction.getRefundedAmount());
        response.setCurrency(transaction.getCurrency());
        response.setMethod(transaction.getMethod());
        response.setProviderTransactionId(transaction.getProviderTransactionId());
        response.setFailureReason(transaction.getFailureReason());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
