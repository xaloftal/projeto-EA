package PSM.Services.Payment;

import java.math.BigDecimal;

public interface PaymentStrategy {

	public boolean pay(BigDecimal _amount);
}