package PSM.Services.CheckOut;

import PSM.UserManagement.User;
import PSM.Ticketing.Title;
import PSM.Services.Payment.PaymentStrategy;

import java.util.List;

public class CheckOutService {

	public boolean checkout(User _user, List<Title> _titles, PaymentStrategy _payment) {
		throw new UnsupportedOperationException();
	}

	public void calculateTotal(List<Title> _titles) {
		throw new UnsupportedOperationException();
	}
}