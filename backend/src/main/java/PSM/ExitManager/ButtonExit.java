package PSM.ExitManager;

import PSM.Travel.Vehicle;
import PSM.Ticketing.Title;
import PSM.Travel.Trip;

public class ButtonExit implements ExitStrategy {

	public boolean exit(Vehicle _vehicle, Title _title) {
		throw new UnsupportedOperationException();
	}

	public void postExitRecord(Title _title, Trip _trip) {
		throw new UnsupportedOperationException();
	}
}