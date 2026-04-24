package PSM.Location;

import PSM.UserManagement.Observer;

public interface Subject {

	public void notifyObservers();

	public void addObserver(Observer _obs);

	public void removeObserver(Observer _obs);
}