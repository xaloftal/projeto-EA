package PSM.payment.domain;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case PENDING -> target == AUTHORIZED || target == DECLINED;
            case AUTHORIZED -> target == CAPTURED;
            case CAPTURED -> target == REFUNDED;
            case DECLINED, REFUNDED -> false;
        };
    }
}
