package PSM.ValidationManager;

import PSM.Ticketing.Title;
import PSM.Travel.Trip;

public interface ValidationStrategy {

	public boolean validate(Title _title, Trip _trip);

	public void postValidationRecord(Title _title, Trip _trip);
}