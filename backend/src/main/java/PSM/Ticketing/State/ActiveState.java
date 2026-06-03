package PSM.Ticketing.State;

import PSM.Ticketing.Title;

public class ActiveState implements TitleState {

	public void expire(Title title) { title.setStatus(new ExpiredState()); }

	public String getStateName() { return "ACTIVE"; }
}