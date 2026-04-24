package PSM.ExitManager;

import PSM.Travel.Vehicle;
import PSM.Ticketing.Title;
import PSM.Travel.Trip;

public interface ExitStrategy {

	public boolean exit(Vehicle _vehicle, Title _title);

	public void postExitRecord(Title _title, Trip _trip);
}