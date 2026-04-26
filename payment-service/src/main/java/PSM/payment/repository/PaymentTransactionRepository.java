package PSM.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import PSM.payment.domain.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(String orderId);
}
