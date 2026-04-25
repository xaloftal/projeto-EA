package PSM.ValidationManager;

import PSM.Ticketing.Title;
import PSM.Travel.Trip;

public class QRCodeValidation implements ValidationStrategy {

	public boolean validate(Title _title, Trip _trip) {
		throw new UnsupportedOperationException();
	}

	public void postValidationRecord(Title _title, Trip _trip) {
		throw new UnsupportedOperationException();
	}
}