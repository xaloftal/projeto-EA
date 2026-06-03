package PSM.Ticketing.State;

import PSM.Ticketing.Title;

public class ExpiredState implements TitleState {

	public void activate(Title title) { title.setStatus(new ActiveState()); }

	public String getStateName() { return "EXPIRED"; }
}